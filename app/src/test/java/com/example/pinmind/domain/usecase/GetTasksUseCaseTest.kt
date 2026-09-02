package com.example.pinmind.domain.usecase

import app.cash.turbine.test
import com.example.pinmind.data.repository.FakeTaskRepository
import com.example.pinmind.domain.model.Task
import com.example.pinmind.domain.model.TaskStatus
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class GetTasksUseCaseTest {

    private lateinit var fakeRepository: FakeTaskRepository
    private lateinit var getTasksUseCase: GetTasksUseCase

    @Before
    fun setup() {
        fakeRepository = FakeTaskRepository()
        getTasksUseCase = GetTasksUseCase(fakeRepository)
    }

    @Test
    fun `getTasks with ACTIVE filter returns only active tasks`() = runTest {
        val task1 = Task(id = 1L, title = "Task 1", status = TaskStatus.ACTIVE)
        val task2 = Task(id = 2L, title = "Task 2", status = TaskStatus.COMPLETED)
        val task3 = Task(id = 3L, title = "Task 3", status = TaskStatus.ACTIVE)

        fakeRepository.setTasks(listOf(task1, task2, task3))

        getTasksUseCase(TaskStatus.ACTIVE).test {
            val activeTasks = awaitItem()
            assertEquals(2, activeTasks.size)
            assertEquals(listOf(1L, 3L), activeTasks.map { it.id })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getTasks with COMPLETED filter returns only completed tasks`() = runTest {
        val task1 = Task(id = 1L, title = "Task 1", status = TaskStatus.ACTIVE)
        val task2 = Task(id = 2L, title = "Task 2", status = TaskStatus.COMPLETED)

        fakeRepository.setTasks(listOf(task1, task2))

        getTasksUseCase(TaskStatus.COMPLETED).test {
            val completedTasks = awaitItem()
            assertEquals(1, completedTasks.size)
            assertEquals(2L, completedTasks.first().id)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getTasks with null filter returns all tasks`() = runTest {
        val task1 = Task(id = 1L, title = "Task 1", status = TaskStatus.ACTIVE)
        val task2 = Task(id = 2L, title = "Task 2", status = TaskStatus.COMPLETED)

        fakeRepository.setTasks(listOf(task1, task2))

        getTasksUseCase(null).test {
            val allTasks = awaitItem()
            assertEquals(2, allTasks.size)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
