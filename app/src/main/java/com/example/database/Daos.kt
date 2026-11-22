package com.example.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AppSettingsDao {
    @Query("SELECT * FROM app_settings WHERE id = 1")
    fun observeSettings(): Flow<AppSettingsEntity?>

    @Query("SELECT * FROM app_settings WHERE id = 1")
    suspend fun getSettings(): AppSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(settings: AppSettingsEntity)
}

@Dao
interface LocationDao {
    @Query("SELECT * FROM locations WHERE isCurrent = 1 ORDER BY updatedAtMillis DESC LIMIT 1")
    fun observeCurrentLocation(): Flow<LocationEntity?>

    @Query("SELECT * FROM locations WHERE isCurrent = 1 ORDER BY updatedAtMillis DESC LIMIT 1")
    suspend fun getCurrentLocation(): LocationEntity?

    @Query("SELECT * FROM locations ORDER BY updatedAtMillis DESC")
    fun observeHistory(): Flow<List<LocationEntity>>

    @Query("UPDATE locations SET isCurrent = 0")
    suspend fun clearCurrentFlag()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(location: LocationEntity): Long
}

@Dao
interface PrayerPreferenceDao {
    @Query("SELECT * FROM prayer_preferences")
    fun observeAll(): Flow<List<PrayerPreferenceEntity>>

    @Query("SELECT * FROM prayer_preferences")
    suspend fun getAll(): List<PrayerPreferenceEntity>

    @Query("SELECT * FROM prayer_preferences WHERE prayerKey = :key")
    suspend fun getByKey(key: String): PrayerPreferenceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(pref: PrayerPreferenceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(prefs: List<PrayerPreferenceEntity>)
}

@Dao
interface PrayerScheduleDao {
    @Query("SELECT * FROM prayer_schedules WHERE scheduledAlarmActive = 1 ORDER BY triggerEpochMillis ASC")
    fun observeActiveSchedules(): Flow<List<PrayerScheduleEntity>>

    @Query("SELECT * FROM prayer_schedules WHERE scheduledAlarmActive = 1 ORDER BY triggerEpochMillis ASC")
    suspend fun getActiveSchedules(): List<PrayerScheduleEntity>

    @Query("DELETE FROM prayer_schedules")
    suspend fun clearAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(schedules: List<PrayerScheduleEntity>)
}

@Dao
interface AzanAudioDao {
    @Query("SELECT * FROM azan_audio")
    fun observeAll(): Flow<List<AzanAudioEntity>>

    @Query("SELECT * FROM azan_audio WHERE id = :id")
    suspend fun getById(id: String): AzanAudioEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<AzanAudioEntity>)
}

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks ORDER BY createdAtMillis DESC")
    fun observeAll(): Flow<List<BookmarkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(bookmark: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface QuranProgressDao {
    @Query("SELECT * FROM quran_progress WHERE id = 1")
    fun observeProgress(): Flow<QuranProgressEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(progress: QuranProgressEntity)
}

@Dao
interface UpdateHistoryDao {
    @Query("SELECT * FROM update_history ORDER BY checkedAtMillis DESC LIMIT 15")
    fun observeRecent(): Flow<List<UpdateHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: UpdateHistoryEntity)
}
