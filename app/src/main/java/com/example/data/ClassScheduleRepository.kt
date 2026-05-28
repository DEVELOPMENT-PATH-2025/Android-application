package com.example.data

import kotlinx.coroutines.flow.Flow

class ClassScheduleRepository(private val dao: ClassScheduleDao) {
    val allSchedules: Flow<List<ClassSchedule>> = dao.getAllSchedules()

    suspend fun getScheduleById(id: Int): ClassSchedule? {
        return dao.getScheduleById(id)
    }

    suspend fun insert(schedule: ClassSchedule): Long {
        return dao.insertSchedule(schedule)
    }

    suspend fun update(schedule: ClassSchedule) {
        dao.updateSchedule(schedule)
    }

    suspend fun delete(schedule: ClassSchedule) {
        dao.deleteSchedule(schedule)
    }
}
