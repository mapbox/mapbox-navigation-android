package com.mapbox.navigation.ui.app.internal.controller

import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.NavigationRouterCallback
import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.app.internal.destination.DestinationAction
import com.mapbox.navigation.ui.app.internal.routefetch.RoutePreviewAction
import com.mapbox.navigation.ui.app.internal.routefetch.RoutePreviewState
import com.mapbox.navigation.ui.app.testing.TestStore
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.spyk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class RoutePreviewStateControllerTest {

    @get:Rule
    val loggerRule = LoggingFrontendTestRule()

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val store = spyk(TestStore())
    private val sut = RoutePreviewStateController(store)

    @Before
    fun setUp() {
        mockkObject(MapboxNavigationApp)
    }

    @After
    fun cleanUp() {
        unmockkAll()
    }

    @Test
    @Suppress("MaxLineLength")
    fun `onAttached should cancel previous route requests when SetDestination action is dispatched`() {
        val options = mockk<RouteOptions>()
        val mapboxNavigation = mockMapboxNavigation()
        every {
            mapboxNavigation.requestRoutes(options, ofType(NavigationRouterCallback::class))
        } returnsMany listOf(1L, 2L, 3L)

        sut.onAttached(mapboxNavigation)
        store.dispatch(RoutePreviewAction.FetchOptions(options))
        store.dispatch(DestinationAction.SetDestination(null))

        verify { mapboxNavigation.cancelRouteRequest(1L) }
    }

    @Test
    @Suppress("MaxLineLength")
    fun `onAttached should cancel previous route requests when FetchOptions action is dispatched`() {
        val mapboxNavigation = mockMapboxNavigation()
        every {
            mapboxNavigation.requestRoutes(any(), ofType(NavigationRouterCallback::class))
        } returnsMany listOf(1L, 2L, 3L)

        sut.onAttached(mapboxNavigation)
        store.dispatch(RoutePreviewAction.FetchOptions(mockk()))
        store.dispatch(RoutePreviewAction.FetchOptions(mockk()))

        verify { mapboxNavigation.cancelRouteRequest(1L) }
    }

    @Test
    fun `should dispatch StartedFetchRequest when fetching routes`() {
        val mapboxNavigation = mockMapboxNavigation()
        every {
            mapboxNavigation.requestRoutes(any(), ofType(NavigationRouterCallback::class))
        } returns 1L

        sut.onAttached(mapboxNavigation)
        store.dispatch(RoutePreviewAction.FetchOptions(mockk()))

        verify { store.dispatch(RoutePreviewAction.StartedFetchRequest(1L)) }
    }

    @Test
    fun `RoutePreviewAction StartedFetchRequest should save requestId in the store`() {
        val mapboxNavigation = mockMapboxNavigation()
        sut.onAttached(mapboxNavigation)

        store.dispatch(RoutePreviewAction.StartedFetchRequest(1L))

        val requestId = (store.state.value.previewRoutes as RoutePreviewState.Fetching).requestId
        assertEquals(1L, requestId)
    }

    @Test
    fun `RoutePreviewAction Ready with an empty list should result in Empty state`() {
        val mapboxNavigation = mockMapboxNavigation()
        every { mapboxNavigation.registerRoutesObserver(any()) } answers {
            firstArg<RoutesObserver>().onRoutesChanged(
                mockk {
                    every { navigationRoutes } returns emptyList()
                }
            )
        }

        sut.onAttached(mapboxNavigation)

        assertTrue(store.state.value.previewRoutes is RoutePreviewState.Empty)
    }

    @Test
    fun `RoutePreviewAction FetchOptions will request routes with the options`() {
        val mapboxNavigation = mockMapboxNavigation()
        val routeOptions = mockk<RouteOptions>()

        sut.onAttached(mapboxNavigation)
        store.dispatch(RoutePreviewAction.FetchOptions(routeOptions))

        assertTrue(store.state.value.previewRoutes is RoutePreviewState.Fetching)
        verify { mapboxNavigation.requestRoutes(routeOptions, any<NavigationRouterCallback>()) }
    }

    @Test
    fun `RoutePreviewAction FetchOptions is canceled when onDetached is called`() {
        val mapboxNavigation = mockMapboxNavigation()
        every { mapboxNavigation.requestRoutes(any(), any<NavigationRouterCallback>()) } answers {
            123L
        }

        sut.onAttached(mapboxNavigation)
        store.dispatch(RoutePreviewAction.FetchOptions(mockk()))
        sut.onDetached(mapboxNavigation)

        verify { mapboxNavigation.cancelRouteRequest(123L) }
    }

    @Test
    fun `RoutePreviewAction FetchOptions will go to Ready state when onRoutesReady`() {
        val mapboxNavigation = mockMapboxNavigation()
        val routes = listOf<NavigationRoute>(mockk())
        val callbackSlot = slot<NavigationRouterCallback>()
        every { mapboxNavigation.requestRoutes(any(), capture(callbackSlot)) } returns 123L
        val routeOptions = mockk<RouteOptions>()

        sut.onAttached(mapboxNavigation)
        store.dispatch(RoutePreviewAction.FetchOptions(routeOptions))
        callbackSlot.captured.onRoutesReady(routes, mockk())

        val readyState = store.state.value.previewRoutes as? RoutePreviewState.Ready
        assertNotNull(readyState)
        assertEquals(readyState?.routes, routes)
    }

    @Test
    fun `RoutePreviewAction FetchOptions will go to Failed state when onFailure`() {
        val mapboxNavigation = mockMapboxNavigation()
        val reasons = listOf<RouterFailure>(mockk())
        val routeOptions = mockk<RouteOptions>()
        val callbackSlot = slot<NavigationRouterCallback>()
        every { mapboxNavigation.requestRoutes(any(), capture(callbackSlot)) } returns 123L

        sut.onAttached(mapboxNavigation)
        store.dispatch(RoutePreviewAction.FetchOptions(routeOptions))
        callbackSlot.captured.onFailure(reasons, routeOptions)

        val readyState = store.state.value.previewRoutes as? RoutePreviewState.Failed
        assertNotNull(readyState)
        assertEquals(readyState?.reasons, reasons)
        assertEquals(readyState?.routeOptions, routeOptions)
    }

    @Test
    fun `RoutePreviewAction FetchOptions will go to Canceled state when onCanceled`() {
        val mapboxNavigation = mockMapboxNavigation()
        val routeOptions = mockk<RouteOptions>()
        val routerOrigin = mockk<RouterOrigin>()
        val callbackSlot = slot<NavigationRouterCallback>()
        every { mapboxNavigation.requestRoutes(any(), capture(callbackSlot)) } returns 123L

        sut.onAttached(mapboxNavigation)
        store.dispatch(RoutePreviewAction.FetchOptions(routeOptions))
        callbackSlot.captured.onCanceled(routeOptions, routerOrigin)

        val readyState = store.state.value.previewRoutes as? RoutePreviewState.Canceled
        assertNotNull(readyState)
        assertEquals(readyState?.routeOptions, routeOptions)
        assertEquals(readyState?.routerOrigin, routerOrigin)
    }

    private fun mockMapboxNavigation(): MapboxNavigation {
        val mapboxNavigation = mockk<MapboxNavigation>(relaxed = true)
        every { MapboxNavigationApp.current() } returns mapboxNavigation
        return mapboxNavigation
    }
}
