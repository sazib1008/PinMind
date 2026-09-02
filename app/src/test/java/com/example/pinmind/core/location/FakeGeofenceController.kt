package com.example.pinmind.core.location

import com.example.pinmind.domain.location.GeofenceController
import com.example.pinmind.domain.model.Task

/**
 * Fake in-memory implementation of [GeofenceController] for unit tests.
 */
class FakeGeofenceController : GeofenceController {

    val registeredTaskIds = mutableSetOf<Long>()

    override suspend fun registerGeofence(task: Task): Result<Unit> {
        registeredTaskIds.add(task.id)
        return Result.success(Unit)
    }

    override suspend fun registerGeofences(tasks: List<Task>): Result<Unit> {
        registeredTaskIds.addAll(tasks.map { it.id })
        return Result.success(Unit)
    }

    override suspend fun removeGeofence(taskId: Long): Result<Unit> {
        registeredTaskIds.remove(taskId)
        return Result.success(Unit)
    }

    override suspend fun removeGeofences(taskIds: List<Long>): Result<Unit> {
        registeredTaskIds.removeAll(taskIds.toSet())
        return Result.success(Unit)
    }

    override suspend fun removeAllGeofences(): Result<Unit> {
        registeredTaskIds.clear()
        return Result.success(Unit)
    }
}
