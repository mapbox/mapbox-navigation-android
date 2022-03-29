package com.mapbox.navigation.dropin.component.routefetch

import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.internal.extensions.inferDeviceLocale
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.NavigationRouterCallback
import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.dropin.util.TestStore
import com.mapbox.navigation.testing.MainCoroutineRule
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
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
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalPreviewMapboxNavigationAPI::class)
internal class RoutesViewModelTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    private lateinit var store: TestStore
    private lateinit var routesViewModel: RoutesViewModel

    @Before
    fun setUp() {
        mockkStatic("com.mapbox.navigation.base.internal.extensions.ContextEx")
        mockkObject(MapboxNavigationApp)
        store = spyk(TestStore())
        routesViewModel = RoutesViewModel(store)
    }

    @After
    fun cleanUp() {
        unmockkAll()
    }

    @Test
    fun `onAttached should observe MapboxNavigation route updates and dispatch Ready action`() {
        val mapboxNavigation = mockMapboxNavigation()
        val routesList = listOf<NavigationRoute>(mockk())
        every { mapboxNavigation.registerRoutesObserver(any()) } answers {
            firstArg<RoutesObserver>().onRoutesChanged(
                mockk {
                    every { navigationRoutes } returns routesList
                }
            )
        }

        routesViewModel.onAttached(mapboxNavigation)

        verify { store.dispatch(RoutesAction.Ready(routesList)) }
    }

    @Test
    fun `RoutesAction Ready with an empty list should result in Empty state`() {
        val mapboxNavigation = mockMapboxNavigation()
        every { mapboxNavigation.registerRoutesObserver(any()) } answers {
            firstArg<RoutesObserver>().onRoutesChanged(
                mockk {
                    every { navigationRoutes } returns emptyList()
                }
            )
        }

        routesViewModel.onAttached(mapboxNavigation)

        assertTrue(store.state.value.routes is RoutesState.Empty)
    }

    @Test
    fun `RoutesAction FetchPoints will request routes with default options`() {
        val mapboxNavigation = mockMapboxNavigation()

        routesViewModel.onAttached(mapboxNavigation)
        store.dispatch(RoutesAction.FetchPoints(mockRoutePoints()))

        assertTrue(store.state.value.routes is RoutesState.Fetching)
        verify { mapboxNavigation.requestRoutes(any(), any<NavigationRouterCallback>()) }
    }

    @Test
    fun `RoutesAction FetchPoints is canceled when onDetached is called`() {
        val mapboxNavigation = mockMapboxNavigation()
        every { mapboxNavigation.requestRoutes(any(), any<NavigationRouterCallback>()) } answers {
            123L
        }

        routesViewModel.onAttached(mapboxNavigation)
        store.dispatch(RoutesAction.FetchPoints(mockRoutePoints()))
        routesViewModel.onDetached(mapboxNavigation)

        verify { mapboxNavigation.cancelRouteRequest(123L) }
    }

    @Test
    fun `RoutesAction FetchPoints will cancel previous`() {
        val mapboxNavigation = mockMapboxNavigation()

        routesViewModel.onAttached(mapboxNavigation)
        store.dispatch(RoutesAction.FetchPoints(mockRoutePoints()))

        assertTrue(store.state.value.routes is RoutesState.Fetching)
        verify { mapboxNavigation.requestRoutes(any(), any<NavigationRouterCallback>()) }
    }

    @Test
    fun `RoutesAction FetchPoints will go to Ready state when onRoutesReady`() {
        val mapboxNavigation = mockMapboxNavigation()
        val routes = listOf<NavigationRoute>(mockk())
        val callbackSlot = slot<NavigationRouterCallback>()
        every { mapboxNavigation.requestRoutes(any(), capture(callbackSlot)) } returns 123L

        routesViewModel.onAttached(mapboxNavigation)
        store.dispatch(RoutesAction.FetchPoints(mockRoutePoints()))
        callbackSlot.captured.onRoutesReady(routes, mockk())
        routesViewModel.onDetached(mapboxNavigation)

        val readyState = store.state.value.routes as? RoutesState.Ready
        assertNotNull(readyState)
        assertEquals(readyState?.routes, routes)
        verify(exactly = 0) { mapboxNavigation.cancelRouteRequest(123L) }
    }

    @Test
    fun `RoutesAction FetchPoints will go to Failed state when onFailure`() {
        val mapboxNavigation = mockMapboxNavigation()
        val reasons = listOf<RouterFailure>(mockk())
        val routeOptions = mockk<RouteOptions>()
        val callbackSlot = slot<NavigationRouterCallback>()
        every { mapboxNavigation.requestRoutes(any(), capture(callbackSlot)) } returns 123L

        routesViewModel.onAttached(mapboxNavigation)
        store.dispatch(RoutesAction.FetchPoints(mockRoutePoints()))
        callbackSlot.captured.onFailure(reasons, routeOptions)
        routesViewModel.onDetached(mapboxNavigation)

        val readyState = store.state.value.routes as? RoutesState.Failed
        assertNotNull(readyState)
        assertEquals(readyState?.reasons, reasons)
        assertEquals(readyState?.routeOptions, routeOptions)
        verify(exactly = 0) { mapboxNavigation.cancelRouteRequest(123L) }
    }

    @Test
    fun `RoutesAction FetchPoints will go to Canceled state when onCanceled`() {
        val mapboxNavigation = mockMapboxNavigation()
        val routeOptions = mockk<RouteOptions>()
        val routerOrigin = mockk<RouterOrigin>()
        val callbackSlot = slot<NavigationRouterCallback>()
        every { mapboxNavigation.requestRoutes(any(), capture(callbackSlot)) } returns 123L

        routesViewModel.onAttached(mapboxNavigation)
        store.dispatch(RoutesAction.FetchPoints(mockRoutePoints()))
        callbackSlot.captured.onCanceled(routeOptions, routerOrigin)
        routesViewModel.onDetached(mapboxNavigation)

        val readyState = store.state.value.routes as? RoutesState.Canceled
        assertNotNull(readyState)
        assertEquals(readyState?.routeOptions, routeOptions)
        assertEquals(readyState?.routerOrigin, routerOrigin)
        verify(exactly = 0) { mapboxNavigation.cancelRouteRequest(123L) }
    }

    @Test
    fun `RoutesAction FetchOptions will request routes with the options`() {
        val mapboxNavigation = mockMapboxNavigation()
        val routeOptions = mockk<RouteOptions>()

        routesViewModel.onAttached(mapboxNavigation)
        store.dispatch(RoutesAction.FetchOptions(routeOptions))

        assertTrue(store.state.value.routes is RoutesState.Fetching)
        verify { mapboxNavigation.requestRoutes(routeOptions, any<NavigationRouterCallback>()) }
    }

    @Test
    fun `RoutesAction FetchOptions is canceled when onDetached is called`() {
        val mapboxNavigation = mockMapboxNavigation()
        every { mapboxNavigation.requestRoutes(any(), any<NavigationRouterCallback>()) } answers {
            123L
        }

        routesViewModel.onAttached(mapboxNavigation)
        store.dispatch(RoutesAction.FetchOptions(mockk()))
        routesViewModel.onDetached(mapboxNavigation)

        verify { mapboxNavigation.cancelRouteRequest(123L) }
    }

    @Test
    fun `RoutesAction FetchOptions will go to Ready state when onRoutesReady`() {
        val mapboxNavigation = mockMapboxNavigation()
        val routes = listOf<NavigationRoute>(mockk())
        val callbackSlot = slot<NavigationRouterCallback>()
        every { mapboxNavigation.requestRoutes(any(), capture(callbackSlot)) } returns 123L
        val routeOptions = mockk<RouteOptions>()

        routesViewModel.onAttached(mapboxNavigation)
        store.dispatch(RoutesAction.FetchOptions(routeOptions))
        callbackSlot.captured.onRoutesReady(routes, mockk())

        val readyState = store.state.value.routes as? RoutesState.Ready
        assertNotNull(readyState)
        assertEquals(readyState?.routes, routes)
    }

    @Test
    fun `RoutesAction FetchOptions will go to Failed state when onFailure`() {
        val mapboxNavigation = mockMapboxNavigation()
        val reasons = listOf<RouterFailure>(mockk())
        val routeOptions = mockk<RouteOptions>()
        val callbackSlot = slot<NavigationRouterCallback>()
        every { mapboxNavigation.requestRoutes(any(), capture(callbackSlot)) } returns 123L

        routesViewModel.onAttached(mapboxNavigation)
        store.dispatch(RoutesAction.FetchOptions(routeOptions))
        callbackSlot.captured.onFailure(reasons, routeOptions)

        val readyState = store.state.value.routes as? RoutesState.Failed
        assertNotNull(readyState)
        assertEquals(readyState?.reasons, reasons)
        assertEquals(readyState?.routeOptions, routeOptions)
    }

    @Test
    fun `RoutesAction FetchOptions will go to Canceled state when onCanceled`() {
        val mapboxNavigation = mockMapboxNavigation()
        val routeOptions = mockk<RouteOptions>()
        val routerOrigin = mockk<RouterOrigin>()
        val callbackSlot = slot<NavigationRouterCallback>()
        every { mapboxNavigation.requestRoutes(any(), capture(callbackSlot)) } returns 123L

        routesViewModel.onAttached(mapboxNavigation)
        store.dispatch(RoutesAction.FetchOptions(routeOptions))
        callbackSlot.captured.onCanceled(routeOptions, routerOrigin)

        val readyState = store.state.value.routes as? RoutesState.Canceled
        assertNotNull(readyState)
        assertEquals(readyState?.routeOptions, routeOptions)
        assertEquals(readyState?.routerOrigin, routerOrigin)
    }

    @Test
    fun `RoutesAction SetRoute with routes will setNavigationRoutes and go to Ready state`() {
        val mapboxNavigation = mockMapboxNavigation()
        val navigationRoutes = listOf<NavigationRoute>(mockk())

        routesViewModel.onAttached(mapboxNavigation)
        store.dispatch(RoutesAction.SetRoutes(navigationRoutes))

        verify { mapboxNavigation.setNavigationRoutes(navigationRoutes) }
        val readyState = store.state.value.routes as? RoutesState.Ready
        assertNotNull(readyState)
        assertEquals(readyState?.routes, navigationRoutes)
    }

    @Test
    fun `RoutesAction SetRoute with no routes will setNavigationRoutes and go to Empty state`() {
        val mapboxNavigation = mockMapboxNavigation()

        routesViewModel.onAttached(mapboxNavigation)
        store.dispatch(RoutesAction.SetRoutes(emptyList()))

        verify { mapboxNavigation.setNavigationRoutes(emptyList()) }
        assertTrue(store.state.value.routes is RoutesState.Empty)
    }

    private fun mockMapboxNavigation(): MapboxNavigation {
        val mapboxNavigation = mockk<MapboxNavigation>(relaxed = true) {
            every { getZLevel() } returns 9
            every { navigationOptions } returns mockk {
                every { navigationOptions } returns mockk {
                    every { applicationContext } returns mockk {
                        every { inferDeviceLocale() } returns Locale.ENGLISH
                        every { resources } returns mockk {
                            every { configuration } returns mockk()
                        }
                    }
                }
            }
        }
        every { MapboxNavigationApp.current() } returns mapboxNavigation
        return mapboxNavigation
    }

    private fun mockRoutePoints() = listOf(
        Point.fromLngLat(-122.2750659, 37.8052036),
        Point.fromLngLat(-122.2647245, 37.8138895)
    )
}
