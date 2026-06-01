package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "class_schedules")
data class ClassSchedule(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val className: String,
    val classNumber: String,
    val startHour: Int,    // 0 to 23
    val startMinute: Int,  // 0 to 59
    val recurMonday: Boolean = false,
    val recurTuesday: Boolean = false,
    val recurWednesday: Boolean = false,
    val recurThursday: Boolean = false,
    val recurFriday: Boolean = false,
    val recurSaturday: Boolean = false,
    val recurSunday: Boolean = false,
    val isEnabled: Boolean = true,
    // Preferred alert sound: Default Deep Pulse, Beryl Radar Pulse, Chime Echo, Digital Warning Buzz, Huge Beep
    val alertSound: String = "Default Deep Pulse",
    // Custom settings
    val custom20MinEnabled: Boolean = true,
    val custom10MinEnabled: Boolean = true,
    val customSoundDuration: Int = 30
) {
    fun hasRecurrence(): Boolean {
        return recurMonday || recurTuesday || recurWednesday || recurThursday || 
               recurFriday || recurSaturday || recurSunday
    }

    fun getRecurrentDaysList(): List<String> {
        val days = mutableListOf<String>()
        if (recurMonday) days.add("Mon")
        if (recurTuesday) days.add("Tue")
        if (recurWednesday) days.add("Wed")
        if (recurThursday) days.add("Thu")
        if (recurFriday) days.add("Fri")
        if (recurSaturday) days.add("Sat")
        if (recurSunday) days.add("Sun")
        return days
    }
}
