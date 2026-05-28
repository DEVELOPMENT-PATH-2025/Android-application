package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.ClassSchedule
import com.example.data.ClassScheduleRepository
import com.example.service.AlertService
import com.example.util.SchedulerHelper
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ClassScheduleViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: ClassScheduleRepository
    val allSchedules: StateFlow<List<ClassSchedule>>

    companion object {
        private const val TAG = "ClassScheduleViewModel"
    }

    init {
        val database = AppDatabase.getDatabase(application)
        repository = ClassScheduleRepository(database.classScheduleDao())
        allSchedules = repository.allSchedules.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    fun addSchedule(
        className: String,
        classNumber: String,
        startHour: Int,
        startMinute: Int,
        recurMonday: Boolean,
        recurTuesday: Boolean,
        recurWednesday: Boolean,
        recurThursday: Boolean,
        recurFriday: Boolean,
        recurSaturday: Boolean,
        recurSunday: Boolean,
        alertSound: String = "Default Deep Pulse",
        custom20MinEnabled: Boolean = true,
        custom10MinEnabled: Boolean = true
    ) {
        viewModelScope.launch {
            val newSchedule = ClassSchedule(
                className = className,
                classNumber = classNumber,
                startHour = startHour,
                startMinute = startMinute,
                recurMonday = recurMonday,
                recurTuesday = recurTuesday,
                recurWednesday = recurWednesday,
                recurThursday = recurThursday,
                recurFriday = recurFriday,
                recurSaturday = recurSaturday,
                recurSunday = recurSunday,
                isEnabled = true,
                alertSound = alertSound,
                custom20MinEnabled = custom20MinEnabled,
                custom10MinEnabled = custom10MinEnabled
            )
            val generatedId = repository.insert(newSchedule).toInt()
            val finalSchedule = newSchedule.copy(id = generatedId)
            SchedulerHelper.scheduleAlarmsForClass(getApplication(), finalSchedule)
            Log.d(TAG, "Added and scheduled class: $className, ID=$generatedId")
        }
    }

    fun updateSchedule(schedule: ClassSchedule) {
        viewModelScope.launch {
            // Cancel current alarms first
            SchedulerHelper.cancelAlarmsForClass(getApplication(), schedule)
            // Update entity
            repository.update(schedule)
            // Schedule new ones based on update
            if (schedule.isEnabled) {
                SchedulerHelper.scheduleAlarmsForClass(getApplication(), schedule)
            }
            Log.d(TAG, "Updated and rescheduled class ID=${schedule.id}")
        }
    }

    fun toggleSchedule(schedule: ClassSchedule, isEnabled: Boolean) {
        viewModelScope.launch {
            val updated = schedule.copy(isEnabled = isEnabled)
            repository.update(updated)
            if (isEnabled) {
                SchedulerHelper.scheduleAlarmsForClass(getApplication(), updated)
            } else {
                SchedulerHelper.cancelAlarmsForClass(getApplication(), updated)
            }
            Log.d(TAG, "Toggled class ID=${schedule.id} enabled=$isEnabled")
        }
    }

    fun deleteSchedule(schedule: ClassSchedule) {
        viewModelScope.launch {
            SchedulerHelper.cancelAlarmsForClass(getApplication(), schedule)
            repository.delete(schedule)
            Log.d(TAG, "Deleted class ID=${schedule.id}")
        }
    }

    // Triggers instant test sound play so user does not need to wait
    fun triggerInstantPreview(minutesBefore: Int, durationSeconds: Int, alertSound: String = "Default Deep Pulse") {
        val context = getApplication<Application>()
        val intent = Intent(context, AlertService::class.java).apply {
            putExtra("class_name", "Test Class (Preview)")
            putExtra("class_number", "Room 404")
            putExtra("minutes_before", minutesBefore)
            putExtra("duration_seconds", durationSeconds)
            putExtra("alert_sound", alertSound)
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            Log.d(TAG, "Triggered instant preview: -$minutesBefore mins, play for $durationSeconds seconds with sound: $alertSound")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting preview service: ${e.message}")
        }
    }
}
