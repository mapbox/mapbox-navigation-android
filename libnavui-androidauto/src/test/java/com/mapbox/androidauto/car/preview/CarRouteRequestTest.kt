package com.mapbox.androidauto.car.preview

import androidx.test.core.app.ApplicationProvider
import com.mapbox.androidauto.testing.MapboxRobolectricTestRunner
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.common.Logger
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.formatter.UnitType
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.NavigationRouterCallback
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.ui.maps.location.NavigationLocationProvider
import io.mockk.CapturingSlot
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.Locale

class CarRouteRequestTest : MapboxRobolectricTestRunner() {

    private val routeOptionsSlot = CapturingSlot<RouteOptions>()
    private val routerCallbackSlot = CapturingSlot<NavigationRouterCallback>()
    private val navigationLocationProvider = mockk<NavigationLocationProvider>()
    private var requestCount = 0L
    private val mapboxNavigation = mockk<MapboxNavigation> {
        every {
            requestRoutes(capture(routeOptionsSlot), capture(routerCallbackSlot))
        } returns requestCount++
        every { cancelRouteRequest(any()) } just Runs
        every { navigationOptions } returns mockk {
            every { applicationContext } returns ApplicationProvider.getApplicationContext()
            every { distanceFormatterOptions } returns mockk {
                every { locale } returns Locale.US
                every { unitType } returns UnitType.METRIC
            }
        }
        every { getZLevel() } returns Z_LEVEL
    }

    private val carRouteRequest = CarRouteRequest(mapboxNavigation, navigationLocationProvider)

    @Before
    fun setup() {
        mockkStatic(Logger::class)
        every { Logger.e(any(), any()) } just Runs
        every { Logger.i(any(), any()) } just Runs
    }

    @After
    fun teardown() {
        unmockkStatic(Logger::class)
    }

    @Test
    fun `onRoutesReady is called after successful request`() {
        every {
            navigationLocationProvider.lastLocation
        } returns mockk {
            every { longitude } returns -121.4670161
            every { latitude } returns 38.5630514
        }
        val callback: CarRouteRequestCallback = mockk(relaxUnitFun = true)
        val searchCoordinate = Point.fromLngLat(-121.467001, 38.568105)
        carRouteRequest.request(
            mockk { every { coordinate } returns searchCoordinate },
            callback
        )

        val routes = listOf(mockk<NavigationRoute>())
        routerCallbackSlot.captured.onRoutesReady(routes, mockk())

        verify(exactly = 1) { callback.onRoutesReady(any(), any()) }
    }

    @Test
    fun `onUnknownCurrentLocation is called when current location is null`() {
        every { navigationLocationProvider.lastLocation } returns null
        val callback: CarRouteRequestCallback = mockk(relaxUnitFun = true)
        val searchCoordinate = Point.fromLngLat(-121.467001, 38.568105)
        carRouteRequest.request(
            mockk { every { coordinate } returns searchCoordinate },
            callback
        )

        verify { callback.onUnknownCurrentLocation() }
    }

    @Test
    fun `onSearchResultLocationUnknown is called when search result coordinate is`() {
        every {
            navigationLocationProvider.lastLocation
        } returns mockk {
            every { longitude } returns -121.4670161
            every { latitude } returns 38.5630514
        }
        val callback: CarRouteRequestCallback = mockk(relaxUnitFun = true)
        carRouteRequest.request(
            mockk { every { coordinate } returns null },
            callback
        )

        verify { callback.onDestinationLocationUnknown() }
    }

    @Test
    fun `onNoRoutesFound is called when route request is canceled`() {
        every {
            navigationLocationProvider.lastLocation
        } returns mockk {
            every { longitude } returns -121.4670161
            every { latitude } returns 38.5630514
        }
        val callback: CarRouteRequestCallback = mockk(relaxUnitFun = true)
        val searchCoordinate = Point.fromLngLat(-121.467001, 38.568105)
        carRouteRequest.request(
            mockk { every { coordinate } returns searchCoordinate },
            callback
        )

        routerCallbackSlot.captured.onCanceled(mockk(), mockk())

        verify { callback.onNoRoutesFound() }
    }

    @Test
    fun `onNoRoutesFound is called when route request fails`() {
        every {
            navigationLocationProvider.lastLocation
        } returns mockk {
            every { longitude } returns -121.4670161
            every { latitude } returns 38.5630514
        }
        val callback: CarRouteRequestCallback = mockk(relaxUnitFun = true)
        val searchCoordinate = Point.fromLngLat(-121.467001, 38.568105)
        carRouteRequest.request(
            mockk { every { coordinate } returns searchCoordinate },
            callback
        )

        routerCallbackSlot.captured.onFailure(mockk(), mockk())

        verify { callback.onNoRoutesFound() }
    }

    @Test
    fun `should cancel previous route request`() {
        every {
            navigationLocationProvider.lastLocation
        } returns mockk {
            every { longitude } returns -121.4670161
            every { latitude } returns 38.5630514
        }
        val callback: CarRouteRequestCallback = mockk(relaxUnitFun = true)
        val searchCoordinate = Point.fromLngLat(-121.467001, 38.568105)
        carRouteRequest.request(
            mockk { every { coordinate } returns searchCoordinate },
            callback
        )
        carRouteRequest.request(
            mockk { every { coordinate } returns searchCoordinate },
            callback
        )

        verify(exactly = 1) { mapboxNavigation.cancelRouteRequest(0) }
    }

    @Test
    fun `z level is passed to route options`() {
        every { navigationLocationProvider.lastLocation } returns mockk {
            every { longitude } returns -121.4670161
            every { latitude } returns 38.5630514
        }
        val callback = mockk<CarRouteRequestCallback>(relaxUnitFun = true)
        val searchCoordinate = Point.fromLngLat(-121.467001, 38.568105)
        carRouteRequest.request(mockk { every { coordinate } returns searchCoordinate }, callback)

        assertEquals(listOf(Z_LEVEL, null), routeOptionsSlot.captured.layersList())
    }

    private companion object {

        private const val Z_LEVEL = 42
    }
}
