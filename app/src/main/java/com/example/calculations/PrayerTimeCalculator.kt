package com.example.calculations

import com.example.model.AsrMethod
import com.example.model.CalculationMethod
import com.example.model.DailyPrayerSchedule
import com.example.model.HighLatitudeRule
import com.example.model.PrayerEntry
import com.example.model.PrayerKey
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.tan

/**
 * Pure offline astronomical Islamic prayer time calculation engine based on
 * standard Jean Meeus solar ephemeris equations and PrayTimes methodology.
 * Zero network access required.
 */
object PrayerTimeCalculator {

    fun calculateForDate(
        date: LocalDate,
        latitude: Double,
        longitude: Double,
        zoneId: ZoneId,
        method: CalculationMethod = CalculationMethod.MWL,
        asrMethod: AsrMethod = AsrMethod.SHAFI,
        highLatitudeRule: HighLatitudeRule = HighLatitudeRule.ANGLE_BASED,
        manualOffsets: Map<PrayerKey, Int> = emptyMap(),
        isRamadan: Boolean = false,
        now: ZonedDateTime = ZonedDateTime.now(zoneId),
        locationName: String = ""
    ): DailyPrayerSchedule {
        val midnight = date.atStartOfDay(zoneId)
        val tzOffsetHours = midnight.offset.totalSeconds / 3600.0
        val jd = julianDate(date.year, date.monthValue, date.dayOfMonth) - longitude / (15.0 * 24.0)

        var fajr = 5.0
        var sunrise = 6.0
        var dhuhr = 12.0
        var asr = 15.0
        var sunset = 18.0
        var maghrib = 18.0
        var isha = 19.5

        // Two iterative refinement passes for sub-minute solar position precision
        repeat(2) {
            val tFajr = sunAngleTime(jd, fajr / 24.0, latitude, method.fajrAngle, ccw = true)
            val tSunrise = sunAngleTime(jd, sunrise / 24.0, latitude, riseSetAngle(0.0), ccw = true)
            val tDhuhr = midDay(jd, dhuhr / 24.0)
            val tAsr = asrTime(jd, asr / 24.0, latitude, asrMethod.shadowFactor.toDouble())
            val tSunset = sunAngleTime(jd, sunset / 24.0, latitude, riseSetAngle(0.0), ccw = false)
            val tMaghrib = tSunset
            val tIsha = if (method.ishaAngle != null) {
                sunAngleTime(jd, isha / 24.0, latitude, method.ishaAngle, ccw = false)
            } else {
                tMaghrib
            }

            fajr = tFajr
            sunrise = tSunrise
            dhuhr = tDhuhr
            asr = tAsr
            sunset = tSunset
            maghrib = tMaghrib
            isha = tIsha
        }

        // Convert solar day fractions into local clock hours
        val lonDiff = tzOffsetHours - longitude / 15.0
        fajr += lonDiff
        sunrise += lonDiff
        dhuhr += lonDiff
        asr += lonDiff
        sunset += lonDiff
        maghrib += lonDiff
        isha += lonDiff

        // Umm Al-Qura fixed interval after Maghrib
        if (method.ishaAngle == null) {
            val interval = if (isRamadan && method.ishaRamadanIntervalMinutes != null) {
                method.ishaRamadanIntervalMinutes
            } else {
                method.ishaIntervalMinutes ?: 90
            }
            isha = maghrib + interval / 60.0
        }

        // Adjust Fajr & Isha for high latitudes when twilight fails or exceeds rule bounds
        if (highLatitudeRule != HighLatitudeRule.NONE) {
            val nightTime = timeDiff(sunset, sunrise)
            val fajrPortion = nightPortion(method.fajrAngle, nightTime, highLatitudeRule)
            if (fajr.isNaN() || timeDiff(fajr, sunrise) > fajrPortion) {
                fajr = sunrise - fajrPortion
            }
            if (method.ishaAngle != null) {
                val ishaPortion = nightPortion(method.ishaAngle, nightTime, highLatitudeRule)
                if (isha.isNaN() || timeDiff(sunset, isha) > ishaPortion) {
                    isha = sunset + ishaPortion
                }
            }
        }

        val rawHours = mapOf(
            PrayerKey.FAJR to fajr,
            PrayerKey.SUNRISE to sunrise,
            PrayerKey.DHUHR to dhuhr,
            PrayerKey.ASR to asr,
            PrayerKey.MAGHRIB to maghrib,
            PrayerKey.ISHA to isha
        )

        val entries = PrayerKey.entries.map { key ->
            val offsetMins = manualOffsets[key] ?: 0
            val hourVal = (rawHours[key] ?: 12.0) + offsetMins / 60.0
            val totalMinutes = (hourVal * 60.0).roundToInt()
            val normalizedMinutes = ((totalMinutes % 1440) + 1440) % 1440
            val h = normalizedMinutes / 60
            val m = normalizedMinutes % 60
            val zdt = date.atTime(h, m).atZone(zoneId)
            PrayerEntry(key = key, time = zdt)
        }

        val nextIdx = entries.indexOfFirst { it.time.isAfter(now) }
        val finalEntries = entries.mapIndexed { idx, entry ->
            entry.copy(
                isNext = idx == nextIdx,
                hasPassed = !entry.time.isAfter(now)
            )
        }

        val nextPrayer = if (nextIdx >= 0) finalEntries[nextIdx] else null
        val prevPrayer = when {
            nextIdx > 0 -> finalEntries[nextIdx - 1]
            nextIdx == -1 -> finalEntries.lastOrNull()
            else -> null
        }

        return DailyPrayerSchedule(
            dateIso = date.toString(),
            locationName = locationName,
            timeZoneId = zoneId.id,
            entries = finalEntries,
            nextPrayer = nextPrayer,
            previousPrayer = prevPrayer
        )
    }

