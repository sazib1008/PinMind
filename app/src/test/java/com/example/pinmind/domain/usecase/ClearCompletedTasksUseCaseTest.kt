package com.example.pinmind.domain.usecase

import com.example.pinmind.data.repository.FakeTaskRepository
import com.example.pinmind.domain.model.Task
import com.example.pinmind.domain.model.TaskStatus
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ClearCompletedTasksUseCaseTest {

    private lateinit var fakeRepository: FakeTaskRepository
    private lateinit var clearCompletedTasksUseCase: ClearCompletedTasksUseCase

    @Before
    fun setup() {
        fakeRepository = FakeTaskRepository()
        clearCompletedTasksUseCase = ClearCompletedTasksUseCase(fakeRepository)
    }

    @Test
    fun `clearing completed tasks removes all COMPLETED tasks while preserving ACTIVE tasks`() = runTest {
        val activeTask = Task(id = 1L, title = "Active Task", status = TaskStatus.ACTIVE)
        val completedTask1 = Task(id = 2L, title = "Completed Task 1", status = TaskStatus.COMPLETED)
        val completedTask2 = Task(id = 3L, title = "Completed Task 2", status = TaskStatus.COMPLETED)

        fakeRepository.setTasks(listOf(activeTask, completedTask1, completedTask2))

        val result = clearCompletedTasksUseCase()

        assertTrue(result.isSuccess)
        val remaining = fakeRepository.getTaskByIdOnce(1L)
        val removed1 = fakeRepository.getTaskByIdOnce(2L)
        val removed2 = fakeRepository.getTaskByIdOnce(3L)

        assertEquals("Active Task", remaining?.title)
        assertEquals(null, removed1)
        assertEquals(null, removed2)
    }
}
