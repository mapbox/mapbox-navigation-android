package com.mapbox.navigation.ui.androidauto.preview

import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.formatter.UnitType
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.NavigationRouterCallback
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.navigation.testing.MapboxJavaObjectsFactory
import com.mapbox.navigation.ui.androidauto.MapboxCarOptions
import com.mapbox.navigation.ui.androidauto.location.CarLocationProvider
import io.mockk.CapturingSlot
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.Locale

class CarRoutePreviewRequestTest {

    @get:Rule
    val loggerRule = LoggingFrontendTestRule()

    private val routeOptionsSlot = CapturingSlot<RouteOptions>()
    private val routerCallbackSlot = CapturingSlot<NavigationRouterCallback>()
    private val options: MapboxCarOptions = mockk {
        every { routeOptionsInterceptor } returns CarRouteOptionsInterceptor { it }
    }

    private val locationProvider = mockk<CarLocationProvider>()
    private var requestCount = 0L
    private val mapboxNavigation = mockk<MapboxNavigation> {
        every {
            requestRoutes(capture(routeOptionsSlot), capture(routerCallbackSlot))
        } returns requestCount++
        every { cancelRouteRequest(any()) } just Runs
        every { navigationOptions } returns mockk {
            every { applicationContext } returns mockk()
            every { distanceFormatterOptions } returns mockk {
                every { locale } returns Locale.US
                every { unitType } returns UnitType.METRIC
            }
        }
        every { getZLevel() } returns Z_LEVEL
    }

    @Before
    fun setup() {
        mockkObject(CarLocationProvider)
        every { CarLocationProvider.getRegisteredInstance() } returns locationProvider
    }

    @After
    fun teardown() {
        unmockkAll()
    }

    private val carRouteRequest = CarRoutePreviewRequest(options)

    @Test
    fun `onRoutesReady is called after successful request`() {
        every {
            locationProvider.lastLocation()
        } returns mockk {
            every { longitude } returns -121.4670161
            every { latitude } returns 38.5630514
        }
        val callback: CarRoutePreviewRequestCallback = mockk(relaxUnitFun = true)
        val searchCoordinate = Point.fromLngLat(-121.467001, 38.568105)
        carRouteRequest.onAttached(mapboxNavigation)
        carRouteRequest.request(
            mockk { every { coordinate } returns searchCoordinate },
            callback,
        )

        val routes = listOf(mockk<NavigationRoute>())
        routerCallbackSlot.captured.onRoutesReady(routes, RouterOrigin.ONLINE)

        verify(exactly = 1) { callback.onRoutesReady(any(), any()) }
    }

    @Test
    fun `onUnknownCurrentLocation is called when current location is null`() {
        every { locationProvider.lastLocation() } returns null
        val callback: CarRoutePreviewRequestCallback = mockk(relaxUnitFun = true)
        val searchCoordinate = Point.fromLngLat(-121.467001, 38.568105)
        carRouteRequest.onAttached(mapboxNavigation)
        carRouteRequest.request(
            mockk { every { coordinate } returns searchCoordinate },
            callback,
        )

        verify { callback.onUnknownCurrentLocation() }
    }

    @Test
    fun `onSearchResultLocationUnknown is called when search result coordinate is`() {
        every {
            locationProvider.lastLocation()
        } returns mockk {
            every { longitude } returns -121.4670161
            every { latitude } returns 38.5630514
        }
        val callback: CarRoutePreviewRequestCallback = mockk(relaxUnitFun = true)
        carRouteRequest.onAttached(mapboxNavigation)
        carRouteRequest.request(
            mockk { every { coordinate } returns null },
            callback,
        )

        verify { callback.onDestinationLocationUnknown() }
    }

    @Test
    fun `onNoRoutesFound is called when route request is canceled`() {
        every {
            locationProvider.lastLocation()
        } returns mockk {
            every { longitude } returns -121.4670161
            every { latitude } returns 38.5630514
        }
        val callback: CarRoutePreviewRequestCallback = mockk(relaxUnitFun = true)
        val searchCoordinate = Point.fromLngLat(-121.467001, 38.568105)
        carRouteRequest.onAttached(mapboxNavigation)
        carRouteRequest.request(
            mockk { every { coordinate } returns searchCoordinate },
            callback,
        )

        routerCallbackSlot.captured.onCanceled(mockk(), RouterOrigin.ONLINE)

        verify { callback.onNoRoutesFound() }
    }

