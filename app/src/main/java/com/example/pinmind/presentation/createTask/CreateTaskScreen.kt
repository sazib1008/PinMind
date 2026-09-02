package com.example.pinmind.presentation.createTask

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.navigation.NavController
import kotlinx.coroutines.flow.merge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.pinmind.R
import com.example.pinmind.domain.model.GeoLocation
import com.example.pinmind.domain.model.TaskPriority

/**
 * Screen for creating a new task or editing an existing task.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateTaskScreen(
    onNavigateBack: () -> Unit,
    onNavigateToMapPicker: (Double?, Double?, Float?) -> Unit,
    navController: NavController? = null,
    viewModel: CreateTaskViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Observe savedStateHandle changes from PickLocationScreen
    val currentBackStackEntry = navController?.currentBackStackEntry
    LaunchedEffect(currentBackStackEntry) {
        val handle = currentBackStackEntry?.savedStateHandle ?: return@LaunchedEffect
        merge(
            handle.getStateFlow<Double?>("latitude", null),
            handle.getStateFlow<Double?>("longitude", null),
            handle.getStateFlow<String?>("address", null),
            handle.getStateFlow<Double?>("picked_lat", null),
            handle.getStateFlow<Double?>("picked_lng", null),
            handle.getStateFlow<String?>("picked_address", null)
        ).collect {
            val lat = handle.get<Double>("latitude")
                ?: handle.get<Double>("picked_lat")
                ?: handle.get<String>("latitude")?.toDoubleOrNull()
                ?: handle.get<String>("picked_lat")?.toDoubleOrNull()

            val lng = handle.get<Double>("longitude")
                ?: handle.get<Double>("picked_lng")
                ?: handle.get<String>("longitude")?.toDoubleOrNull()
                ?: handle.get<String>("picked_lng")?.toDoubleOrNull()

            if (lat != null && lng != null) {
                val radius = handle.get<Float>("radius")
                    ?: handle.get<Float>("picked_radius")
                    ?: handle.get<Double>("radius")?.toFloat()
                    ?: handle.get<Double>("picked_radius")?.toFloat()
                    ?: 100f

                val address = handle.get<String>("address")
                    ?: handle.get<String>("picked_address")

                val name = handle.get<String>("location_name")
                    ?: handle.get<String>("picked_name")
                    ?: address
                    ?: "Selected Location"

                viewModel.updateLocation(
                    latitude = lat,
                    longitude = lng,
                    address = address,
                    radius = radius,
                    name = name
                )
            }
        }
    }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onNavigateBack()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(
                            if (uiState.isEditMode) R.string.nav_edit_task else R.string.nav_create_task
                        ),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Title Field
                OutlinedTextField(
                    value = uiState.title,
                    onValueChange = viewModel::onTitleChanged,
                    label = { Text(text = stringResource(R.string.field_title)) },
                    placeholder = { Text(text = stringResource(R.string.field_title_hint)) },
                    leadingIcon = {
                        Icon(imageVector = Icons.Filled.Title, contentDescription = null)
                    },
                    isError = uiState.titleError != null,
                    supportingText = uiState.titleError?.let { { Text(text = it) } },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Description Field
                OutlinedTextField(
                    value = uiState.description,
                    onValueChange = viewModel::onDescriptionChanged,
                    label = { Text(text = stringResource(R.string.field_description)) },
                    placeholder = { Text(text = stringResource(R.string.field_description_hint)) },
                    leadingIcon = {
                        Icon(imageVector = Icons.Filled.Description, contentDescription = null)
                    },
                    minLines = 3,
                    maxLines = 5,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Category Field with Quick Suggestions
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = uiState.category,
                        onValueChange = viewModel::onCategoryChanged,
                        label = { Text(text = stringResource(R.string.field_category)) },
                        leadingIcon = {
                            Icon(imageVector = Icons.Filled.Category, contentDescription = null)
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    val presetCategories = listOf("Errands", "Work", "Personal", "Health", "Shopping", "Home")
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        presetCategories.forEach { cat ->
                            FilterChip(
                                selected = uiState.category.equals(cat, ignoreCase = true),
                                onClick = { viewModel.onCategoryChanged(cat) },
                                label = { Text(text = cat, style = MaterialTheme.typography.labelSmall) },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }

                // Priority Selection
                Text(
                    text = stringResource(R.string.field_priority),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )


                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TaskPriority.entries.forEach { priority ->
                        val (labelRes, _) = when (priority) {
                            TaskPriority.LOW -> R.string.priority_low to 0
                            TaskPriority.MEDIUM -> R.string.priority_medium to 0
                            TaskPriority.HIGH -> R.string.priority_high to 0
                            TaskPriority.URGENT -> R.string.priority_urgent to 0
                        }
                        FilterChip(
                            selected = uiState.priority == priority,
                            onClick = { viewModel.onPriorityChanged(priority) },
                            label = { Text(text = stringResource(labelRes)) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Flag,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Location Reminder Section
                Text(
                    text = stringResource(R.string.field_location),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )

                val location = uiState.geoLocation
                if (location != null) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.LocationOn,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = location.locationName.ifBlank { "Selected Pin" },
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                        location.address?.let { addr ->
                                            Text(
                                                text = addr,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                            )
                                        }
                                    }
                                }

                                IconButton(onClick = { viewModel.onLocationUpdated(null) }) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = stringResource(R.string.action_remove_location)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "Radius: ${location.radiusMeters.toInt()} meters",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Slider(
                                value = location.radiusMeters,
                                onValueChange = { newRadius ->
                                    viewModel.onLocationUpdated(location.copy(radiusMeters = newRadius))
                                },
                                valueRange = 50f..1000f,
                                steps = 18
                            )

                            OutlinedButton(
                                onClick = {
                                    onNavigateToMapPicker(
                                        location.latitude,
                                        location.longitude,
                                        location.radiusMeters
                                    )
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(imageVector = Icons.Filled.Edit, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = stringResource(R.string.action_change_location))
                            }
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = { onNavigateToMapPicker(null, null, null) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Filled.LocationOn, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = stringResource(R.string.action_add_location))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Save Action Button
                Button(
                    onClick = viewModel::saveTask,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                ) {
                    Icon(imageVector = Icons.Filled.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(
                            if (uiState.isEditMode) R.string.action_update else R.string.action_save
                        ),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}

/**
 * AddTaskScreen for creating a new task, with savedStateHandle location listener.
 */
@Composable
fun AddTaskScreen(
    onNavigateBack: () -> Unit,
    onNavigateToMapPicker: (Double?, Double?, Float?) -> Unit,
    navController: NavController? = null,
    viewModel: CreateTaskViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    CreateTaskScreen(
        onNavigateBack = onNavigateBack,
        onNavigateToMapPicker = onNavigateToMapPicker,
        navController = navController,
        viewModel = viewModel,
        modifier = modifier
    )
}

/**
 * EditTaskScreen for editing an existing task, with savedStateHandle location listener.
 */
@Composable
fun EditTaskScreen(
    onNavigateBack: () -> Unit,
    onNavigateToMapPicker: (Double?, Double?, Float?) -> Unit,
    navController: NavController? = null,
    viewModel: CreateTaskViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    CreateTaskScreen(
        onNavigateBack = onNavigateBack,
        onNavigateToMapPicker = onNavigateToMapPicker,
        navController = navController,
        viewModel = viewModel,
        modifier = modifier
    )
}

