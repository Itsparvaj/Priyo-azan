package com.example

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.azan.AzanEngine
import com.example.azan.BootAndTimeChangeReceiver
import com.example.database.PriyoAzanDatabase
import com.example.database.PriyoAzanRepository
import com.example.model.PrayerKey
import com.example.model.SoundMode
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var context: Context
    private lateinit var db: PriyoAzanDatabase
    private lateinit var repo: PriyoAzanRepository
    private lateinit var engine: AzanEngine

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, PriyoAzanDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = PriyoAzanRepository(db)
        engine = AzanEngine(context, repo)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun readAppNameFromContext() {
        val appName = context.getString(R.string.app_name)
        assertEquals("Priyo Azan", appName)
    }

    @Test
    fun sqlitePersistence_preservesLocationAndPrayerPreferences() = runTest {
        repo.ensureInitialized()
        repo.saveCurrentLocation("Makkah", "Saudi Arabia", 21.4225, 39.8262, "Asia/Riyadh", "CITY_PRESET")
        repo.updatePrayerPreference(PrayerKey.ASR) {
            it.copy(soundMode = SoundMode.GENTLE_CHIME.name, manualOffsetMinutes = 2)
        }

        val loc = repo.getCurrentLocationSnapshot()
        assertEquals("Makkah", loc?.name)
        val asrPref = repo.getPrayerPreferencesSnapshot().first { it.prayerKey == PrayerKey.ASR.name }
        assertEquals(SoundMode.GENTLE_CHIME.name, asrPref.soundMode)
        assertEquals(2, asrPref.manualOffsetMinutes)
    }

    @Test
    fun azanEngine_schedulesAlarmsWithoutDuplicatesAndHandlesTimezoneChange() = runTest {
        repo.ensureInitialized()
        val firstRun = engine.schedulePrayerAlarms()
        assertEquals(5, firstRun.size) // 5 obligatory prayers; Sunrise muted by default

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val shadowAlarm = Shadows.shadowOf(alarmManager)
        val countAfterFirst = shadowAlarm.scheduledAlarms.size
        assertTrue(countAfterFirst >= 5)

        // Rescheduling must update existing PendingIntents instead of creating duplicates
        val secondRun = engine.rescheduleAllAlarms()
        assertEquals(5, secondRun.size)
        assertEquals(countAfterFirst, shadowAlarm.scheduledAlarms.size)

        // Verify BootAndTimeChangeReceiver handles BOOT_COMPLETED cleanly
        val receiver = BootAndTimeChangeReceiver()
        receiver.onReceive(context, Intent(Intent.ACTION_BOOT_COMPLETED))
        receiver.onReceive(context, Intent(Intent.ACTION_TIMEZONE_CHANGED))
    }

    @Test
    fun repository_locksOfficialUpdateRepositoryOnInitialization() = runTest {
        db.appSettingsDao().upsert(
            com.example.database.AppSettingsEntity(
                githubRepoOwner = "old-owner",
                githubRepoName = "old-repo"
            )
        )
        repo.ensureInitialized()
        val settings = repo.getSettingsSnapshot()
        assertEquals("Itsparvaj", settings.githubRepoOwner)
        assertEquals("Priyo-azan", settings.githubRepoName)
    }
}