    @Test
    fun `onNoRoutesFound is called when route request fails`() {
        every {
            locationProvider.lastLocation()
        } returns mockk {
            every { longitude } returns -121.4670161
            every { latitude } returns 38.5630514
        }
        val callback: CarRoutePreviewRequestCallback = mockk(relaxUnitFun = true)
        val searchCoordinate = Point.fromLngLat(-121.467001, 38.568105)
        carRouteRequest.onAttached(mapboxNavigation)
        carRouteRequest.request(
            mockk { every { coordinate } returns searchCoordinate },
            callback,
        )

        routerCallbackSlot.captured.onFailure(mockk(), mockk())

        verify { callback.onNoRoutesFound() }
    }

    @Test
    fun `onNoRoutesFound is called when mapboxNavigation is not attached`() {
        every {
            locationProvider.lastLocation()
        } returns mockk {
            every { longitude } returns -121.4670161
            every { latitude } returns 38.5630514
        }
        val callback: CarRoutePreviewRequestCallback = mockk(relaxUnitFun = true)
        val searchCoordinate = Point.fromLngLat(-121.467001, 38.568105)
        carRouteRequest.request(
            mockk { every { coordinate } returns searchCoordinate },
            callback,
        )
        carRouteRequest.onAttached(mapboxNavigation)

        verify { callback.onNoRoutesFound() }
    }

    @Test
    fun `should cancel previous route request`() {
        every {
            locationProvider.lastLocation()
        } returns mockk {
            every { longitude } returns -121.4670161
            every { latitude } returns 38.5630514
        }
        val callback: CarRoutePreviewRequestCallback = mockk(relaxUnitFun = true)
        val searchCoordinate = Point.fromLngLat(-121.467001, 38.568105)
        carRouteRequest.onAttached(mapboxNavigation)
        carRouteRequest.request(
            mockk { every { coordinate } returns searchCoordinate },
            callback,
        )
        carRouteRequest.request(
            mockk { every { coordinate } returns searchCoordinate },
            callback,
        )

        verify(exactly = 1) { mapboxNavigation.cancelRouteRequest(0) }
    }

    @Test
    fun `z level is passed to route options`() {
        every { locationProvider.lastLocation() } returns mockk {
            every { longitude } returns -121.4670161
            every { latitude } returns 38.5630514
        }
        val callback = mockk<CarRoutePreviewRequestCallback>(relaxUnitFun = true)
        val searchCoordinate = Point.fromLngLat(-121.467001, 38.568105)
        carRouteRequest.onAttached(mapboxNavigation)
        carRouteRequest.request(mockk { every { coordinate } returns searchCoordinate }, callback)

        assertEquals(listOf(Z_LEVEL, null), routeOptionsSlot.captured.layersList())
    }

    @Test
    fun `custom route options provided by interceptor are used for route request`() {
        val customRouteOptions = MapboxJavaObjectsFactory.routeOptions(
            coordinates = listOf(Point.fromLngLat(23.4, 12.56), Point.fromLngLat(98.7, 45.4)),
        )
        every {
            options.routeOptionsInterceptor
        } returns CarRouteOptionsInterceptor { customRouteOptions.toBuilder() }
        every { locationProvider.lastLocation() } returns mockk {
            every { longitude } returns -121.4670161
            every { latitude } returns 38.5630514
        }
        val callback = mockk<CarRoutePreviewRequestCallback>(relaxUnitFun = true)
        val searchCoordinate = Point.fromLngLat(-121.467001, 38.568105)
        carRouteRequest.onAttached(mapboxNavigation)
        carRouteRequest.request(mockk { every { coordinate } returns searchCoordinate }, callback)

        assertEquals(customRouteOptions, routeOptionsSlot.captured)
    }

    private companion object {

        private const val Z_LEVEL = 42
    }
}
