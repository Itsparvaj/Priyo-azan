package com.example.database

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        AppSettingsEntity::class,
        LocationEntity::class,
        PrayerPreferenceEntity::class,
        PrayerScheduleEntity::class,
        AzanAudioEntity::class,
        BookmarkEntity::class,
        QuranProgressEntity::class,
        UpdateHistoryEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class PriyoAzanDatabase : RoomDatabase() {
    abstract fun appSettingsDao(): AppSettingsDao
    abstract fun locationDao(): LocationDao
    abstract fun prayerPreferenceDao(): PrayerPreferenceDao
    abstract fun prayerScheduleDao(): PrayerScheduleDao
    abstract fun azanAudioDao(): AzanAudioDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun quranProgressDao(): QuranProgressDao
    abstract fun updateHistoryDao(): UpdateHistoryDao

    companion object {
        private const val TAG = "PriyoAzanDatabase"
        private const val DB_NAME = "priyo_azan.db"

        @Volatile
        private var INSTANCE: PriyoAzanDatabase? = null

        /**
         * Example forward migration placeholder for future schema updates so user settings
         * and bookmarks are preserved across APK upgrades.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Future schema migrations go here without destructive fallback.
            }
        }

        fun getInstance(context: Context): PriyoAzanDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context.applicationContext).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(appContext: Context): PriyoAzanDatabase {
            return Room.databaseBuilder(
                appContext,
                PriyoAzanDatabase::class.java,
                DB_NAME
            )
                .addCallback(object : Callback() {
                    override fun onOpen(db: SupportSQLiteDatabase) {
                        super.onOpen(db)
                        try {
                            db.execSQL("PRAGMA foreign_keys=ON;")
                        } catch (t: Throwable) {
                            Log.w(TAG, "Unable to enforce PRAGMA foreign_keys", t)
                        }
                    }
                })
                .build()
        }
    }
}
