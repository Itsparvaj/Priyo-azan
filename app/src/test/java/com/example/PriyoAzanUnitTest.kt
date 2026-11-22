package com.example

import com.example.calculations.HijriCalendarEngine
import com.example.calculations.PrayerTimeCalculator
import com.example.calculations.QiblaCalculator
import com.example.localization.AppStrings
import com.example.model.AppLanguage
import com.example.model.AsrMethod
import com.example.model.CalculationMethod
import com.example.model.HighLatitudeRule
import com.example.model.PrayerKey
import com.example.updater.GitHubUpdateManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class PriyoAzanUnitTest {

    @Test
    fun prayerTimes_dhaka_orderedChronologically() {
        val zone = ZoneId.of("Asia/Dhaka")
        val date = LocalDate.of(2026, 3, 15)
        val schedule = PrayerTimeCalculator.calculateForDate(
            date = date,
            latitude = 23.8103,
            longitude = 90.4125,
            zoneId = zone,
            method = CalculationMethod.KARACHI,
            asrMethod = AsrMethod.HANAFI,
            highLatitudeRule = HighLatitudeRule.ANGLE_BASED,
            now = date.atTime(4, 0).atZone(zone),
            locationName = "Dhaka"
        )
        assertEquals(6, schedule.entries.size)
        for (i in 0 until schedule.entries.size - 1) {
            assertTrue(
                "Expected ${schedule.entries[i].key} before ${schedule.entries[i + 1].key}",
                schedule.entries[i].time.isBefore(schedule.entries[i + 1].time)
            )
        }
        assertEquals(PrayerKey.FAJR, schedule.nextPrayer?.key)
    }

    @Test
    fun prayerTimes_hanafiAsrIsLaterThanShafiAsr() {
        val zone = ZoneId.of("Asia/Dhaka")
        val date = LocalDate.of(2026, 6, 10)
        val shafi = PrayerTimeCalculator.calculateForDate(
            date, 23.8103, 90.4125, zone, CalculationMethod.MWL, AsrMethod.SHAFI
        ).entries.first { it.key == PrayerKey.ASR }.time
        val hanafi = PrayerTimeCalculator.calculateForDate(
            date, 23.8103, 90.4125, zone, CalculationMethod.MWL, AsrMethod.HANAFI
        ).entries.first { it.key == PrayerKey.ASR }.time
        assertTrue(hanafi.isAfter(shafi))
    }

    @Test
    fun prayerTimes_ummAlQura_ishaIs90MinutesAfterMaghribOutsideRamadan() {
        val zone = ZoneId.of("Asia/Riyadh")
        val date = LocalDate.of(2026, 5, 1)
        val schedule = PrayerTimeCalculator.calculateForDate(
            date, 21.4225, 39.8262, zone, CalculationMethod.UMM_AL_QURA, isRamadan = false
        )
        val maghrib = schedule.entries.first { it.key == PrayerKey.MAGHRIB }.time
        val isha = schedule.entries.first { it.key == PrayerKey.ISHA }.time
        assertEquals(90L, java.time.Duration.between(maghrib, isha).toMinutes())
    }

    @Test
    fun prayerTimes_ummAlQura_ishaIs120MinutesAfterMaghribInRamadan() {
        val zone = ZoneId.of("Asia/Riyadh")
        val date = LocalDate.of(2026, 3, 5)
        val schedule = PrayerTimeCalculator.calculateForDate(
            date, 21.4225, 39.8262, zone, CalculationMethod.UMM_AL_QURA, isRamadan = true
        )
        val maghrib = schedule.entries.first { it.key == PrayerKey.MAGHRIB }.time
        val isha = schedule.entries.first { it.key == PrayerKey.ISHA }.time
        assertEquals(120L, java.time.Duration.between(maghrib, isha).toMinutes())
    }

    @Test
    fun prayerTimes_manualOffsetAndUpcomingAcrossMidnight() {
        val zone = ZoneId.of("UTC")
        val date = LocalDate.of(2026, 4, 1)
        val base = PrayerTimeCalculator.calculateForDate(date, 51.5074, -0.1278, zone)
        val shifted = PrayerTimeCalculator.calculateForDate(
            date, 51.5074, -0.1278, zone, manualOffsets = mapOf(PrayerKey.FAJR to 5)
        )
        val diff = java.time.Duration.between(
            base.entries.first().time,
            shifted.entries.first().time
        ).toMinutes()
        assertEquals(5L, diff)

        val lateNight = date.atTime(23, 55).atZone(zone)
        val upcoming = PrayerTimeCalculator.findUpcomingPrayer(
            51.5074, -0.1278, zone, CalculationMethod.MWL, AsrMethod.SHAFI,
            HighLatitudeRule.ANGLE_BASED, emptyMap(), now = lateNight
        )
        assertEquals(PrayerKey.FAJR, upcoming.key)
        assertEquals(date.plusDays(1), upcoming.time.toLocalDate())
    }

    @Test
    fun qiblaCalculator_accurateBearingsForGlobalCities() {
        val dhaka = QiblaCalculator.calculate(23.8103, 90.4125)
        assertEquals(277.6, dhaka.bearingDegrees, 1.5)
        assertTrue(dhaka.distanceKm in 5000.0..5400.0)

        val london = QiblaCalculator.calculate(51.5074, -0.1278)
        assertEquals(118.9, london.bearingDegrees, 1.5)

        val newYork = QiblaCalculator.calculate(40.7128, -74.0060)
        assertEquals(58.5, newYork.bearingDegrees, 1.5)
    }

    @Test
    fun hijriCalendar_offsetAndGridWorkOffline() {
        val baseDate = LocalDate.of(2026, 11, 22)
        val h0 = HijriCalendarEngine.toHijri(baseDate, 0)
        val hPlus1 = HijriCalendarEngine.toHijri(baseDate, 1)
        assertTrue(hPlus1.day != h0.day || hPlus1.month != h0.month)
        val grid = HijriCalendarEngine.buildHijriMonthGrid(h0.year, h0.month, 0, baseDate)
        assertTrue(grid.size in 29..30)
        assertTrue(grid.any { it.isToday })
    }

    @Test
    fun localization_supportsEnglishBengaliArabicAndRtl() {
        assertFalse(AppLanguage.ENGLISH.isRtl)
        assertFalse(AppLanguage.BENGALI.isRtl)
        assertTrue(AppLanguage.ARABIC.isRtl)

        assertEquals("প্রিয় আজান", AppStrings.forLanguage(AppLanguage.BENGALI).appName)
        assertEquals("بريو أذان", AppStrings.forLanguage(AppLanguage.ARABIC).appName)
        assertEquals("১২:৩০", AppStrings.localizeDigits("12:30", AppLanguage.BENGALI))
        assertEquals("١٢:٣٠", AppStrings.localizeDigits("12:30", AppLanguage.ARABIC))
    }

    @Test
    fun githubUpdater_manifestParsingVersionCheckAndSha256() {
        assertEquals("Itsparvaj", GitHubUpdateManager.OFFICIAL_GITHUB_OWNER)
        assertEquals("Priyo-azan", GitHubUpdateManager.OFFICIAL_GITHUB_REPO)
        assertEquals("PriyoBlog.com", AppStrings.forLanguage(AppLanguage.ENGLISH).aboutPublisherText)
        assertEquals("PriyoBlog.com", AppStrings.forLanguage(AppLanguage.BENGALI).aboutPublisherText)
        assertEquals("PriyoBlog.com", AppStrings.forLanguage(AppLanguage.ARABIC).aboutPublisherText)

        val json = """
            {
              "version": "1.2.0",
              "versionCode": 3,
              "platform": "android",
              "releaseNotes": "Improved Azan stability",
              "apkUrl": "https://github.com/Itsparvaj/Priyo-azan/releases/download/v1.2.0/app-release.apk",
              "minimumSupportedVersion": "1.0.0",
              "sha256": "abc"
            }
        """.trimIndent()
        val manifest = GitHubUpdateManager.parseManifest(json)
        assertEquals("1.2.0", manifest.version)
        assertEquals(3, manifest.versionCode)
        assertTrue(GitHubUpdateManager.isNewerVersion(2, "1.1.0", 1, "1.0.0"))
        assertFalse(GitHubUpdateManager.isNewerVersion(1, "1.0.0", 1, "1.0.0"))

        val temp = File.createTempFile("priyo_sha_test", ".bin")
        try {
            temp.writeText("priyo-azan-apk-payload")
            val digest1 = GitHubUpdateManager.computeSha256(temp)
            val digest2 = GitHubUpdateManager.computeSha256(temp)
            assertEquals(64, digest1.length)
            assertEquals(digest1, digest2)
        } finally {
            temp.delete()
        }
    }

    @Test
    fun githubUpdater_repoUrlParsingAndReleaseApiFallback() {
        assertEquals("Itsparvaj" to "Priyo-azan", GitHubUpdateManager.parseRepoTarget("https://github.com/Itsparvaj/Priyo-azan"))
        assertEquals("Itsparvaj" to "Priyo-azan", GitHubUpdateManager.parseRepoTarget("https://github.com/Itsparvaj/Priyo-azan.git/"))
        assertEquals("Itsparvaj" to "Priyo-azan", GitHubUpdateManager.parseRepoTarget("Itsparvaj/Priyo-azan"))
        assertEquals(null, GitHubUpdateManager.parseRepoTarget("invalid-only-one-part"))

        val apiJson = """
            {
              "tag_name": "v1.1.0",
              "name": "Priyo Azan v1.1.0",
              "body": "Added Digital Tasbih & Masnoon Duas",
              "draft": false,
              "assets": [
                {
                  "name": "notes.txt",
                  "browser_download_url": "https://github.com/Itsparvaj/Priyo-azan/releases/download/v1.1.0/notes.txt"
                },
                {
                  "name": "priyo-azan-release.apk",
                  "browser_download_url": "https://github.com/Itsparvaj/Priyo-azan/releases/download/v1.1.0/priyo-azan-release.apk",
                  "digest": "sha256:deadbeef123456"
                }
              ]
            }
        """.trimIndent()
        val parsed = GitHubUpdateManager.parseGitHubReleaseApi(apiJson)!!
        assertEquals("1.1.0", parsed.version)
        assertEquals("deadbeef123456", parsed.sha256)
        assertTrue(parsed.apkUrl.endsWith(".apk"))
        assertTrue(GitHubUpdateManager.isNewerVersion(parsed.versionCode, parsed.version, 1, "1.0"))
        assertFalse(GitHubUpdateManager.isNewerVersion(0, "1.0", 1, "1.0"))
    }

    @Test
    fun spiritualContent_duasDhikrAndDailyMilestonesAreValid() {
        val duas = com.example.spiritual.SpiritualContent.masnoonDuas
        assertTrue(duas.size >= 7)
        assertTrue(duas.all { it.arabic.isNotBlank() && it.meaningBn.isNotBlank() && it.meaningEn.isNotBlank() })

        val presets = com.example.spiritual.SpiritualContent.dhikrPresets
        assertTrue(presets.size >= 6)
        assertTrue(presets.all { it.defaultTarget > 0 && it.arabic.isNotBlank() })

        val verse = com.example.spiritual.SpiritualContent.verseForDayOfYear(123)
        assertTrue(verse.arabic.isNotBlank() && verse.meaningBn.isNotBlank())

        val zone = ZoneId.of("Asia/Dhaka")
        val today = LocalDate.of(2026, 3, 15)
        val todaySched = PrayerTimeCalculator.calculateForDate(today, 23.8103, 90.4125, zone)
        val tomorrowSched = PrayerTimeCalculator.calculateForDate(today.plusDays(1), 23.8103, 90.4125, zone)
        val milestones = com.example.spiritual.SpiritualContent.computeDailyMilestones(
            fajr = todaySched.entries.first { it.key == PrayerKey.FAJR }.time,
            sunrise = todaySched.entries.first { it.key == PrayerKey.SUNRISE }.time,
            dhuhr = todaySched.entries.first { it.key == PrayerKey.DHUHR }.time,
            maghrib = todaySched.entries.first { it.key == PrayerKey.MAGHRIB }.time,
            nextFajr = tomorrowSched.entries.first { it.key == PrayerKey.FAJR }.time
        )
        assertTrue(milestones.sehriEnd.isBefore(milestones.ishraq))
        assertTrue(milestones.ishraq.isBefore(milestones.zawal))
        assertTrue(milestones.zawal.isBefore(milestones.iftar))
        assertTrue(milestones.iftar.isBefore(milestones.tahajjudStart))
        assertTrue(milestones.tahajjudStart.isBefore(tomorrowSched.entries.first { it.key == PrayerKey.FAJR }.time))
    }
}
