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
import com.squareup.moshi.Json
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

data class GitHubReleaseAssetDto(
    val name: String = "",
    @Json(name = "browser_download_url") val browserDownloadUrl: String = "",
    val digest: String? = null
)

data class GitHubReleaseDto(
    @Json(name = "tag_name") val tagName: String = "",
    val name: String? = null,
    val body: String? = null,
    val draft: Boolean = false,
    val prerelease: Boolean = false,
    val assets: List<GitHubReleaseAssetDto> = emptyList()
)

/**
 * Secure HTTPS GitHub Releases APK update checker and SHA-256 verifier.
 *
 * Resolution strategy:
 * 1. Direct `update-manifest.json` asset on the latest GitHub Release (gives exact versionCode & SHA-256).
 * 2. Automatic fallback to GitHub REST API `/repos/{owner}/{repo}/releases/latest` for standard
 *    GitHub Releases containing any `.apk` asset (extracting optional SHA-256 asset or digest).
 *
 * Never silently installs APKs; always requires explicit user approval and
 * delegates to Android's system package installer via FileProvider.
 */
class GitHubUpdateManager(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
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
        val cleanRepo = repo.trim().removeSuffix(".git")
        if (cleanOwner.isEmpty() || cleanRepo.isEmpty()) {
            return@withContext UpdateCheckResult.Error("GitHub repository is not configured")
        }

        // Step 1: Try update-manifest.json attached to latest GitHub Release
        val manifestUrl = "https://github.com/$cleanOwner/$cleanRepo/releases/latest/download/update-manifest.json"
        val manifestReq = Request.Builder()
            .url(manifestUrl)
            .header("Accept", "application/json")
            .header("User-Agent", USER_AGENT)
            .build()

        val manifestAttempt = runCatching {
            client.newCall(manifestReq).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string().orEmpty()
                    if (body.isNotBlank()) {
                        val manifest = parseManifest(body)
                        if (manifest.platform.lowercase() != "android") {
                            return@use UpdateCheckResult.Error("Manifest platform mismatch")
                        }
                        return@use evaluateManifestVersion(manifest)
                    }
                }
                null
            }
        }.getOrNull()

        if (manifestAttempt != null) {
            return@withContext manifestAttempt
        }

        // Step 2: Fallback to GitHub REST API releases/latest for any standard release with an .apk asset
        val apiUrl = "https://api.github.com/repos/$cleanOwner/$cleanRepo/releases/latest"
        val apiReq = Request.Builder()
            .url(apiUrl)
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", USER_AGENT)
            .build()

        runCatching {
            client.newCall(apiReq).execute().use { response ->
                when {
                    response.code == 404 -> {
                        UpdateCheckResult.Error("REPO_OR_RELEASE_NOT_FOUND")
                    }
                    response.code == 403 || response.code == 429 -> {
                        UpdateCheckResult.Error("GitHub API rate limit reached. Try again later.")
                    }
                    !response.isSuccessful -> {
                        UpdateCheckResult.Error("HTTP ${response.code} from GitHub Releases")
                    }
                    else -> {
                        val body = response.body?.string()
                            ?: return@use UpdateCheckResult.Error("Empty response from GitHub")
                        val manifest = parseGitHubReleaseApi(body)
                            ?: return@use UpdateCheckResult.Error("No APK file found in latest GitHub Release")
                        evaluateManifestVersion(manifest)
                    }
                }
            }
        }.getOrElse { err ->
            UpdateCheckResult.Error(err.message ?: "Unable to reach GitHub Releases")
        }
    }

    private fun evaluateManifestVersion(manifest: UpdateManifest): UpdateCheckResult {
        return if (isNewerVersion(
                remoteVersionCode = manifest.versionCode,
                remoteVersionName = manifest.version,
                localVersionCode = BuildConfig.VERSION_CODE,
                localVersionName = BuildConfig.VERSION_NAME
            )
        ) {
            UpdateCheckResult.UpdateAvailable(manifest)
        } else {
            UpdateCheckResult.UpToDate(BuildConfig.VERSION_NAME, manifest.version)
        }
    }

    fun downloadAndVerifyApk(manifest: UpdateManifest): Flow<DownloadProgress> = flow {
        if (!manifest.apkUrl.startsWith("https://")) {
            emit(DownloadProgress.Failed("APK URL must use HTTPS"))
            return@flow
        }
        val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
        val targetFile = File(updatesDir, "priyo-azan-${manifest.version}.apk")

        // Clean up older cached APKs
        updatesDir.listFiles()?.forEach { file ->
            if (file.name != targetFile.name && file.extension.equals("apk", ignoreCase = true)) {
                runCatching { file.delete() }
            }
        }

        // Avoid re-downloading if already present and verified
        if (targetFile.exists() && targetFile.length() > 0L) {
            val existingHash = computeSha256(targetFile)
            val hashMatches = manifest.sha256.isBlank() ||
                existingHash.equals(manifest.sha256.trim(), ignoreCase = true)
            if (hashMatches && verifyPackageIdentity(targetFile)) {
                emit(DownloadProgress.VerifiedReady(targetFile, existingHash))
                return@flow
            }
        }

        val req = Request.Builder()
            .url(manifest.apkUrl)
            .header("User-Agent", USER_AGENT)
            .build()

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
        const val OFFICIAL_GITHUB_OWNER = "Itsparvaj"
        const val OFFICIAL_GITHUB_REPO = "Priyo-azan"
        private const val USER_AGENT = "PriyoAzan-Android-Updater/1.0"

        private val moshi: Moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
        private val manifestAdapter = moshi.adapter(UpdateManifest::class.java)
        private val releaseAdapter = moshi.adapter(GitHubReleaseDto::class.java)

        fun parseManifest(jsonString: String): UpdateManifest {
            return manifestAdapter.fromJson(jsonString)
                ?: throw IllegalArgumentException("Invalid update manifest JSON")
        }

        /**
         * Parses a standard GitHub REST API `/releases/latest` JSON payload into an [UpdateManifest],
         * selecting the best `.apk` asset and extracting SHA-256 from asset digest when present.
         */
        fun parseGitHubReleaseApi(jsonString: String): UpdateManifest? {
            val release = releaseAdapter.fromJson(jsonString) ?: return null
            if (release.draft) return null
            val apkAsset = release.assets
                .filter { it.name.endsWith(".apk", ignoreCase = true) && it.browserDownloadUrl.startsWith("https://") }
                .sortedByDescending { asset ->
                    when {
                        asset.name.contains("release", ignoreCase = true) -> 2
                        asset.name.contains("signed", ignoreCase = true) -> 1
                        else -> 0
                    }
                }
                .firstOrNull() ?: return null

            val cleanVersion = release.tagName.trim().removePrefix("v").removePrefix("V")
                .ifBlank { "1.0.0" }
            val digestSha = apkAsset.digest?.let { raw ->
                if (raw.startsWith("sha256:", ignoreCase = true)) {
                    raw.substringAfter(":").trim()
                } else ""
            }.orEmpty()

            return UpdateManifest(
                version = cleanVersion,
                versionCode = 0, // Fallback relies on semver comparison of tagName
                platform = "android",
                releaseNotes = release.body?.trim().orEmpty().ifBlank { release.name.orEmpty() },
                apkUrl = apkAsset.browserDownloadUrl,
                minimumSupportedVersion = "1.0",
                sha256 = digestSha
            )
        }

        /**
         * Normalizes user-entered GitHub repository strings such as:
         * - `https://github.com/user/priyo-azan`
         * - `https://github.com/user/priyo-azan.git`
         * - `github.com/user/priyo-azan`
         * - `user/priyo-azan`
         */
        fun parseRepoTarget(rawInput: String): Pair<String, String>? {
            val cleaned = rawInput.trim()
                .removePrefix("https://")
                .removePrefix("http://")
                .removePrefix("www.")
                .removePrefix("github.com/")
                .trim('/')
                .removeSuffix(".git")
            val parts = cleaned.split("/").filter { it.isNotBlank() }
            if (parts.size < 2) return null
            return parts[0].trim() to parts[1].trim()
        }

        fun isNewerVersion(
            remoteVersionCode: Int,
            remoteVersionName: String,
            localVersionCode: Int,
            localVersionName: String
        ): Boolean {
            if (remoteVersionCode > localVersionCode) return true
            if (remoteVersionCode in 1 until localVersionCode) return false
            return compareSemver(remoteVersionName, localVersionName) > 0
        }

        fun compareSemver(a: String, b: String): Int {
            val cleanA = a.trim().removePrefix("v").removePrefix("V").substringBefore("-")
            val cleanB = b.trim().removePrefix("v").removePrefix("V").substringBefore("-")
            val pa = cleanA.split(".").map { it.toIntOrNull() ?: 0 }
            val pb = cleanB.split(".").map { it.toIntOrNull() ?: 0 }
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
