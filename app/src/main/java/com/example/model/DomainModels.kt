package com.example.model

import java.time.ZonedDateTime

enum class PrayerKey(val defaultOrder: Int, val isObligatoryAzanPrayer: Boolean) {
    FAJR(0, true),
    SUNRISE(1, false),
    DHUHR(2, true),
    ASR(3, true),
    MAGHRIB(4, true),
    ISHA(5, true);

    companion object {
        fun fromName(name: String): PrayerKey =
            entries.firstOrNull { it.name == name } ?: FAJR
    }
}

enum class CalculationMethod(
    val fajrAngle: Double,
    val ishaAngle: Double?,
    val ishaIntervalMinutes: Int?,
    val ishaRamadanIntervalMinutes: Int? = null
) {
    MWL(18.0, 17.0, null),
    UMM_AL_QURA(18.5, null, 90, 120),
    EGYPTIAN(19.5, 17.5, null),
    KARACHI(18.0, 18.0, null),
    ISNA(15.0, 15.0, null);

    companion object {
        fun fromName(name: String): CalculationMethod =
            entries.firstOrNull { it.name == name } ?: MWL
    }
}

enum class AsrMethod(val shadowFactor: Int) {
    SHAFI(1),
    HANAFI(2);

    companion object {
        fun fromName(name: String): AsrMethod =
            entries.firstOrNull { it.name == name } ?: SHAFI
    }
}

enum class HighLatitudeRule {
    NONE,
    MIDDLE_OF_NIGHT,
    ONE_SEVENTH,
    ANGLE_BASED;

    companion object {
        fun fromName(name: String): HighLatitudeRule =
            entries.firstOrNull { it.name == name } ?: ANGLE_BASED
    }
}

enum class SoundMode {
    FULL_AZAN,
    GENTLE_CHIME,
    NOTIFICATION_ONLY,
    MUTED;

    companion object {
        fun fromName(name: String): SoundMode =
            entries.firstOrNull { it.name == name } ?: FULL_AZAN
    }
}

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK;

    companion object {
        fun fromName(name: String): ThemeMode =
            entries.firstOrNull { it.name == name } ?: SYSTEM
    }
}

enum class AppLanguage(val code: String, val isRtl: Boolean) {
    ENGLISH("en", false),
    BENGALI("bn", false),
    ARABIC("ar", true);

    companion object {
        fun fromCode(code: String): AppLanguage =
            entries.firstOrNull { it.code == code } ?: ENGLISH
    }
}

enum class FontScaleOption(val scale: Float) {
    SMALL(0.92f),
    NORMAL(1.0f),
    LARGE(1.15f);

    companion object {
        fun fromName(name: String): FontScaleOption =
            entries.firstOrNull { it.name == name } ?: NORMAL
    }
}

data class PrayerEntry(
    val key: PrayerKey,
    val time: ZonedDateTime,
    val isNext: Boolean = false,
    val hasPassed: Boolean = false
)

data class DailyPrayerSchedule(
    val dateIso: String,
    val locationName: String,
    val timeZoneId: String,
    val entries: List<PrayerEntry>,
    val nextPrayer: PrayerEntry?,
    val previousPrayer: PrayerEntry?
)

data class HijriDateInfo(
    val day: Int,
    val month: Int,
    val year: Int,
    val gregorianIso: String,
    val isRamadan: Boolean
)

data class IslamicHoliday(
    val hijriMonth: Int,
    val hijriDay: Int,
    val key: String
)

data class QiblaInfo(
    val bearingDegrees: Double,
    val distanceKm: Double
)

data class CityPreset(
    val id: String,
    val name: String,
    val country: String,
    val latitude: Double,
    val longitude: Double,
    val timeZoneId: String,
    val recommendedMethod: CalculationMethod
)

data class SurahInfo(
    val number: Int,
    val arabicName: String,
    val englishName: String,
    val bengaliName: String,
    val revelationType: String,
    val ayahCount: Int
)

data class AyahItem(
    val surahNumber: Int,
    val ayahNumber: Int,
    val arabicText: String,
    val englishTranslation: String,
    val bengaliTranslation: String
)

data class AzanStatus(
    val exactAlarmAllowed: Boolean,
    val notificationsAllowed: Boolean,
    val batteryOptimizationIgnored: Boolean,
    val scheduledCount: Int,
    val nextScheduledLabel: String?,
    val nextScheduledEpochMillis: Long?
)

data class UpdateManifest(
    val version: String,
    val versionCode: Int,
    val platform: String,
    val releaseNotes: String,
    val apkUrl: String,
    val minimumSupportedVersion: String,
    val sha256: String
)
