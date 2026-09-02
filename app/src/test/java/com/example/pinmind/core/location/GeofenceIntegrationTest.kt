package com.example.pinmind.core.location

import com.example.pinmind.data.repository.FakeTaskRepository
import com.example.pinmind.domain.model.GeoLocation
import com.example.pinmind.domain.model.Task
import com.example.pinmind.domain.model.TaskStatus
import com.example.pinmind.domain.usecase.CreateTaskUseCase
import com.example.pinmind.domain.usecase.DeleteTaskUseCase
import com.example.pinmind.domain.usecase.ToggleTaskStatusUseCase
import com.example.pinmind.domain.usecase.UpdateTaskUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GeofenceIntegrationTest {

    private lateinit var fakeRepository: FakeTaskRepository
    private lateinit var fakeGeofenceController: FakeGeofenceController
    private lateinit var createTaskUseCase: CreateTaskUseCase
    private lateinit var updateTaskUseCase: UpdateTaskUseCase
    private lateinit var deleteTaskUseCase: DeleteTaskUseCase
    private lateinit var toggleTaskStatusUseCase: ToggleTaskStatusUseCase

    @Before
    fun setup() {
        fakeRepository = FakeTaskRepository()
        fakeGeofenceController = FakeGeofenceController()
        createTaskUseCase = CreateTaskUseCase(fakeRepository, fakeGeofenceController)
        updateTaskUseCase = UpdateTaskUseCase(fakeRepository, fakeGeofenceController)
        deleteTaskUseCase = DeleteTaskUseCase(fakeRepository, fakeGeofenceController)
        toggleTaskStatusUseCase = ToggleTaskStatusUseCase(fakeRepository, fakeGeofenceController)
    }

    @Test
    fun `creating task with active location registers geofence`() = runTest {
        val task = Task(
            title = "Supermarket",
            status = TaskStatus.ACTIVE,
            geoLocation = GeoLocation(37.7749, -122.4194, 150f, "Supermarket")
        )

        val result = createTaskUseCase(task)

        assertTrue(result.isSuccess)
        val taskId = result.getOrNull()!!
        assertTrue(fakeGeofenceController.registeredTaskIds.contains(taskId))
    }

    @Test
    fun `creating task without location does not register geofence`() = runTest {
        val task = Task(title = "Study Kotlin", status = TaskStatus.ACTIVE, geoLocation = null)

        val result = createTaskUseCase(task)

        assertTrue(result.isSuccess)
        val taskId = result.getOrNull()!!
        assertFalse(fakeGeofenceController.registeredTaskIds.contains(taskId))
    }

    @Test
    fun `completing task removes its active geofence`() = runTest {
        val task = Task(
            id = 5L,
            title = "Pharmacy",
            status = TaskStatus.ACTIVE,
            geoLocation = GeoLocation(37.7749, -122.4194, 100f)
        )
        fakeRepository.insertTask(task)
        fakeGeofenceController.registeredTaskIds.add(5L)

        toggleTaskStatusUseCase(task)

        assertFalse(fakeGeofenceController.registeredTaskIds.contains(5L))
    }

    @Test
    fun `deleting task removes its active geofence`() = runTest {
        val task = Task(
            id = 12L,
            title = "Hardware Store",
            status = TaskStatus.ACTIVE,
            geoLocation = GeoLocation(37.7749, -122.4194, 100f)
        )
        fakeRepository.insertTask(task)
        fakeGeofenceController.registeredTaskIds.add(12L)

        deleteTaskUseCase(task)

        assertFalse(fakeGeofenceController.registeredTaskIds.contains(12L))
    }
}
