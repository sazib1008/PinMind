package com.example.pinmind.domain.usecase

import com.example.pinmind.domain.location.GeofenceController
import com.example.pinmind.domain.model.Task
import com.example.pinmind.domain.repository.TaskRepository
import javax.inject.Inject

/**
 * Use case to update an existing task.
 */
class UpdateTaskUseCase @Inject constructor(
    private val taskRepository: TaskRepository,
    private val geofenceController: GeofenceController
) {
    /**
     * Validates and updates a task.
     *
     * @param task The task domain model with updated values.
     * @return [Result] containing Unit on success or an error on failure.
     */
    suspend operator fun invoke(task: Task): Result<Unit> {
        if (task.title.isBlank()) {
            return Result.failure(IllegalArgumentException("Task title cannot be empty"))
        }

        task.geoLocation?.let { location ->
            if (location.radiusMeters <= 0f) {
                return Result.failure(IllegalArgumentException("Geofence radius must be greater than 0"))
            }
            if (location.latitude !in -90.0..90.0 || location.longitude !in -180.0..180.0) {
                return Result.failure(IllegalArgumentException("Invalid geographic coordinates"))
            }
        }

        return try {
            taskRepository.updateTask(task)
            if (task.hasActiveGeofence) {
                geofenceController.registerGeofence(task)
            } else {
                geofenceController.removeGeofence(task.id)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

