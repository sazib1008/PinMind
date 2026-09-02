package com.example.pinmind.presentation.taskDetails

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.example.pinmind.core.location.FakeGeofenceController
import com.example.pinmind.data.repository.FakeTaskRepository
import com.example.pinmind.domain.model.Task
import com.example.pinmind.domain.model.TaskStatus
import com.example.pinmind.domain.usecase.DeleteTaskUseCase
import com.example.pinmind.domain.usecase.GetTaskByIdUseCase
import com.example.pinmind.domain.usecase.ToggleTaskStatusUseCase
import com.example.pinmind.util.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class TaskDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeRepository: FakeTaskRepository
    private lateinit var fakeGeofenceController: FakeGeofenceController
    private lateinit var getTaskByIdUseCase: GetTaskByIdUseCase
    private lateinit var toggleTaskStatusUseCase: ToggleTaskStatusUseCase
    private lateinit var deleteTaskUseCase: DeleteTaskUseCase

    @Before
    fun setup() {
        fakeRepository = FakeTaskRepository()
        fakeGeofenceController = FakeGeofenceController()
        getTaskByIdUseCase = GetTaskByIdUseCase(fakeRepository)
        toggleTaskStatusUseCase = ToggleTaskStatusUseCase(fakeRepository, fakeGeofenceController)
        deleteTaskUseCase = DeleteTaskUseCase(fakeRepository, fakeGeofenceController)
    }


    @Test
    fun `loads task by id on initialization`() = runTest {
        val task = Task(id = 10L, title = "Important Task")
        fakeRepository.insertTask(task)

        val viewModel = TaskDetailViewModel(
            savedStateHandle = SavedStateHandle(mapOf("taskId" to 10L)),
            getTaskByIdUseCase = getTaskByIdUseCase,
            toggleTaskStatusUseCase = toggleTaskStatusUseCase,
            deleteTaskUseCase = deleteTaskUseCase
        )

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("Important Task", state.task?.title)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `toggling status updates task to COMPLETED`() = runTest {
        val task = Task(id = 10L, title = "Important Task", status = TaskStatus.ACTIVE)
        fakeRepository.insertTask(task)

        val viewModel = TaskDetailViewModel(
            savedStateHandle = SavedStateHandle(mapOf("taskId" to 10L)),
            getTaskByIdUseCase = getTaskByIdUseCase,
            toggleTaskStatusUseCase = toggleTaskStatusUseCase,
            deleteTaskUseCase = deleteTaskUseCase
        )

        viewModel.uiState.test {
            val initial = awaitItem()
            assertEquals(TaskStatus.ACTIVE, initial.task?.status)

            viewModel.onToggleStatus()

            val updated = awaitItem()
            assertEquals(TaskStatus.COMPLETED, updated.task?.status)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `deleting task deletes it from repository and sets isDeleted to true`() = runTest {
        val task = Task(id = 10L, title = "Task to delete")
        fakeRepository.insertTask(task)

        val viewModel = TaskDetailViewModel(
            savedStateHandle = SavedStateHandle(mapOf("taskId" to 10L)),
            getTaskByIdUseCase = getTaskByIdUseCase,
            toggleTaskStatusUseCase = toggleTaskStatusUseCase,
            deleteTaskUseCase = deleteTaskUseCase
        )

        viewModel.uiState.test {
            val initial = awaitItem()
            assertEquals(10L, initial.task?.id)

            viewModel.onDeleteTask()

            val deletedState = awaitItem()
            assertTrue(deletedState.isDeleted)
            cancelAndIgnoreRemainingEvents()
        }

        assertNull(fakeRepository.getTaskByIdOnce(10L))
    }

}
