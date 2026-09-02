package com.example.pinmind.domain.location

import com.example.pinmind.domain.model.Task

/**
 * Domain-level interface for managing geofence registration lifecycle.
 * Zero Android dependencies.
 */
interface GeofenceController {

    /**
     * Registers a geofence for the specified task.
     */
    suspend fun registerGeofence(task: Task): Result<Unit>

    /**
     * Registers geofences for a batch of tasks.
     */
    suspend fun registerGeofences(tasks: List<Task>): Result<Unit>

    /**
     * Removes the geofence for the given task ID.
     */
    suspend fun removeGeofence(taskId: Long): Result<Unit>

    /**
     * Removes geofences for a list of task IDs.
     */
    suspend fun removeGeofences(taskIds: List<Long>): Result<Unit>

    /**
     * Clears all registered geofences.
     */
    suspend fun removeAllGeofences(): Result<Unit>
}
