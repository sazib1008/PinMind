package com.example.pinmind.presentation.taskDetails

import com.example.pinmind.domain.model.Task

/**
 * UI State for the Task Details screen.
 */
data class TaskDetailUiState(
    val isLoading: Boolean = true,
    val task: Task? = null,
    val isDeleted: Boolean = false,
    val errorMessage: String? = null
)
