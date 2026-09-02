package com.example.pinmind.presentation.map

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.nav_map_picker), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
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
                                isMapDragging = true
                                viewModel.onMapTapped(center.latitude, center.longitude)
                                return true
                            }

                            override fun onZoom(event: ZoomEvent?): Boolean {
                                val center = mapCenter
                                viewModel.onMapTapped(center.latitude, center.longitude)
                                return true
                            }
                        })

                        // Tap event overlay to snap center to tap point
                        val tapOverlay = MapEventsOverlay(object : MapEventsReceiver {
                            override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                                controller.animateTo(p)
                                viewModel.onMapTapped(p.latitude, p.longitude)
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
                    // Update geofence radius polygon
                    view.overlays.removeAll { it is Polygon }

                    uiState.selectedLocation?.let { loc ->
                        val centerPoint = GeoPoint(loc.latitude, loc.longitude)
                        val circlePolygon = Polygon(view).apply {
                            points = Polygon.pointsAsCircle(centerPoint, loc.radiusMeters.toDouble())
                            fillPaint.color = circleFillArgb
                            outlinePaint.color = primaryArgb
                            outlinePaint.strokeWidth = 4f
                        }
                        view.overlays.add(circlePolygon)
                    }
                    view.invalidate()
                }
            )

            // Centered Target Pin Overlay (Fixed at map center with target shadow)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 200.dp), // Offsets center above bottom card
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
                            .size(48.dp)
                            .offset(y = (-12).dp)
                    )
                    // Target Dot
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                    )
                }
            }

            // Top Instruction Pill
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                shadowElevation = 4.dp,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.map_tap_instruction),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // GPS Re-center FAB
            FloatingActionButton(
                onClick = {
                    val permState = LocationPermissionHelper.getPermissionState(context)
                    if (permState == LocationPermissionState.Denied) {
                        viewModel.requestPermissionFlow()
                    } else {
                        viewModel.fetchDeviceLocation()
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 240.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.MyLocation,
                    contentDescription = stringResource(R.string.map_current_location)
                )
            }

            // Bottom Location & Radius Card
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
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
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
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
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
                            CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        }
                    }

                    // Radius slider
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(R.string.field_radius),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = stringResource(R.string.map_radius_label, uiState.radiusMeters.toInt()),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                    }

                    Slider(
                        value = uiState.radiusMeters,
                        onValueChange = viewModel::onRadiusChanged,
                        valueRange = 50f..1000f,
                        steps = 18
                    )

                    // Confirm Button
                    Button(
                        onClick = {
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

                            onLocationConfirmed(confirmedLoc)
                        },
                        enabled = uiState.selectedLocation != null || mapView.mapCenter != null,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Icon(imageVector = Icons.Filled.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.map_confirm_location),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
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

