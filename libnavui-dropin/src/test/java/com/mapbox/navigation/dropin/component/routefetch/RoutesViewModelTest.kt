package com.mapbox.navigation.dropin.component.routefetch

import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.internal.extensions.inferDeviceLocale
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.NavigationRouterCallback
import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.testing.MainCoroutineRule
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
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

@OptIn(ExperimentalCoroutinesApi::class)
internal class RoutesViewModelTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    private val routesViewModel = RoutesViewModel()

    @Before
    fun setUp() {
        mockkStatic("com.mapbox.navigation.base.internal.extensions.ContextEx")
    }

    @After
    fun cleanUp() {
        unmockkAll()
    }

    @Test
    fun `default state is Empty`() {
        val mapboxNavigation = mockk<MapboxNavigation>(relaxed = true)

        routesViewModel.onAttached(mapboxNavigation)

        assertEquals(RoutesState.Empty, routesViewModel.state.value)
    }

    @Test
    fun `route changes with routes will be Ready state`() {
        val mapboxNavigation = mockk<MapboxNavigation>(relaxed = true)
        every { mapboxNavigation.registerRoutesObserver(any()) } answers {
            firstArg<RoutesObserver>().onRoutesChanged(
                mockk {
                    every { navigationRoutes } returns listOf(mockk())
                }
            )
        }

        routesViewModel.onAttached(mapboxNavigation)

        assertTrue(routesViewModel.state.value is RoutesState.Ready)
    }

    @Test
    fun `route changes to empty route will be Empty state`() {
        val mapboxNavigation = mockk<MapboxNavigation>(relaxed = true)
        every { mapboxNavigation.registerRoutesObserver(any()) } answers {
            firstArg<RoutesObserver>().onRoutesChanged(
                mockk {
                    every { navigationRoutes } returns emptyList()
                }
            )
        }

        routesViewModel.onAttached(mapboxNavigation)

        assertTrue(routesViewModel.state.value is RoutesState.Empty)
    }

    @Test
    fun `RoutesAction FetchPoints will request routes with default options`() {
        val mapboxNavigation = mockMapboxNavigation()

        routesViewModel.onAttached(mapboxNavigation)
        routesViewModel.invoke(RoutesAction.FetchPoints(mockRoutePoints()))

        assertTrue(routesViewModel.state.value is RoutesState.Fetching)
        verify { mapboxNavigation.requestRoutes(any(), any<NavigationRouterCallback>()) }
    }

    @Test
    fun `RoutesAction FetchPoints is canceled when onDetached is called`() {
        val mapboxNavigation = mockMapboxNavigation()
        every { mapboxNavigation.requestRoutes(any(), any<NavigationRouterCallback>()) } answers {
            123L
        }

        routesViewModel.onAttached(mapboxNavigation)
        routesViewModel.invoke(RoutesAction.FetchPoints(mockRoutePoints()))
        routesViewModel.onDetached(mapboxNavigation)

        verify { mapboxNavigation.cancelRouteRequest(123L) }
    }

    @Test
    fun `RoutesAction FetchPoints will cancel previous`() {
        val mapboxNavigation = mockMapboxNavigation()

        routesViewModel.onAttached(mapboxNavigation)
        routesViewModel.invoke(RoutesAction.FetchPoints(mockRoutePoints()))

        assertTrue(routesViewModel.state.value is RoutesState.Fetching)
        verify { mapboxNavigation.requestRoutes(any(), any<NavigationRouterCallback>()) }
    }

    @Test
    fun `RoutesAction FetchPoints will go to Ready state when onRoutesReady`() {
        val mapboxNavigation = mockMapboxNavigation()
        val routes = listOf<NavigationRoute>(mockk())
        every { mapboxNavigation.requestRoutes(any(), any<NavigationRouterCallback>()) } answers {
            secondArg<NavigationRouterCallback>().onRoutesReady(routes, mockk())
            123L
        }

        routesViewModel.onAttached(mapboxNavigation)
        routesViewModel.invoke(RoutesAction.FetchPoints(mockRoutePoints()))
        routesViewModel.onDetached(mapboxNavigation)

        val readyState = routesViewModel.state.value as? RoutesState.Ready
        assertNotNull(readyState)
        assertEquals(readyState?.routes, routes)
        verify(exactly = 0) { mapboxNavigation.cancelRouteRequest(123L) }
    }

    @Test
    fun `RoutesAction FetchPoints will go to Failed state when onFailure`() {
        val mapboxNavigation = mockMapboxNavigation()
        val reasons = listOf<RouterFailure>(mockk())
        val routeOptions = mockk<RouteOptions>()
        every { mapboxNavigation.requestRoutes(any(), any<NavigationRouterCallback>()) } answers {
            secondArg<NavigationRouterCallback>().onFailure(reasons, routeOptions)
            123L
        }

        routesViewModel.onAttached(mapboxNavigation)
        routesViewModel.invoke(RoutesAction.FetchPoints(mockRoutePoints()))
        routesViewModel.onDetached(mapboxNavigation)

        val readyState = routesViewModel.state.value as? RoutesState.Failed
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
        every { mapboxNavigation.requestRoutes(any(), any<NavigationRouterCallback>()) } answers {
            secondArg<NavigationRouterCallback>().onCanceled(routeOptions, routerOrigin)
            123L
        }

        routesViewModel.onAttached(mapboxNavigation)
        routesViewModel.invoke(RoutesAction.FetchPoints(mockRoutePoints()))
        routesViewModel.onDetached(mapboxNavigation)

        val readyState = routesViewModel.state.value as? RoutesState.Canceled
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
        routesViewModel.invoke(RoutesAction.FetchOptions(routeOptions))

        assertTrue(routesViewModel.state.value is RoutesState.Fetching)
        verify { mapboxNavigation.requestRoutes(routeOptions, any<NavigationRouterCallback>()) }
    }

    @Test
    fun `RoutesAction FetchOptions is canceled when onDetached is called`() {
        val mapboxNavigation = mockMapboxNavigation()
        every { mapboxNavigation.requestRoutes(any(), any<NavigationRouterCallback>()) } answers {
            123L
        }

        routesViewModel.onAttached(mapboxNavigation)
        routesViewModel.invoke(RoutesAction.FetchOptions(mockk()))
        routesViewModel.onDetached(mapboxNavigation)

        verify { mapboxNavigation.cancelRouteRequest(123L) }
    }

    @Test
    fun `RoutesAction FetchOptions will go to Ready state when onRoutesReady`() {
        val mapboxNavigation = mockMapboxNavigation()
        val routes = listOf<NavigationRoute>(mockk())
        every { mapboxNavigation.requestRoutes(any(), any<NavigationRouterCallback>()) } answers {
            secondArg<NavigationRouterCallback>().onRoutesReady(routes, mockk())
            123L
        }
        val routeOptions = mockk<RouteOptions>()

        routesViewModel.onAttached(mapboxNavigation)
        routesViewModel.invoke(RoutesAction.FetchOptions(routeOptions))

        val readyState = routesViewModel.state.value as? RoutesState.Ready
        assertNotNull(readyState)
        assertEquals(readyState?.routes, routes)
    }

    @Test
    fun `RoutesAction FetchOptions will go to Failed state when onFailure`() {
        val mapboxNavigation = mockMapboxNavigation()
        val reasons = listOf<RouterFailure>(mockk())
        val routeOptions = mockk<RouteOptions>()
        every { mapboxNavigation.requestRoutes(any(), any<NavigationRouterCallback>()) } answers {
            secondArg<NavigationRouterCallback>().onFailure(reasons, routeOptions)
            123L
        }

        routesViewModel.onAttached(mapboxNavigation)
        routesViewModel.invoke(RoutesAction.FetchOptions(routeOptions))

        val readyState = routesViewModel.state.value as? RoutesState.Failed
        assertNotNull(readyState)
        assertEquals(readyState?.reasons, reasons)
        assertEquals(readyState?.routeOptions, routeOptions)
    }

    @Test
    fun `RoutesAction FetchOptions will go to Canceled state when onCanceled`() {
        val mapboxNavigation = mockMapboxNavigation()
        val routeOptions = mockk<RouteOptions>()
        val routerOrigin = mockk<RouterOrigin>()
        every { mapboxNavigation.requestRoutes(any(), any<NavigationRouterCallback>()) } answers {
            secondArg<NavigationRouterCallback>().onCanceled(routeOptions, routerOrigin)
            123L
        }

        routesViewModel.onAttached(mapboxNavigation)
        routesViewModel.invoke(RoutesAction.FetchOptions(routeOptions))

        val readyState = routesViewModel.state.value as? RoutesState.Canceled
        assertNotNull(readyState)
        assertEquals(readyState?.routeOptions, routeOptions)
        assertEquals(readyState?.routerOrigin, routerOrigin)
    }

    @Test
    fun `RoutesAction SetRoute with routes will setNavigationRoutes and go to Ready state`() {
        val mapboxNavigation = mockMapboxNavigation()
        val navigationRoutes = listOf<NavigationRoute>(mockk())

        routesViewModel.onAttached(mapboxNavigation)
        routesViewModel.invoke(RoutesAction.SetRoutes(navigationRoutes))

        verify { mapboxNavigation.setNavigationRoutes(navigationRoutes) }
        val readyState = routesViewModel.state.value as? RoutesState.Ready
        assertNotNull(readyState)
        assertEquals(readyState?.routes, navigationRoutes)
    }

    @Test
    fun `RoutesAction SetRoute with no routes will setNavigationRoutes and go to Empty state`() {
        val mapboxNavigation = mockMapboxNavigation()

        routesViewModel.onAttached(mapboxNavigation)
        routesViewModel.invoke(RoutesAction.SetRoutes(emptyList()))

        verify { mapboxNavigation.setNavigationRoutes(emptyList()) }
        assertTrue(routesViewModel.state.value is RoutesState.Empty)
    }

    private fun mockMapboxNavigation() = mockk<MapboxNavigation>(relaxed = true) {
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

    private fun mockRoutePoints() = listOf(
        Point.fromLngLat(-122.2750659, 37.8052036),
        Point.fromLngLat(-122.2647245, 37.8138895)
    )
}
