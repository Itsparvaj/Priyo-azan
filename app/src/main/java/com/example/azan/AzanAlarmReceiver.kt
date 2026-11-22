package com.example.azan

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.database.PriyoAzanDatabase
import com.example.database.PriyoAzanRepository
import com.example.model.PrayerKey
import com.example.model.SoundMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AzanAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prayerKeyName = intent.getStringExtra(AzanEngine.EXTRA_PRAYER_KEY) ?: return
        val soundMode = SoundMode.fromName(
            intent.getStringExtra(AzanEngine.EXTRA_SOUND_MODE) ?: SoundMode.FULL_AZAN.name
        )
        val audioId = intent.getStringExtra(AzanEngine.EXTRA_AUDIO_ID) ?: "mosque_classic"
        val formattedTime = intent.getStringExtra(AzanEngine.EXTRA_FORMATTED_TIME) ?: ""

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repo = PriyoAzanRepository(PriyoAzanDatabase.getInstance(context))
                val engine = AzanEngine(context, repo)
                engine.ensureNotificationChannels()

                when (soundMode) {
                    SoundMode.FULL_AZAN, SoundMode.GENTLE_CHIME -> {
                        val audio = repo.getAudioById(audioId)
                        val rawName = audio?.rawResName ?: "azan_mosque_classic"
                        val serviceIntent = Intent(context, AzanPlaybackService::class.java).apply {
                            action = AzanPlaybackService.ACTION_START
                            putExtra(AzanEngine.EXTRA_PRAYER_KEY, prayerKeyName)
                            putExtra(AzanEngine.EXTRA_FORMATTED_TIME, formattedTime)
                            putExtra(AzanPlaybackService.EXTRA_RAW_RES_NAME, rawName)
                        }
                        ContextCompat.startForegroundService(context, serviceIntent)
                    }
                    SoundMode.NOTIFICATION_ONLY -> {
                        if (engine.areNotificationsAllowed()) {
                            postSilentNotification(context, prayerKeyName, formattedTime)
                        }
                    }
                    SoundMode.MUTED -> Unit
                }

                // Immediately schedule next day's occurrence for this and upcoming prayers
                engine.schedulePrayerAlarms()
            } catch (t: Throwable) {
                android.util.Log.e("AzanAlarmReceiver", "Failed to handle prayer alarm", t)
            } finally {
                pendingResult?.finish()
            }
        }
    }

    private fun postSilentNotification(context: Context, prayerKeyName: String, time: String) {
        val openAppIntent = Intent(context, MainActivity::class.java)
        val contentPi = PendingIntent.getActivity(
            context,
            2001,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val prayer = PrayerKey.fromName(prayerKeyName)
        val notification = NotificationCompat.Builder(context, AzanEngine.CHANNEL_AZAN_ALERTS)
            .setSmallIcon(R.drawable.priyo_azan_icon_fg)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText("${prayer.name.lowercase().replaceFirstChar { it.uppercase() }} • $time")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentPi)
            .build()
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(5000 + prayer.defaultOrder, notification)
    }
}
