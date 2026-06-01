package com.example.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.ClassSchedule
import com.example.receiver.AlarmReceiver
import java.util.Calendar

object SchedulerHelper {
    private const val TAG = "SchedulerHelper"

    fun scheduleAlarmsForClass(context: Context, schedule: ClassSchedule) {
        if (!schedule.isEnabled) {
            cancelAlarmsForClass(context, schedule)
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val daysOfWeek = mutableListOf<Int>()
        if (schedule.recurSunday) daysOfWeek.add(Calendar.SUNDAY)
        if (schedule.recurMonday) daysOfWeek.add(Calendar.MONDAY)
        if (schedule.recurTuesday) daysOfWeek.add(Calendar.TUESDAY)
        if (schedule.recurWednesday) daysOfWeek.add(Calendar.WEDNESDAY)
        if (schedule.recurThursday) daysOfWeek.add(Calendar.THURSDAY)
        if (schedule.recurFriday) daysOfWeek.add(Calendar.FRIDAY)
        if (schedule.recurSaturday) daysOfWeek.add(Calendar.SATURDAY)

        // If no days are selected, default to today if prior alert time is in the future, else tomorrow
        if (daysOfWeek.isEmpty()) {
            val now = Calendar.getInstance()
            val todayTarget20 = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, schedule.startHour)
                set(Calendar.MINUTE, schedule.startMinute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                add(Calendar.MINUTE, -20)
            }
            val targetDay = if (todayTarget20.after(now)) {
                now.get(Calendar.DAY_OF_WEEK)
            } else {
                val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
                cal.get(Calendar.DAY_OF_WEEK)
            }
            daysOfWeek.add(targetDay)
        }

        for (day in daysOfWeek) {
            if (schedule.custom20MinEnabled) {
                // Schedule 20 mins prior: alarmType=0, duration=15s
                scheduleAlarmAtOffset(context, alarmManager, schedule, day, 20, 15, 0)
            }

            if (schedule.custom10MinEnabled) {
                // Schedule 10 mins prior: alarmType=1, duration from schedule
                scheduleAlarmAtOffset(context, alarmManager, schedule, day, 10, schedule.customSoundDuration, 1)
            }
        }
    }

    private fun scheduleAlarmAtOffset(
        context: Context,
        alarmManager: AlarmManager,
        schedule: ClassSchedule,
        dayOfWeek: Int,
        minutesBefore: Int,
        soundDurationSeconds: Int,
        alarmType: Int
    ) {
        val triggerTimeMs = calculateAlarmTime(schedule.startHour, schedule.startMinute, dayOfWeek, minutesBefore)
        val requestCode = (schedule.id * 100) + (dayOfWeek * 10) + alarmType

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("class_id", schedule.id)
            putExtra("class_name", schedule.className)
            putExtra("class_number", schedule.classNumber)
            putExtra("minutes_before", minutesBefore)
            putExtra("duration_seconds", soundDurationSeconds)
            putExtra("alarm_type", alarmType)
            putExtra("day_of_week", dayOfWeek)
        }

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, flags)

        try {
            val showIntent = Intent(context, com.example.MainActivity::class.java)
            val showPendingIntent = PendingIntent.getActivity(context, requestCode, showIntent, flags)
            val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerTimeMs, showPendingIntent)
            alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
            
            Log.d(TAG, "Scheduled class alert: ID $requestCode, ${schedule.className} in -$minutesBefore mins at ${java.util.Date(triggerTimeMs)}")
        } catch (e: SecurityException) {
            // Fallback for strict OS constraints
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                triggerTimeMs,
                pendingIntent
            )
            Log.e(TAG, "Exact alarm permission restricted, falling back to inexact alarm: ${e.message}")
        }
    }

    fun cancelAlarmsForClass(context: Context, schedule: ClassSchedule) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val days = listOf(
            Calendar.SUNDAY, Calendar.MONDAY, Calendar.TUESDAY, 
            Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY
        )

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_NO_CREATE
        }

        for (day in days) {
            for (alarmType in 0..1) {
                val requestCode = (schedule.id * 100) + (day * 10) + alarmType
                val intent = Intent(context, AlarmReceiver::class.java)
                val pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, flags)
                if (pendingIntent != null) {
                    alarmManager.cancel(pendingIntent)
                    pendingIntent.cancel()
                }
            }
        }
        Log.d(TAG, "Cancelled all potential alarms for class schedule ID: ${schedule.id}")
    }

    fun calculateAlarmTime(startHour: Int, startMinute: Int, targetDayOfWeek: Int, minutesBefore: Int): Long {
        val now = Calendar.getInstance()
        val alarmCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, startHour)
            set(Calendar.MINUTE, startMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.MINUTE, -minutesBefore)
        }

        val currentDayOfWeek = now.get(Calendar.DAY_OF_WEEK)
        var dayDiff = targetDayOfWeek - currentDayOfWeek
        alarmCal.add(Calendar.DAY_OF_YEAR, dayDiff)

        // If the trigger time computed lands in the past, roll it over to next week
        if (alarmCal.before(now)) {
            alarmCal.add(Calendar.DAY_OF_YEAR, 7)
        }

        return alarmCal.timeInMillis
    }
}
