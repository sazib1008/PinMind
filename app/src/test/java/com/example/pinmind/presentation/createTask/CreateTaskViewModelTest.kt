package com.example.pinmind.presentation.createTask

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.example.pinmind.core.location.FakeGeofenceController
import com.example.pinmind.data.repository.FakeTaskRepository
import com.example.pinmind.domain.model.Task
import com.example.pinmind.domain.model.TaskPriority
import com.example.pinmind.domain.usecase.CreateTaskUseCase
import com.example.pinmind.domain.usecase.GetTaskByIdUseCase
import com.example.pinmind.domain.usecase.UpdateTaskUseCase
import com.example.pinmind.util.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class CreateTaskViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeRepository: FakeTaskRepository
    private lateinit var fakeGeofenceController: FakeGeofenceController
    private lateinit var createTaskUseCase: CreateTaskUseCase
    private lateinit var updateTaskUseCase: UpdateTaskUseCase
    private lateinit var getTaskByIdUseCase: GetTaskByIdUseCase

    @Before
    fun setup() {
        fakeRepository = FakeTaskRepository()
        fakeGeofenceController = FakeGeofenceController()
        createTaskUseCase = CreateTaskUseCase(fakeRepository, fakeGeofenceController)
        updateTaskUseCase = UpdateTaskUseCase(fakeRepository, fakeGeofenceController)
        getTaskByIdUseCase = GetTaskByIdUseCase(fakeRepository)
    }


    @Test
    fun `saving task with empty title sets titleError`() = runTest {
        val viewModel = CreateTaskViewModel(
            savedStateHandle = SavedStateHandle(),
            createTaskUseCase = createTaskUseCase,
            updateTaskUseCase = updateTaskUseCase,
            getTaskByIdUseCase = getTaskByIdUseCase
        )

        viewModel.saveTask()

        viewModel.uiState.test {
            val state = awaitItem()
            assertNotNull(state.titleError)
            assertEquals("Title cannot be empty", state.titleError)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `saving valid task creates task and sets isSaved to true`() = runTest {
        val viewModel = CreateTaskViewModel(
            savedStateHandle = SavedStateHandle(),
            createTaskUseCase = createTaskUseCase,
            updateTaskUseCase = updateTaskUseCase,
            getTaskByIdUseCase = getTaskByIdUseCase
        )

        viewModel.onTitleChanged("Doctor Appointment")
        viewModel.onDescriptionChanged("Bring medical records")
        viewModel.onCategoryChanged("Health")
        viewModel.onPriorityChanged(TaskPriority.HIGH)
        viewModel.saveTask()

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.isSaved)
            cancelAndIgnoreRemainingEvents()
        }

        val persisted = fakeRepository.getTaskByIdOnce(1L)
        assertEquals("Doctor Appointment", persisted?.title)
        assertEquals("Health", persisted?.category)
        assertEquals(TaskPriority.HIGH, persisted?.priority)
    }

    @Test
    fun `editing existing task loads details and updates successfully`() = runTest {
        val existingTask = Task(
            id = 42L,
            title = "Existing Title",
            description = "Existing Description",
            category = "Work"
        )
        fakeRepository.insertTask(existingTask)

        val viewModel = CreateTaskViewModel(
            savedStateHandle = SavedStateHandle(mapOf("taskId" to 42L)),
            createTaskUseCase = createTaskUseCase,
            updateTaskUseCase = updateTaskUseCase,
            getTaskByIdUseCase = getTaskByIdUseCase
        )

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("Existing Title", state.title)
            cancelAndIgnoreRemainingEvents()
        }

        viewModel.onTitleChanged("Updated Title")
        viewModel.saveTask()

        val updated = fakeRepository.getTaskByIdOnce(42L)
        assertEquals("Updated Title", updated?.title)
    }

    @Test
    fun `initial location in savedStateHandle updates task state with attached location`() = runTest {
        val savedStateHandle = SavedStateHandle(
            mapOf(
                "latitude" to 37.7749,
                "longitude" to -122.4194,
                "address" to "123 Market St, San Francisco",
                "radius" to 150f
            )
        )
        val viewModel = CreateTaskViewModel(
            savedStateHandle = savedStateHandle,
            createTaskUseCase = createTaskUseCase,
            updateTaskUseCase = updateTaskUseCase,
            getTaskByIdUseCase = getTaskByIdUseCase
        )

        viewModel.uiState.test {
            val state = awaitItem()
            assertNotNull(state.geoLocation)
            assertEquals(37.7749, state.geoLocation!!.latitude, 0.0001)
            assertEquals(-122.4194, state.geoLocation!!.longitude, 0.0001)
            assertEquals("123 Market St, San Francisco", state.geoLocation!!.address)
            assertEquals(150f, state.geoLocation!!.radiusMeters, 0.01f)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setting location in savedStateHandle dynamically updates task state with attached location`() = runTest {
        val savedStateHandle = SavedStateHandle()
        val viewModel = CreateTaskViewModel(
            savedStateHandle = savedStateHandle,
            createTaskUseCase = createTaskUseCase,
            updateTaskUseCase = updateTaskUseCase,
            getTaskByIdUseCase = getTaskByIdUseCase
        )

        viewModel.uiState.test {
            val initial = awaitItem()
            assertEquals(null, initial.geoLocation)

            // Simulate returning from PickLocationScreen where metadata and coordinates are set
            savedStateHandle["address"] = "New York, NY"
            savedStateHandle["latitude"] = 40.7128
            savedStateHandle["longitude"] = -74.0060

            testScheduler.runCurrent()

            val updated = awaitItem()
            assertNotNull(updated.geoLocation)
            assertEquals(40.7128, updated.geoLocation!!.latitude, 0.0001)
            assertEquals(-74.0060, updated.geoLocation!!.longitude, 0.0001)
            assertEquals("New York, NY", updated.geoLocation!!.address)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `saving edited task with attached location persists location to repository for Task Details`() = runTest {
        val existingTask = Task(
            id = 10L,
            title = "Office Errand",
            geoLocation = null
        )
        fakeRepository.insertTask(existingTask)

        val savedStateHandle = SavedStateHandle(mapOf("taskId" to 10L))
        val viewModel = CreateTaskViewModel(
            savedStateHandle = savedStateHandle,
            createTaskUseCase = createTaskUseCase,
            updateTaskUseCase = updateTaskUseCase,
            getTaskByIdUseCase = getTaskByIdUseCase
        )

        // Set location via savedStateHandle as done by PickLocationScreen
        savedStateHandle["latitude"] = 37.7749
        savedStateHandle["longitude"] = -122.4194
        savedStateHandle["address"] = "Market St, San Francisco"

        testScheduler.advanceUntilIdle()

        viewModel.saveTask()
        testScheduler.advanceUntilIdle()

        // Verify task persisted in repository has the attached location
        val persisted = fakeRepository.getTaskByIdOnce(10L)
        assertNotNull(persisted)
        assertNotNull(persisted?.geoLocation)
        assertEquals(37.7749, persisted!!.geoLocation!!.latitude, 0.0001)
        assertEquals(-122.4194, persisted.geoLocation!!.longitude, 0.0001)
        assertEquals("Market St, San Francisco", persisted.geoLocation!!.address)
    }
}

