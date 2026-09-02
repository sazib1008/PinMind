package com.example.pinmind.presentation.navigation

/**
 * Type-safe navigation routes for PinMind.
 */
sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object CreateTask : Screen("create_task")
    data object EditTask : Screen("edit_task/{taskId}") {
        fun createRoute(taskId: Long) = "edit_task/$taskId"
    }
    data object TaskDetails : Screen("task_details/{taskId}") {
        fun createRoute(taskId: Long) = "task_details/$taskId"
    }
    data object History : Screen("history")
    data object MapPicker : Screen("map_picker?lat={lat}&lng={lng}&radius={radius}") {
        fun createRoute(lat: Double? = null, lng: Double? = null, radius: Float? = null): String {
            return if (lat != null && lng != null) {
                "map_picker?lat=$lat&lng=$lng&radius=${radius ?: 100f}"
            } else {
                "map_picker"
            }
        }
    }
}
