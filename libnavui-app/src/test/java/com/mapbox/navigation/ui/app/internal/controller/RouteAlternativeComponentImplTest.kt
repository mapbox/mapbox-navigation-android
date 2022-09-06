package com.mapbox.navigation.ui.app.internal.controller

import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.ui.app.internal.Store
import com.mapbox.navigation.ui.app.internal.routefetch.RoutesAction
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class RouteAlternativeComponentImplTest {

    private val store = mockk<Store> {
        every { dispatch(any()) } just Runs
    }
    private val sut = RouteAlternativeComponentImpl(store)

    @Test
    fun `when alternative routes are available`() {
        val mapboxNavigation = mockk<MapboxNavigation>(relaxed = true)
        val currentLegIndex = 0
        val routes = listOf<NavigationRoute>(mockk(), mockk())

        sut.onAlternativeRoutesUpdated(currentLegIndex, mapboxNavigation, routes)

        verify {
            store.dispatch(RoutesAction.SetRoutes(routes, currentLegIndex))
        }
    }
}
