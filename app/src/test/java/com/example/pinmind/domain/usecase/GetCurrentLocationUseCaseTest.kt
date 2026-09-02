package com.example.pinmind.domain.usecase

import com.example.pinmind.core.location.FakeLocationClient
import com.example.pinmind.domain.model.GeoLocation
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GetCurrentLocationUseCaseTest {

    private lateinit var fakeLocationClient: FakeLocationClient
    private lateinit var getCurrentLocationUseCase: GetCurrentLocationUseCase

    @Before
    fun setup() {
        fakeLocationClient = FakeLocationClient()
        getCurrentLocationUseCase = GetCurrentLocationUseCase(fakeLocationClient)
    }

    @Test
    fun `when location is available returns success with GeoLocation`() = runTest {
        val expected = GeoLocation(latitude = 37.7749, longitude = -122.4194, radiusMeters = 100f)
        fakeLocationClient.locationToReturn = expected

        val result = getCurrentLocationUseCase()

        assertTrue(result.isSuccess)
        assertEquals(expected, result.getOrNull())
    }

    @Test
    fun `when location is unavailable returns failure`() = runTest {
        fakeLocationClient.locationToReturn = null

        val result = getCurrentLocationUseCase()

        assertTrue(result.isFailure)
    }
}
