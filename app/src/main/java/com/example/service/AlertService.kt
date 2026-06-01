package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.receiver.SnoozeReceiver
import kotlinx.coroutines.*

class AlertService : Service() {
    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Default + serviceJob)
    private var wakeLock: PowerManager.WakeLock? = null
    private var audioTrack: AudioTrack? = null

    companion object {
        private const val TAG = "AlertService"
        private const val NOTIFICATION_ID = 4004
        private const val CHANNEL_ID = "faculty_timer_channel"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "AlertService created")
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "FacultyTimer::AlertWakeLock").apply {
            acquire(5 * 60 * 1000L /* 5 minutes max */)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val className = intent?.getStringExtra("class_name") ?: "Class"
        val classNumber = intent?.getStringExtra("class_number") ?: "N/A"
        val minutesBefore = intent?.getIntExtra("minutes_before", 20) ?: 20
        val durationSeconds = intent?.getIntExtra("duration_seconds", 15) ?: 15
        val alertSound = intent?.getStringExtra("alert_sound") ?: "Default Deep Pulse"

        Log.d(TAG, "AlertService started: $className ($classNumber) starting in $minutesBefore mins, play for $durationSeconds s with sound: $alertSound")

        // Display Notification and enter Foreground Service state with Snooze Options
        showAndRunForegroundNotification(className, classNumber, minutesBefore, durationSeconds, alertSound)

        // Synthesize and play the preferred sound
        playDeepPulsingSound(durationSeconds, alertSound)

        return START_NOT_STICKY
    }

    private fun showAndRunForegroundNotification(
        className: String,
        classNumber: String,
        minutesBefore: Int,
        durationSeconds: Int,
        alertSound: String
    ) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Faculty Class Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Urgent warnings for upcoming classes"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val heading = "Class Starting in $minutesBefore Minutes!"
        val details = "Your class '$className' ($classNumber) starts soon. Sound '$alertSound' playing."

        // Snooze Intents
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val snooze5Intent = Intent(this, SnoozeReceiver::class.java).apply {
            putExtra("class_name", className)
            putExtra("class_number", classNumber)
            putExtra("minutes_before", minutesBefore)
            putExtra("duration_seconds", durationSeconds)
            putExtra("snooze_minutes", 5)
            putExtra("alert_sound", alertSound)
        }
        val pSnooze5 = PendingIntent.getBroadcast(this, 101, snooze5Intent, flags)

        val snooze10Intent = Intent(this, SnoozeReceiver::class.java).apply {
            putExtra("class_name", className)
            putExtra("class_number", classNumber)
            putExtra("minutes_before", minutesBefore)
            putExtra("duration_seconds", durationSeconds)
            putExtra("snooze_minutes", 10)
            putExtra("alert_sound", alertSound)
        }
        val pSnooze10 = PendingIntent.getBroadcast(this, 102, snooze10Intent, flags)

        val dismissIntent = Intent(this, SnoozeReceiver::class.java).apply {
            putExtra("class_name", className)
            putExtra("class_number", classNumber)
            putExtra("minutes_before", minutesBefore)
            putExtra("duration_seconds", durationSeconds)
            putExtra("snooze_minutes", 0) // 0 means dismiss
            putExtra("alert_sound", alertSound)
        }
        val pDismiss = PendingIntent.getBroadcast(this, 103, dismissIntent, flags)

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(heading)
            .setContentText(details)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .setAutoCancel(false)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Dismiss", pDismiss)
            .addAction(android.R.drawable.ic_lock_idle_alarm, "Snooze 5 Min", pSnooze5)
            .addAction(android.R.drawable.ic_lock_idle_alarm, "Snooze 10 Min", pSnooze10)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { 
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SHORT_SERVICE)
        } else if (Build.VERSION.SDK_INT >= 29) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun playDeepPulsingSound(durationSeconds: Int, alertSound: String) {
        serviceScope.launch {
            try {
                // Ensure previous instance is stopped
                stopAudioTrack()

                val sampleRate = 8000
                // 21 seconds gives a clean looping period for 0.7s, 1.0s, 0.3s, and 1.5s cycle arrays.
                val loopDurationSeconds = 21
                val totalSamples = loopDurationSeconds * sampleRate
                val bufferData = ShortArray(totalSamples)

                for (i in 0 until totalSamples) {
                    val timeSec = i.toDouble() / sampleRate
                    var volume = 1.0
                    var angle = 0.0

                    when (alertSound) {
                        "Beryl Radar Pulse" -> {
                            // Radar sweep: 0.4s sound, 0.3s silence; frequency sweeping from 300Hz to 500Hz
                            val cycle = timeSec % 0.7
                            val isSounding = cycle < 0.4
                            volume = if (isSounding) 1.0 else 0.0
                            val sweepFreq = 300.0 + (cycle * 500.0)
                            angle = 2.0 * Math.PI * sweepFreq * timeSec
                        }
                        "Chime Echo" -> {
                            // Chime double pulse: 0.15s chime, 0.1s silence, 0.15s chime, 0.6s silence; 600Hz frequency with decay
                            val cycle = timeSec % 1.0
                            val isSounding = (cycle < 0.15) || (cycle in 0.25..0.4)
                            volume = if (isSounding) {
                                val phase = cycle % 0.25
                                1.0 - (phase * 4.0)
                            } else {
                                0.0
                            }
                            angle = 2.0 * Math.PI * 600.0 * timeSec
                        }
                        "Digital Warning Buzz" -> {
                            // High intensity digital rapid beeping: 0.15s beep on, 0.15s off; 1000Hz frequency
                            val cycle = timeSec % 0.30
                            val isSounding = cycle < 0.15
                            volume = if (isSounding) 1.0 else 0.0
                            angle = 2.0 * Math.PI * 1000.0 * timeSec
                        }
                        "Huge Beep" -> {
                            // Extremely loud, persistent beep: 1.0s on, 0.5s off; 800Hz
                            val cycle = timeSec % 1.5
                            val isSounding = cycle < 1.0
                            volume = if (isSounding) 1.0 else 0.0
                            angle = 2.0 * Math.PI * 800.0 * timeSec
                        }
                        else -> { // Default Deep Pulse (140Hz)
                            val isSounding = (timeSec % 1.0) < 0.6
                            volume = if (isSounding) 1.0 else 0.0
                            angle = 2.0 * Math.PI * 140.0 * timeSec
                        }
                    }

                    bufferData[i] = (Math.sin(angle) * volume * Short.MAX_VALUE).toInt().toShort()
                }

                audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        android.media.AudioAttributes.Builder()
                            .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(bufferData.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build().apply {
                        write(bufferData, 0, bufferData.size)
                        setLoopPoints(0, bufferData.size, -1) // Loop infinitely
                        play()
                    }

                // Play for the configured duration in seconds
                delay((durationSeconds * 1000L) + 500L)
            } catch (e: Exception) {
                Log.e(TAG, "AudioTrack playing error: ${e.message}")
            } finally {
                stopSelf()
            }
        }
    }

    private fun stopAudioTrack() {
        try {
            audioTrack?.let {
                if (it.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    it.stop()
                }
                it.release()
            }
            audioTrack = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping audio track: ${e.message}")
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "AlertService destroyed")
        stopAudioTrack()
        serviceJob.cancel()
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
        super.onDestroy()
    }
}
