package com.example.database

import android.util.Log
import com.example.model.PrayerKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.ZoneId

class PriyoAzanRepository(private val db: PriyoAzanDatabase) {
    private val settingsDao = db.appSettingsDao()
    private val locationDao = db.locationDao()
    private val prayerPrefDao = db.prayerPreferenceDao()
    private val scheduleDao = db.prayerScheduleDao()
    private val audioDao = db.azanAudioDao()
    private val bookmarkDao = db.bookmarkDao()
    private val progressDao = db.quranProgressDao()
    private val updateHistoryDao = db.updateHistoryDao()

    val settingsFlow: Flow<AppSettingsEntity> = settingsDao.observeSettings().map {
        it ?: AppSettingsEntity()
    }

    val currentLocationFlow: Flow<LocationEntity?> = locationDao.observeCurrentLocation()

    val prayerPreferencesFlow: Flow<List<PrayerPreferenceEntity>> = prayerPrefDao.observeAll()

    val activeSchedulesFlow: Flow<List<PrayerScheduleEntity>> = scheduleDao.observeActiveSchedules()

    val azanAudioFlow: Flow<List<AzanAudioEntity>> = audioDao.observeAll()

    val bookmarksFlow: Flow<List<BookmarkEntity>> = bookmarkDao.observeAll()

    val quranProgressFlow: Flow<QuranProgressEntity?> = progressDao.observeProgress()

    val updateHistoryFlow: Flow<List<UpdateHistoryEntity>> = updateHistoryDao.observeRecent()

    suspend fun ensureInitialized() {
        try {
            val existingSettings = settingsDao.getSettings()
            if (existingSettings == null) {
                settingsDao.upsert(AppSettingsEntity())
            } else if (existingSettings.fullAzanAudioId == "makkah_tone" ||
                existingSettings.fajrAzanAudioId == "fajr_tone" ||
                existingSettings.githubRepoOwner != "Itsparvaj" ||
                existingSettings.githubRepoName != "Priyo-azan"
            ) {
                settingsDao.upsert(
                    existingSettings.copy(
                        fullAzanAudioId = if (existingSettings.fullAzanAudioId == "makkah_tone") "mosque_classic" else existingSettings.fullAzanAudioId,
                        fajrAzanAudioId = if (existingSettings.fajrAzanAudioId == "fajr_tone") "mosque_levant" else existingSettings.fajrAzanAudioId,
                        githubRepoOwner = "Itsparvaj",
                        githubRepoName = "Priyo-azan"
                    )
                )
            }
            if (prayerPrefDao.getAll().isEmpty()) {
                val defaults = PrayerKey.entries.map { key ->
                    val isSunrise = key == PrayerKey.SUNRISE
                    val isFajr = key == PrayerKey.FAJR
                    PrayerPreferenceEntity(
                        prayerKey = key.name,
                        azanEnabled = !isSunrise,
                        notificationEnabled = !isSunrise,
                        soundMode = if (isSunrise) "MUTED" else "FULL_AZAN",
                        audioId = if (isFajr) "mosque_levant" else "mosque_classic",
                        manualOffsetMinutes = 0
                    )
                }
                prayerPrefDao.upsertAll(defaults)
            }
            audioDao.upsertAll(defaultAudioCatalog())
            if (locationDao.getCurrentLocation() == null) {
                // Default initial coordinates before GPS permission or city selection
                locationDao.insert(
                    LocationEntity(
                        name = "Dhaka",
                        country = "Bangladesh",
                        latitude = 23.8103,
                        longitude = 90.4125,
                        timeZoneId = ZoneId.systemDefault().id.ifBlank { "Asia/Dhaka" },
                        isCurrent = true,
                        source = "DEFAULT_PRESET"
                    )
                )
            }
        } catch (t: Throwable) {
            Log.e("PriyoAzanRepository", "Database initialization recovery", t)
        }
    }

    suspend fun saveCustomAudio(title: String, uriString: String) {
        audioDao.upsertAll(
            listOf(
                AzanAudioEntity(
                    id = "custom_device_azan",
                    title = title.ifBlank { "Custom Mosque Azan" },
                    muezzin = "Device Audio File",
                    style = "Custom MP3",
                    isFajrSpecific = false,
                    durationSeconds = 0,
                    rawResName = uriString,
                    isReferenceTone = false,
                    licenseNote = "User-selected local audio file."
                )
            )
        )
        updateSettings { it.copy(fullAzanAudioId = "custom_device_azan") }
    }

    suspend fun getSettingsSnapshot(): AppSettingsEntity {
        ensureInitialized()
        return settingsDao.getSettings() ?: AppSettingsEntity()
    }

