package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.AppDatabase
import com.example.util.SchedulerHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || 
            intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            Log.d(TAG, "Device boot completed. Re-scheduling all active alarms...")
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getDatabase(context.applicationContext)
                    val schedules = db.classScheduleDao().getAllSchedules().first()
                    var count = 0
                    for (schedule in schedules) {
                        if (schedule.isEnabled) {
                            SchedulerHelper.scheduleAlarmsForClass(context.applicationContext, schedule)
                            count++
                        }
                    }
                    Log.d(TAG, "Successfully rescheduled $count active class alarms after reboot.")
                } catch (e: Exception) {
                    Log.e(TAG, "Error rescheduling alarms on boot: ${e.message}")
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
