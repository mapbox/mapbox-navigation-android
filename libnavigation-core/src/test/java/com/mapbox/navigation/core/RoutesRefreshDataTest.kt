package com.mapbox.navigation.core

import com.mapbox.navigation.base.route.NavigationRoute
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class RoutesRefreshDataTest {

    @Test
    fun `allRoutesProgressData for no alternatives`() {
        val primaryRoute = mockk<NavigationRoute>(relaxed = true)
        val primaryRouteProgressData = RouteProgressData(1, 2, 3)
        val expected = listOf(primaryRoute to primaryRouteProgressData)

        val actual = RoutesRefreshData(primaryRoute, primaryRouteProgressData, emptyList())

        assertEquals(expected, actual.allRoutesRefreshData)
    }

    @Test
    fun `allRoutesProgressData for alternatives`() {
        val primaryRoute = mockk<NavigationRoute>(relaxed = true)
        val alternativeRoute = mockk<NavigationRoute>(relaxed = true)
        val primaryRouteProgressData = RouteProgressData(1, 2, 3)
        val alternativeRouteProgressData = RouteProgressData(4, 5, 6)
        val expected = listOf(
            primaryRoute to primaryRouteProgressData,
            alternativeRoute to alternativeRouteProgressData
        )

        val actual = RoutesRefreshData(
            primaryRoute,
            primaryRouteProgressData,
            listOf(alternativeRoute to alternativeRouteProgressData)
        )

        assertEquals(expected, actual.allRoutesRefreshData)
    }
}
