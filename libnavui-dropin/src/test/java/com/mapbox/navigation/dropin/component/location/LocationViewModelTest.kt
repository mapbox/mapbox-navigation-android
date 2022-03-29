package com.mapbox.navigation.dropin.component.location

import android.location.Location
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.dropin.util.TestStore
import com.mapbox.navigation.dropin.util.TestingUtil.makeLocationMatcherResult
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.testing.MockLoggerRule
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.spyk
import io.mockk.unmockkAll
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class, ExperimentalCoroutinesApi::class)
class LocationViewModelTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    @get:Rule
    val mockLoggerTestRule = MockLoggerRule()

    private val locationObserverSlot = slot<LocationObserver>()
    private val store = spyk(TestStore())
    private val locationViewModel = LocationViewModel(store)

    private fun mockMapboxNavigation(): MapboxNavigation {
        val mapboxNavigation = mockk<MapboxNavigation>(relaxed = true) {
            every { registerLocationObserver(capture(locationObserverSlot)) } just Runs
        }
        every { MapboxNavigationApp.current() } returns mapboxNavigation
        return mapboxNavigation
    }

    @Before
    fun setup() {
        mockkObject(MapboxNavigationApp)
    }

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `onAttached will registerLocationObserver onNewLocationMatcherResult updates state`() =
        runBlockingTest {
            locationViewModel.onAttached(mockMapboxNavigation())
            val newLocationUpdate = makeLocationMatcherResult(1.0, 2.0, 0f)
            locationObserverSlot.captured.onNewLocationMatcherResult(newLocationUpdate)

            verify {
                store.dispatch(LocationAction.Update(newLocationUpdate))
            }
        }

    @Test
    fun `onAttached registerLocationObserver onNewLocationMatcherResult updates navigationLocationProvider`() =
        runBlockingTest {
            val mockLocation: Location = mockk {
                every { longitude } returns -109.587335
                every { latitude } returns 38.731370
            }

            locationViewModel.onAttached(mockMapboxNavigation())
            locationObserverSlot.captured.onNewLocationMatcherResult(
                mockk(relaxed = true) {
                    every { enhancedLocation } returns mockLocation
                    every { keyPoints } returns listOf(mockLocation)
                }
            )

            with(locationViewModel.navigationLocationProvider.lastLocation!!) {
                assertEquals(-109.587335, longitude, 0.00001)
                assertEquals(38.731370, latitude, 0.00001)
            }
        }

    @Test
    fun `onAttached registerLocationObserver onNewLocationMatcherResult updates lastPoint`() =
        runBlockingTest {
            val mockLocation: Location = mockk {
                every { longitude } returns -109.587335
                every { latitude } returns 38.731370
            }

            locationViewModel.onAttached(mockMapboxNavigation())
            locationObserverSlot.captured.onNewLocationMatcherResult(
                mockk(relaxed = true) {
                    every { enhancedLocation } returns mockLocation
                    every { keyPoints } returns listOf(mockLocation)
                }
            )

            with(locationViewModel.lastPoint!!) {
                assertEquals(-109.587335, longitude(), 0.00001)
                assertEquals(38.731370, latitude(), 0.00001)
            }
        }

    @Test
    fun `onDetached will unregisterLocationObserver`() = runBlockingTest {
        val mockMapboxNavigation = mockMapboxNavigation()
        locationViewModel.onAttached(mockMapboxNavigation)
        locationViewModel.onDetached(mockMapboxNavigation)

        verifyOrder {
            mockMapboxNavigation.registerLocationObserver(any())
            mockMapboxNavigation.unregisterLocationObserver(any())
        }
    }
}