    /**
     * Resolves the single upcoming prayer across today and tomorrow so countdown
     * continues seamlessly after Isha into next day's Fajr.
     */
    fun findUpcomingPrayer(
        latitude: Double,
        longitude: Double,
        zoneId: ZoneId,
        method: CalculationMethod,
        asrMethod: AsrMethod,
        highLatitudeRule: HighLatitudeRule,
        manualOffsets: Map<PrayerKey, Int>,
        isRamadan: Boolean = false,
        now: ZonedDateTime = ZonedDateTime.now(zoneId)
    ): PrayerEntry {
        val todaySchedule = calculateForDate(
            date = now.toLocalDate(),
            latitude = latitude,
            longitude = longitude,
            zoneId = zoneId,
            method = method,
            asrMethod = asrMethod,
            highLatitudeRule = highLatitudeRule,
            manualOffsets = manualOffsets,
            isRamadan = isRamadan,
            now = now
        )
        todaySchedule.nextPrayer?.let { return it }

        val tomorrowSchedule = calculateForDate(
            date = now.toLocalDate().plusDays(1),
            latitude = latitude,
            longitude = longitude,
            zoneId = zoneId,
            method = method,
            asrMethod = asrMethod,
            highLatitudeRule = highLatitudeRule,
            manualOffsets = manualOffsets,
            isRamadan = isRamadan,
            now = now
        )
        return tomorrowSchedule.entries.first().copy(isNext = true, hasPassed = false)
    }

    private fun julianDate(year: Int, month: Int, day: Int): Double {
        var y = year
        var m = month
        if (m <= 2) {
            y -= 1
            m += 12
        }
        val a = floor(y / 100.0)
        val b = 2 - a + floor(a / 4.0)
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + b - 1524.5
    }

    private data class SunPosition(val declination: Double, val equationOfTime: Double)

    private fun sunPosition(jd: Double): SunPosition {
        val d = jd - 2451545.0
        val g = fixAngle(357.529 + 0.98560028 * d)
        val q = fixAngle(280.459 + 0.98564736 * d)
        val l = fixAngle(q + 1.915 * dsin(g) + 0.020 * dsin(2 * g))
        val e = 23.439 - 0.00000036 * d
        val ra = datan2(dcos(e) * dsin(l), dcos(l)) / 15.0
        val eqt = q / 15.0 - fixHour(ra)
        val decl = dasin(dsin(e) * dsin(l))
        return SunPosition(decl, eqt)
    }

    private fun midDay(jd: Double, timeFraction: Double): Double {
        val eqt = sunPosition(jd + timeFraction).equationOfTime
        return fixHour(12.0 - eqt)
    }

    private fun sunAngleTime(
        jd: Double,
        timeFraction: Double,
        latitude: Double,
        angle: Double,
        ccw: Boolean
    ): Double {
        val decl = sunPosition(jd + timeFraction).declination
        val noon = midDay(jd, timeFraction)
        val cosH = (-dsin(angle) - dsin(decl) * dsin(latitude)) / (dcos(decl) * dcos(latitude))
        if (cosH < -1.0 || cosH > 1.0) return Double.NaN
        val t = dacos(cosH) / 15.0
        return noon + if (ccw) -t else t
    }

    private fun asrTime(jd: Double, timeFraction: Double, latitude: Double, factor: Double): Double {
        val decl = sunPosition(jd + timeFraction).declination
        val angle = -darccot(factor + dtan(abs(latitude - decl)))
        return sunAngleTime(jd, timeFraction, latitude, angle, ccw = false)
    }

    private fun riseSetAngle(elevationMeters: Double): Double {
        val angle = 0.0347 * kotlin.math.sqrt(elevationMeters.coerceAtLeast(0.0))
        return 0.833 + angle
    }

    private fun nightPortion(angle: Double, night: Double, rule: HighLatitudeRule): Double {
        val portion = when (rule) {
            HighLatitudeRule.ANGLE_BASED -> angle / 60.0
            HighLatitudeRule.ONE_SEVENTH -> 1.0 / 7.0
            HighLatitudeRule.MIDDLE_OF_NIGHT -> 0.5
            HighLatitudeRule.NONE -> 0.0
        }
        return portion * night
    }

    private fun timeDiff(time1: Double, time2: Double): Double = fixHour(time2 - time1)

    private fun fixAngle(a: Double): Double = fix(a, 360.0)
    private fun fixHour(a: Double): Double = fix(a, 24.0)
    private fun fix(a: Double, b: Double): Double {
        var v = a - b * floor(a / b)
        if (v < 0) v += b
        return v
    }

    private fun dtr(d: Double): Double = d * PI / 180.0
    private fun rtd(r: Double): Double = r * 180.0 / PI
    private fun dsin(d: Double): Double = sin(dtr(d))
    private fun dcos(d: Double): Double = cos(dtr(d))
    private fun dtan(d: Double): Double = tan(dtr(d))
    private fun dasin(x: Double): Double = rtd(asin(x.coerceIn(-1.0, 1.0)))
    private fun dacos(x: Double): Double = rtd(acos(x.coerceIn(-1.0, 1.0)))
    private fun datan2(y: Double, x: Double): Double = rtd(atan2(y, x))
    private fun darccot(x: Double): Double = rtd(atan(1.0 / x))
}
