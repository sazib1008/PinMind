package com.example.pinmind.presentation.map

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.example.pinmind.core.location.FakeLocationClient
import com.example.pinmind.core.location.GeocoderHelper
import com.example.pinmind.core.location.LocationPermissionState
import com.example.pinmind.domain.model.GeoLocation
import com.example.pinmind.domain.usecase.GetCurrentLocationUseCase
import com.example.pinmind.domain.usecase.ReverseGeocodeUseCase
import com.example.pinmind.util.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class FakeGeocoderHelper : GeocoderHelper(
    context = object : android.content.ContextWrapper(null) {}
) {
    override suspend fun getAddressFromCoordinates(
        latitude: Double,
        longitude: Double
    ): Pair<String, String?> {
        return "Test Place" to "123 Test St"
    }
}

class MapViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeLocationClient: FakeLocationClient
    private lateinit var getCurrentLocationUseCase: GetCurrentLocationUseCase
    private lateinit var fakeGeocoderHelper: FakeGeocoderHelper

    @Before
    fun setup() {
        fakeLocationClient = FakeLocationClient()
        fakeLocationClient.locationToReturn = GeoLocation(37.7749, -122.4194, 100f)
        getCurrentLocationUseCase = GetCurrentLocationUseCase(fakeLocationClient)
        fakeGeocoderHelper = FakeGeocoderHelper()
    }

    @Test
    fun `initializes with coordinates from SavedStateHandle arguments`() = runTest {
        val args = mapOf(
            "lat" to "40.7128",
            "lng" to "-74.0060",
            "radius" to "250"
        )
        val savedStateHandle = SavedStateHandle(args)

        val viewModel = MapViewModel(
            savedStateHandle = savedStateHandle,
            getCurrentLocationUseCase = getCurrentLocationUseCase,
            reverseGeocodeUseCase = ReverseGeocodeUseCase(fakeGeocoderHelper)
        )

        viewModel.uiState.test {
            val state = awaitItem()
            assertNotNull(state.selectedLocation)
            assertEquals(40.7128, state.selectedLocation!!.latitude, 0.0001)
            assertEquals(-74.0060, state.selectedLocation!!.longitude, 0.0001)
            assertEquals(250f, state.radiusMeters)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `changing radius updates state and selected location radius`() = runTest {
        val viewModel = MapViewModel(
            savedStateHandle = SavedStateHandle(),
            getCurrentLocationUseCase = getCurrentLocationUseCase,
            reverseGeocodeUseCase = ReverseGeocodeUseCase(fakeGeocoderHelper)
        )

        viewModel.onRadiusChanged(450f)

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(450f, state.radiusMeters)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `tapping map updates selected location coordinates`() = runTest {
        val viewModel = MapViewModel(
            savedStateHandle = SavedStateHandle(),
            getCurrentLocationUseCase = getCurrentLocationUseCase,
            reverseGeocodeUseCase = ReverseGeocodeUseCase(fakeGeocoderHelper)
        )

        viewModel.onMapTapped(51.5074, -0.1278)

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(51.5074, state.selectedLocation?.latitude ?: 0.0, 0.0001)
            assertEquals(-0.1278, state.selectedLocation?.longitude ?: 0.0, 0.0001)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `permission state change updates uiState`() = runTest {
        val viewModel = MapViewModel(
            savedStateHandle = SavedStateHandle(),
            getCurrentLocationUseCase = getCurrentLocationUseCase,
            reverseGeocodeUseCase = ReverseGeocodeUseCase(fakeGeocoderHelper)
        )

        viewModel.onPermissionStateUpdated(LocationPermissionState.GrantedAllTime)

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(LocationPermissionState.GrantedAllTime, state.permissionState)
            cancelAndIgnoreRemainingEvents()
        }
    }
}

