package com.example.pinmind.core.database

import androidx.room.TypeConverter
import com.example.pinmind.domain.model.GeofenceTransitionType
import com.example.pinmind.domain.model.TaskPriority
import com.example.pinmind.domain.model.TaskStatus

/**
 * Room TypeConverters for persisting enums.
 */
class Converters {

    @TypeConverter
    fun fromTaskPriority(priority: TaskPriority): String = priority.name

    @TypeConverter
    fun toTaskPriority(value: String): TaskPriority = try {
        TaskPriority.valueOf(value)
    } catch (e: IllegalArgumentException) {
        TaskPriority.MEDIUM
    }

    @TypeConverter
    fun fromTaskStatus(status: TaskStatus): String = status.name

    @TypeConverter
    fun toTaskStatus(value: String): TaskStatus = try {
        TaskStatus.valueOf(value)
    } catch (e: IllegalArgumentException) {
        TaskStatus.ACTIVE
    }

    @TypeConverter
    fun fromGeofenceTransitionType(type: GeofenceTransitionType): String = type.name

    @TypeConverter
    fun toGeofenceTransitionType(value: String): GeofenceTransitionType = try {
        GeofenceTransitionType.valueOf(value)
    } catch (e: IllegalArgumentException) {
        GeofenceTransitionType.ENTER_OR_DWELL
    }
}
