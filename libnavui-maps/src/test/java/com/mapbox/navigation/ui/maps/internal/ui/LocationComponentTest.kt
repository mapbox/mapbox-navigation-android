package com.mapbox.navigation.ui.maps.internal.ui

import android.location.LocationManager
import com.mapbox.common.location.Location
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.maps.location.NavigationLocationProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class LocationComponentTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    private lateinit var sut: LocationComponent

    private lateinit var mapboxNavigation: MapboxNavigation
    private lateinit var locationProvider: NavigationLocationProvider

    @Before
    fun setUp() {
        mapboxNavigation = mockk(relaxed = true)
        locationProvider = spyk(NavigationLocationProvider())

        sut = LocationComponent(locationProvider)
    }

    @Test
    fun `onAttached should subscribe to location updates`() = runBlockingTest {
        val location = location(3.0, 4.0)
        givenLocationUpdate(location)

        sut.onAttached(mapboxNavigation)

        verify { locationProvider.changePosition(location, emptyList()) }
    }

    private fun givenLocationUpdate(location: Location) {
        val result = mockk<LocationMatcherResult> {
            every { enhancedLocation } returns location
            every { keyPoints } returns emptyList()
        }
        val locObserver = slot<LocationObserver>()
        every {
            mapboxNavigation.registerLocationObserver(capture(locObserver))
        } answers {
            locObserver.captured.onNewRawLocation(location)
            locObserver.captured.onNewLocationMatcherResult(result)
        }
    }

    private fun location(latitude: Double, longitude: Double) = Location.Builder()
        .source(LocationManager.PASSIVE_PROVIDER)
        .latitude(latitude)
        .longitude(longitude)
        .build()
}
