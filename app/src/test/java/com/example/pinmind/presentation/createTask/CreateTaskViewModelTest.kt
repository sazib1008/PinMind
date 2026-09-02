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
}
