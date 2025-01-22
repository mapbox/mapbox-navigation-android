package com.mapbox.navigation.ui.maps.internal.ui

import android.location.LocationManager
import com.mapbox.common.location.Location
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.maps.camera.data.MapboxNavigationViewportDataSource
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verifyOrder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NavigationCameraComponentTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    private lateinit var mapboxNavigation: MapboxNavigation
    private lateinit var dataSource: MapboxNavigationViewportDataSource
    private lateinit var sut: NavigationCameraComponent

    @Before
    fun setUp() {
        mapboxNavigation = mockk(relaxed = true)
        dataSource = mockk(relaxed = true)
        sut = NavigationCameraComponent(dataSource, mockk())
    }

    @Test
    fun `should observe route progress and update viewport data source`() = runBlockingTest {
        val routeProgress = mockk<RouteProgress>()
        given(routeProgress = routeProgress)

        sut.onAttached(mapboxNavigation)

        verifyOrder {
            dataSource.onRouteProgressChanged(routeProgress)
            dataSource.evaluate()
        }
    }

    @Test
    fun `should observe location and update viewport data source`() = runBlockingTest {
        val location = Location.Builder()
            .source(LocationManager.PASSIVE_PROVIDER)
            .latitude(1.0)
            .longitude(2.0)
            .build()
        val locationResult = mockk<LocationMatcherResult> {
            every { enhancedLocation } returns location
        }
        given(locationResult = locationResult)

        sut.onAttached(mapboxNavigation)

        verifyOrder {
            dataSource.onLocationChanged(location)
            dataSource.evaluate()
        }
    }

    private fun given(
        routeProgress: RouteProgress? = null,
        locationResult: LocationMatcherResult? = null,
    ) {
        if (routeProgress != null) {
            val progressObserver = slot<RouteProgressObserver>()
            every {
                mapboxNavigation.registerRouteProgressObserver(capture(progressObserver))
            } answers {
                progressObserver.captured.onRouteProgressChanged(routeProgress)
            }
        }
        if (locationResult != null) {
            val locationObserver = slot<LocationObserver>()
            every {
                mapboxNavigation.registerLocationObserver(capture(locationObserver))
            } answers {
                locationObserver.captured.onNewLocationMatcherResult(locationResult)
                locationObserver.captured.onNewRawLocation(locationResult.enhancedLocation)
            }
        }
    }
}
