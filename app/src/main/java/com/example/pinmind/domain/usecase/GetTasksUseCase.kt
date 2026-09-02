package com.example.pinmind.domain.usecase

import com.example.pinmind.domain.model.Task
import com.example.pinmind.domain.model.TaskStatus
import com.example.pinmind.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case to retrieve tasks filtered by status (or all tasks).
 */
class GetTasksUseCase @Inject constructor(
    private val taskRepository: TaskRepository
) {
    /**
     * Observes tasks based on the given filter.
     *
     * @param filter Optional status filter (e.g. [TaskStatus.ACTIVE], [TaskStatus.COMPLETED]). If null, returns all tasks.
     */
    operator fun invoke(filter: TaskStatus? = null): Flow<List<Task>> {
        return when (filter) {
            TaskStatus.ACTIVE -> taskRepository.getActiveTasks()
            TaskStatus.COMPLETED, TaskStatus.ARCHIVED -> taskRepository.getCompletedTasks()
            null -> taskRepository.getAllTasks()
        }
    }
}
