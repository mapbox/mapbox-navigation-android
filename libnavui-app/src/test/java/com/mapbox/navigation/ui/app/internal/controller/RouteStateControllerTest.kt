package com.mapbox.navigation.ui.app.internal.controller

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.ui.app.internal.routefetch.RoutesAction
import com.mapbox.navigation.ui.app.testing.TestStore
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalPreviewMapboxNavigationAPI::class)
internal class RouteStateControllerTest {

    @Test
    fun `when RoutesAction SetRoute it sets route to mapboxNavigation`() {
        val store = spyk(TestStore())
        val sut = RouteStateController(store)
        val mapboxNavigation = mockk<MapboxNavigation>(relaxed = true)
        val routes = listOf(mockk<NavigationRoute>())
        every { mapboxNavigation.registerRoutesObserver(any()) } answers {
            firstArg<RoutesObserver>().onRoutesChanged(
                mockk {
                    every { navigationRoutes } returns routes
                }
            )
        }

        sut.onAttached(mapboxNavigation)

        store.dispatch(RoutesAction.SetRoutes(routes))

        assertEquals(store.state.value.routes.size, routes.size)
    }

    @Test
    fun `when RoutesAction SetRouteIndex it sets route to mapboxNavigation`() {
        val store = spyk(TestStore())
        val sut = RouteStateController(store)
        val mapboxNavigation = mockk<MapboxNavigation>(relaxed = true)
        val routes = listOf(mockk<NavigationRoute>())
        every { mapboxNavigation.registerRoutesObserver(any()) } answers {
            firstArg<RoutesObserver>().onRoutesChanged(
                mockk {
                    every { navigationRoutes } returns routes
                }
            )
        }

        sut.onAttached(mapboxNavigation)

        store.dispatch(RoutesAction.SetRoutes(routes, 1))

        assertEquals(store.state.value.routes.size, routes.size)
    }
}
