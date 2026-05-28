package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.AppDatabase
import com.example.service.AlertService
import com.example.util.SchedulerHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "AlarmReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val classId = intent.getIntExtra("class_id", -1)
        val className = intent.getStringExtra("class_name") ?: "Class"
        val classNumber = intent.getStringExtra("class_number") ?: "N/A"
        val minutesBefore = intent.getIntExtra("minutes_before", 20) ?: 20
        val durationSeconds = intent.getIntExtra("duration_seconds", 15) ?: 15
        val alertSound = intent.getStringExtra("alert_sound")

        Log.d(TAG, "Alarm received: ID $classId, $className ($classNumber), starting in $minutesBefore mins")

        val serviceIntent = Intent(context, AlertService::class.java).apply {
            putExtra("class_name", className)
            putExtra("class_number", classNumber)
            putExtra("minutes_before", minutesBefore)
            putExtra("duration_seconds", durationSeconds)
            putExtra("alert_sound", alertSound ?: "Default Deep Pulse")
        }

        if (classId != -1) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getDatabase(context.applicationContext)
                    val schedule = db.classScheduleDao().getScheduleById(classId)
                    if (schedule != null) {
                        serviceIntent.putExtra("alert_sound", schedule.alertSound)
                    }

                    startAlertService(context, serviceIntent)

                    if (schedule != null && schedule.isEnabled) {
                        SchedulerHelper.scheduleAlarmsForClass(context.applicationContext, schedule)
                        Log.d(TAG, "Rescheduled schedule ID $classId for subsequent week occurrences")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Attempting to reschedule failed: ${e.message}")
                    startAlertService(context, serviceIntent)
                } finally {
                    pendingResult.finish()
                }
            }
        } else {
            // snooze trigger
            startAlertService(context, serviceIntent)
        }
    }

    private fun startAlertService(context: Context, serviceIntent: Intent) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed starting AlertService via foreground start: ${e.message}")
            try {
                context.startService(serviceIntent)
            } catch (ex: Exception) {
                Log.e(TAG, "Failed standard service start as fallback: ${ex.message}")
            }
        }
    }
}
