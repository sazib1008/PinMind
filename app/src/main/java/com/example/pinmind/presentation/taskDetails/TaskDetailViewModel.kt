package com.example.pinmind.presentation.taskDetails

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pinmind.domain.usecase.DeleteTaskUseCase
import com.example.pinmind.domain.usecase.GetTaskByIdUseCase
import com.example.pinmind.domain.usecase.ToggleTaskStatusUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for viewing and managing a single task's details.
 */
@HiltViewModel
class TaskDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getTaskByIdUseCase: GetTaskByIdUseCase,
    private val toggleTaskStatusUseCase: ToggleTaskStatusUseCase,
    private val deleteTaskUseCase: DeleteTaskUseCase
) : ViewModel() {

    private val taskId: Long = checkNotNull(savedStateHandle.get<Long>("taskId"))
    private val _uiState = MutableStateFlow(TaskDetailUiState())
    val uiState: StateFlow<TaskDetailUiState> = _uiState.asStateFlow()

    init {
        loadTask()
    }

    private fun loadTask() {
        viewModelScope.launch {
            getTaskByIdUseCase(taskId).collect { task ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        task = task,
                        errorMessage = if (task == null && !it.isDeleted) "Task not found" else null
                    )
                }
            }
        }
    }

    fun onToggleStatus() {
        val currentTask = _uiState.value.task ?: return
        viewModelScope.launch {
            val result = toggleTaskStatusUseCase(currentTask)
            if (result.isFailure) {
                _uiState.update {
                    it.copy(errorMessage = result.exceptionOrNull()?.message ?: "Failed to update task")
                }
            }
        }
    }

    fun onDeleteTask() {
        val currentTask = _uiState.value.task ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = deleteTaskUseCase(currentTask)
            if (result.isSuccess) {
                _uiState.update { it.copy(isLoading = false, isDeleted = true) }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = result.exceptionOrNull()?.message ?: "Failed to delete task"
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
