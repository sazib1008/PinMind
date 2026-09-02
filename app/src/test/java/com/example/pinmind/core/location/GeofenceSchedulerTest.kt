package com.example.pinmind.core.location

import com.example.pinmind.data.repository.FakeTaskRepository
import com.example.pinmind.domain.model.GeoLocation
import com.example.pinmind.domain.model.Task
import com.example.pinmind.domain.model.TaskStatus
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GeofenceSchedulerTest {

    private lateinit var fakeRepository: FakeTaskRepository
    private lateinit var fakeGeofenceController: FakeGeofenceController
    private lateinit var fakeLocationClient: FakeLocationClient
    private lateinit var scheduler: GeofenceScheduler

    @Before
    fun setup() {
        fakeRepository = FakeTaskRepository()
        fakeGeofenceController = FakeGeofenceController()
        fakeLocationClient = FakeLocationClient()
        scheduler = GeofenceScheduler(
            taskRepository = fakeRepository,
            geofenceController = fakeGeofenceController,
            locationClient = fakeLocationClient
        )
    }

    @Test
    fun `calculates Haversine distance accurately between two coordinates`() {
        // San Francisco (37.7749, -122.4194) to Oakland (37.8044, -122.2712) ~13.5 km (13500m)
        val distance = scheduler.calculateDistanceMeters(
            lat1 = 37.7749,
            lon1 = -122.4194,
            lat2 = 37.8044,
            lon2 = -122.2712
        )

        assertTrue("Expected distance ~13.5km, got: $distance", distance in 13000.0..14000.0)
    }

    @Test
    fun `scheduleClosestGeofences filters out tasks beyond 20km and registers close tasks`() = runTest {
        val userLocation = GeoLocation(latitude = 37.7749, longitude = -122.4194, radiusMeters = 100f) // San Francisco

        val closeTask = Task(
            id = 1L,
            title = "Close Task (Oakland)",
            status = TaskStatus.ACTIVE,
            geoLocation = GeoLocation(37.8044, -122.2712, 100f) // ~13km
        )
        val farTask = Task(
            id = 2L,
            title = "Far Task (Los Angeles)",
            status = TaskStatus.ACTIVE,
            geoLocation = GeoLocation(34.0522, -118.2437, 100f) // ~550km
        )

        fakeRepository.setTasks(listOf(farTask, closeTask))

        val result = scheduler.scheduleClosestGeofences(referenceLocation = userLocation)

        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull())
        assertTrue(fakeGeofenceController.registeredTaskIds.contains(1L))
        assertTrue(!fakeGeofenceController.registeredTaskIds.contains(2L))
    }

    @Test
    fun `scheduleClosestGeofences caps registered geofences to MAX_ACTIVE_GEOFENCES`() = runTest {
        val userLocation = GeoLocation(latitude = 37.7749, longitude = -122.4194, radiusMeters = 100f)

        // Create 60 active tasks with locations
        val tasks = (1L..60L).map { i ->
            Task(
                id = i,
                title = "Task $i",
                status = TaskStatus.ACTIVE,
                geoLocation = GeoLocation(37.7749 + (i * 0.001), -122.4194, 100f)
            )
        }
        fakeRepository.setTasks(tasks)

        val result = scheduler.scheduleClosestGeofences(referenceLocation = userLocation)

        assertTrue(result.isSuccess)
        assertEquals(GeofenceScheduler.MAX_ACTIVE_GEOFENCES, result.getOrNull())
        assertEquals(GeofenceScheduler.MAX_ACTIVE_GEOFENCES, fakeGeofenceController.registeredTaskIds.size)

        // Verifies the closest 50 tasks (IDs 1 through 50) were registered
        for (i in 1L..50L) {
            assertTrue("Task $i should be registered", fakeGeofenceController.registeredTaskIds.contains(i))
        }
        // Verifies the remaining tasks (51..60) were not registered
        for (i in 51L..60L) {
            assertTrue("Task $i should NOT be registered", !fakeGeofenceController.registeredTaskIds.contains(i))
        }
    }

    @Test
    fun `scheduleClosestGeofences removes all geofences when no active tasks exist`() = runTest {
        fakeGeofenceController.registeredTaskIds.addAll(listOf(1L, 2L, 3L))
        fakeRepository.setTasks(emptyList())

        val result = scheduler.scheduleClosestGeofences()

        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrNull())
        assertTrue(fakeGeofenceController.registeredTaskIds.isEmpty())
    }
}
