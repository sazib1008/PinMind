package com.example.pinmind.presentation.map

import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import com.example.pinmind.core.notification.NotificationPermissionHelper
import com.example.pinmind.presentation.permission.NotificationPermissionRationaleDialog
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.pinmind.R
import com.example.pinmind.core.location.LocationPermissionHelper
import com.example.pinmind.core.location.LocationPermissionState
import com.example.pinmind.domain.model.GeoLocation
import com.example.pinmind.presentation.permission.LocationPermissionFlow
import androidx.navigation.NavController
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.ITileSource
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

/**
 * Standard unwatermarked OpenStreetMap HTTPS tile source using standard OSM mirrors.
 */
private val OSM_STANDARD_TILES: ITileSource = XYTileSource(
    "OpenStreetMap_Standard",
    0,
    19,
    256,
    ".png",
    arrayOf(
        "https://a.tile.openstreetmap.org/",
        "https://b.tile.openstreetmap.org/",
        "https://c.tile.openstreetmap.org/"
    ),
    "© OpenStreetMap contributors"
)

/**
 * Interactive OpenStreetMap location picker with smooth pan-to-center tracking,
 * real-time geofence radius overlay, and reverse-geocoded address display.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationPickerScreen(
    onNavigateBack: () -> Unit,
    onLocationConfirmed: (GeoLocation) -> Unit = {},
    navController: NavController? = null,
    viewModel: MapViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val primaryColor = MaterialTheme.colorScheme.primary
    val primaryArgb = primaryColor.toArgb()
    val circleFillArgb = primaryColor.copy(alpha = 0.20f).toArgb()

    var isMapDragging by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val permState = LocationPermissionHelper.getPermissionState(context)
        viewModel.onPermissionStateUpdated(permState)
        if (permState == LocationPermissionState.Denied) {
            viewModel.requestPermissionFlow()
        }
    }

    if (uiState.showPermissionFlow) {
        LocationPermissionFlow(
            onPermissionStateChanged = viewModel::onPermissionStateUpdated,
            onDismiss = viewModel::dismissPermissionFlow
        )
    }

    // Configure and remember MapView instance
    val mapView = remember {
        Configuration.getInstance().userAgentValue = "PinMindApp/1.0 (${context.packageName})"

        // Wipe any locally saved "403 Access Blocked" error tiles
        try {
            Configuration.getInstance().osmdroidTileCache?.let { cacheDir ->
                if (cacheDir.exists()) {
                    cacheDir.deleteRecursively()
                }
            }
        } catch (_: Exception) {}

        MapView(context).apply {
            setTileSource(OSM_STANDARD_TILES)
            tileProvider?.clearTileCache()
            setMultiTouchControls(true)
            isTilesScaledToDpi = true
            controller.setZoom(16.0)
            val initialLat = uiState.selectedLocation?.latitude ?: 37.7749
            val initialLng = uiState.selectedLocation?.longitude ?: -122.4194
            controller.setCenter(GeoPoint(initialLat, initialLng))
        }
    }

    // Lifecycle handling for MapView
    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_DESTROY -> mapView.onDetach()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDetach()
        }
    }

    // Move map when selected location updates externally (e.g. GPS fix)
    LaunchedEffect(uiState.currentDeviceLocation) {
        uiState.currentDeviceLocation?.let { loc ->
            mapView.controller.animateTo(GeoPoint(loc.latitude, loc.longitude))
        }
    }

    var showRadiusSheet by remember { mutableStateOf(false) }
    var showNotificationRationale by remember { mutableStateOf(false) }

    val confirmLocation: () -> Unit = {
        val currentLoc = uiState.selectedLocation
        val lat = currentLoc?.latitude ?: mapView.mapCenter.latitude
        val lng = currentLoc?.longitude ?: mapView.mapCenter.longitude
        val radius = uiState.radiusMeters
        val name = currentLoc?.locationName?.ifBlank { null } ?: "Selected Location"
        val address = currentLoc?.address ?: name

        val confirmedLoc = GeoLocation(
            latitude = lat,
            longitude = lng,
            radiusMeters = radius,
            locationName = name,
            address = address
        )

        navController?.previousBackStackEntry?.savedStateHandle?.let { handle ->
            handle["address"] = confirmedLoc.address
            handle["radius"] = confirmedLoc.radiusMeters
            handle["location_name"] = confirmedLoc.locationName
            handle["picked_address"] = confirmedLoc.address
            handle["picked_radius"] = confirmedLoc.radiusMeters
            handle["picked_name"] = confirmedLoc.locationName
            handle["picked_lat"] = confirmedLoc.latitude
            handle["picked_lng"] = confirmedLoc.longitude
            handle["latitude"] = confirmedLoc.latitude
            handle["longitude"] = confirmedLoc.longitude
        }

        val deviceLoc = uiState.currentDeviceLocation
        if (deviceLoc != null) {
            val distResults = FloatArray(1)
            android.location.Location.distanceBetween(
                deviceLoc.latitude, deviceLoc.longitude,
                confirmedLoc.latitude, confirmedLoc.longitude,
                distResults
            )
            val dist = distResults[0]
            if (dist <= confirmedLoc.radiusMeters) {
                android.widget.Toast.makeText(
                    context,
                    "📍 Inside reminder radius (${dist.toInt()}m <= ${confirmedLoc.radiusMeters.toInt()}m). Reminder will trigger upon saving task!",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }

        onLocationConfirmed(confirmedLoc)
    }

    if (showNotificationRationale) {
        NotificationPermissionRationaleDialog(
            onPermissionResult = {
                showNotificationRationale = false
                confirmLocation()
            },
            onDismiss = {
                showNotificationRationale = false
                confirmLocation()
            }
        )
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // OpenStreetMap View
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = {
                mapView.apply {
                    // Scroll and Zoom listener to update location in real-time as user pans
                    addMapListener(object : MapListener {
                        override fun onScroll(event: ScrollEvent?): Boolean {
                            val center = mapCenter
                            if (center != null) {
                                viewModel.onMapCenterChanged(center.latitude, center.longitude)
                            }
                            return true
                        }

                        override fun onZoom(event: ZoomEvent?): Boolean {
                            val center = mapCenter
                            if (center != null) {
                                viewModel.onMapCenterChanged(center.latitude, center.longitude)
                            }
                            return true
                        }
                    })

                    // Tap event overlay to snap center to tap point
                    val tapOverlay = MapEventsOverlay(object : MapEventsReceiver {
                        override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                            controller.animateTo(p)
                            viewModel.onMapCenterChanged(p.latitude, p.longitude)
                            return true
                        }

                        override fun longPressHelper(p: GeoPoint): Boolean = false
                    })
                    overlays.add(0, tapOverlay)

                    // GPS Location overlay
                    if (uiState.permissionState != LocationPermissionState.Denied) {
                        val locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(context), this).apply {
                            enableMyLocation()
                        }
                        overlays.add(locationOverlay)
                    }
                }
            },
            update = { view ->
                // Update geofence radius polygon centered at current target center
                view.overlays.removeAll { it is Polygon }

                val center = view.mapCenter
                if (center != null) {
                    val centerPoint = GeoPoint(center.latitude, center.longitude)
                    val circlePolygon = Polygon(view).apply {
                        points = Polygon.pointsAsCircle(centerPoint, uiState.radiusMeters.toDouble())
                        fillPaint.color = circleFillArgb
                        outlinePaint.color = primaryArgb
                        outlinePaint.strokeWidth = 4f
                    }
                    view.overlays.add(circlePolygon)
                }
                view.invalidate()
            }
        )

        // Fixed Center-Target Pin directly over the center of the map
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Place,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(44.dp)
                        .offset(y = (-22).dp)
                )
                // Target Dot at exact center
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                )
            }
        }

        // Top Floating Bar (Single Row: Back Button + Expanded Search Bar)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Back Navigation Button
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 6.dp,
                    modifier = Modifier.size(48.dp)
                ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Expanded Search Bar
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = viewModel::onSearchQueryChanged,
                        placeholder = {
                            Text(
                                text = stringResource(R.string.map_search_hint),
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingIcon = {
                            if (uiState.isSearching) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            } else if (uiState.searchQuery.isNotEmpty()) {
                                IconButton(onClick = viewModel::clearSearch) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = stringResource(R.string.cd_clear)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Auto-complete suggestions dropdown list
            if (uiState.searchResults.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp)
                    ) {
                        items(uiState.searchResults) { result ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        keyboardController?.hide()
                                        focusManager.clearFocus()
                                        viewModel.onSearchResultSelected(result)
                                        mapView.controller.setZoom(17.0)
                                        mapView.controller.animateTo(
                                            GeoPoint(result.latitude, result.longitude)
                                        )
                                    }
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.LocationOn,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = result.shortName,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = result.displayName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        }

        // GPS Re-center FAB (positioned comfortably above the compact bottom card)
        FloatingActionButton(
            onClick = {
                val permState = LocationPermissionHelper.getPermissionState(context)
                if (permState == LocationPermissionState.Denied) {
                    viewModel.requestPermissionFlow()
                } else {
                    viewModel.fetchDeviceLocation()
                    uiState.currentDeviceLocation?.let { loc ->
                        mapView.controller.animateTo(GeoPoint(loc.latitude, loc.longitude))
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 145.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.MyLocation,
                contentDescription = stringResource(R.string.map_current_location)
            )
        }

        // Compact Bottom Location & Actions Card
        Card(
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val location = uiState.selectedLocation
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Filled.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = location?.locationName?.ifBlank { "Selected Pin" } ?: "Pan map to select location",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        location?.address?.let { addr ->
                            Text(
                                text = addr,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    if (uiState.isResolvingAddress) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    }
                }

                // Two Buttons Row: Radius Button + Confirm Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Outlined Radius Trigger Button
                    OutlinedButton(
                        onClick = { showRadiusSheet = true },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Tune,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.map_radius_label, uiState.radiusMeters.toInt()),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    // Confirm Button taking remaining weight
                    Button(
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                !NotificationPermissionHelper.hasNotificationPermission(context)
                            ) {
                                showNotificationRationale = true
                            } else {
                                confirmLocation()
                            }
                        },
                        enabled = uiState.selectedLocation != null || mapView.mapCenter != null,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Icon(imageVector = Icons.Filled.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.map_confirm_location),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // Radius Selection Modal BottomSheet
        if (showRadiusSheet) {
            ModalBottomSheet(
                onDismissRequest = { showRadiusSheet = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(R.string.field_radius),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.map_radius_label, uiState.radiusMeters.toInt()),
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Text(
                        text = "Adjust the boundary circle for this location reminder. You will be alerted upon entering.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Slider(
                        value = uiState.radiusMeters,
                        onValueChange = viewModel::onRadiusChanged,
                        valueRange = 50f..1000f,
                        steps = 18,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("50 m", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        Text("500 m", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        Text("1000 m", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    }

                    Button(
                        onClick = { showRadiusSheet = false },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text(
                            text = "Done",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Alias for LocationPickerScreen.
 */
@Composable
fun PickLocationScreen(
    onNavigateBack: () -> Unit,
    onLocationConfirmed: (GeoLocation) -> Unit = {},
    navController: NavController? = null,
    viewModel: MapViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    LocationPickerScreen(
        onNavigateBack = onNavigateBack,
        onLocationConfirmed = onLocationConfirmed,
        navController = navController,
        viewModel = viewModel,
        modifier = modifier
    )
}

