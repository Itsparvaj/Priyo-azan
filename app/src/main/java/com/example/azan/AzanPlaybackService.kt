package com.example.azan

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R

/**
 * Foreground Service (mediaPlayback) for offline Azan audio playback using
 * proper Android AudioAttributes and AudioFocus, respecting device DND/volume settings.
 */
class AzanPlaybackService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private lateinit var audioManager: AudioManager

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopPlaybackAndSelf()
            return START_NOT_STICKY
        }

        val prayerName = intent?.getStringExtra(AzanEngine.EXTRA_PRAYER_KEY) ?: "Prayer"
        val formattedTime = intent?.getStringExtra(AzanEngine.EXTRA_FORMATTED_TIME) ?: ""
        val rawResName = intent?.getStringExtra(EXTRA_RAW_RES_NAME) ?: "azan_mosque_classic"

        _isPlaying.value = true
        _activeLabel.value = prayerName
        startForegroundNotification(prayerName, formattedTime)

        startLocalAudio(rawResName)
        return START_NOT_STICKY
    }

    private fun startForegroundNotification(prayerKey: String, formattedTime: String) {
        val openIntent = Intent(this, MainActivity::class.java)
        val openPi = PendingIntent.getActivity(
            this,
            3001,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = Intent(this, AzanPlaybackService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPi = PendingIntent.getService(
            this,
            3002,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val readablePrayer = prayerKey.lowercase().replaceFirstChar { it.uppercase() }
        val notification = NotificationCompat.Builder(this, AzanEngine.CHANNEL_AZAN_ALERTS)
            .setSmallIcon(R.drawable.priyo_azan_icon_fg)
            .setContentTitle("${getString(R.string.app_name)} • $readablePrayer")
            .setContentText(formattedTime.ifBlank { readablePrayer })
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
            .setContentIntent(openPi)
            .addAction(0, getString(R.string.stop_azan), stopPi)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun startLocalAudio(rawResOrUri: String) {
        stopMediaOnly()
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()

        val focusGranted = requestFocus(attrs)
        if (!focusGranted) {
            stopPlaybackAndSelf()
            return
        }

        val started = runCatching {
            val mp = MediaPlayer().apply {
                setAudioAttributes(attrs)
                if (rawResOrUri.startsWith("content://") || rawResOrUri.startsWith("file://")) {
                    setDataSource(applicationContext, android.net.Uri.parse(rawResOrUri))
                } else {
                    val resId = resources.getIdentifier(rawResOrUri, "raw", packageName)
                        .let { if (it != 0) it else resources.getIdentifier("azan_mosque_classic", "raw", packageName) }
                    if (resId == 0) throw IllegalStateException("Missing raw audio resource")
                    val afd = resources.openRawResourceFd(resId)
                    setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                    afd.close()
                }
                setOnCompletionListener { stopPlaybackAndSelf() }
                setOnErrorListener { _, _, _ ->
                    stopPlaybackAndSelf()
                    true
                }
                prepare()
                start()
            }
            mediaPlayer = mp
            true
        }.getOrDefault(false)

        if (!started) {
            // Fallback to bundled mosque Azan MP3 if custom URI fails
            if (rawResOrUri.startsWith("content://")) {
                startLocalAudio("azan_mosque_classic")
            } else {
                stopPlaybackAndSelf()
            }
        }
    }

    private fun requestFocus(attrs: AudioAttributes): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(attrs)
                .setOnAudioFocusChangeListener { change ->
                    if (change == AudioManager.AUDIOFOCUS_LOSS) {
                        stopPlaybackAndSelf()
                    }
                }
                .build()
            audioFocusRequest = req
            audioManager.requestAudioFocus(req) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                null,
                AudioManager.STREAM_ALARM,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
            ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
    }

    private fun stopMediaOnly() {
        runCatching {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        }
        mediaPlayer = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        }
    }

    private fun stopPlaybackAndSelf() {
        _isPlaying.value = false
        _activeLabel.value = null
        stopMediaOnly()
        stopForeground(STOP_FOREGROUND_REMOVE)
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(NOTIFICATION_ID)
        stopSelf()
    }

    override fun onDestroy() {
        _isPlaying.value = false
        _activeLabel.value = null
        stopMediaOnly()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START = "com.example.AZAN_PLAYBACK_START"
        const val ACTION_STOP = "com.example.AZAN_PLAYBACK_STOP"
        const val EXTRA_RAW_RES_NAME = "extra_raw_res_name"
        private const val NOTIFICATION_ID = 4901

        private val _isPlaying = kotlinx.coroutines.flow.MutableStateFlow(false)
        val isPlaying: kotlinx.coroutines.flow.StateFlow<Boolean> = _isPlaying

        private val _activeLabel = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
        val activeLabel: kotlinx.coroutines.flow.StateFlow<String?> = _activeLabel
    }
}
