package com.example.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import com.example.model.CalculationMethod
import com.example.model.CityPreset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.time.ZoneId
import java.util.Locale
import kotlin.coroutines.resume

data class DetectedLocationResult(
    val name: String,
    val country: String,
    val latitude: Double,
    val longitude: Double,
    val timeZoneId: String,
    val source: String
)

class LocationService(private val context: Context) {

    fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    @SuppressLint("MissingPermission")
    suspend fun detectCurrentLocation(): Result<DetectedLocationResult> = withContext(Dispatchers.IO) {
        if (!hasLocationPermission()) {
            return@withContext Result.failure(SecurityException("Location permission not granted"))
        }
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val providers = listOf(
            LocationManager.FUSED_PROVIDER,
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER
        ).filter { runCatching { lm.isProviderEnabled(it) }.getOrDefault(false) }

        var location: Location? = null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            for (provider in providers) {
                location = getCurrentLocationApi30(lm, provider)
                if (location != null) break
            }
        }

        if (location == null) {
            val allProviders = lm.getProviders(true)
            location = allProviders
                .mapNotNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }
                .maxByOrNull { it.time }
        }

        if (location == null) {
            return@withContext Result.failure(IllegalStateException("No GPS or network fix available"))
        }

        val tzId = ZoneId.systemDefault().id
        val (city, country) = reverseGeocodeSafe(location.latitude, location.longitude)

        Result.success(
            DetectedLocationResult(
                name = city,
                country = country,
                latitude = location.latitude,
                longitude = location.longitude,
                timeZoneId = tzId,
                source = "GPS"
            )
        )
    }

    @SuppressLint("MissingPermission")
    private suspend fun getCurrentLocationApi30(
        lm: LocationManager,
        provider: String
    ): Location? = suspendCancellableCoroutine { cont ->
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            cont.resume(null)
            return@suspendCancellableCoroutine
        }
        val signal = CancellationSignal()
        cont.invokeOnCancellation { signal.cancel() }
        runCatching {
            lm.getCurrentLocation(
                provider,
                signal,
                context.mainExecutor
            ) { loc ->
                if (cont.isActive) cont.resume(loc)
            }
        }.onFailure {
            if (cont.isActive) cont.resume(null)
        }
    }

    @Suppress("DEPRECATION")
    private fun reverseGeocodeSafe(lat: Double, lon: Double): Pair<String, String> {
        return runCatching {
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses = geocoder.getFromLocation(lat, lon, 1)
            val addr = addresses?.firstOrNull()
            val city = addr?.locality ?: addr?.subAdminArea ?: addr?.adminArea
                ?: "%.3f°, %.3f°".format(lat, lon)
            val country = addr?.countryName ?: ""
            city to country
        }.getOrElse {
            "%.3f°, %.3f°".format(lat, lon) to ""
        }
    }

    companion object {
        val cityPresets: List<CityPreset> = listOf(
            CityPreset("dhaka", "Dhaka", "Bangladesh", 23.8103, 90.4125, "Asia/Dhaka", CalculationMethod.KARACHI),
            CityPreset("chattogram", "Chattogram", "Bangladesh", 22.3569, 91.7832, "Asia/Dhaka", CalculationMethod.KARACHI),
            CityPreset("sylhet", "Sylhet", "Bangladesh", 24.8949, 91.8687, "Asia/Dhaka", CalculationMethod.KARACHI),
            CityPreset("makkah", "Makkah", "Saudi Arabia", 21.4225, 39.8262, "Asia/Riyadh", CalculationMethod.UMM_AL_QURA),
            CityPreset("madinah", "Madinah", "Saudi Arabia", 24.4672, 39.6111, "Asia/Riyadh", CalculationMethod.UMM_AL_QURA),
            CityPreset("riyadh", "Riyadh", "Saudi Arabia", 24.7136, 46.6753, "Asia/Riyadh", CalculationMethod.UMM_AL_QURA),
            CityPreset("cairo", "Cairo", "Egypt", 30.0444, 31.2357, "Africa/Cairo", CalculationMethod.EGYPTIAN),
            CityPreset("istanbul", "Istanbul", "Turkey", 41.0082, 28.9784, "Europe/Istanbul", CalculationMethod.MWL),
            CityPreset("karachi", "Karachi", "Pakistan", 24.8607, 67.0011, "Asia/Karachi", CalculationMethod.KARACHI),
            CityPreset("kuala_lumpur", "Kuala Lumpur", "Malaysia", 3.1390, 101.6869, "Asia/Kuala_Lumpur", CalculationMethod.MWL),
            CityPreset("jakarta", "Jakarta", "Indonesia", -6.2088, 106.8456, "Asia/Jakarta", CalculationMethod.MWL),
            CityPreset("london", "London", "United Kingdom", 51.5074, -0.1278, "Europe/London", CalculationMethod.MWL),
            CityPreset("new_york", "New York", "United States", 40.7128, -74.0060, "America/New_York", CalculationMethod.ISNA),
            CityPreset("toronto", "Toronto", "Canada", 43.6532, -79.3832, "America/Toronto", CalculationMethod.ISNA)
        )
    }
}
