package com.example.pinmind.domain.model

/**
 * Pure domain model representing a task in PinMind.
 *
 * @property id Unique identifier for the task (0 for unpersisted tasks).
 * @property title Task title.
 * @property description Detailed task notes or instructions.
 * @property category Category or tag grouping (e.g. "Work", "Errands", "Personal").
 * @property priority Task priority level.
 * @property status Current lifecycle status of the task.
 * @property geoLocation Optional geographic point and radius for location triggering.
 * @property transitionType Geofence transition type (default ENTER_OR_DWELL).
 * @property dwellTimeSeconds Dwell duration in seconds before firing when inside the radius (default 60s).
 * @property dueDate Optional deadline timestamp (in epoch milliseconds).
 * @property createdAt Epoch timestamp when the task was created.
 * @property completedAt Epoch timestamp when the task was completed, if applicable.
 */
data class Task(
    val id: Long = 0L,
    val title: String,
    val description: String = "",
    val category: String = "General",
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val status: TaskStatus = TaskStatus.ACTIVE,
    val geoLocation: GeoLocation? = null,
    val transitionType: GeofenceTransitionType = GeofenceTransitionType.ENTER_OR_DWELL,
    val dwellTimeSeconds: Int = 60,
    val dueDate: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
) {
    /**
     * Whether this task has an active location attached and is in ACTIVE state.
     */
    val hasActiveGeofence: Boolean
        get() = status == TaskStatus.ACTIVE && geoLocation != null
}
