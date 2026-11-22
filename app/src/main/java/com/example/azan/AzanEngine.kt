package com.example.azan

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.example.R
import com.example.calculations.HijriCalendarEngine
import com.example.calculations.PrayerTimeCalculator
import com.example.database.PrayerPreferenceEntity
import com.example.database.PrayerScheduleEntity
import com.example.database.PriyoAzanRepository
import com.example.model.AsrMethod
import com.example.model.AzanStatus
import com.example.model.CalculationMethod
import com.example.model.HighLatitudeRule
import com.example.model.PrayerKey
import com.example.model.SoundMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * Native Kotlin Azan Engine implementing exact AlarmManager scheduling,
 * duplicate-alarm prevention via stable request codes, reboot recovery,
 * audio preview, and permission diagnostics.
 */
class AzanEngine(
    private val context: Context,
    private val repository: PriyoAzanRepository
) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private var previewPlayer: MediaPlayer? = null
    private val _previewPlayingId = MutableStateFlow<String?>(null)
    val previewPlayingId: StateFlow<String?> = _previewPlayingId.asStateFlow()

    init {
        ensureNotificationChannels()
    }

    fun ensureNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val alertChannel = NotificationChannel(
                CHANNEL_AZAN_ALERTS,
                context.getString(R.string.notif_channel_azan_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notif_channel_azan_desc)
                enableVibration(true)
            }
            val playbackChannel = NotificationChannel(
                CHANNEL_AZAN_PLAYBACK,
                context.getString(R.string.notif_channel_playback_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.notif_channel_playback_desc)
                setSound(null, null)
            }
            nm.createNotificationChannel(alertChannel)
            nm.createNotificationChannel(playbackChannel)
        }
    }

    suspend fun schedulePrayerAlarms(): List<PrayerScheduleEntity> {
        repository.ensureInitialized()
        val settings = repository.getSettingsSnapshot()
        val location = repository.getCurrentLocationSnapshot() ?: return emptyList()
        val prefs = repository.getPrayerPreferencesSnapshot().associateBy { PrayerKey.fromName(it.prayerKey) }

        val zoneId = runCatching { ZoneId.of(location.timeZoneId) }.getOrElse { ZoneId.systemDefault() }
        val now = ZonedDateTime.now(zoneId)
        val method = CalculationMethod.fromName(settings.calculationMethod)
        val asrMethod = AsrMethod.fromName(settings.asrMethod)
        val highLat = HighLatitudeRule.fromName(settings.highLatitudeRule)
        val offsets = prefs.mapValues { it.value.manualOffsetMinutes }
        val isRamadan = HijriCalendarEngine.toHijri(now.toLocalDate(), settings.hijriOffsetDays).isRamadan

        val todaySchedule = PrayerTimeCalculator.calculateForDate(
            date = now.toLocalDate(),
            latitude = location.latitude,
            longitude = location.longitude,
            zoneId = zoneId,
            method = method,
            asrMethod = asrMethod,
            highLatitudeRule = highLat,
            manualOffsets = offsets,
            isRamadan = isRamadan,
            now = now,
            locationName = location.name
        )

        val tomorrowDate = now.toLocalDate().plusDays(1)
        val tomorrowRamadan = HijriCalendarEngine.toHijri(tomorrowDate, settings.hijriOffsetDays).isRamadan
        val tomorrowSchedule = PrayerTimeCalculator.calculateForDate(
            date = tomorrowDate,
            latitude = location.latitude,
            longitude = location.longitude,
            zoneId = zoneId,
            method = method,
            asrMethod = asrMethod,
            highLatitudeRule = highLat,
            manualOffsets = offsets,
            isRamadan = tomorrowRamadan,
            now = now,
            locationName = location.name
        )

        val formatter = DateTimeFormatter.ofPattern("hh:mm a")
        val scheduledList = mutableListOf<PrayerScheduleEntity>()

        for (key in PrayerKey.entries) {
            val pref = prefs[key]
            val enabled = pref != null && (pref.azanEnabled || pref.notificationEnabled) &&
                SoundMode.fromName(pref.soundMode) != SoundMode.MUTED

            val targetEntry = todaySchedule.entries.firstOrNull { it.key == key && it.time.isAfter(now) }
                ?: tomorrowSchedule.entries.firstOrNull { it.key == key }

            if (!enabled || targetEntry == null) {
                cancelSingleAlarm(key)
                continue
            }

            val triggerMillis = targetEntry.time.toInstant().toEpochMilli()
            val audioId = when {
                SoundMode.fromName(pref.soundMode) == SoundMode.GENTLE_CHIME -> "gentle_chime"
                key == PrayerKey.FAJR -> settings.fajrAzanAudioId
                else -> settings.fullAzanAudioId
            }

            val pendingIntent = buildAlarmPendingIntent(
                prayerKey = key,
                soundMode = pref.soundMode,
                audioId = audioId,
                dateIso = targetEntry.time.toLocalDate().toString(),
                formattedTime = targetEntry.time.format(formatter)
            )

            scheduleExactOrInexact(triggerMillis, pendingIntent)

            scheduledList.add(
                PrayerScheduleEntity(
                    scheduleKey = "${targetEntry.time.toLocalDate()}_${key.name}",
                    dateIso = targetEntry.time.toLocalDate().toString(),
                    prayerKey = key.name,
                    triggerEpochMillis = triggerMillis,
                    formattedTime = targetEntry.time.format(formatter),
                    scheduledAlarmActive = true
                )
            )
        }

        repository.replaceActiveSchedules(scheduledList)
        return scheduledList
    }

    fun cancelPrayerAlarms() {
        for (key in PrayerKey.entries) {
            cancelSingleAlarm(key)
        }
    }

    suspend fun rescheduleAllAlarms(): List<PrayerScheduleEntity> {
        cancelPrayerAlarms()
        return schedulePrayerAlarms()
    }

    suspend fun getScheduledAlarms(): List<PrayerScheduleEntity> =
        repository.getActiveSchedulesSnapshot()

    suspend fun updateAzanSettings(
        prayerKey: PrayerKey,
        azanEnabled: Boolean? = null,
        notificationEnabled: Boolean? = null,
        soundMode: SoundMode? = null,
        manualOffsetMinutes: Int? = null
    ) {
        repository.updatePrayerPreference(prayerKey) { current ->
            current.copy(
                azanEnabled = azanEnabled ?: current.azanEnabled,
                notificationEnabled = notificationEnabled ?: current.notificationEnabled,
                soundMode = soundMode?.name ?: current.soundMode,
                manualOffsetMinutes = manualOffsetMinutes ?: current.manualOffsetMinutes
            )
        }
        rescheduleAllAlarms()
    }

    suspend fun getAzanStatus(): AzanStatus {
        val active = repository.getActiveSchedulesSnapshot()
            .filter { it.triggerEpochMillis > System.currentTimeMillis() }
            .sortedBy { it.triggerEpochMillis }
        val next = active.firstOrNull()
        return AzanStatus(
            exactAlarmAllowed = canScheduleExactAlarms(),
            notificationsAllowed = areNotificationsAllowed(),
            batteryOptimizationIgnored = isBatteryOptimizationIgnored(),
            scheduledCount = active.size,
            nextScheduledLabel = next?.let { "${it.prayerKey} • ${it.formattedTime}" },
            nextScheduledEpochMillis = next?.triggerEpochMillis
        )
    }

    fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    fun areNotificationsAllowed(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun isBatteryOptimizationIgnored(): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun openExactAlarmSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            runCatching { context.startActivity(intent) }
        }
    }

    fun openBatteryOptimizationSettings() {
        val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        runCatching { context.startActivity(intent) }
    }

    fun previewAudio(audioId: String, rawResOrUri: String) {
        stopPreview()
        val player = runCatching {
            MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                if (rawResOrUri.startsWith("content://") || rawResOrUri.startsWith("file://")) {
                    setDataSource(context, android.net.Uri.parse(rawResOrUri))
                } else {
                    val resId = context.resources.getIdentifier(rawResOrUri, "raw", context.packageName)
                        .let { if (it != 0) it else context.resources.getIdentifier("azan_mosque_classic", "raw", context.packageName) }
                    if (resId == 0) return@runCatching null
                    val afd = context.resources.openRawResourceFd(resId) ?: return@runCatching null
                    setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                    afd.close()
                }
                setOnCompletionListener {
                    _previewPlayingId.value = null
                    it.release()
                    if (previewPlayer === it) previewPlayer = null
                }
                prepare()
                start()
            }
        }.getOrNull() ?: return
        previewPlayer = player
        _previewPlayingId.value = audioId
    }

    suspend fun triggerTestAzanPlayback(prayerKey: PrayerKey = PrayerKey.DHUHR) {
        stopPreview()
        ensureNotificationChannels()
        val settings = repository.getSettingsSnapshot()
        val audioId = if (prayerKey == PrayerKey.FAJR) settings.fajrAzanAudioId else settings.fullAzanAudioId
        val audio = repository.getAudioById(audioId)
        val rawOrUri = audio?.rawResName ?: "azan_mosque_classic"
        val serviceIntent = Intent(context, AzanPlaybackService::class.java).apply {
            action = AzanPlaybackService.ACTION_START
            putExtra(EXTRA_PRAYER_KEY, prayerKey.name)
            putExtra(EXTRA_FORMATTED_TIME, "Azan Voice Test")
            putExtra(AzanPlaybackService.EXTRA_RAW_RES_NAME, rawOrUri)
        }
        ContextCompat.startForegroundService(context, serviceIntent)
    }

    fun stopPreview() {
        runCatching {
            previewPlayer?.stop()
            previewPlayer?.release()
        }
        previewPlayer = null
        _previewPlayingId.value = null
    }

    fun stopActiveAzanService() {
        val intent = Intent(context, AzanPlaybackService::class.java).apply {
            action = AzanPlaybackService.ACTION_STOP
        }
        runCatching { context.startService(intent) }
    }

    private fun scheduleExactOrInexact(triggerMillis: Long, pendingIntent: PendingIntent) {
        if (canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerMillis,
                pendingIntent
            )
        } else {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerMillis,
                pendingIntent
            )
        }
    }

    private fun cancelSingleAlarm(prayerKey: PrayerKey) {
        val intent = Intent(context, AzanAlarmReceiver::class.java)
        val pi = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_BASE + prayerKey.defaultOrder,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pi != null) {
            alarmManager.cancel(pi)
            pi.cancel()
        }
    }

    private fun buildAlarmPendingIntent(
        prayerKey: PrayerKey,
        soundMode: String,
        audioId: String,
        dateIso: String,
        formattedTime: String
    ): PendingIntent {
        val intent = Intent(context, AzanAlarmReceiver::class.java).apply {
            action = ACTION_PRAYER_ALARM
            putExtra(EXTRA_PRAYER_KEY, prayerKey.name)
            putExtra(EXTRA_SOUND_MODE, soundMode)
            putExtra(EXTRA_AUDIO_ID, audioId)
            putExtra(EXTRA_DATE_ISO, dateIso)
            putExtra(EXTRA_FORMATTED_TIME, formattedTime)
        }
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_BASE + prayerKey.defaultOrder,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        const val CHANNEL_AZAN_ALERTS = "priyo_azan_alerts"
        const val CHANNEL_AZAN_PLAYBACK = "priyo_azan_playback"
        const val ACTION_PRAYER_ALARM = "com.example.ACTION_PRAYER_ALARM"
        const val EXTRA_PRAYER_KEY = "extra_prayer_key"
        const val EXTRA_SOUND_MODE = "extra_sound_mode"
        const val EXTRA_AUDIO_ID = "extra_audio_id"
        const val EXTRA_DATE_ISO = "extra_date_iso"
        const val EXTRA_FORMATTED_TIME = "extra_formatted_time"
        private const val REQUEST_CODE_BASE = 4100
    }
}
