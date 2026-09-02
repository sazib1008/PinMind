package com.example.pinmind.presentation.home

import com.example.pinmind.domain.model.Task
import com.example.pinmind.domain.model.TaskStatus

/**
 * Filter tab options for the Home screen.
 */
enum class TaskFilterTab {
    ALL,
    ACTIVE,
    COMPLETED
}

/**
 * UI State for the Home screen.
 */
data class HomeUiState(
    val isLoading: Boolean = true,
    val tasks: List<Task> = emptyList(),
    val filteredTasks: List<Task> = emptyList(),
    val selectedFilter: TaskFilterTab = TaskFilterTab.ACTIVE,
    val searchQuery: String = "",
    val errorMessage: String? = null
)
