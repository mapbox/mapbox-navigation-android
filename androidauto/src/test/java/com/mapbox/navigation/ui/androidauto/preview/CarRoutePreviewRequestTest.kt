@file:OptIn(com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI::class)

package com.mapbox.navigation.ui.androidauto.preview

import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.formatter.UnitType
import com.mapbox.navigation.base.internal.RouterFailureFactory
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.NavigationRouterCallback
import com.mapbox.navigation.base.route.RouterFailureType
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.navigation.testing.MapboxJavaObjectsFactory
import com.mapbox.navigation.ui.androidauto.MapboxCarOptions
import com.mapbox.navigation.ui.androidauto.location.CarLocationProvider
import io.mockk.Called
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
import java.net.URL
import java.util.Locale

class CarRoutePreviewRequestTest {

    @get:Rule
    val loggerRule = LoggingFrontendTestRule()

    private val routeOptionsList = mutableListOf<RouteOptions>()
    private val routerCallbackList = mutableListOf<NavigationRouterCallback>()
    private val options: MapboxCarOptions = mockk {
        every { routeOptionsInterceptor } returns CarRouteOptionsInterceptor { it }
    }

    private val locationProvider = mockk<CarLocationProvider>()
    private var requestCount = 0L
    private val mapboxNavigation = mockk<MapboxNavigation> {
        every {
            requestRoutes(any(), any())
        } answers {
            routeOptionsList.add(firstArg())
            routerCallbackList.add(secondArg())
            requestCount++
        }
        every { cancelRouteRequest(any()) } just Runs
        every { setRoutesPreview(any(), any()) } just Runs
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

    private fun routerFailure(type: String) = RouterFailureFactory.create(
        url = URL("https://example.com"),
        routerOrigin = RouterOrigin.ONLINE,
        message = "failure",
        type = type,
    )

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
        routerCallbackList.last().onRoutesReady(routes, RouterOrigin.ONLINE)

        verify(exactly = 1) { mapboxNavigation.setRoutesPreview(routes) }
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
    fun `cancellation of the active request is silent`() {
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

        routerCallbackList.last().onCanceled(mockk(), RouterOrigin.ONLINE)

        verify { callback wasNot Called }
    }

    @Test
    fun `onNoRoutesFound is called when the router determines no route exists`() {
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

        routerCallbackList.last().onFailure(
            listOf(routerFailure(RouterFailureType.ROUTE_CREATION_ERROR)),
            mockk(),
        )

        verify(exactly = 1) { callback.onNoRoutesFound() }
        verify(exactly = 0) { callback.onNetworkFailure() }
        verify(exactly = 0) { callback.onRoutingFailure(any()) }
    }

    @Test
    fun `onNetworkFailure is called when route request fails due to a network error`() {
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

        routerCallbackList.last().onFailure(
            listOf(routerFailure(RouterFailureType.NETWORK_ERROR)),
            mockk(),
        )

        verify(exactly = 1) { callback.onNetworkFailure() }
        verify(exactly = 0) { callback.onNoRoutesFound() }
        verify(exactly = 0) { callback.onRoutingFailure(any()) }
    }

    @Test
    fun `onRoutingFailure is called for other failure types`() {
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

        val reasons = listOf(routerFailure(RouterFailureType.UNKNOWN_ERROR))
        routerCallbackList.last().onFailure(reasons, mockk())

        verify(exactly = 1) { callback.onRoutingFailure(reasons) }
        verify(exactly = 0) { callback.onNoRoutesFound() }
        verify(exactly = 0) { callback.onNetworkFailure() }
    }

    @Test
    fun `onNetworkFailure takes precedence over onNoRoutesFound for a mixed reasons list`() {
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

        val reasons = listOf(
            routerFailure(RouterFailureType.ROUTE_CREATION_ERROR),
            routerFailure(RouterFailureType.NETWORK_ERROR),
        )
        routerCallbackList.last().onFailure(reasons, mockk())

        verify(exactly = 1) { callback.onNetworkFailure() }
        verify(exactly = 0) { callback.onNoRoutesFound() }
        verify(exactly = 0) { callback.onRoutingFailure(any()) }
    }

    @Test
    fun `late onRoutesReady from a superseded request does not mutate state or invoke callbacks`() {
        every {
            locationProvider.lastLocation()
        } returns mockk {
            every { longitude } returns -121.4670161
            every { latitude } returns 38.5630514
        }
        val searchCoordinate = Point.fromLngLat(-121.467001, 38.568105)
        val staleCallback: CarRoutePreviewRequestCallback = mockk(relaxUnitFun = true)
        val activeCallback: CarRoutePreviewRequestCallback = mockk(relaxUnitFun = true)
        carRouteRequest.onAttached(mapboxNavigation)
        carRouteRequest.request(
            mockk { every { coordinate } returns searchCoordinate },
            staleCallback,
        )
        carRouteRequest.request(
            mockk { every { coordinate } returns searchCoordinate },
            activeCallback,
        )

        val staleRouterCallback = routerCallbackList[0]
        val activeRouterCallback = routerCallbackList[1]

        val staleRoutes = listOf(mockk<NavigationRoute>())
        staleRouterCallback.onRoutesReady(staleRoutes, RouterOrigin.ONLINE)

        verify(exactly = 0) { mapboxNavigation.setRoutesPreview(any()) }
        verify { staleCallback wasNot Called }
        assertEquals(emptyList<NavigationRoute>(), carRouteRequest.repository?.routes?.value)

        val activeRoutes = listOf(mockk<NavigationRoute>())
        activeRouterCallback.onRoutesReady(activeRoutes, RouterOrigin.ONLINE)

        verify(exactly = 1) { mapboxNavigation.setRoutesPreview(activeRoutes) }
        verify(exactly = 1) { activeCallback.onRoutesReady(any(), activeRoutes) }
        assertEquals(activeRoutes, carRouteRequest.repository?.routes?.value)
    }

    @Test
    fun `late onFailure and onCanceled from a superseded request are silent`() {
        every {
            locationProvider.lastLocation()
        } returns mockk {
            every { longitude } returns -121.4670161
            every { latitude } returns 38.5630514
        }
        val searchCoordinate = Point.fromLngLat(-121.467001, 38.568105)
        val staleCallback: CarRoutePreviewRequestCallback = mockk(relaxUnitFun = true)
        val activeCallback: CarRoutePreviewRequestCallback = mockk(relaxUnitFun = true)
        carRouteRequest.onAttached(mapboxNavigation)
        carRouteRequest.request(
            mockk { every { coordinate } returns searchCoordinate },
            staleCallback,
        )
        carRouteRequest.request(
            mockk { every { coordinate } returns searchCoordinate },
            activeCallback,
        )

        val staleRouterCallback = routerCallbackList[0]
        staleRouterCallback.onCanceled(mockk(), RouterOrigin.ONLINE)
        staleRouterCallback.onFailure(
            listOf(routerFailure(RouterFailureType.ROUTE_CREATION_ERROR)),
            mockk(),
        )

        verify { staleCallback wasNot Called }
    }

    @Test
    fun `explicit cancelRequest then a late callback is silent`() {
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

        carRouteRequest.cancelRequest()

        val routerCallback = routerCallbackList.last()
        routerCallback.onRoutesReady(listOf(mockk()), RouterOrigin.ONLINE)
        routerCallback.onFailure(
            listOf(routerFailure(RouterFailureType.ROUTE_CREATION_ERROR)),
            mockk(),
        )
        routerCallback.onCanceled(mockk(), RouterOrigin.ONLINE)

        verify { callback wasNot Called }
        verify(exactly = 0) { mapboxNavigation.setRoutesPreview(any()) }
    }

    @Test
    fun `stale callback after detach and reattach does not mutate the new repository`() {
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

        val staleRouterCallback = routerCallbackList.last()

        carRouteRequest.onDetached(mapboxNavigation)
        carRouteRequest.onAttached(mapboxNavigation)
        val repositoryAfterReattach = carRouteRequest.repository

        staleRouterCallback.onRoutesReady(listOf(mockk()), RouterOrigin.ONLINE)

        verify { callback wasNot Called }
        verify(exactly = 0) { mapboxNavigation.setRoutesPreview(any()) }
        assertEquals(emptyList<NavigationRoute>(), repositoryAfterReattach?.routes?.value)
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

        assertEquals(listOf(Z_LEVEL, null), routeOptionsList.last().layersList())
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

        assertEquals(customRouteOptions, routeOptionsList.last())
    }

    private companion object {

        private const val Z_LEVEL = 42
    }
}
