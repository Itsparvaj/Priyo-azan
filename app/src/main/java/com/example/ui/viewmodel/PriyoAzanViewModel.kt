package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.PriyoAzanApp
import com.example.azan.AzanPlaybackService
import com.example.calculations.HijriCalendarEngine
import com.example.calculations.PrayerTimeCalculator
import com.example.calculations.QiblaCalculator
import com.example.database.AppSettingsEntity
import com.example.database.AzanAudioEntity
import com.example.database.BookmarkEntity
import com.example.database.LocationEntity
import com.example.database.PrayerPreferenceEntity
import com.example.database.PrayerScheduleEntity
import com.example.database.QuranProgressEntity
import com.example.database.UpdateHistoryEntity
import com.example.model.AppLanguage
import com.example.model.AsrMethod
import com.example.model.AzanStatus
import com.example.model.CalculationMethod
import com.example.model.CityPreset
import com.example.model.DailyPrayerSchedule
import com.example.model.FontScaleOption
import com.example.model.HighLatitudeRule
import com.example.model.HijriDateInfo
import com.example.model.PrayerEntry
import com.example.model.PrayerKey
import com.example.model.QiblaInfo
import com.example.model.SoundMode
import com.example.model.SurahInfo
import com.example.model.ThemeMode
import com.example.model.UpdateManifest
import com.example.sensors.CompassReading
import com.example.spiritual.DailyMilestones
import com.example.spiritual.DailyVerse
import com.example.spiritual.SpiritualContent
import com.example.updater.DownloadProgress
import com.example.updater.GitHubUpdateManager
import com.example.updater.UpdateCheckResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

data class TasbihState(
    val selectedDhikrIndex: Int = 0,
    val count: Int = 0,
    val target: Int = 33,
    val rounds: Int = 0
)

data class PriyoAzanUiState(
    val isReady: Boolean = false,
    val settings: AppSettingsEntity = AppSettingsEntity(),
    val location: LocationEntity? = null,
    val prayerPreferences: Map<PrayerKey, PrayerPreferenceEntity> = emptyMap(),
    val todaySchedule: DailyPrayerSchedule? = null,
    val selectedDateSchedule: DailyPrayerSchedule? = null,
    val selectedDate: LocalDate = LocalDate.now(),
    val dailyMilestones: DailyMilestones? = null,
    val selectedDateMilestones: DailyMilestones? = null,
    val dailyVerse: DailyVerse = SpiritualContent.verseForDayOfYear(LocalDate.now().dayOfYear),
    val upcomingPrayer: PrayerEntry? = null,
    val countdownText: String = "--:--:--",
    val countdownProgress: Float = 0f,
    val currentTimeFormatted: String = "",
    val hijriToday: HijriDateInfo = HijriCalendarEngine.toHijri(LocalDate.now()),
    val daysToRamadan: Long = 0L,
    val qiblaInfo: QiblaInfo = QiblaCalculator.calculate(23.8103, 90.4125),
    val compassReading: CompassReading = CompassReading(),
    val azanAudioCatalog: List<AzanAudioEntity> = emptyList(),
    val activeSchedules: List<PrayerScheduleEntity> = emptyList(),
    val azanStatus: AzanStatus = AzanStatus(false, false, false, 0, null, null),
    val previewPlayingId: String? = null,
    val isAzanServicePlaying: Boolean = false,
    val playingAudioLabel: String? = null,
    val tasbih: TasbihState = TasbihState(),
    val surahs: List<SurahInfo> = emptyList(),
    val bookmarks: List<BookmarkEntity> = emptyList(),
    val quranProgress: QuranProgressEntity? = null,
    val updateHistory: List<UpdateHistoryEntity> = emptyList(),
    val updateStatusMessage: String? = null,
    val availableUpdateManifest: UpdateManifest? = null,
    val downloadProgress: DownloadProgress? = null,
    val verifiedApkFile: File? = null,
    val locationDetecting: Boolean = false,
    val locationStatusMessage: String? = null
) {
    val isAnyAudioPlaying: Boolean
        get() = isAzanServicePlaying || previewPlayingId != null
}

class PriyoAzanViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as PriyoAzanApp
    private val repo = app.repository
    private val azanEngine = app.azanEngine
    private val locationService = app.locationService
    private val compassManager = app.compassSensorManager
    private val quranRepo = app.quranRepository
    private val updateManager = app.updateManager

    private val selectedDateFlow = MutableStateFlow(LocalDate.now())
    private val azanStatusFlow = MutableStateFlow(
        AzanStatus(
            exactAlarmAllowed = azanEngine.canScheduleExactAlarms(),
            notificationsAllowed = azanEngine.areNotificationsAllowed(),
            batteryOptimizationIgnored = azanEngine.isBatteryOptimizationIgnored(),
            scheduledCount = 0,
            nextScheduledLabel = null,
            nextScheduledEpochMillis = null
        )
    )
    private val updateStatusFlow = MutableStateFlow<String?>(null)
    private val availableManifestFlow = MutableStateFlow<UpdateManifest?>(null)
    private val downloadProgressFlow = MutableStateFlow<DownloadProgress?>(null)
    private val verifiedApkFlow = MutableStateFlow<File?>(null)
    private val locationDetectingFlow = MutableStateFlow(false)
    private val locationStatusFlow = MutableStateFlow<String?>(null)
    private val tasbihFlow = MutableStateFlow(TasbihState())

    private val tickerFlow = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(1000L)
        }
    }

    private val baseDataFlow = combine(
        repo.settingsFlow,
        repo.currentLocationFlow,
        repo.prayerPreferencesFlow,
        repo.azanAudioFlow,
        repo.activeSchedulesFlow
    ) { settings, location, prefs, audios, schedules ->
        BaseData(settings, location, prefs, audios, schedules)
    }

    private val quranAndUpdateFlow = combine(
        repo.bookmarksFlow,
        repo.quranProgressFlow,
        repo.updateHistoryFlow,
        azanEngine.previewPlayingId,
        compassManager.reading
    ) { bookmarks, progress, history, previewId, compass ->
        QuranAndExtra(bookmarks, progress, history, previewId, compass)
    }

    private val playbackAndTasbihFlow = combine(
        AzanPlaybackService.isPlaying,
        AzanPlaybackService.activeLabel,
        tasbihFlow
    ) { isSvcPlaying, svcLabel, tasbih ->
        Triple(isSvcPlaying, svcLabel, tasbih)
    }

    private val transientFlow = combine(
        selectedDateFlow,
        azanStatusFlow,
        updateStatusFlow,
        availableManifestFlow,
        downloadProgressFlow
    ) { date, status, updMsg, manifest, dlProg ->
        TransientState(date, status, updMsg, manifest, dlProg)
    }

    val uiState: StateFlow<PriyoAzanUiState> = combine(
        baseDataFlow,
        combine(quranAndUpdateFlow, playbackAndTasbihFlow) { q, p -> q to p },
        transientFlow,
        combine(verifiedApkFlow, locationDetectingFlow, locationStatusFlow) { apk, det, locMsg ->
            Triple(apk, det, locMsg)
        },
        tickerFlow
    ) { base, quranAndPlayback, trans, locAndApk, _ ->
        val extra = quranAndPlayback.first
        val (isSvcPlaying, svcLabel, tasbihState) = quranAndPlayback.second

        val loc = base.location
        val lat = loc?.latitude ?: 23.8103
        val lon = loc?.longitude ?: 90.4125
        val zoneId = runCatching { ZoneId.of(loc?.timeZoneId ?: ZoneId.systemDefault().id) }
            .getOrElse { ZoneId.systemDefault() }
        val now = ZonedDateTime.now(zoneId)
        val method = CalculationMethod.fromName(base.settings.calculationMethod)
        val asrMethod = AsrMethod.fromName(base.settings.asrMethod)
        val highLat = HighLatitudeRule.fromName(base.settings.highLatitudeRule)
        val prefMap = base.prayerPrefs.associateBy { PrayerKey.fromName(it.prayerKey) }
        val offsets = prefMap.mapValues { it.value.manualOffsetMinutes }

        val hijriToday = HijriCalendarEngine.toHijri(now.toLocalDate(), base.settings.hijriOffsetDays)
        val todaySchedule = PrayerTimeCalculator.calculateForDate(
            date = now.toLocalDate(),
            latitude = lat,
            longitude = lon,
            zoneId = zoneId,
            method = method,
            asrMethod = asrMethod,
            highLatitudeRule = highLat,
            manualOffsets = offsets,
            isRamadan = hijriToday.isRamadan,
            now = now,
            locationName = loc?.name ?: "Dhaka"
        )
        val tomorrowSchedule = PrayerTimeCalculator.calculateForDate(
            date = now.toLocalDate().plusDays(1),
            latitude = lat,
            longitude = lon,
            zoneId = zoneId,
            method = method,
            asrMethod = asrMethod,
            highLatitudeRule = highLat,
            manualOffsets = offsets,
            isRamadan = hijriToday.isRamadan,
            now = now,
            locationName = loc?.name ?: "Dhaka"
        )
        val todayMilestones = SpiritualContent.computeDailyMilestones(
            fajr = todaySchedule.entries.first { it.key == PrayerKey.FAJR }.time,
            sunrise = todaySchedule.entries.first { it.key == PrayerKey.SUNRISE }.time,
            dhuhr = todaySchedule.entries.first { it.key == PrayerKey.DHUHR }.time,
            maghrib = todaySchedule.entries.first { it.key == PrayerKey.MAGHRIB }.time,
            nextFajr = tomorrowSchedule.entries.first { it.key == PrayerKey.FAJR }.time
        )

        val selectedHijri = HijriCalendarEngine.toHijri(trans.selectedDate, base.settings.hijriOffsetDays)
        val selectedSchedule = PrayerTimeCalculator.calculateForDate(
            date = trans.selectedDate,
            latitude = lat,
            longitude = lon,
            zoneId = zoneId,
            method = method,
            asrMethod = asrMethod,
            highLatitudeRule = highLat,
            manualOffsets = offsets,
            isRamadan = selectedHijri.isRamadan,
            now = now,
            locationName = loc?.name ?: "Dhaka"
        )
        val nextDaySelectedSchedule = PrayerTimeCalculator.calculateForDate(
            date = trans.selectedDate.plusDays(1),
            latitude = lat,
            longitude = lon,
            zoneId = zoneId,
            method = method,
            asrMethod = asrMethod,
            highLatitudeRule = highLat,
            manualOffsets = offsets,
            isRamadan = selectedHijri.isRamadan,
            now = now,
            locationName = loc?.name ?: "Dhaka"
        )
        val selectedMilestones = SpiritualContent.computeDailyMilestones(
            fajr = selectedSchedule.entries.first { it.key == PrayerKey.FAJR }.time,
            sunrise = selectedSchedule.entries.first { it.key == PrayerKey.SUNRISE }.time,
            dhuhr = selectedSchedule.entries.first { it.key == PrayerKey.DHUHR }.time,
            maghrib = selectedSchedule.entries.first { it.key == PrayerKey.MAGHRIB }.time,
            nextFajr = nextDaySelectedSchedule.entries.first { it.key == PrayerKey.FAJR }.time
        )

        val upcoming = PrayerTimeCalculator.findUpcomingPrayer(
            latitude = lat,
            longitude = lon,
            zoneId = zoneId,
            method = method,
            asrMethod = asrMethod,
            highLatitudeRule = highLat,
            manualOffsets = offsets,
            isRamadan = hijriToday.isRamadan,
            now = now
        )

        val remaining = Duration.between(now, upcoming.time).coerceAtLeast(Duration.ZERO)
        val hrs = remaining.toHours()
        val mins = (remaining.toMinutes() % 60)
        val secs = (remaining.seconds % 60)
        val countdownStr = String.format("%02d:%02d:%02d", hrs, mins, secs)

        val prevTime = todaySchedule.previousPrayer?.time ?: now.minusHours(4)
        val totalWindow = Duration.between(prevTime, upcoming.time).seconds.coerceAtLeast(1L)
        val elapsed = Duration.between(prevTime, now).seconds.coerceIn(0L, totalWindow)
        val progress = (elapsed.toFloat() / totalWindow.toFloat()).coerceIn(0f, 1f)

        val timeFormatter = java.time.format.DateTimeFormatter.ofPattern("hh:mm a")
        val resolvedPlayingLabel = svcLabel ?: base.audios.firstOrNull { it.id == extra.previewId }?.title

        PriyoAzanUiState(
            isReady = true,
            settings = base.settings,
            location = loc,
            prayerPreferences = prefMap,
            todaySchedule = todaySchedule,
            selectedDateSchedule = selectedSchedule,
            selectedDate = trans.selectedDate,
            dailyMilestones = todayMilestones,
            selectedDateMilestones = selectedMilestones,
            dailyVerse = SpiritualContent.verseForDayOfYear(now.dayOfYear),
            upcomingPrayer = upcoming,
            countdownText = countdownStr,
            countdownProgress = progress,
            currentTimeFormatted = now.format(timeFormatter),
            hijriToday = hijriToday,
            daysToRamadan = HijriCalendarEngine.daysUntilRamadan(now.toLocalDate(), base.settings.hijriOffsetDays),
            qiblaInfo = QiblaCalculator.calculate(lat, lon),
            compassReading = extra.compass,
            azanAudioCatalog = base.audios,
            activeSchedules = base.schedules,
            azanStatus = trans.azanStatus,
            previewPlayingId = extra.previewId,
            isAzanServicePlaying = isSvcPlaying,
            playingAudioLabel = resolvedPlayingLabel,
            tasbih = tasbihState,
            surahs = quranRepo.getAllSurahs(),
            bookmarks = extra.bookmarks,
            quranProgress = extra.progress,
            updateHistory = extra.history,
            updateStatusMessage = trans.updateMsg,
            availableUpdateManifest = trans.manifest,
            downloadProgress = trans.downloadProgress,
            verifiedApkFile = locAndApk.first,
            locationDetecting = locAndApk.second,
            locationStatusMessage = locAndApk.third
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = PriyoAzanUiState(surahs = quranRepo.getAllSurahs())
    )

    init {
        viewModelScope.launch {
            repo.ensureInitialized()
            val loc = repo.getCurrentLocationSnapshot()
            if (loc != null) {
                compassManager.setCoordinates(loc.latitude, loc.longitude)
            }
            azanEngine.schedulePrayerAlarms()
            refreshAzanDiagnostics()
            maybeAutoCheckUpdates()
        }
    }

    fun refreshAzanDiagnostics() {
        viewModelScope.launch {
            azanStatusFlow.value = azanEngine.getAzanStatus()
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            repo.updateSettings { it.copy(onboardingCompleted = true) }
            azanEngine.rescheduleAllAlarms()
            refreshAzanDiagnostics()
        }
    }

    fun detectGpsLocation() {
        viewModelScope.launch {
            locationDetectingFlow.value = true
            locationStatusFlow.value = null
            val res = locationService.detectCurrentLocation()
            res.onSuccess { det ->
                repo.saveCurrentLocation(
                    name = det.name,
                    country = det.country,
                    latitude = det.latitude,
                    longitude = det.longitude,
                    timeZoneId = det.timeZoneId,
                    source = det.source
                )
                compassManager.setCoordinates(det.latitude, det.longitude)
                azanEngine.rescheduleAllAlarms()
                refreshAzanDiagnostics()
                locationStatusFlow.value = "${det.name}, ${det.country}".trimEnd(',', ' ')
            }.onFailure { err ->
                locationStatusFlow.value = err.message ?: "GPS unavailable"
            }
            locationDetectingFlow.value = false
        }
    }

    fun selectCityPreset(city: CityPreset) {
        viewModelScope.launch {
            repo.saveCurrentLocation(
                name = city.name,
                country = city.country,
                latitude = city.latitude,
                longitude = city.longitude,
                timeZoneId = city.timeZoneId,
                source = "CITY_PRESET"
            )
            compassManager.setCoordinates(city.latitude, city.longitude)
            azanEngine.rescheduleAllAlarms()
            refreshAzanDiagnostics()
            locationStatusFlow.value = "${city.name}, ${city.country}"
        }
    }

    fun setSelectedDate(date: LocalDate) {
        selectedDateFlow.value = date
    }

    fun updateCalculationMethod(method: CalculationMethod) {
        viewModelScope.launch {
            repo.updateSettings { it.copy(calculationMethod = method.name) }
            azanEngine.rescheduleAllAlarms()
            refreshAzanDiagnostics()
        }
    }

    fun updateAsrMethod(asr: AsrMethod) {
        viewModelScope.launch {
            repo.updateSettings { it.copy(asrMethod = asr.name) }
            azanEngine.rescheduleAllAlarms()
            refreshAzanDiagnostics()
        }
    }

    fun updateHighLatitudeRule(rule: HighLatitudeRule) {
        viewModelScope.launch {
            repo.updateSettings { it.copy(highLatitudeRule = rule.name) }
            azanEngine.rescheduleAllAlarms()
            refreshAzanDiagnostics()
        }
    }

    fun updateHijriOffset(offset: Int) {
        val clamped = offset.coerceIn(-2, 2)
        viewModelScope.launch {
            repo.updateSettings { it.copy(hijriOffsetDays = clamped) }
            azanEngine.rescheduleAllAlarms()
        }
    }

    fun updateThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            repo.updateSettings { it.copy(themeMode = mode.name) }
        }
    }

    fun updateLanguage(language: AppLanguage) {
        viewModelScope.launch {
            repo.updateSettings { it.copy(languageCode = language.code) }
        }
    }

    fun updateFontScale(option: FontScaleOption) {
        viewModelScope.launch {
            repo.updateSettings { it.copy(fontScaleOption = option.name) }
        }
    }

    fun updatePrayerPreference(
        key: PrayerKey,
        azanEnabled: Boolean? = null,
        notificationEnabled: Boolean? = null,
        soundMode: SoundMode? = null,
        manualOffsetMinutes: Int? = null
    ) {
        viewModelScope.launch {
            azanEngine.updateAzanSettings(
                prayerKey = key,
                azanEnabled = azanEnabled,
                notificationEnabled = notificationEnabled,
                soundMode = soundMode,
                manualOffsetMinutes = manualOffsetMinutes
            )
            refreshAzanDiagnostics()
        }
    }

    fun selectDefaultAudio(audioId: String, isFajr: Boolean) {
        viewModelScope.launch {
            repo.updateSettings {
                if (isFajr) it.copy(fajrAzanAudioId = audioId) else it.copy(fullAzanAudioId = audioId)
            }
            azanEngine.rescheduleAllAlarms()
            refreshAzanDiagnostics()
        }
    }

    fun previewAudio(audio: AzanAudioEntity) {
        if (azanEngine.previewPlayingId.value == audio.id) {
            azanEngine.stopPreview()
        } else {
            azanEngine.previewAudio(audio.id, audio.rawResName)
        }
    }

    fun stopPreview() {
        azanEngine.stopPreview()
    }

    fun triggerTestAzanVoice() {
        viewModelScope.launch {
            azanEngine.triggerTestAzanPlayback()
        }
    }

    fun stopAzanService() {
        azanEngine.stopPreview()
        azanEngine.stopActiveAzanService()
    }

    fun importCustomAzanUri(uri: android.net.Uri, title: String) {
        viewModelScope.launch {
            runCatching {
                app.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            repo.saveCustomAudio(title, uri.toString())
            azanEngine.rescheduleAllAlarms()
            refreshAzanDiagnostics()
        }
    }

    fun rescheduleAlarmsNow() {
        viewModelScope.launch {
            azanEngine.rescheduleAllAlarms()
            refreshAzanDiagnostics()
        }
    }

    fun openExactAlarmSettings() = azanEngine.openExactAlarmSettings()
    fun openBatteryOptimizationSettings() = azanEngine.openBatteryOptimizationSettings()

    fun startCompass() {
        val loc = uiState.value.location
        if (loc != null) {
            compassManager.setCoordinates(loc.latitude, loc.longitude)
        }
        compassManager.start()
    }

    fun stopCompass() {
        compassManager.stop()
    }

    fun tapTasbih() {
        val current = tasbihFlow.value
        val nextCount = current.count + 1
        if (nextCount >= current.target) {
            tasbihFlow.value = current.copy(count = 0, rounds = current.rounds + 1)
        } else {
            tasbihFlow.value = current.copy(count = nextCount)
        }
    }

    fun resetTasbih() {
        val current = tasbihFlow.value
        tasbihFlow.value = current.copy(count = 0, rounds = 0)
    }

    fun setTasbihTarget(target: Int) {
        val current = tasbihFlow.value
        tasbihFlow.value = current.copy(target = target.coerceIn(1, 1000), count = 0)
    }

    fun selectDhikr(index: Int) {
        val presets = SpiritualContent.dhikrPresets
        val safeIdx = index.coerceIn(0, presets.lastIndex)
        val preset = presets[safeIdx]
        tasbihFlow.value = TasbihState(
            selectedDhikrIndex = safeIdx,
            count = 0,
            target = preset.defaultTarget,
            rounds = 0
        )
    }

    fun toggleBookmark(surahNumber: Int, ayahNumber: Int, surahName: String, arabicSnippet: String) {
        val id = "$surahNumber:$ayahNumber"
        val currentlySaved = uiState.value.bookmarks.any { it.id == id }
        viewModelScope.launch {
            repo.toggleBookmark(surahNumber, ayahNumber, surahName, arabicSnippet, currentlySaved)
        }
    }

    fun saveQuranProgress(surahNumber: Int, ayahNumber: Int, surahName: String) {
        viewModelScope.launch {
            repo.saveQuranProgress(surahNumber, ayahNumber, surahName)
        }
    }

    fun dismissUpdateBanner() {
        availableManifestFlow.value = null
    }

    private suspend fun maybeAutoCheckUpdates() {
        val settings = repo.getSettingsSnapshot()
        val sixHoursMs = 6L * 60L * 60L * 1000L
        if (System.currentTimeMillis() - settings.lastUpdateCheckMillis < sixHoursMs) return
        if (!updateManager.isNetworkAvailable()) return
        checkUpdatesInternal(manual = false)
    }

    fun checkForUpdatesManually() {
        viewModelScope.launch {
            checkUpdatesInternal(manual = true)
        }
    }

    private suspend fun checkUpdatesInternal(manual: Boolean) {
        if (manual) updateStatusFlow.value = "CHECKING"
        val result = updateManager.checkForUpdates(
            owner = GitHubUpdateManager.OFFICIAL_GITHUB_OWNER,
            repo = GitHubUpdateManager.OFFICIAL_GITHUB_REPO
        )
        when (result) {
            is UpdateCheckResult.NoInternet -> {
                if (manual) updateStatusFlow.value = "NO_INTERNET"
            }
            is UpdateCheckResult.UpToDate -> {
                availableManifestFlow.value = null
                repo.updateSettings {
                    it.copy(
                        lastUpdateCheckMillis = System.currentTimeMillis(),
                        lastKnownLatestVersion = result.latestVersion
                    )
                }
                repo.recordUpdateCheck(result.latestVersion, 0, "UP_TO_DATE", "No newer release")
                if (manual) updateStatusFlow.value = "UP_TO_DATE:${result.latestVersion}"
            }
            is UpdateCheckResult.UpdateAvailable -> {
                repo.updateSettings {
                    it.copy(
                        lastUpdateCheckMillis = System.currentTimeMillis(),
                        lastKnownLatestVersion = result.manifest.version
                    )
                }
                repo.recordUpdateCheck(
                    result.manifest.version,
                    result.manifest.versionCode,
                    "UPDATE_AVAILABLE",
                    result.manifest.releaseNotes
                )
                availableManifestFlow.value = result.manifest
                updateStatusFlow.value = "AVAILABLE:${result.manifest.version}"
            }
            is UpdateCheckResult.Error -> {
                if (manual) {
                    updateStatusFlow.value = if (result.message == "REPO_OR_RELEASE_NOT_FOUND") {
                        "NOT_FOUND"
                    } else {
                        "ERROR:${result.message}"
                    }
                }
            }
        }
    }

    fun downloadVerifiedUpdate() {
        val manifest = availableManifestFlow.value ?: return
        viewModelScope.launch {
            updateManager.downloadAndVerifyApk(manifest).collect { prog ->
                downloadProgressFlow.value = prog
                if (prog is DownloadProgress.VerifiedReady) {
                    verifiedApkFlow.value = prog.apkFile
                    repo.recordUpdateCheck(
                        manifest.version,
                        manifest.versionCode,
                        "VERIFIED_SHA256",
                        "SHA-256 verified (${prog.sha256.take(12)}...)"
                    )
                }
            }
        }
    }

    fun installVerifiedApk(): Boolean {
        val file = verifiedApkFlow.value ?: return false
        if (!updateManager.canRequestPackageInstalls()) {
            updateManager.openUnknownAppSourcesSettings()
            return false
        }
        return updateManager.launchPackageInstaller(file)
    }

    fun canRequestPackageInstalls(): Boolean = updateManager.canRequestPackageInstalls()

    private data class BaseData(
        val settings: AppSettingsEntity,
        val location: LocationEntity?,
        val prayerPrefs: List<PrayerPreferenceEntity>,
        val audios: List<AzanAudioEntity>,
        val schedules: List<PrayerScheduleEntity>
    )

    private data class QuranAndExtra(
        val bookmarks: List<BookmarkEntity>,
        val progress: QuranProgressEntity?,
        val history: List<UpdateHistoryEntity>,
        val previewId: String?,
        val compass: CompassReading
    )

    private data class TransientState(
        val selectedDate: LocalDate,
        val azanStatus: AzanStatus,
        val updateMsg: String?,
        val manifest: UpdateManifest?,
        val downloadProgress: DownloadProgress?
    )
}
