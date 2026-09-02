package com.example.pinmind.domain.usecase

import com.example.pinmind.core.location.FakeGeofenceController
import com.example.pinmind.data.repository.FakeTaskRepository

import com.example.pinmind.domain.model.GeoLocation
import com.example.pinmind.domain.model.Task
import com.example.pinmind.domain.model.TaskPriority
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CreateTaskUseCaseTest {

    private lateinit var fakeRepository: FakeTaskRepository
    private lateinit var fakeGeofenceController: FakeGeofenceController
    private lateinit var createTaskUseCase: CreateTaskUseCase

    @Before
    fun setup() {
        fakeRepository = FakeTaskRepository()
        fakeGeofenceController = FakeGeofenceController()
        createTaskUseCase = CreateTaskUseCase(fakeRepository, fakeGeofenceController)
    }


    @Test
    fun `creating task with blank title returns failure`() = runTest {
        val task = Task(
            title = "   ",
            priority = TaskPriority.HIGH
        )

        val result = createTaskUseCase(task)

        assertTrue(result.isFailure)
        assertEquals("Task title cannot be empty", result.exceptionOrNull()?.message)
    }

    @Test
    fun `creating task with invalid geofence radius returns failure`() = runTest {
        val task = Task(
            title = "Buy Coffee",
            geoLocation = GeoLocation(
                latitude = 37.7749,
                longitude = -122.4194,
                radiusMeters = -10f
            )
        )

        val result = createTaskUseCase(task)

        assertTrue(result.isFailure)
        assertEquals("Geofence radius must be greater than 0", result.exceptionOrNull()?.message)
    }

    @Test
    fun `creating valid task persists successfully and returns generated id`() = runTest {
        val task = Task(
            title = "Pick up package",
            description = "At the post office",
            category = "Errands",
            priority = TaskPriority.HIGH,
            geoLocation = GeoLocation(
                latitude = 37.7749,
                longitude = -122.4194,
                radiusMeters = 150f,
                locationName = "Post Office"
            )
        )

        val result = createTaskUseCase(task)

        assertTrue(result.isSuccess)
        val generatedId = result.getOrNull()
        assertEquals(1L, generatedId)

        val persisted = fakeRepository.getTaskByIdOnce(1L)
        assertEquals("Pick up package", persisted?.title)
        assertEquals("Errands", persisted?.category)
        assertEquals(150f, persisted?.geoLocation?.radiusMeters)
    }
}
