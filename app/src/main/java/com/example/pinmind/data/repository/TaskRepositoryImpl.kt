package com.example.pinmind.data.repository

import com.example.pinmind.data.local.TaskDao
import com.example.pinmind.data.mapper.toDomain
import com.example.pinmind.data.mapper.toEntity
import com.example.pinmind.domain.model.Task
import com.example.pinmind.domain.model.TaskStatus
import com.example.pinmind.domain.repository.TaskRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Concrete implementation of [TaskRepository] interfacing with the Room [TaskDao].
 */
class TaskRepositoryImpl @Inject constructor(
    private val taskDao: TaskDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : TaskRepository {

    override fun getAllTasks(): Flow<List<Task>> {
        return taskDao.getAllTasks()
            .map { it.toDomain() }
            .flowOn(ioDispatcher)
    }

    override fun getActiveTasks(): Flow<List<Task>> {
        return taskDao.getTasksByStatus(TaskStatus.ACTIVE)
            .map { it.toDomain() }
            .flowOn(ioDispatcher)
    }

    override fun getCompletedTasks(): Flow<List<Task>> {
        return taskDao.getTasksByStatus(TaskStatus.COMPLETED)
            .map { it.toDomain() }
            .flowOn(ioDispatcher)
    }

    override fun getTaskById(taskId: Long): Flow<Task?> {
        return taskDao.getTaskById(taskId)
            .map { it?.toDomain() }
            .flowOn(ioDispatcher)
    }

    override suspend fun getTaskByIdOnce(taskId: Long): Task? = withContext(ioDispatcher) {
        taskDao.getTaskByIdOnce(taskId)?.toDomain()
    }

    override suspend fun insertTask(task: Task): Long = withContext(ioDispatcher) {
        taskDao.insertTask(task.toEntity())
    }

    override suspend fun updateTask(task: Task) = withContext(ioDispatcher) {
        taskDao.updateTask(task.toEntity())
    }

    override suspend fun deleteTask(task: Task) = withContext(ioDispatcher) {
        taskDao.deleteTask(task.toEntity())
    }

    override suspend fun deleteTaskById(taskId: Long) = withContext(ioDispatcher) {
        taskDao.deleteTaskById(taskId)
    }

    override suspend fun getActiveTasksWithGeofence(): List<Task> = withContext(ioDispatcher) {
        taskDao.getActiveTasksWithGeofence().toDomain()
    }

    override suspend fun clearCompletedTasks() = withContext(ioDispatcher) {
        taskDao.clearCompletedTasks()
    }
}

