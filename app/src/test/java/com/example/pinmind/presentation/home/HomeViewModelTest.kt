package com.example.pinmind.presentation.home

import app.cash.turbine.test
import com.example.pinmind.core.location.FakeGeofenceController
import com.example.pinmind.data.repository.FakeTaskRepository
import com.example.pinmind.domain.model.Task
import com.example.pinmind.domain.model.TaskStatus
import com.example.pinmind.domain.usecase.DeleteTaskUseCase
import com.example.pinmind.domain.usecase.GetTasksUseCase
import com.example.pinmind.domain.usecase.ToggleTaskStatusUseCase
import com.example.pinmind.util.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeRepository: FakeTaskRepository
    private lateinit var fakeGeofenceController: FakeGeofenceController
    private lateinit var getTasksUseCase: GetTasksUseCase
    private lateinit var toggleTaskStatusUseCase: ToggleTaskStatusUseCase
    private lateinit var deleteTaskUseCase: DeleteTaskUseCase
    private lateinit var viewModel: HomeViewModel

    @Before
    fun setup() {
        fakeRepository = FakeTaskRepository()
        fakeGeofenceController = FakeGeofenceController()
        getTasksUseCase = GetTasksUseCase(fakeRepository)
        toggleTaskStatusUseCase = ToggleTaskStatusUseCase(fakeRepository, fakeGeofenceController)
        deleteTaskUseCase = DeleteTaskUseCase(fakeRepository, fakeGeofenceController)

        viewModel = HomeViewModel(
            getTasksUseCase = getTasksUseCase,
            toggleTaskStatusUseCase = toggleTaskStatusUseCase,
            deleteTaskUseCase = deleteTaskUseCase
        )
    }


    @Test
    fun `initial uiState loads active tasks by default`() = runTest {
        val task1 = Task(id = 1L, title = "Task 1", status = TaskStatus.ACTIVE)
        val task2 = Task(id = 2L, title = "Task 2", status = TaskStatus.COMPLETED)
        fakeRepository.setTasks(listOf(task1, task2))

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertEquals(TaskFilterTab.ACTIVE, state.selectedFilter)
            assertEquals(1, state.filteredTasks.size)
            assertEquals(1L, state.filteredTasks.first().id)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `switching filter to COMPLETED updates filteredTasks accordingly`() = runTest {
        val task1 = Task(id = 1L, title = "Task 1", status = TaskStatus.ACTIVE)
        val task2 = Task(id = 2L, title = "Task 2", status = TaskStatus.COMPLETED)
        fakeRepository.setTasks(listOf(task1, task2))

        viewModel.onFilterSelected(TaskFilterTab.COMPLETED)

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(TaskFilterTab.COMPLETED, state.selectedFilter)
            assertEquals(1, state.filteredTasks.size)
            assertEquals(2L, state.filteredTasks.first().id)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `searching queries filter tasks by title and description`() = runTest {
        val task1 = Task(id = 1L, title = "Buy Milk", description = "Whole milk", status = TaskStatus.ACTIVE)
        val task2 = Task(id = 2L, title = "Call Dentist", description = "Appointment", status = TaskStatus.ACTIVE)
        fakeRepository.setTasks(listOf(task1, task2))

        viewModel.onSearchQueryChanged("Milk")

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("Milk", state.searchQuery)
            assertEquals(1, state.filteredTasks.size)
            assertEquals("Buy Milk", state.filteredTasks.first().title)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
