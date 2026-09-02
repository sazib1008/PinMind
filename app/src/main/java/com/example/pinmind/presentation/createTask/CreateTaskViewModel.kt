package com.example.pinmind.presentation.createTask

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pinmind.domain.model.GeoLocation
import com.example.pinmind.domain.model.GeofenceTransitionType
import com.example.pinmind.domain.model.Task
import com.example.pinmind.domain.model.TaskPriority
import com.example.pinmind.domain.usecase.CreateTaskUseCase
import com.example.pinmind.domain.usecase.GetTaskByIdUseCase
import com.example.pinmind.domain.usecase.UpdateTaskUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel managing the creation or editing of a task.
 */
@HiltViewModel
class CreateTaskViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val createTaskUseCase: CreateTaskUseCase,
    private val updateTaskUseCase: UpdateTaskUseCase,
    private val getTaskByIdUseCase: GetTaskByIdUseCase
) : ViewModel() {

    private val taskId: Long? = savedStateHandle.get<Long>("taskId")?.takeIf { it > 0L }
    private val _uiState = MutableStateFlow(CreateTaskUiState(taskId = taskId))
    val uiState: StateFlow<CreateTaskUiState> = _uiState.asStateFlow()

    init {
        taskId?.let { id ->
            loadTask(id)
        }

        viewModelScope.launch {
            savedStateHandle.getStateFlow<Double?>("picked_lat", null).collect { lat ->
                val lng = savedStateHandle.get<Double>("picked_lng")
                val radius = savedStateHandle.get<Float>("picked_radius") ?: 100f
                val name = savedStateHandle.get<String>("picked_name") ?: ""
                val address = savedStateHandle.get<String>("picked_address")

                if (lat != null && lng != null) {
                    onLocationUpdated(
                        GeoLocation(
                            latitude = lat,
                            longitude = lng,
                            radiusMeters = radius,
                            locationName = name,
                            address = address
                        )
                    )
                }
            }
        }
    }


    private fun loadTask(id: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            getTaskByIdUseCase(id).collect { task ->
                if (task != null) {
                    _uiState.update { current ->
                        current.copy(
                            title = task.title,
                            description = task.description,
                            category = task.category,
                            priority = task.priority,
                            geoLocation = task.geoLocation,
                            transitionType = task.transitionType,
                            dwellTimeSeconds = task.dwellTimeSeconds,
                            isLoading = false
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Task not found") }
                }
            }
        }
    }

    fun onTitleChanged(title: String) {
        _uiState.update { it.copy(title = title, titleError = null) }
    }

    fun onDescriptionChanged(description: String) {
        _uiState.update { it.copy(description = description) }
    }

    fun onCategoryChanged(category: String) {
        _uiState.update { it.copy(category = category) }
    }

    fun onPriorityChanged(priority: TaskPriority) {
        _uiState.update { it.copy(priority = priority) }
    }

    fun onLocationUpdated(location: GeoLocation?) {
        _uiState.update { it.copy(geoLocation = location) }
    }

    fun onTransitionTypeChanged(type: GeofenceTransitionType) {
        _uiState.update { it.copy(transitionType = type) }
    }

    fun onDwellTimeChanged(seconds: Int) {
        _uiState.update { it.copy(dwellTimeSeconds = seconds) }
    }

    fun saveTask() {
        val state = _uiState.value
        if (state.title.isBlank()) {
            _uiState.update { it.copy(titleError = "Title cannot be empty") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val task = Task(
                id = state.taskId ?: 0L,
                title = state.title.trim(),
                description = state.description.trim(),
                category = state.category.trim().ifBlank { "General" },
                priority = state.priority,
                geoLocation = state.geoLocation,
                transitionType = state.transitionType,
                dwellTimeSeconds = state.dwellTimeSeconds
            )

            val result = if (state.isEditMode) {
                updateTaskUseCase(task)
            } else {
                createTaskUseCase(task).map { Unit }
            }

            if (result.isSuccess) {
                _uiState.update { it.copy(isLoading = false, isSaved = true) }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = result.exceptionOrNull()?.message ?: "Failed to save task"
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
