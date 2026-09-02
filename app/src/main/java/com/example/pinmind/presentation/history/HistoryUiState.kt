package com.example.pinmind.presentation.history

import com.example.pinmind.domain.model.Task

/**
 * UI State for the Task History / Completed screen.
 */
data class HistoryUiState(
    val isLoading: Boolean = true,
    val completedTasks: List<Task> = emptyList(),
    val searchQuery: String = "",
    val errorMessage: String? = null,
    val showClearDialog: Boolean = false
) {
    val filteredTasks: List<Task>
        get() = if (searchQuery.isBlank()) {
            completedTasks
        } else {
            completedTasks.filter { task ->
                task.title.contains(searchQuery, ignoreCase = true) ||
                        task.description.contains(searchQuery, ignoreCase = true) ||
                        task.category.contains(searchQuery, ignoreCase = true) ||
                        task.geoLocation?.locationName?.contains(searchQuery, ignoreCase = true) == true
            }
        }
}
