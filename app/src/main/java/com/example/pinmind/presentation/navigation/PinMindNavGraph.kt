package com.example.pinmind.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.pinmind.presentation.createTask.CreateTaskScreen
import com.example.pinmind.presentation.home.HomeScreen
import com.example.pinmind.presentation.taskDetails.TaskDetailScreen

/**
 * Main NavHost configuring all application screen destinations.
 */
@Composable
fun PinMindNavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.Home.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToCreateTask = {
                    navController.navigate(Screen.CreateTask.route)
                },
                onNavigateToTaskDetails = { taskId ->
                    navController.navigate(Screen.TaskDetails.createRoute(taskId))
                },
                onNavigateToHistory = {
                    navController.navigate(Screen.History.route)
                }
            )
        }

        composable(Screen.History.route) {
            com.example.pinmind.presentation.history.HistoryScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToTaskDetails = { taskId ->
                    navController.navigate(Screen.TaskDetails.createRoute(taskId))
                }
            )
        }


        composable(Screen.CreateTask.route) {
            com.example.pinmind.presentation.createTask.AddTaskScreen(
                navController = navController,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToMapPicker = { lat, lng, radius ->
                    navController.navigate(Screen.MapPicker.createRoute(lat, lng, radius))
                }
            )
        }

        composable(
            route = Screen.EditTask.route,
            arguments = listOf(
                navArgument("taskId") { type = NavType.LongType }
            )
        ) {
            com.example.pinmind.presentation.createTask.EditTaskScreen(
                navController = navController,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToMapPicker = { lat, lng, radius ->
                    navController.navigate(Screen.MapPicker.createRoute(lat, lng, radius))
                }
            )
        }

        composable(
            route = Screen.TaskDetails.route,
            arguments = listOf(
                navArgument("taskId") { type = NavType.LongType }
            )
        ) {
            TaskDetailScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEditTask = { taskId ->
                    navController.navigate(Screen.EditTask.createRoute(taskId))
                }
            )
        }

        composable(
            route = Screen.MapPicker.route,
            arguments = listOf(
                navArgument("lat") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("lng") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("radius") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) {
            com.example.pinmind.presentation.map.PickLocationScreen(
                navController = navController,
                onNavigateBack = { navController.popBackStack() },
                onLocationConfirmed = { location ->
                    navController.previousBackStackEntry?.savedStateHandle?.apply {
                        set("address", location.address)
                        set("radius", location.radiusMeters)
                        set("location_name", location.locationName)
                        set("picked_address", location.address)
                        set("picked_radius", location.radiusMeters)
                        set("picked_name", location.locationName)
                        set("picked_lat", location.latitude)
                        set("picked_lng", location.longitude)
                        set("latitude", location.latitude)
                        set("longitude", location.longitude)
                    }
                    navController.popBackStack()
                }
            )
        }
    }
}

