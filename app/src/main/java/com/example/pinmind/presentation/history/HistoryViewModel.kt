package com.example.pinmind.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pinmind.domain.model.Task
import com.example.pinmind.domain.usecase.ClearCompletedTasksUseCase
import com.example.pinmind.domain.usecase.DeleteTaskUseCase
import com.example.pinmind.domain.usecase.GetTasksUseCase
import com.example.pinmind.domain.usecase.ToggleTaskStatusUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.example.pinmind.domain.model.TaskStatus

/**
 * ViewModel managing the Task History screen state and actions.
 */
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val getTasksUseCase: GetTasksUseCase,
    private val toggleTaskStatusUseCase: ToggleTaskStatusUseCase,
    private val deleteTaskUseCase: DeleteTaskUseCase,
    private val clearCompletedTasksUseCase: ClearCompletedTasksUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        loadCompletedTasks()
    }

    private fun loadCompletedTasks() {
        viewModelScope.launch {
            getTasksUseCase(TaskStatus.COMPLETED)
                .catch { error ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = error.message) }
                }
                .collect { tasks ->
                    _uiState.update { it.copy(isLoading = false, completedTasks = tasks) }
                }
        }
    }


    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun onToggleTaskStatus(task: Task) {
        viewModelScope.launch {
            toggleTaskStatusUseCase(task)
        }
    }

    fun onDeleteTask(taskId: Long) {
        viewModelScope.launch {
            deleteTaskUseCase(taskId)
        }
    }

    fun showClearConfirmDialog(show: Boolean) {
        _uiState.update { it.copy(showClearDialog = show) }
    }

    fun onClearAllCompleted() {
        viewModelScope.launch {
            clearCompletedTasksUseCase()
            _uiState.update { it.copy(showClearDialog = false) }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
