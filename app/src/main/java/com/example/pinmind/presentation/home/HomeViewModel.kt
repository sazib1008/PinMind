package com.example.pinmind.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pinmind.domain.model.Task
import com.example.pinmind.domain.model.TaskStatus
import com.example.pinmind.domain.usecase.DeleteTaskUseCase
import com.example.pinmind.domain.usecase.GetTasksUseCase
import com.example.pinmind.domain.usecase.ToggleTaskStatusUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel managing the state and user actions on the Home screen.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getTasksUseCase: GetTasksUseCase,
    private val toggleTaskStatusUseCase: ToggleTaskStatusUseCase,
    private val deleteTaskUseCase: DeleteTaskUseCase
) : ViewModel() {

    private val _selectedFilter = MutableStateFlow(TaskFilterTab.ACTIVE)
    private val _searchQuery = MutableStateFlow("")
    private val _errorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<HomeUiState> = combine(
        getTasksUseCase(),
        _selectedFilter,
        _searchQuery,
        _errorMessage
    ) { tasks, filter, query, error ->
        val filtered = tasks.filter { task ->
            val matchesFilter = when (filter) {
                TaskFilterTab.ALL -> true
                TaskFilterTab.ACTIVE -> task.status == TaskStatus.ACTIVE
                TaskFilterTab.COMPLETED -> task.status == TaskStatus.COMPLETED
            }
            val matchesSearch = query.isBlank() ||
                    task.title.contains(query, ignoreCase = true) ||
                    task.description.contains(query, ignoreCase = true) ||
                    task.category.contains(query, ignoreCase = true) ||
                    (task.geoLocation?.locationName?.contains(query, ignoreCase = true) == true)

            matchesFilter && matchesSearch
        }

        HomeUiState(
            isLoading = false,
            tasks = tasks,
            filteredTasks = filtered,
            selectedFilter = filter,
            searchQuery = query,
            errorMessage = error
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState(isLoading = true)
    )

    fun onFilterSelected(filter: TaskFilterTab) {
        _selectedFilter.value = filter
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onToggleTaskStatus(task: Task) {
        viewModelScope.launch {
            val result = toggleTaskStatusUseCase(task)
            if (result.isFailure) {
                _errorMessage.value = result.exceptionOrNull()?.message ?: "Failed to update task"
            }
        }
    }

    fun onDeleteTask(task: Task) {
        viewModelScope.launch {
            val result = deleteTaskUseCase(task)
            if (result.isFailure) {
                _errorMessage.value = result.exceptionOrNull()?.message ?: "Failed to delete task"
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
