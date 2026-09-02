package com.example.pinmind.presentation.map

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.example.pinmind.core.location.FakeLocationClient
import com.example.pinmind.core.location.GeocoderHelper
import com.example.pinmind.core.location.LocationPermissionState
import com.example.pinmind.core.location.NominatimSearchHelper
import com.example.pinmind.domain.model.GeoLocation
import com.example.pinmind.domain.model.SearchLocationResult
import com.example.pinmind.domain.usecase.GetCurrentLocationUseCase
import com.example.pinmind.domain.usecase.ReverseGeocodeUseCase
import com.example.pinmind.domain.usecase.SearchLocationUseCase
import com.example.pinmind.util.MainDispatcherRule
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
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

class FakeNominatimSearchHelper : NominatimSearchHelper(
    context = object : android.content.ContextWrapper(null) {}
) {
    override suspend fun searchLocations(query: String): List<SearchLocationResult> {
        return if (query.contains("Dhanmondi")) {
            listOf(
                SearchLocationResult(
                    displayName = "Dhanmondi Lake, Dhaka, Bangladesh",
                    shortName = "Dhanmondi Lake",
                    latitude = 23.7512,
                    longitude = 90.3789
                )
            )
        } else {
            emptyList()
        }
    }
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class MapViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeLocationClient: FakeLocationClient
    private lateinit var getCurrentLocationUseCase: GetCurrentLocationUseCase
    private lateinit var fakeGeocoderHelper: FakeGeocoderHelper
    private lateinit var reverseGeocodeUseCase: ReverseGeocodeUseCase
    private lateinit var fakeNominatimSearchHelper: FakeNominatimSearchHelper
    private lateinit var searchLocationUseCase: SearchLocationUseCase

    @Before
    fun setup() {
        fakeLocationClient = FakeLocationClient()
        fakeLocationClient.locationToReturn = GeoLocation(37.7749, -122.4194, 100f)
        getCurrentLocationUseCase = GetCurrentLocationUseCase(fakeLocationClient)
        fakeGeocoderHelper = FakeGeocoderHelper()
        reverseGeocodeUseCase = ReverseGeocodeUseCase(fakeGeocoderHelper)
        fakeNominatimSearchHelper = FakeNominatimSearchHelper()
        searchLocationUseCase = SearchLocationUseCase(fakeNominatimSearchHelper)
    }

    private fun createViewModel(savedStateHandle: SavedStateHandle = SavedStateHandle()): MapViewModel {
        return MapViewModel(
            savedStateHandle = savedStateHandle,
            getCurrentLocationUseCase = getCurrentLocationUseCase,
            reverseGeocodeUseCase = reverseGeocodeUseCase,
            searchLocationUseCase = searchLocationUseCase
        )
    }

    @Test
    fun `initializes with coordinates from SavedStateHandle arguments`() = runTest {
        val args = mapOf(
            "lat" to "40.7128",
            "lng" to "-74.0060",
            "radius" to "250"
        )
        val savedStateHandle = SavedStateHandle(args)
        val viewModel = createViewModel(savedStateHandle)

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
        val viewModel = createViewModel()

        viewModel.onRadiusChanged(450f)

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(450f, state.radiusMeters)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `tapping map updates selected location coordinates`() = runTest {
        val viewModel = createViewModel()

        viewModel.onMapTapped(51.5074, -0.1278)

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(51.5074, state.selectedLocation?.latitude ?: 0.0, 0.0001)
            assertEquals(-0.1278, state.selectedLocation?.longitude ?: 0.0, 0.0001)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `moving map center updates coordinates and debounces reverse geocoding`() = runTest {
        val viewModel = createViewModel()

        viewModel.onMapCenterChanged(23.8103, 90.4125)

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(23.8103, state.selectedLocation?.latitude ?: 0.0, 0.0001)
            assertEquals(90.4125, state.selectedLocation?.longitude ?: 0.0, 0.0001)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `search query triggers searchLocationUseCase and updates searchResults`() = runTest {
        val viewModel = createViewModel()

        viewModel.onSearchQueryChanged("Dhanmondi Lake")
        advanceTimeBy(500)
        runCurrent()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(1, state.searchResults.size)
            assertEquals("Dhanmondi Lake", state.searchResults.first().shortName)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `selecting search result updates selected location and clears search results`() = runTest {
        val viewModel = createViewModel()
        val result = SearchLocationResult(
            displayName = "Dhanmondi Lake, Dhaka, Bangladesh",
            shortName = "Dhanmondi Lake",
            latitude = 23.7512,
            longitude = 90.3789
        )

        viewModel.onSearchResultSelected(result)

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(23.7512, state.selectedLocation?.latitude ?: 0.0, 0.0001)
            assertEquals(90.3789, state.selectedLocation?.longitude ?: 0.0, 0.0001)
            assertEquals("Dhanmondi Lake", state.selectedLocation?.locationName)
            assertEquals(0, state.searchResults.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `permission state change updates uiState`() = runTest {
        val viewModel = createViewModel()

        viewModel.onPermissionStateUpdated(LocationPermissionState.GrantedAllTime)

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(LocationPermissionState.GrantedAllTime, state.permissionState)
            cancelAndIgnoreRemainingEvents()
        }
    }
}


