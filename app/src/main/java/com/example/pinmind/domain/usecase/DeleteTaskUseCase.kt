package com.example.pinmind.domain.usecase

import com.example.pinmind.domain.location.GeofenceController
import com.example.pinmind.domain.model.Task
import com.example.pinmind.domain.repository.TaskRepository
import javax.inject.Inject

/**
 * Use case to delete a task and remove its active geofence.
 */
class DeleteTaskUseCase @Inject constructor(
    private val taskRepository: TaskRepository,
    private val geofenceController: GeofenceController
) {
    /**
     * Deletes the given task and unregisters its geofence.
     */
    suspend operator fun invoke(task: Task): Result<Unit> {
        return try {
            taskRepository.deleteTask(task)
            geofenceController.removeGeofence(task.id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Deletes the task identified by [taskId] and unregisters its geofence.
     */
    suspend operator fun invoke(taskId: Long): Result<Unit> {
        return try {
            taskRepository.deleteTaskById(taskId)
            geofenceController.removeGeofence(taskId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

