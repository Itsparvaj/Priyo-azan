# Priyo Azan

**Priyo Azan** is an original, offline-first native Android Islamic prayer, automatic Azan, Qibla, Hijri calendar, and Quran application built in **Kotlin & Jetpack Compose** with **Room (SQLite)** persistence and **AlarmManager** native scheduling.

## Key Capabilities

- **Offline Prayer Calculation Engine (`PrayerTimeCalculator`)**: Astronomical solar ephemeris calculations supporting Muslim World League (MWL), Umm Al-Qura (including 90m / 120m Ramadan Isha), Egyptian, Karachi, and ISNA methods, Shafi'i & Hanafi Asr, high-latitude rules, DST/timezone shifts, and per-prayer minute offsets.
- **Native Automatic Azan Engine (`AzanEngine`, `AzanAlarmReceiver`, `BootAndTimeChangeReceiver`, `AzanPlaybackService`)**:
  - Uses `AlarmManager` with stable `PendingIntent` request codes to prevent duplicate alarms.
  - Restores alarms automatically after `BOOT_COMPLETED`, `TIME_SET`, `TIMEZONE_CHANGED`, `DATE_CHANGED`, and `MY_PACKAGE_REPLACED`.
  - Plays bundled offline audio via a `mediaPlayback` foreground service respecting Android audio focus and system volume/DND policies.
- **Native Location & Qibla Compass**:
  - Uses Android `LocationManager` + `Geocoder` with offline coordinate reuse and curated city presets.
  - Real `SensorManager` (`TYPE_ROTATION_VECTOR` + `ACCELEROMETER`/`MAGNETIC_FIELD` + `GeomagneticField` declination) Qibla compass with calibration guidance.
- **Offline Hijri Calendar & Quran Module**:
  - Algorithmic Hijri calendar with `-2..+2` day offset and moon-sighting variance disclaimer.
  - Complete 114-Surah catalog, 6 verified offline Surahs in Arabic (`Amiri`), English, and Bengali, search, bookmarks, and reading progress, plus `assets/quran/quran_dataset.json` import support.
- **GitHub Releases APK Updater (`GitHubUpdateManager`)**:
  - Checks `update-manifest.json` over HTTPS, downloads APK with progress, verifies SHA-256 checksum & package ID, and delegates to Android's system package installer via `FileProvider`.
- **Trilingual Localization**: English, Bengali (`বাংলা`), and Arabic (`العربية` with full RTL layout).

## Build APK & AAB

### Debug / Release APK
```bash
gradle :app:assembleDebug
gradle :app:assembleRelease
```

### Play App Bundle (AAB)
```bash
gradle :app:bundleRelease
```

## Run Tests
```bash
gradle :app:testDebugUnitTest
```
