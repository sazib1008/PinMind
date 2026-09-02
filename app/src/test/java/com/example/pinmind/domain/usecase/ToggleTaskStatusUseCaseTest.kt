package com.example.pinmind.domain.usecase

import com.example.pinmind.core.location.FakeGeofenceController
import com.example.pinmind.data.repository.FakeTaskRepository
import com.example.pinmind.domain.model.Task
import com.example.pinmind.domain.model.TaskStatus
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ToggleTaskStatusUseCaseTest {

    private lateinit var fakeRepository: FakeTaskRepository
    private lateinit var fakeGeofenceController: FakeGeofenceController
    private lateinit var toggleTaskStatusUseCase: ToggleTaskStatusUseCase

    @Before
    fun setup() {
        fakeRepository = FakeTaskRepository()
        fakeGeofenceController = FakeGeofenceController()
        toggleTaskStatusUseCase = ToggleTaskStatusUseCase(fakeRepository, fakeGeofenceController)
    }


    @Test
    fun `toggling ACTIVE task changes status to COMPLETED and sets completedAt timestamp`() = runTest {
        val task = Task(id = 1L, title = "Active Task", status = TaskStatus.ACTIVE, completedAt = null)
        fakeRepository.insertTask(task)

        val result = toggleTaskStatusUseCase(task)

        assertTrue(result.isSuccess)
        val updated = result.getOrNull()
        assertEquals(TaskStatus.COMPLETED, updated?.status)
        assertNotNull(updated?.completedAt)

        val persisted = fakeRepository.getTaskByIdOnce(1L)
        assertEquals(TaskStatus.COMPLETED, persisted?.status)
        assertNotNull(persisted?.completedAt)
    }

    @Test
    fun `toggling COMPLETED task changes status to ACTIVE and clears completedAt timestamp`() = runTest {
        val task = Task(
            id = 2L,
            title = "Completed Task",
            status = TaskStatus.COMPLETED,
            completedAt = System.currentTimeMillis()
        )
        fakeRepository.insertTask(task)

        val result = toggleTaskStatusUseCase(task)

        assertTrue(result.isSuccess)
        val updated = result.getOrNull()
        assertEquals(TaskStatus.ACTIVE, updated?.status)
        assertNull(updated?.completedAt)

        val persisted = fakeRepository.getTaskByIdOnce(2L)
        assertEquals(TaskStatus.ACTIVE, persisted?.status)
        assertNull(persisted?.completedAt)
    }
}
