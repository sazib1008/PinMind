package com.example.pinmind.domain.repository

import com.example.pinmind.domain.model.Task
import kotlinx.coroutines.flow.Flow

/**
 * Domain-level abstraction for managing tasks.
 * Zero Android dependencies.
 */
interface TaskRepository {

    /**
     * Observes all tasks sorted by creation date descending.
     */
    fun getAllTasks(): Flow<List<Task>>

    /**
     * Observes all active tasks.
     */
    fun getActiveTasks(): Flow<List<Task>>

    /**
     * Observes completed / archived tasks.
     */
    fun getCompletedTasks(): Flow<List<Task>>

    /**
     * Observes a specific task by ID.
     */
    fun getTaskById(taskId: Long): Flow<Task?>

    /**
     * Retrieves a specific task snapshot by ID once.
     */
    suspend fun getTaskByIdOnce(taskId: Long): Task?

    /**
     * Inserts a new task and returns its generated ID.
     */
    suspend fun insertTask(task: Task): Long

    /**
     * Updates an existing task.
     */
    suspend fun updateTask(task: Task)

    /**
     * Deletes a task.
     */
    suspend fun deleteTask(task: Task)

    /**
     * Deletes a task by ID.
     */
    suspend fun deleteTaskById(taskId: Long)

    /**
     * Retrieves all active tasks that have a location configured (used for geofence registration).
     */
    suspend fun getActiveTasksWithGeofence(): List<Task>

    /**
     * Deletes all tasks with status COMPLETED from the database.
     */
    suspend fun clearCompletedTasks()
}

