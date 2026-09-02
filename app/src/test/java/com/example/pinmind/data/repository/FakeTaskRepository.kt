package com.example.pinmind.data.repository

import com.example.pinmind.domain.model.Task
import com.example.pinmind.domain.model.TaskStatus
import com.example.pinmind.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * In-memory fake implementation of [TaskRepository] for deterministic unit testing.
 */
class FakeTaskRepository : TaskRepository {

    private val tasksFlow = MutableStateFlow<List<Task>>(emptyList())
    private var nextId: Long = 1L

    override fun getAllTasks(): Flow<List<Task>> = tasksFlow

    override fun getActiveTasks(): Flow<List<Task>> {
        return tasksFlow.map { list -> list.filter { it.status == TaskStatus.ACTIVE } }
    }

    override fun getCompletedTasks(): Flow<List<Task>> {
        return tasksFlow.map { list -> list.filter { it.status == TaskStatus.COMPLETED } }
    }

    override fun getTaskById(taskId: Long): Flow<Task?> {
        return tasksFlow.map { list -> list.find { it.id == taskId } }
    }

    override suspend fun getTaskByIdOnce(taskId: Long): Task? {
        return tasksFlow.value.find { it.id == taskId }
    }

    override suspend fun insertTask(task: Task): Long {
        val assignedId = if (task.id == 0L) nextId++ else task.id
        val taskToInsert = task.copy(id = assignedId)
        val current = tasksFlow.value.toMutableList()
        current.removeAll { it.id == assignedId }
        current.add(0, taskToInsert)
        tasksFlow.value = current
        return assignedId
    }

    override suspend fun updateTask(task: Task) {
        val current = tasksFlow.value.toMutableList()
        val index = current.indexOfFirst { it.id == task.id }
        if (index != -1) {
            current[index] = task
            tasksFlow.value = current
        }
    }

    override suspend fun deleteTask(task: Task) {
        deleteTaskById(task.id)
    }

    override suspend fun deleteTaskById(taskId: Long) {
        val current = tasksFlow.value.toMutableList()
        current.removeAll { it.id == taskId }
        tasksFlow.value = current
    }

    override suspend fun getActiveTasksWithGeofence(): List<Task> {
        return tasksFlow.value.filter { it.hasActiveGeofence }
    }

    override suspend fun clearCompletedTasks() {
        val current = tasksFlow.value.toMutableList()
        current.removeAll { it.status == TaskStatus.COMPLETED }
        tasksFlow.value = current
    }

    fun setTasks(tasks: List<Task>) {
        tasksFlow.value = tasks
    }
}

