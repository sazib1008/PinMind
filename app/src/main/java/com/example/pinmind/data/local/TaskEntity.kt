package com.example.pinmind.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.pinmind.domain.model.GeofenceTransitionType
import com.example.pinmind.domain.model.TaskPriority
import com.example.pinmind.domain.model.TaskStatus

/**
 * Room Entity representing a task in the local SQLite database.
 */
@Entity(
    tableName = "tasks",
    indices = [
        Index(value = ["status"]),
        Index(value = ["createdAt"]),
        Index(value = ["latitude", "longitude"])
    ]
)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val description: String,
    val category: String,
    val priority: TaskPriority,
    val status: TaskStatus,
    val latitude: Double?,
    val longitude: Double?,
    val radiusMeters: Float?,
    val locationName: String?,
    val address: String?,
    val transitionType: GeofenceTransitionType,
    val dwellTimeSeconds: Int,
    val dueDate: Long?,
    val createdAt: Long,
    val completedAt: Long?
)
