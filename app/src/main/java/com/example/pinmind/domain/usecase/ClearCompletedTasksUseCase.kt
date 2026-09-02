package com.example.pinmind.domain.usecase

import com.example.pinmind.domain.repository.TaskRepository
import javax.inject.Inject

/**
 * Use case to delete all completed tasks from storage.
 */
class ClearCompletedTasksUseCase @Inject constructor(
    private val taskRepository: TaskRepository
) {
    /**
     * Clears all completed tasks.
     *
     * @return [Result] containing Unit on success or an exception on failure.
     */
    suspend operator fun invoke(): Result<Unit> {
        return try {
            taskRepository.clearCompletedTasks()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
