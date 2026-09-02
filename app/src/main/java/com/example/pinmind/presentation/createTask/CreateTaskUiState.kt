package com.example.pinmind.presentation.createTask

import com.example.pinmind.domain.model.GeoLocation
import com.example.pinmind.domain.model.GeofenceTransitionType
import com.example.pinmind.domain.model.TaskPriority

/**
 * UI State for the Create/Edit Task screen.
 */
data class CreateTaskUiState(
    val taskId: Long? = null,
    val title: String = "",
    val description: String = "",
    val category: String = "General",
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val geoLocation: GeoLocation? = null,
    val transitionType: GeofenceTransitionType = GeofenceTransitionType.ENTER_OR_DWELL,
    val dwellTimeSeconds: Int = 60,
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val titleError: String? = null,
    val errorMessage: String? = null
) {
    val isEditMode: Boolean
        get() = taskId != null && taskId > 0L
}
