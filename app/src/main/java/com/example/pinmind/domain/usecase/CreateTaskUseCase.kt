package com.example.pinmind.domain.usecase

import com.example.pinmind.domain.location.GeofenceController
import com.example.pinmind.domain.model.Task
import com.example.pinmind.domain.repository.TaskRepository

import javax.inject.Inject

/**
 * Use case to create and persist a new task.
 */
class CreateTaskUseCase @Inject constructor(
    private val taskRepository: TaskRepository,
    private val geofenceController: GeofenceController
) {
    /**
     * Validates and creates a new task.
     *
     * @param task The task domain model to create.
     * @return [Result] containing the generated ID on success or an [IllegalArgumentException] on validation failure.
     */
    suspend operator fun invoke(task: Task): Result<Long> {
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
            val insertedId = taskRepository.insertTask(task)
            val savedTask = task.copy(id = insertedId)
            if (savedTask.hasActiveGeofence) {
                geofenceController.registerGeofence(savedTask)
            }
            Result.success(insertedId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

