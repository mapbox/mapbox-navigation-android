package com.mapbox.navigation.dropin.component.location

import android.location.Location
import com.mapbox.android.core.location.LocationEngineCallback
import com.mapbox.android.core.location.LocationEngineResult
import com.mapbox.common.Logger
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.testing.MockLoggerRule
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class, ExperimentalCoroutinesApi::class)
class LocationViewModelTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    @get:Rule
    val mockLoggerTestRule = MockLoggerRule()

    @Test
    fun `default state is a null location`() = runBlockingTest {
        val locationViewModel = LocationViewModel()

        val location = locationViewModel.state.value

        assertNull(location)
    }

    @Test
    fun `onAttached will use current location to update state`() = runBlockingTest {
        val locationViewModel = LocationViewModel()
        val mapboxNavigation = mockMapboxNavigationLastLocation(
            mockk {
                every { longitude } returns -109.587335
                every { latitude } returns 38.731370
            }
        )

        locationViewModel.onAttached(mapboxNavigation)

        with(locationViewModel.state.value!!) {
            assertEquals(-109.587335, longitude, 0.00001)
            assertEquals(38.731370, latitude, 0.00001)
        }
    }

    @Test
    fun `onAttached will use current location to update navigationLocationProvider`() =
        runBlockingTest {
            val locationViewModel = LocationViewModel()
            val mapboxNavigation = mockMapboxNavigationLastLocation(
                mockk {
                    every { longitude } returns -109.587335
                    every { latitude } returns 38.731370
                }
            )

            locationViewModel.onAttached(mapboxNavigation)

            with(locationViewModel.navigationLocationProvider.lastLocation!!) {
                assertEquals(-109.587335, longitude, 0.00001)
                assertEquals(38.731370, latitude, 0.00001)
            }
        }

    @Test
    fun `onAttached will use current location to update lastPoint`() = runBlockingTest {
        val locationViewModel = LocationViewModel()
        val mapboxNavigation = mockMapboxNavigationLastLocation(
            mockk {
                every { longitude } returns -109.587335
                every { latitude } returns 38.731370
            }
        )

        locationViewModel.onAttached(mapboxNavigation)

        with(locationViewModel.lastPoint!!) {
            assertEquals(-109.587335, longitude(), 0.00001)
            assertEquals(38.731370, latitude(), 0.00001)
        }
    }

    @Test
    fun `onAttached will not log error when getLastLocation fails`() = runBlockingTest {
        val locationViewModel = LocationViewModel()
        val callbackSlot = slot<LocationEngineCallback<LocationEngineResult>>()
        val mapboxNavigation = mockk<MapboxNavigation>(relaxed = true) {
            every { navigationOptions } returns mockk {
                every { locationEngine } returns mockk {
                    every { getLastLocation(capture(callbackSlot)) } answers {
                        callbackSlot.captured.onFailure(mockk())
                    }
                }
            }
        }

        locationViewModel.onAttached(mapboxNavigation)

        assertNull(locationViewModel.state.value)
        verify(exactly = 1) { Logger.e("MbxLocationViewModel", any()) }
    }

    @Test
    fun `onAttached registerLocationObserver onNewRawLocation are ignored`() = runBlockingTest {
        val locationViewModel = LocationViewModel()
        val locationObserver = slot<LocationObserver>()
        val mapboxNavigation = mockk<MapboxNavigation>(relaxed = true) {
            every { navigationOptions } returns mockk {
                every { locationEngine } returns mockk {
                    every { getLastLocation(any()) } just Runs
                    every { registerLocationObserver(capture(locationObserver)) } answers {
                        locationObserver.captured.onNewRawLocation(
                            mockk {
                                every { longitude } returns -109.587335
                                every { latitude } returns 38.731370
                            }
                        )
                    }
                }
            }
        }

        locationViewModel.onAttached(mapboxNavigation)

        assertNull(locationViewModel.state.value)
    }

    @Test
    fun `onAttached registerLocationObserver onNewLocationMatcherResult updates state`() =
        runBlockingTest {
            val locationViewModel = LocationViewModel()
            val mapboxNavigation = mockMapboxNavigationRegisterObserver(
                mockk {
                    every { longitude } returns -109.587335
                    every { latitude } returns 38.731370
                }
            )

            locationViewModel.onAttached(mapboxNavigation)

            with(locationViewModel.state.value!!) {
                assertEquals(-109.587335, longitude, 0.00001)
                assertEquals(38.731370, latitude, 0.00001)
            }
        }

    @Test
    fun `onAttached registerLocationObserver onNewLocationMatcherResult updates navigationLocationProvider`() =
        runBlockingTest {
            val locationViewModel = LocationViewModel()
            val mapboxNavigation = mockMapboxNavigationRegisterObserver(
                mockk {
                    every { longitude } returns -109.587335
                    every { latitude } returns 38.731370
                }
            )

            locationViewModel.onAttached(mapboxNavigation)

            with(locationViewModel.navigationLocationProvider.lastLocation!!) {
                assertEquals(-109.587335, longitude, 0.00001)
                assertEquals(38.731370, latitude, 0.00001)
            }
        }

    @Test
    fun `onAttached registerLocationObserver onNewLocationMatcherResult updates lastPoint`() =
        runBlockingTest {
            val locationViewModel = LocationViewModel()
            val mapboxNavigation = mockMapboxNavigationRegisterObserver(
                mockk {
                    every { longitude } returns -109.587335
                    every { latitude } returns 38.731370
                }
            )

            locationViewModel.onAttached(mapboxNavigation)

            with(locationViewModel.lastPoint!!) {
                assertEquals(-109.587335, longitude(), 0.00001)
                assertEquals(38.731370, latitude(), 0.00001)
            }
        }

    @Test
    fun `onDetached will unregisterLocationObserver`() = runBlockingTest {
        val locationViewModel = LocationViewModel()
        val mapboxNavigation = mockMapboxNavigationRegisterObserver(
            mockk {
                every { longitude } returns -109.587335
                every { latitude } returns 38.731370
            }
        )

        locationViewModel.onAttached(mapboxNavigation)
        locationViewModel.onDetached(mapboxNavigation)

        verifyOrder {
            mapboxNavigation.registerLocationObserver(any())
            mapboxNavigation.unregisterLocationObserver(any())
        }
    }

    private fun mockMapboxNavigationLastLocation(_lastLocation: Location?): MapboxNavigation {
        val callbackSlot = slot<LocationEngineCallback<LocationEngineResult>>()
        return mockk(relaxed = true) {
            every { navigationOptions } returns mockk {
                every { locationEngine } returns mockk {
                    every { getLastLocation(capture(callbackSlot)) } answers {
                        callbackSlot.captured.onSuccess(
                            mockk {
                                every { lastLocation } returns _lastLocation
                            }
                        )
                    }
                }
            }
        }
    }

    private fun mockMapboxNavigationRegisterObserver(
        _enhancedLocation: Location
    ): MapboxNavigation {
        val locationObserver = slot<LocationObserver>()
        return mockk(relaxed = true) {
            every { navigationOptions } returns mockk {
                every { locationEngine } returns mockk {
                    every { getLastLocation(any()) } just Runs
                    every { registerLocationObserver(capture(locationObserver)) } answers {
                        locationObserver.captured.onNewLocationMatcherResult(
                            mockk {
                                every { enhancedLocation } returns _enhancedLocation
                                every { keyPoints } returns emptyList()
                            }
                        )
                    }
                }
            }
        }
    }
}
