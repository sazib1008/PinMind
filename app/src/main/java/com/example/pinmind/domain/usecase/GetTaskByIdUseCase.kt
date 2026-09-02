package com.example.pinmind.domain.usecase

import com.example.pinmind.domain.model.Task
import com.example.pinmind.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case to observe a specific task by its unique ID.
 */
class GetTaskByIdUseCase @Inject constructor(
    private val taskRepository: TaskRepository
) {
    /**
     * Returns a Flow emitting the task with the specified ID, or null if not found.
     */
    operator fun invoke(taskId: Long): Flow<Task?> {
        return taskRepository.getTaskById(taskId)
    }
}
