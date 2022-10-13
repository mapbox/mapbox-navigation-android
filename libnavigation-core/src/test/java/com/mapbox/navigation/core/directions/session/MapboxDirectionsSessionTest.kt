package com.mapbox.navigation.core.directions.session

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.base.internal.CurrentIndicesFactory
import com.mapbox.navigation.base.internal.NavigationRouterV2
import com.mapbox.navigation.base.internal.route.RouteCompatibilityCache
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.NavigationRouterCallback
import com.mapbox.navigation.base.route.NavigationRouterRefreshCallback
import com.mapbox.navigation.base.route.NavigationRouterRefreshError
import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.core.BasicSetRoutesInfo
import com.mapbox.navigation.core.NavigationComponentProvider
import com.mapbox.navigation.testing.factories.createDirectionsResponse
import com.mapbox.navigation.testing.factories.createNavigationRoutes
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MapboxDirectionsSessionTest {

    private lateinit var session: MapboxDirectionsSession

    private val router: NavigationRouterV2 = mockk(relaxUnitFun = true)
    private val routeOptions: RouteOptions = mockk(relaxUnitFun = true)
    private val routerCallback: NavigationRouterCallback = mockk(relaxUnitFun = true)
    private val routesRefreshRequestCallback: NavigationRouterRefreshCallback =
        mockk(relaxUnitFun = true)
    private val observer: RoutesObserver = mockk(relaxUnitFun = true)
    private val route: NavigationRoute = mockk(relaxUnitFun = true)
    private val routes: List<NavigationRoute> = listOf(route)
    private val currentIndices = CurrentIndicesFactory.createIndices(1, 2, 3)
    private lateinit var routeCallback: NavigationRouterCallback
    private lateinit var refreshCallback: NavigationRouterRefreshCallback

    private val routeRequestId = 1L
    private val routeRefreshRequestId = 2L
    private val mockSetRoutesInfo = BasicSetRoutesInfo(RoutesExtra.ROUTES_UPDATE_REASON_NEW, 0)

    @Before
    fun setUp() {
        val routeOptionsBuilder: RouteOptions.Builder = mockk(relaxUnitFun = true)
        every { routeOptionsBuilder.waypointIndices(any()) } returns routeOptionsBuilder
        every { routeOptionsBuilder.waypointNames(any()) } returns routeOptionsBuilder
        every { routeOptionsBuilder.waypointTargets(any()) } returns routeOptionsBuilder
        every { routeOptionsBuilder.build() } returns routeOptions
        every { routeOptions.toBuilder() } returns routeOptionsBuilder
        every { routeOptions.waypointIndices() } returns ""
        every { routeOptions.waypointNames() } returns ""
        every { routeOptions.waypointTargets() } returns ""
        val routeBuilder: DirectionsRoute.Builder = mockk(relaxUnitFun = true)
        every { route.directionsRoute.toBuilder() } returns routeBuilder
        every { routeBuilder.routeOptions(any()) } returns routeBuilder
        every { routeBuilder.build() } returns mockk()

        val routeListener = slot<NavigationRouterCallback>()
        val refreshListener = slot<NavigationRouterRefreshCallback>()
        every { router.getRoute(routeOptions, capture(routeListener)) } answers {
            routeCallback = routeListener.captured
            routeRequestId
        }
        every {
            router.getRouteRefresh(route, currentIndices, capture(refreshListener))
        } answers {
            refreshCallback = refreshListener.captured
            routeRefreshRequestId
        }
        every { route.routeOptions } returns routeOptions
        mockkObject(NavigationComponentProvider)
        every { routerCallback.onRoutesReady(any(), any()) } answers {
            this.value
        }
        session = MapboxDirectionsSession(router)
    }

    @Test
    fun initialState() {
        assertNull(session.getPrimaryRouteOptions())
        assertEquals(session.routes, emptyList<DirectionsRoute>())
    }

    @Test
    fun `route response - success`() {
        val mockOrigin = mockk<RouterOrigin>()
        session.requestRoutes(routeOptions, routerCallback)
        routeCallback.onRoutesReady(routes, mockOrigin)

        verify(exactly = 1) { routerCallback.onRoutesReady(routes, mockOrigin) }
    }

    @Test
    fun `route request returns id`() {
        assertEquals(
            1L,
            session.requestRoutes(routeOptions, routerCallback)
        )
    }

    @Test
    fun `route response - failure`() {
        val reasons: List<RouterFailure> = listOf(mockk())
        session.requestRoutes(routeOptions, routerCallback)
        routeCallback.onFailure(reasons, routeOptions)

        verify(exactly = 1) {
            routerCallback.onFailure(reasons, routeOptions)
        }
    }

    @Test
    fun `route response - canceled`() {
        val mockOrigin = mockk<RouterOrigin>()
        session.requestRoutes(routeOptions, routerCallback)
        routeCallback.onCanceled(routeOptions, mockOrigin)

        verify(exactly = 1) {
            routerCallback.onCanceled(routeOptions, mockOrigin)
        }
    }

    @Test
    fun `route refresh response - success`() {
        session.requestRouteRefresh(route, currentIndices, routesRefreshRequestCallback)
        refreshCallback.onRefreshReady(route)

        verify(exactly = 1) { routesRefreshRequestCallback.onRefreshReady(route) }
    }

    @Test
    fun `route refresh request returns id`() {
        assertEquals(
            2L,
            session.requestRouteRefresh(route, currentIndices, routesRefreshRequestCallback)
        )
    }

    @Test
    fun `route refresh response - failure`() {
        val error: NavigationRouterRefreshError = mockk()
        session.requestRouteRefresh(route, currentIndices, routesRefreshRequestCallback)
        refreshCallback.onFailure(error)

        verify(exactly = 1) {
            routesRefreshRequestCallback.onFailure(error)
        }
    }

    @Test
    fun getRouteOptions() {
        session.setRoutes(routes, BasicSetRoutesInfo(RoutesExtra.ROUTES_UPDATE_REASON_NEW, 0))
        assertEquals(routeOptions, session.getPrimaryRouteOptions())
    }

    @Test
    fun getInitialLegIndex() {
        val initialLegIndex = 2
        session.setRoutes(
            routes,
            BasicSetRoutesInfo(RoutesExtra.ROUTES_UPDATE_REASON_NEW, initialLegIndex)
        )
        assertEquals(initialLegIndex, session.initialLegIndex)
    }

    @Test
    fun cancelAll() {
        session.cancelAll()
        verify { router.cancelAll() }
    }

    @Test
    fun cancelRouteRequest() {
        session.cancelRouteRequest(1L)
        verify { router.cancelRouteRequest(1L) }
    }

    @Test
    fun cancelRouteRefresh() {
        session.cancelRouteRefreshRequest(1L)
        verify { router.cancelRouteRefreshRequest(1L) }
    }

    @Test
    fun shutDown() {
        session.shutdown()
        verify { router.shutdown() }
    }

    @Test
    fun `when route set, observer notified`() {
        val slot = slot<RoutesUpdatedResult>()
        every { observer.onRoutesChanged(capture(slot)) } just runs

        session.registerRoutesObserver(observer)
        session.setRoutes(routes, mockSetRoutesInfo)

        verify(exactly = 1) { observer.onRoutesChanged(slot.captured) }
        assertEquals(slot.captured.reason, mockSetRoutesInfo.reason)
        assertEquals(slot.captured.navigationRoutes, routes)
    }

    @Test
    fun `when route set, compatibility cache notified`() {
        mockkObject(RouteCompatibilityCache)
        session.setRoutes(routes, mockSetRoutesInfo)

        verify(exactly = 1) { RouteCompatibilityCache.setDirectionsSessionResult(routes) }
        verify(exactly = 0) { RouteCompatibilityCache.cacheCreationResult(routes) }

        unmockkObject(RouteCompatibilityCache)
    }

    @Test
    fun `when route cleared, compatibility cache notified`() {
        session.setRoutes(routes, mockSetRoutesInfo)

        mockkObject(RouteCompatibilityCache)
        session.setRoutes(
            emptyList(),
            BasicSetRoutesInfo(RoutesExtra.ROUTES_UPDATE_REASON_CLEAN_UP, 0)
        )

        verify(exactly = 1) { RouteCompatibilityCache.setDirectionsSessionResult(emptyList()) }

        unmockkObject(RouteCompatibilityCache)
    }

    @Test
    fun `observer notified on subscribe with actual route data`() {
        session.setRoutes(routes, BasicSetRoutesInfo(RoutesExtra.ROUTES_UPDATE_REASON_NEW, 0))
        val slot = slot<RoutesUpdatedResult>()
        every { observer.onRoutesChanged(capture(slot)) } just runs

        session.registerRoutesObserver(observer)

        verify(exactly = 1) { observer.onRoutesChanged(slot.captured) }
        assertEquals(
            "Routes update reason",
            RoutesExtra.ROUTES_UPDATE_REASON_NEW,
            slot.captured.reason
        )
        assertEquals("Routes", routes, slot.captured.navigationRoutes)
    }

    @Test
    fun `observer notified on subscribe with explicit empty route data`() {
        session.setRoutes(
            emptyList(),
            BasicSetRoutesInfo(RoutesExtra.ROUTES_UPDATE_REASON_CLEAN_UP, 0)
        )
        val slot = slot<RoutesUpdatedResult>()
        every { observer.onRoutesChanged(capture(slot)) } just runs

        session.registerRoutesObserver(observer)

        verify(exactly = 1) { observer.onRoutesChanged(slot.captured) }
        assertEquals(
            "Routes update reason",
            RoutesExtra.ROUTES_UPDATE_REASON_CLEAN_UP,
            slot.captured.reason
        )
        assertEquals(
            "Routes",
            emptyList<NavigationRoute>(),
            slot.captured.navigationRoutes
        )
    }

    @Test
    fun `observer not notified on subscribe with implicit empty route data `() {
        val slot = slot<RoutesUpdatedResult>()
        every { observer.onRoutesChanged(capture(slot)) } just runs

        session.registerRoutesObserver(observer)

        verify(exactly = 0) { observer.onRoutesChanged(any()) }
    }

    @Test
    fun `when route cleared after non-empty, observer notified`() {
        val slot = mutableListOf<RoutesUpdatedResult>()
        every { observer.onRoutesChanged(capture(slot)) } just runs

        session.registerRoutesObserver(observer)
        session.setRoutes(routes, BasicSetRoutesInfo(RoutesExtra.ROUTES_UPDATE_REASON_NEW, 0))
        session.setRoutes(
            emptyList(),
            BasicSetRoutesInfo(RoutesExtra.ROUTES_UPDATE_REASON_CLEAN_UP, 0)
        )

        assertTrue("Number of onRoutesChanged invocations", slot.size == 2)
        assertEquals(
            "First routes update reason",
            RoutesExtra.ROUTES_UPDATE_REASON_NEW,
            slot[0].reason
        )
        assertEquals("First routes", routes, slot[0].navigationRoutes)
        assertEquals(
            "Second routes update reason",
            RoutesExtra.ROUTES_UPDATE_REASON_CLEAN_UP,
            slot[1].reason
        )
        assertEquals("Second routes", emptyList<DirectionsRoute>(), slot[1].navigationRoutes)
    }

    @Test
    fun `when route cleared for the first time, observer notified`() {
        val slot = mutableListOf<RoutesUpdatedResult>()
        every { observer.onRoutesChanged(capture(slot)) } just runs

        session.registerRoutesObserver(observer)
        session.setRoutes(
            emptyList(),
            BasicSetRoutesInfo(RoutesExtra.ROUTES_UPDATE_REASON_CLEAN_UP, 0)
        )

        assertTrue("Number of onRoutesChanged invocations", slot.size == 1)
        assertEquals(
            "Routes update reason",
            RoutesExtra.ROUTES_UPDATE_REASON_CLEAN_UP,
            slot[0].reason
        )
        assertEquals("Routes", emptyList<NavigationRoute>(), slot[0].navigationRoutes)
    }

    @Test
    fun `when route cleared for the second first time, observer not notified`() {
        session.setRoutes(
            emptyList(),
            BasicSetRoutesInfo(RoutesExtra.ROUTES_UPDATE_REASON_CLEAN_UP, 0)
        )

        session.registerRoutesObserver(observer)
        clearMocks(observer)
        session.setRoutes(
            emptyList(),
            BasicSetRoutesInfo(RoutesExtra.ROUTES_UPDATE_REASON_CLEAN_UP, 0)
        )

        verify(exactly = 0) {
            observer.onRoutesChanged(any())
        }
    }

    @Test
    fun `when new route available, observer notified`() {
        val slot = slot<RoutesUpdatedResult>()
        every { observer.onRoutesChanged(capture(slot)) } just runs

        session.registerRoutesObserver(observer)
        session.setRoutes(routes, BasicSetRoutesInfo(RoutesExtra.ROUTES_UPDATE_REASON_NEW, 0))
        val newRoutes: List<NavigationRoute> = listOf(
            mockk {
                every { directionsRoute } returns mockk()
            }
        )
        session.setRoutes(newRoutes, BasicSetRoutesInfo(RoutesExtra.ROUTES_UPDATE_REASON_NEW, 0))

        verify(exactly = 1) { observer.onRoutesChanged(slot.captured) }
        assertEquals(slot.captured.reason, RoutesExtra.ROUTES_UPDATE_REASON_NEW)
        assertEquals(slot.captured.navigationRoutes, newRoutes)
    }

    @Test
    fun `setting a route does not impact ongoing route request`() {
        session.requestRoutes(routeOptions, routerCallback)
        session.setRoutes(routes, mockSetRoutesInfo)
        verify(exactly = 0) { router.cancelAll() }
    }

    @Test
    fun unregisterAllRouteObservers() {
        session.registerRoutesObserver(observer)
        session.unregisterAllRoutesObservers()
        session.setRoutes(routes, mockSetRoutesInfo)

        verify(exactly = 0) { observer.onRoutesChanged(any()) }
    }

    @Test
    fun `set previewed route`() {
        session.registerRoutesObserver(observer)
        val testRoutes = createNavigationRoutes()

        session.setRoutes(
            testRoutes,
            BasicSetRoutesInfo(RoutesExtra.ROUTES_UPDATE_REASON_PREVIEW, 0)
        )

        assertEquals(testRoutes, session.previewedRoutes)
        verify {
            observer.onRoutesChanged(
                match {
                    it.navigationRoutes == testRoutes
                        && it.reason == RoutesExtra.ROUTES_UPDATE_REASON_PREVIEW
                }
            )
        }
        assertEquals(emptyList<NavigationRoute>(), session.routes)
    }

    @Test
    fun `register route observer during preview state`() {
        val testRoutes = createNavigationRoutes()
        session.setRoutes(
            testRoutes,
            BasicSetRoutesInfo(RoutesExtra.ROUTES_UPDATE_REASON_PREVIEW, 0)
        )

        session.registerRoutesObserver(observer)

        verify {
            observer.onRoutesChanged(
                match {
                    it.navigationRoutes == testRoutes
                        && it.reason == RoutesExtra.ROUTES_UPDATE_REASON_PREVIEW
                }
            )
        }
    }

    @Test
    fun `clean set previewed route`() {
        session.setRoutes(
            createNavigationRoutes(),
            BasicSetRoutesInfo(RoutesExtra.ROUTES_UPDATE_REASON_PREVIEW, 0)
        )
        session.registerRoutesObserver(observer)

        session.setRoutes(
            emptyList(),
            BasicSetRoutesInfo(RoutesExtra.ROUTES_UPDATE_REASON_CLEAN_UP, 0)
        )

        assertEquals(emptyList<NavigationRoute>(), session.previewedRoutes)
        assertEquals(emptyList<NavigationRoute>(), session.routes)
        verify {
            observer.onRoutesChanged(
                match {
                    it.navigationRoutes.isEmpty()
                        && it.reason == RoutesExtra.ROUTES_UPDATE_REASON_CLEAN_UP
                }
            )
        }
    }

    @Test
    fun `set route preview after active navigation`() {
        session.setRoutes(
            createNavigationRoutes(createDirectionsResponse(uuid = "test1")),
            BasicSetRoutesInfo(RoutesExtra.ROUTES_UPDATE_REASON_NEW, 0)
        )

        val testPreviewRoutes = createNavigationRoutes(createDirectionsResponse(uuid = "test2"))
        session.setRoutes(
            testPreviewRoutes,
            BasicSetRoutesInfo(RoutesExtra.ROUTES_UPDATE_REASON_PREVIEW, 0)
        )

        assertEquals(testPreviewRoutes, session.previewedRoutes)
        assertEquals(emptyList<NavigationRoute>(), session.routes)
    }
}
