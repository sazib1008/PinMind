package com.example.pinmind.domain.usecase

import com.example.pinmind.domain.location.GeofenceController
import com.example.pinmind.domain.model.Task
import com.example.pinmind.domain.model.TaskStatus
import com.example.pinmind.domain.repository.TaskRepository
import javax.inject.Inject

/**
 * Use case to toggle a task's status between ACTIVE and COMPLETED.
 */
class ToggleTaskStatusUseCase @Inject constructor(
    private val taskRepository: TaskRepository,
    private val geofenceController: GeofenceController
) {
    /**
     * Toggles the task's completion status and manages geofence state.
     *
     * @param task The task to toggle.
     * @return [Result] containing the updated [Task] instance on success.
     */
    suspend operator fun invoke(task: Task): Result<Task> {
        val newStatus = if (task.status == TaskStatus.ACTIVE) {
            TaskStatus.COMPLETED
        } else {
            TaskStatus.ACTIVE
        }
        val completedAt = if (newStatus == TaskStatus.COMPLETED) {
            System.currentTimeMillis()
        } else {
            null
        }

        val updatedTask = task.copy(
            status = newStatus,
            completedAt = completedAt
        )

        return try {
            taskRepository.updateTask(updatedTask)
            if (updatedTask.hasActiveGeofence) {
                geofenceController.registerGeofence(updatedTask)
            } else {
                geofenceController.removeGeofence(updatedTask.id)
            }
            Result.success(updatedTask)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

