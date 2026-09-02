package com.example.pinmind.presentation.history

import app.cash.turbine.test
import com.example.pinmind.core.location.FakeGeofenceController
import com.example.pinmind.data.repository.FakeTaskRepository
import com.example.pinmind.domain.model.Task
import com.example.pinmind.domain.model.TaskStatus
import com.example.pinmind.domain.usecase.ClearCompletedTasksUseCase
import com.example.pinmind.domain.usecase.DeleteTaskUseCase
import com.example.pinmind.domain.usecase.GetTasksUseCase
import com.example.pinmind.domain.usecase.ToggleTaskStatusUseCase
import com.example.pinmind.util.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class HistoryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeRepository: FakeTaskRepository
    private lateinit var fakeGeofenceController: FakeGeofenceController
    private lateinit var getTasksUseCase: GetTasksUseCase
    private lateinit var toggleTaskStatusUseCase: ToggleTaskStatusUseCase
    private lateinit var deleteTaskUseCase: DeleteTaskUseCase
    private lateinit var clearCompletedTasksUseCase: ClearCompletedTasksUseCase
    private lateinit var viewModel: HistoryViewModel

    @Before
    fun setup() {
        fakeRepository = FakeTaskRepository()
        fakeGeofenceController = FakeGeofenceController()
        getTasksUseCase = GetTasksUseCase(fakeRepository)
        toggleTaskStatusUseCase = ToggleTaskStatusUseCase(fakeRepository, fakeGeofenceController)
        deleteTaskUseCase = DeleteTaskUseCase(fakeRepository, fakeGeofenceController)
        clearCompletedTasksUseCase = ClearCompletedTasksUseCase(fakeRepository)

        val task1 = Task(id = 1L, title = "Active Task", status = TaskStatus.ACTIVE)
        val task2 = Task(id = 2L, title = "Completed Task 1", status = TaskStatus.COMPLETED)
        val task3 = Task(id = 3L, title = "Completed Task 2", status = TaskStatus.COMPLETED)
        fakeRepository.setTasks(listOf(task1, task2, task3))

        viewModel = HistoryViewModel(
            getTasksUseCase = getTasksUseCase,
            toggleTaskStatusUseCase = toggleTaskStatusUseCase,
            deleteTaskUseCase = deleteTaskUseCase,
            clearCompletedTasksUseCase = clearCompletedTasksUseCase
        )
    }

    @Test
    fun `initial uiState loads completed tasks only`() = runTest {
        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertEquals(2, state.completedTasks.size)
            assertTrue(state.completedTasks.all { it.status == TaskStatus.COMPLETED })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `searching queries filter history tasks`() = runTest {
        viewModel.onSearchQueryChanged("Task 1")

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(1, state.filteredTasks.size)
            assertEquals("Completed Task 1", state.filteredTasks.first().title)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `clearing all completed removes completed tasks and closes dialog`() = runTest {
        viewModel.showClearConfirmDialog(true)

        viewModel.uiState.test {
            val dialogState = awaitItem()
            assertTrue(dialogState.showClearDialog)
            cancelAndIgnoreRemainingEvents()
        }

        viewModel.onClearAllCompleted()

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.showClearDialog)
            assertEquals(0, state.completedTasks.size)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
