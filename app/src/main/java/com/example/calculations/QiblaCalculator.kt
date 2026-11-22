package com.example.calculations

import com.example.model.QiblaInfo
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Spherical trigonometry Qibla bearing & great-circle distance calculator toward
 * the Sacred Kaaba in Makkah (21.422487° N, 39.826206° E).
 */
object QiblaCalculator {
    const val KAABA_LAT = 21.422487
    const val KAABA_LON = 39.826206
    private const val EARTH_RADIUS_KM = 6371.0088

    fun calculate(latitude: Double, longitude: Double): QiblaInfo {
        val phi1 = Math.toRadians(latitude)
        val phi2 = Math.toRadians(KAABA_LAT)
        val deltaLambda = Math.toRadians(KAABA_LON - longitude)

        val y = sin(deltaLambda) * cos(phi2)
        val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(deltaLambda)
        val theta = atan2(y, x)
        val bearing = ((Math.toDegrees(theta) % 360.0) + 360.0) % 360.0

        val dLat = phi2 - phi1
        val a = sin(dLat / 2.0).pow(2) + cos(phi1) * cos(phi2) * sin(deltaLambda / 2.0).pow(2)
        val c = 2.0 * atan2(sqrt(a), sqrt((1.0 - a).coerceAtLeast(0.0)))
        val distanceKm = EARTH_RADIUS_KM * c

        return QiblaInfo(
            bearingDegrees = bearing,
            distanceKm = distanceKm
        )
    }
}