    suspend fun updateSettings(transform: (AppSettingsEntity) -> AppSettingsEntity) {
        val current = getSettingsSnapshot()
        settingsDao.upsert(transform(current))
    }

    suspend fun getCurrentLocationSnapshot(): LocationEntity? {
        ensureInitialized()
        return locationDao.getCurrentLocation()
    }

    suspend fun saveCurrentLocation(
        name: String,
        country: String,
        latitude: Double,
        longitude: Double,
        timeZoneId: String,
        source: String
    ) {
        require(latitude in -90.0..90.0) { "Latitude out of bounds" }
        require(longitude in -180.0..180.0) { "Longitude out of bounds" }
        locationDao.clearCurrentFlag()
        locationDao.insert(
            LocationEntity(
                name = name.ifBlank { "%.4f, %.4f".format(latitude, longitude) },
                country = country,
                latitude = latitude,
                longitude = longitude,
                timeZoneId = timeZoneId,
                isCurrent = true,
                source = source,
                updatedAtMillis = System.currentTimeMillis()
            )
        )
    }

    suspend fun getPrayerPreferencesSnapshot(): List<PrayerPreferenceEntity> {
        ensureInitialized()
        return prayerPrefDao.getAll()
    }

    suspend fun updatePrayerPreference(
        prayerKey: PrayerKey,
        transform: (PrayerPreferenceEntity) -> PrayerPreferenceEntity
    ) {
        ensureInitialized()
        val current = prayerPrefDao.getByKey(prayerKey.name)
            ?: PrayerPreferenceEntity(prayerKey = prayerKey.name)
        prayerPrefDao.upsert(transform(current))
    }

    suspend fun replaceActiveSchedules(schedules: List<PrayerScheduleEntity>) {
        scheduleDao.clearAll()
        scheduleDao.upsertAll(schedules)
    }

    suspend fun getActiveSchedulesSnapshot(): List<PrayerScheduleEntity> =
        scheduleDao.getActiveSchedules()

    suspend fun getAudioById(id: String): AzanAudioEntity? = audioDao.getById(id)

    suspend fun toggleBookmark(
        surahNumber: Int,
        ayahNumber: Int,
        surahName: String,
        arabicSnippet: String,
        currentlyBookmarked: Boolean
    ) {
        val id = "$surahNumber:$ayahNumber"
        if (currentlyBookmarked) {
            bookmarkDao.deleteById(id)
        } else {
            bookmarkDao.upsert(
                BookmarkEntity(
                    id = id,
                    surahNumber = surahNumber,
                    ayahNumber = ayahNumber,
                    surahName = surahName,
                    arabicSnippet = arabicSnippet
                )
            )
        }
    }

    suspend fun saveQuranProgress(surahNumber: Int, ayahNumber: Int, surahName: String) {
        progressDao.upsert(
            QuranProgressEntity(
                id = 1,
                surahNumber = surahNumber,
                ayahNumber = ayahNumber,
                surahName = surahName,
                updatedAtMillis = System.currentTimeMillis()
            )
        )
    }

    suspend fun recordUpdateCheck(version: String, versionCode: Int, status: String, notes: String) {
        updateHistoryDao.insert(
            UpdateHistoryEntity(
                version = version,
                versionCode = versionCode,
                status = status,
                releaseNotes = notes
            )
        )
    }

    companion object {
        fun defaultAudioCatalog(): List<AzanAudioEntity> = listOf(
            AzanAudioEntity(
                id = "mosque_classic",
                title = "Mosque Adhan (Full Voice)",
                muezzin = "Muezzin Recitation • 2:34",
                style = "Traditional Mosque",
                isFajrSpecific = false,
                durationSeconds = 154,
                rawResName = "azan_mosque_classic",
                isReferenceTone = false,
                licenseNote = "CC0 1.0 Public Domain human Muezzin recording (Wikimedia Commons)."
            ),
            AzanAudioEntity(
                id = "mosque_levant",
                title = "Classic Haramain-Style Adhan",
                muezzin = "Muezzin Recitation • 2:38",
                style = "Melodic Dawn / Prayer",
                isFajrSpecific = true,
                durationSeconds = 158,
                rawResName = "azan_mosque_levant",
                isReferenceTone = false,
                licenseNote = "Public Domain human Muezzin recording (Wikimedia Commons)."
            ),
            AzanAudioEntity(
                id = "gentle_chime",
                title = "Gentle Prayer Chime",
                muezzin = "Short Alert • 0:03",
                style = "Minimal Soft",
                isFajrSpecific = false,
                durationSeconds = 3,
                rawResName = "azan_gentle_chime",
                isReferenceTone = true,
                licenseNote = "Short bundled notification chime."
            )
        )
    }
}
