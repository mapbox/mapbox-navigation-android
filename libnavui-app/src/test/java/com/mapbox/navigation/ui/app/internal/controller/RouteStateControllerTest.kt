package com.mapbox.navigation.ui.app.internal.controller

import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.ui.app.internal.routefetch.RoutesAction
import com.mapbox.navigation.ui.app.testing.TestStore
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Test

internal class RouteStateControllerTest {

    private val store = TestStore()
    private val sut = RouteStateController(store)
    private val mapboxNavigation = mockk<MapboxNavigation>(relaxed = true)
    private val routes = listOf(mockk<NavigationRoute>())

    @Test
    fun `when RoutesAction SetRoute it sets route to mapboxNavigation`() {
        sut.onAttached(mapboxNavigation)

        store.dispatch(RoutesAction.SetRoutes(routes, legIndex = 1))

        assertEquals(store.state.value.routes, routes)
        verify(exactly = 1) { mapboxNavigation.setNavigationRoutes(routes, initialLegIndex = 1) }
    }

    @Test
    fun `when RoutesAction SynchronizeRoutes it does not set route to mapboxNavigation`() {
        sut.onAttached(mapboxNavigation)

        store.dispatch(RoutesAction.SynchronizeRoutes(routes))

        assertEquals(store.state.value.routes, routes)
        verify(exactly = 0) { mapboxNavigation.setNavigationRoutes(any(), any(), any()) }
    }

    @Test
    fun `when RoutesObserver is invoked it does not set route to mapboxNavigation`() {
        every { mapboxNavigation.registerRoutesObserver(any()) } answers {
            firstArg<RoutesObserver>().onRoutesChanged(
                mockk {
                    every { navigationRoutes } returns this@RouteStateControllerTest.routes
                }
            )
        }

        sut.onAttached(mapboxNavigation)

        assertEquals(store.state.value.routes, routes)
        verify(exactly = 0) { mapboxNavigation.setNavigationRoutes(any(), any(), any()) }
    }
}
