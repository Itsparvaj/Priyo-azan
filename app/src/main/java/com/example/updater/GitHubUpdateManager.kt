package com.example.updater

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.example.BuildConfig
import com.example.model.UpdateManifest
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

sealed class UpdateCheckResult {
    data object NoInternet : UpdateCheckResult()
    data class UpToDate(val currentVersion: String, val latestVersion: String) : UpdateCheckResult()
    data class UpdateAvailable(val manifest: UpdateManifest) : UpdateCheckResult()
    data class Error(val message: String) : UpdateCheckResult()
}

sealed class DownloadProgress {
    data class InProgress(val bytesRead: Long, val totalBytes: Long) : DownloadProgress()
    data class VerifiedReady(val apkFile: File, val sha256: String) : DownloadProgress()
    data class Failed(val reason: String) : DownloadProgress()
}

/**
 * Secure HTTPS GitHub Releases APK update checker and SHA-256 verifier.
 * Never silently installs APKs; always requires explicit user approval and
 * delegates to Android's system package installer via FileProvider.
 */
class GitHubUpdateManager(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    fun isNetworkAvailable(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    suspend fun checkForUpdates(owner: String, repo: String): UpdateCheckResult = withContext(Dispatchers.IO) {
        if (!isNetworkAvailable()) {
            return@withContext UpdateCheckResult.NoInternet
        }
        val cleanOwner = owner.trim()
        val cleanRepo = repo.trim()
        if (cleanOwner.isEmpty() || cleanRepo.isEmpty()) {
            return@withContext UpdateCheckResult.Error("GitHub repository owner/name is not configured")
        }

        // First try direct manifest asset on latest GitHub release, then GitHub API releases/latest
        val manifestUrl = "https://github.com/$cleanOwner/$cleanRepo/releases/latest/download/update-manifest.json"
        val request = Request.Builder()
            .url(manifestUrl)
            .header("Accept", "application/json")
            .build()

        runCatching {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@runCatching UpdateCheckResult.Error("HTTP ${response.code} from GitHub Releases")
                }
                val body = response.body?.string()
                    ?: return@runCatching UpdateCheckResult.Error("Empty manifest response")
                val manifest = parseManifest(body)
                if (manifest.platform.lowercase() != "android") {
                    return@runCatching UpdateCheckResult.Error("Manifest platform mismatch")
                }
                if (isNewerVersion(manifest.versionCode, manifest.version, BuildConfig.VERSION_CODE, BuildConfig.VERSION_NAME)) {
                    UpdateCheckResult.UpdateAvailable(manifest)
                } else {
                    UpdateCheckResult.UpToDate(BuildConfig.VERSION_NAME, manifest.version)
                }
            }
        }.getOrElse { err ->
            UpdateCheckResult.Error(err.message ?: "Unable to reach GitHub Releases")
        }
    }

    fun downloadAndVerifyApk(manifest: UpdateManifest): Flow<DownloadProgress> = flow {
        if (!manifest.apkUrl.startsWith("https://")) {
            emit(DownloadProgress.Failed("APK URL must use HTTPS"))
            return@flow
        }
        val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
        val targetFile = File(updatesDir, "priyo-azan-${manifest.version}.apk")

        // Avoid re-downloading if already present and verified
        if (targetFile.exists() && manifest.sha256.isNotBlank()) {
            val existingHash = computeSha256(targetFile)
            if (existingHash.equals(manifest.sha256.trim(), ignoreCase = true) &&
                verifyPackageIdentity(targetFile)
            ) {
                emit(DownloadProgress.VerifiedReady(targetFile, existingHash))
                return@flow
            }
        }

        val req = Request.Builder().url(manifest.apkUrl).build()
        val callResult = runCatching {
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    emit(DownloadProgress.Failed("Download failed (HTTP ${resp.code})"))
                    return@flow
                }
                val body = resp.body ?: run {
                    emit(DownloadProgress.Failed("Empty APK stream"))
                    return@flow
                }
                val total = body.contentLength()
                var readTotal = 0L
                val buf = ByteArray(8192)
                body.byteStream().use { input ->
                    targetFile.outputStream().use { out ->
                        while (true) {
                            val n = input.read(buf)
                            if (n < 0) break
                            out.write(buf, 0, n)
                            readTotal += n
                            emit(DownloadProgress.InProgress(readTotal, total))
                        }
                    }
                }
            }
        }
        if (callResult.isFailure) {
            targetFile.delete()
            emit(DownloadProgress.Failed(callResult.exceptionOrNull()?.message ?: "Download error"))
            return@flow
        }

        val actualSha256 = computeSha256(targetFile)
        val expected = manifest.sha256.trim()
        if (expected.isNotBlank() && !actualSha256.equals(expected, ignoreCase = true)) {
            targetFile.delete()
            emit(DownloadProgress.Failed("SHA-256 verification failed"))
            return@flow
        }
        if (!verifyPackageIdentity(targetFile)) {
            targetFile.delete()
            emit(DownloadProgress.Failed("APK package identity mismatch"))
            return@flow
        }
        emit(DownloadProgress.VerifiedReady(targetFile, actualSha256))
    }.flowOn(Dispatchers.IO)

    fun canRequestPackageInstalls(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    fun openUnknownAppSourcesSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = Uri.parse("package:${context.packageName}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            runCatching { context.startActivity(intent) }
        }
    }

    fun launchPackageInstaller(apkFile: File): Boolean {
        return runCatching {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }

    private fun verifyPackageIdentity(apkFile: File): Boolean {
        val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageArchiveInfo(
                apkFile.absolutePath,
                PackageManager.PackageInfoFlags.of(0)
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageArchiveInfo(apkFile.absolutePath, 0)
        }
        return info?.packageName == context.packageName
    }

    companion object {
        private val moshi: Moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
        private val manifestAdapter = moshi.adapter(UpdateManifest::class.java)

        fun parseManifest(jsonString: String): UpdateManifest {
            return manifestAdapter.fromJson(jsonString)
                ?: throw IllegalArgumentException("Invalid update manifest JSON")
        }

        fun isNewerVersion(
            remoteVersionCode: Int,
            remoteVersionName: String,
            localVersionCode: Int,
            localVersionName: String
        ): Boolean {
            if (remoteVersionCode > localVersionCode) return true
            if (remoteVersionCode < localVersionCode) return false
            return compareSemver(remoteVersionName, localVersionName) > 0
        }

        fun compareSemver(a: String, b: String): Int {
            val pa = a.removePrefix("v").split(".").map { it.toIntOrNull() ?: 0 }
            val pb = b.removePrefix("v").split(".").map { it.toIntOrNull() ?: 0 }
            val max = maxOf(pa.size, pb.size)
            for (i in 0 until max) {
                val va = pa.getOrElse(i) { 0 }
                val vb = pb.getOrElse(i) { 0 }
                if (va != vb) return va.compareTo(vb)
            }
            return 0
        }

        fun computeSha256(file: File): String {
            val digest = MessageDigest.getInstance("SHA-256")
            FileInputStream(file).use { fis ->
                val buffer = ByteArray(8192)
                while (true) {
                    val read = fis.read(buffer)
                    if (read <= 0) break
                    digest.update(buffer, 0, read)
                }
            }
            return digest.digest().joinToString("") { "%02x".format(it) }
        }
    }
}
