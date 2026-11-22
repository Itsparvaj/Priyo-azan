package com.example.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val onboardingCompleted: Boolean = false,
    val calculationMethod: String = "MWL",
    val asrMethod: String = "SHAFI",
    val highLatitudeRule: String = "ANGLE_BASED",
    val hijriOffsetDays: Int = 0,
    val themeMode: String = "SYSTEM",
    val fontScaleOption: String = "NORMAL",
    val languageCode: String = "en",
    val fullAzanAudioId: String = "mosque_classic",
    val fajrAzanAudioId: String = "mosque_levant",
    val githubRepoOwner: String = "priyo-azan",
    val githubRepoName: String = "priyo-azan-android",
    val lastUpdateCheckMillis: Long = 0L,
    val lastKnownLatestVersion: String? = null
)

@Entity(tableName = "locations")
data class LocationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val country: String,
    val latitude: Double,
    val longitude: Double,
    val timeZoneId: String,
    val isCurrent: Boolean = true,
    val source: String = "PRESET",
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "prayer_preferences")
data class PrayerPreferenceEntity(
    @PrimaryKey val prayerKey: String,
    val azanEnabled: Boolean = true,
    val notificationEnabled: Boolean = true,
    val soundMode: String = "FULL_AZAN",
    val audioId: String = "mosque_classic",
    val manualOffsetMinutes: Int = 0
)

@Entity(tableName = "prayer_schedules")
data class PrayerScheduleEntity(
    @PrimaryKey val scheduleKey: String, // e.g. "2026-11-22_FAJR"
    val dateIso: String,
    val prayerKey: String,
    val triggerEpochMillis: Long,
    val formattedTime: String,
    val scheduledAlarmActive: Boolean
)

@Entity(tableName = "azan_audio")
data class AzanAudioEntity(
    @PrimaryKey val id: String,
    val title: String,
    val muezzin: String,
    val style: String,
    val isFajrSpecific: Boolean,
    val durationSeconds: Int,
    val rawResName: String,
    val isReferenceTone: Boolean,
    val licenseNote: String
)

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey val id: String, // "surah:ayah"
    val surahNumber: Int,
    val ayahNumber: Int,
    val surahName: String,
    val arabicSnippet: String,
    val note: String = "",
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "quran_progress")
data class QuranProgressEntity(
    @PrimaryKey val id: Int = 1,
    val surahNumber: Int,
    val ayahNumber: Int,
    val surahName: String,
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "update_history")
data class UpdateHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val version: String,
    val versionCode: Int,
    val status: String,
    val releaseNotes: String,
    val checkedAtMillis: Long = System.currentTimeMillis()
)
