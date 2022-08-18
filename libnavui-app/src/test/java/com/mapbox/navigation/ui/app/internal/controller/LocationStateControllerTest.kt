package com.mapbox.navigation.ui.app.internal.controller

import android.location.Location
import android.location.LocationManager
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.app.internal.location.LocationAction
import com.mapbox.navigation.ui.app.testing.TestStore
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LocationStateControllerTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    private lateinit var store: TestStore
    private lateinit var sut: LocationStateController
    private lateinit var mapboxNavigation: MapboxNavigation

    @Before
    fun setUp() {
        mapboxNavigation = mockk(relaxed = true)
        store = spyk(TestStore())
        sut = LocationStateController(store)
    }

    @Test
    fun `onAttached should subscribe as LocationObserver and dispatch location Update action`() =
        runBlockingTest {
            val locationMatcherResult = locationMatcherResult(location(1.0, 2.0))
            givenLocationUpdate(locationMatcherResult)

            sut.onAttached(mapboxNavigation)

            verify { store.dispatch(LocationAction.Update(locationMatcherResult)) }
        }

    @Test
    fun `process should process LocationAction and update store state`() {
        val action = LocationAction.Update(
            locationMatcherResult(location(2.0, 3.0))
        )

        val state = sut.process(store.state.value, action)

        assertEquals(action.result, state.location)
    }

    private fun givenLocationUpdate(result: LocationMatcherResult) {
        val locObserver = slot<LocationObserver>()
        every {
            mapboxNavigation.registerLocationObserver(capture(locObserver))
        } answers {
            locObserver.captured.onNewRawLocation(result.enhancedLocation)
            locObserver.captured.onNewLocationMatcherResult(result)
        }
    }

    private fun locationMatcherResult(location: Location) = mockk<LocationMatcherResult> {
        every { enhancedLocation } returns location
        every { keyPoints } returns emptyList()
    }

    private fun location(latitude: Double, longitude: Double) = Location(
        LocationManager.PASSIVE_PROVIDER
    ).apply {
        this.latitude = latitude
        this.longitude = longitude
    }
}
