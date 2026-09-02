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
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel managing the creation or editing of a task.
 */
@HiltViewModel
class CreateTaskViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val createTaskUseCase: CreateTaskUseCase,
    private val updateTaskUseCase: UpdateTaskUseCase,
    private val getTaskByIdUseCase: GetTaskByIdUseCase
) : ViewModel() {

    private val taskId: Long? = savedStateHandle.get<Long>("taskId")?.takeIf { it > 0L }
    private val _uiState = MutableStateFlow(CreateTaskUiState(taskId = taskId))
    val uiState: StateFlow<CreateTaskUiState> = _uiState.asStateFlow()

    private var originalTask: Task? = null

    init {
        taskId?.let { id ->
            loadTask(id)
        }

        viewModelScope.launch {
            // Observe savedStateHandle for location updates passed back from MapPicker
            merge(
                savedStateHandle.getStateFlow<Double?>("latitude", null),
                savedStateHandle.getStateFlow<Double?>("longitude", null),
                savedStateHandle.getStateFlow<String?>("address", null),
                savedStateHandle.getStateFlow<Double?>("picked_lat", null),
                savedStateHandle.getStateFlow<Double?>("picked_lng", null),
                savedStateHandle.getStateFlow<String?>("picked_address", null)
            ).collect {
                extractLocationFromSavedStateHandle()?.let { location ->
                    onLocationUpdated(location)
                }
            }
        }
    }

    private fun extractLocationFromSavedStateHandle(): GeoLocation? {
        val lat = savedStateHandle.get<Double>("latitude")
            ?: savedStateHandle.get<Double>("picked_lat")
            ?: savedStateHandle.get<String>("latitude")?.toDoubleOrNull()
            ?: savedStateHandle.get<String>("picked_lat")?.toDoubleOrNull()

        val lng = savedStateHandle.get<Double>("longitude")
            ?: savedStateHandle.get<Double>("picked_lng")
            ?: savedStateHandle.get<String>("longitude")?.toDoubleOrNull()
            ?: savedStateHandle.get<String>("picked_lng")?.toDoubleOrNull()

        if (lat == null || lng == null) return null

        val radius = savedStateHandle.get<Float>("radius")
            ?: savedStateHandle.get<Float>("picked_radius")
            ?: savedStateHandle.get<Double>("radius")?.toFloat()
            ?: savedStateHandle.get<Double>("picked_radius")?.toFloat()
            ?: savedStateHandle.get<String>("radius")?.toFloatOrNull()
            ?: savedStateHandle.get<String>("picked_radius")?.toFloatOrNull()
            ?: _uiState.value.geoLocation?.radiusMeters
            ?: 100f

        val address = savedStateHandle.get<String>("address")
            ?: savedStateHandle.get<String>("picked_address")

        val name = savedStateHandle.get<String>("location_name")
            ?: savedStateHandle.get<String>("picked_name")
            ?: address
            ?: "Selected Location"

        return GeoLocation(
            latitude = lat,
            longitude = lng,
            radiusMeters = radius,
            locationName = name,
            address = address
        )
    }

    private fun loadTask(id: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            getTaskByIdUseCase(id).take(1).collect { task ->
                if (task != null) {
                    originalTask = task
                    _uiState.update { current ->
                        current.copy(
                            title = current.title.ifBlank { task.title },
                            description = current.description.ifBlank { task.description },
                            category = if (current.category == "General") task.category else current.category,
                            priority = task.priority,
                            geoLocation = current.geoLocation ?: task.geoLocation,
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

    fun updateLocation(
        latitude: Double,
        longitude: Double,
        address: String? = null,
        radius: Float = 100f,
        name: String? = null
    ) {
        onLocationUpdated(
            GeoLocation(
                latitude = latitude,
                longitude = longitude,
                radiusMeters = radius,
                locationName = name ?: address ?: "Selected Location",
                address = address
            )
        )
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

            val task = (originalTask ?: Task(id = state.taskId ?: 0L, title = state.title)).copy(
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
