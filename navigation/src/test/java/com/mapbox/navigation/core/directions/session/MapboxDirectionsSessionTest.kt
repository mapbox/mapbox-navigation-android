package com.mapbox.navigation.core.directions.session

import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.bindgen.DataRef
import com.mapbox.navigation.base.internal.RouteRefreshRequestData
import com.mapbox.navigation.base.internal.route.routeOptions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.NavigationRouterCallback
import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.core.NavigationComponentProvider
import com.mapbox.navigation.core.SetRoutes
import com.mapbox.navigation.core.internal.router.GetRouteSignature
import com.mapbox.navigation.core.internal.router.NavigationRouterRefreshCallback
import com.mapbox.navigation.core.internal.router.NavigationRouterRefreshError
import com.mapbox.navigation.core.internal.router.Router
import com.mapbox.navigation.core.internal.utils.mapToReason
import com.mapbox.navigation.testing.MapboxJavaObjectsFactory
import com.mapbox.navigation.testing.factories.createNavigationRoute
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MapboxDirectionsSessionTest {

    private lateinit var session: MapboxDirectionsSession

    private val router: Router = mockk(relaxUnitFun = true)
    private val routeOptions: RouteOptions = MapboxJavaObjectsFactory.routeOptions()
    private val signature = mockk<GetRouteSignature>()
    private val routerCallback: NavigationRouterCallback = mockk(relaxUnitFun = true)
    private val routesRefreshRequestCallback: NavigationRouterRefreshCallback =
        mockk(relaxUnitFun = true)
    private val observer: RoutesObserver = mockk(relaxUnitFun = true)
    private val route: NavigationRoute = mockk(relaxUnitFun = true)
    private val ignoredRoute: IgnoredRoute = mockk(relaxUnitFun = true)
    private val routes: List<NavigationRoute> = listOf(route)
    private val ignoredRoutes: List<IgnoredRoute> = listOf(ignoredRoute)
    private val routeRefreshRequestData = RouteRefreshRequestData(1, 2, 3, emptyMap())
    private val refreshResponse: DataRef = mockk(relaxUnitFun = true)
    private lateinit var routeCallback: NavigationRouterCallback
    private lateinit var refreshCallback: NavigationRouterRefreshCallback

    private val routeRequestId = 1L
    private val routeRefreshRequestId = 2L
    private val mockSetRoutesInfo = SetRoutes.NewRoutes(0)

    @Before
    fun setUp() {
        every { route.directionsRoute } returns MapboxJavaObjectsFactory.directionsRoute()

        val routeListener = slot<NavigationRouterCallback>()
        val refreshListener = slot<NavigationRouterRefreshCallback>()
        every { router.getRoute(routeOptions, signature, capture(routeListener)) } answers {
            routeCallback = routeListener.captured
            routeRequestId
        }
        every {
            router.getRouteRefresh(route, routeRefreshRequestData, capture(refreshListener))
        } answers {
            refreshCallback = refreshListener.captured
            routeRefreshRequestId
        }
        mockkObject(NavigationComponentProvider)
        every { routerCallback.onRoutesReady(any(), any()) } answers {
            this.value
        }
        session = MapboxDirectionsSession(router)
    }

    @Test
    fun initialState() {
        assertNull(session.getPrimaryRouteOptions())
        assertNull(session.routesUpdatedResult)
    }

    @Test
    fun `route response - success`() {
        val mockOrigin = RouterOrigin.ONLINE
        session.requestRoutes(routeOptions, signature, routerCallback)
        routeCallback.onRoutesReady(routes, mockOrigin)

        verify(exactly = 1) { routerCallback.onRoutesReady(routes, mockOrigin) }
    }

    @Test
    fun `route request returns id`() {
        assertEquals(
            1L,
            session.requestRoutes(routeOptions, signature, routerCallback),
        )
    }

    @Test
    fun `route response - failure`() {
        val reasons: List<RouterFailure> = listOf(mockk())
        session.requestRoutes(routeOptions, signature, routerCallback)
        routeCallback.onFailure(reasons, routeOptions)

        verify(exactly = 1) {
            routerCallback.onFailure(reasons, routeOptions)
        }
    }

    @Test
    fun `route response - canceled`() {
        val mockOrigin = RouterOrigin.ONLINE
        session.requestRoutes(routeOptions, signature, routerCallback)
        routeCallback.onCanceled(routeOptions, mockOrigin)

        verify(exactly = 1) {
            routerCallback.onCanceled(routeOptions, mockOrigin)
        }
    }

    @Test
    fun `route refresh response - success`() {
        session.requestRouteRefresh(route, routeRefreshRequestData, routesRefreshRequestCallback)
        refreshCallback.onRefreshReady(route, refreshResponse)

        verify(exactly = 1) { routesRefreshRequestCallback.onRefreshReady(route, refreshResponse) }
    }

    @Test
    fun `route refresh request returns id`() {
        assertEquals(
            2L,
            session.requestRouteRefresh(
                route,
                routeRefreshRequestData,
                routesRefreshRequestCallback,
            ),
        )
    }

    @Test
    fun `route refresh response - failure`() {
        val error: NavigationRouterRefreshError = mockk()
        session.requestRouteRefresh(route, routeRefreshRequestData, routesRefreshRequestCallback)
        refreshCallback.onFailure(error)

        verify(exactly = 1) {
            routesRefreshRequestCallback.onFailure(error)
        }
    }

    @Test
    fun getRouteOptions() {
        val testRoute = createNavigationRoute()
        session.setNavigationRoutesFinished(
            DirectionsSessionRoutes(
                listOf(testRoute),
                emptyList(),
                SetRoutes.NewRoutes(0),
            ),
        )
        assertEquals(testRoute.routeOptions, session.getPrimaryRouteOptions())
    }

    @Test
    fun getInitialLegIndex() {
        val cases = listOf(
            SetRoutes.NewRoutes(2) to 2,
            SetRoutes.CleanUp to 0,
            SetRoutes.Reroute(5) to 5,
            SetRoutes.Alternatives(3) to 3,
            SetRoutes.RefreshRoutes.RefreshControllerRefresh(
                mockk {
                    every { primaryRouteRefresherResult } returns mockk {
                        every { routeProgressData } returns mockk {
                            every { legIndex } returns 4
                        }
                    }
                },
            ) to 4,
            SetRoutes.RefreshRoutes.ExternalRefresh(
                legIndex = 5,
                isManual = true,
            ) to 5,
        )

        cases.forEach { (setRoutes, expectedInitialLegIndex) ->
            session.setNavigationRoutesFinished(
                DirectionsSessionRoutes(routes, emptyList(), setRoutes),
            )

            assertEquals(expectedInitialLegIndex, session.initialLegIndex)
        }
        assertEquals("6 (one per each sealed class) cases must be covered", 6, cases.size)
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

        session.registerSetNavigationRoutesFinishedObserver(observer)
        session.setNavigationRoutesFinished(
            DirectionsSessionRoutes(routes, ignoredRoutes, mockSetRoutesInfo),
        )

        verify(exactly = 1) { observer.onRoutesChanged(slot.captured) }
        assertEquals(slot.captured.reason, mockSetRoutesInfo.mapToReason())
        assertEquals(slot.captured.navigationRoutes, routes)
        assertEquals(slot.captured.ignoredRoutes, ignoredRoutes)
    }

    @Test
    fun `observer notified on subscribe with actual route data`() {
        session.setNavigationRoutesFinished(
            DirectionsSessionRoutes(routes, ignoredRoutes, SetRoutes.NewRoutes(0)),
        )
        val slot = slot<RoutesUpdatedResult>()
        every { observer.onRoutesChanged(capture(slot)) } just runs

        session.registerSetNavigationRoutesFinishedObserver(observer)

        verify(exactly = 1) { observer.onRoutesChanged(slot.captured) }
        assertEquals(
            "Routes update reason",
            RoutesExtra.ROUTES_UPDATE_REASON_NEW,
            slot.captured.reason,
        )
        assertEquals("Routes", routes, slot.captured.navigationRoutes)
        assertEquals("Ignored routes", listOf(ignoredRoute), slot.captured.ignoredRoutes)
    }

    @Test
    fun `observer notified on subscribe with explicit empty route data`() {
        session.setNavigationRoutesFinished(
            DirectionsSessionRoutes(emptyList(), emptyList(), SetRoutes.CleanUp),
        )
        val slot = slot<RoutesUpdatedResult>()
        every { observer.onRoutesChanged(capture(slot)) } just runs

        session.registerSetNavigationRoutesFinishedObserver(observer)

        verify(exactly = 1) { observer.onRoutesChanged(slot.captured) }
        assertEquals(
            "Routes update reason",
            RoutesExtra.ROUTES_UPDATE_REASON_CLEAN_UP,
            slot.captured.reason,
        )
        assertEquals(
            "Routes",
            emptyList<NavigationRoute>(),
            slot.captured.navigationRoutes,
        )
    }

    @Test
    fun `observer not notified on subscribe with implicit empty route data `() {
        val slot = slot<RoutesUpdatedResult>()
        every { observer.onRoutesChanged(capture(slot)) } just runs

        session.registerSetNavigationRoutesFinishedObserver(observer)

        verify(exactly = 0) { observer.onRoutesChanged(any()) }
    }

    @Test
    fun `when route cleared after non-empty, observer notified`() {
        val slot = mutableListOf<RoutesUpdatedResult>()
        every { observer.onRoutesChanged(capture(slot)) } just runs

        session.registerSetNavigationRoutesFinishedObserver(observer)
        session.setNavigationRoutesFinished(
            DirectionsSessionRoutes(routes, ignoredRoutes, SetRoutes.NewRoutes(0)),
        )
        session.setNavigationRoutesFinished(
            DirectionsSessionRoutes(emptyList(), emptyList(), SetRoutes.CleanUp),
        )

        assertTrue("Number of onRoutesChanged invocations", slot.size == 2)
        assertEquals(
            "First routes update reason",
            RoutesExtra.ROUTES_UPDATE_REASON_NEW,
            slot[0].reason,
        )
        assertEquals("First routes", routes, slot[0].navigationRoutes)
        assertEquals("First ignored routes", listOf(ignoredRoute), slot[0].ignoredRoutes)
        assertEquals(
            "Second routes update reason",
            RoutesExtra.ROUTES_UPDATE_REASON_CLEAN_UP,
            slot[1].reason,
        )
        assertEquals("Second routes", emptyList<NavigationRoute>(), slot[1].navigationRoutes)
        assertEquals("Second ignored routes", emptyList<IgnoredRoute>(), slot[1].ignoredRoutes)
    }

    @Test
    fun `when route cleared for the first time, observer notified`() {
        val slot = mutableListOf<RoutesUpdatedResult>()
        every { observer.onRoutesChanged(capture(slot)) } just runs

        session.registerSetNavigationRoutesFinishedObserver(observer)
        session.setNavigationRoutesFinished(
            DirectionsSessionRoutes(emptyList(), emptyList(), SetRoutes.CleanUp),
        )

        assertTrue("Number of onRoutesChanged invocations", slot.size == 1)
        assertEquals(
            "Routes update reason",
            RoutesExtra.ROUTES_UPDATE_REASON_CLEAN_UP,
            slot[0].reason,
        )
        assertEquals("Routes", emptyList<NavigationRoute>(), slot[0].navigationRoutes)
        assertEquals("Ignored routes", emptyList<IgnoredRoute>(), slot[0].ignoredRoutes)
    }

    @Test
    fun `when route cleared for the second first time, observer not notified`() {
        session.setNavigationRoutesFinished(
            DirectionsSessionRoutes(emptyList(), emptyList(), SetRoutes.CleanUp),
        )

        session.registerSetNavigationRoutesFinishedObserver(observer)
        clearMocks(observer)
        session.setNavigationRoutesFinished(
            DirectionsSessionRoutes(emptyList(), emptyList(), SetRoutes.CleanUp),
        )

        verify(exactly = 0) {
            observer.onRoutesChanged(any())
        }
    }

    @Test
    fun `when new route available, observer notified`() {
        val slot = slot<RoutesUpdatedResult>()
        every { observer.onRoutesChanged(capture(slot)) } just runs

        session.registerSetNavigationRoutesFinishedObserver(observer)
        session.setNavigationRoutesFinished(
            DirectionsSessionRoutes(routes, ignoredRoutes, SetRoutes.NewRoutes(0)),
        )
        val newRoutes: List<NavigationRoute> = listOf(
            mockk {
                every { directionsRoute } returns mockk()
            },
        )
        val newIgnoredRoutes = listOf(mockk<IgnoredRoute>(relaxed = true))
        session.setNavigationRoutesFinished(
            DirectionsSessionRoutes(newRoutes, newIgnoredRoutes, SetRoutes.NewRoutes(0)),
        )

        verify(exactly = 1) { observer.onRoutesChanged(slot.captured) }
        assertEquals(slot.captured.reason, RoutesExtra.ROUTES_UPDATE_REASON_NEW)
        assertEquals(slot.captured.navigationRoutes, newRoutes)
        assertEquals(slot.captured.ignoredRoutes, newIgnoredRoutes)
    }

    @Test
    fun `setting a route does not impact ongoing route request`() {
        session.requestRoutes(routeOptions, signature, routerCallback)
        session.setNavigationRoutesFinished(
            DirectionsSessionRoutes(routes, ignoredRoutes, mockSetRoutesInfo),
        )
        verify(exactly = 0) { router.cancelAll() }
    }

    @Test
    fun unregisterAllRouteObservers() {
        session.registerSetNavigationRoutesFinishedObserver(observer)
        session.unregisterAllSetNavigationRoutesFinishedObserver()
        session.setNavigationRoutesFinished(
            DirectionsSessionRoutes(routes, ignoredRoutes, mockSetRoutesInfo),
        )

        verify(exactly = 0) { observer.onRoutesChanged(any()) }
    }

    @Test
    fun `routes when routesUpdatedResult is null`() {
        session.setNavigationRoutesFinished(
            DirectionsSessionRoutes(routes, ignoredRoutes, mockSetRoutesInfo),
        )

        assertEquals(routes, session.routes)
    }

    @Test
    fun `routes when routesUpdatedResult is not null`() {
        assertEquals(0, session.routes.size)
    }
}
