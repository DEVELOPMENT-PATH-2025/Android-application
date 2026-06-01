package com.example.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.widget.Toast
import com.example.service.AlertService

class SnoozeReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "SnoozeReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val className = intent.getStringExtra("class_name") ?: "Class"
        val classNumber = intent.getStringExtra("class_number") ?: "N/A"
        val minutesBefore = intent.getIntExtra("minutes_before", 20)
        val durationSeconds = intent.getIntExtra("duration_seconds", 15)
        val snoozeMinutes = intent.getIntExtra("snooze_minutes", 5)
        val alertSound = intent.getStringExtra("alert_sound") ?: "Default Deep Pulse"

        Log.d(TAG, "Snoozing alarm for '$className' by $snoozeMinutes minutes. Sound: $alertSound")

        // 1. Stop the sounding AlertService
        try {
            val stopIntent = Intent(context, AlertService::class.java)
            context.stopService(stopIntent)
            
            // Also explicitly cancel the notification
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            notificationManager.cancel(4004) // NOTIFICATION_ID from AlertService
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping AlertService: ${e.message}")
        }

        if (snoozeMinutes <= 0) {
            Toast.makeText(context, "Alarm dismissed", Toast.LENGTH_SHORT).show()
            return
        }

        // 2. Schedule next Alarm in snoozeMinutes
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerTimeMs = System.currentTimeMillis() + (snoozeMinutes * 60 * 1000L)

        // Route this back to AlarmReceiver
        val alarmIntent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("class_id", -1) // -1 signifies a temporary/snoozed alarm (no recurring reschedule in AlarmReceiver)
            putExtra("class_name", "$className (Snoozed)")
            putExtra("class_number", classNumber)
            putExtra("minutes_before", minutesBefore)
            putExtra("duration_seconds", durationSeconds)
            putExtra("alert_sound", alertSound)
        }

        // Generating a unique request code for the PendingIntent using timestamp
        val requestCode = (System.currentTimeMillis() % 100000).toInt() + 9999

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getBroadcast(context, requestCode, alarmIntent, flags)

        try {
            val showIntent = Intent(context, com.example.MainActivity::class.java)
            val showPendingIntent = PendingIntent.getActivity(context, requestCode, showIntent, flags)
            val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerTimeMs, showPendingIntent)
            alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
            Toast.makeText(context, "Snoozed '$className' for $snoozeMinutes minutes", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTimeMs, pendingIntent)
            Log.e(TAG, "Failed to set exact snooze alarm: ${e.message}")
            Toast.makeText(context, "Snoozed '$className' (inexact)", Toast.LENGTH_SHORT).show()
        }
    }
}
