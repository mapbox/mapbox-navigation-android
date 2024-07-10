package com.mapbox.navigation.base.internal.route

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.navigation.base.route.NavigationRoute
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RouteCompatibilityCacheTest {
    @Test
    fun `empty cache returns null`() {
        assertNull(RouteCompatibilityCache.getFor(mockk()))
    }

    @Test
    fun `directions session cache returns the route`() {
        val directionsRouteMock: DirectionsRoute = mockk()
        val navigationRoute: NavigationRoute = mockk {
            every { directionsRoute } returns directionsRouteMock
        }
        RouteCompatibilityCache.setDirectionsSessionResult(listOf(navigationRoute))

        val result = RouteCompatibilityCache.getFor(directionsRouteMock)

        assertEquals(navigationRoute, result)
    }

    @Test
    fun `creation cache returns the route`() {
        val directionsRouteMock: DirectionsRoute = mockk()
        val navigationRoute: NavigationRoute = mockk {
            every { directionsRoute } returns directionsRouteMock
        }
        RouteCompatibilityCache.cacheCreationResult(listOf(navigationRoute))

        val result = RouteCompatibilityCache.getFor(directionsRouteMock)

        assertEquals(navigationRoute, result)
    }

    @Test
    fun `directions session setter clears the cache`() {
        val directionsRouteMock1: DirectionsRoute = mockk()
        val navigationRoute1: NavigationRoute = mockk {
            every { directionsRoute } returns directionsRouteMock1
        }
        RouteCompatibilityCache.cacheCreationResult(listOf(navigationRoute1))

        val directionsRouteMock2: DirectionsRoute = mockk()
        val navigationRoute2: NavigationRoute = mockk {
            every { directionsRoute } returns directionsRouteMock2
        }
        RouteCompatibilityCache.setDirectionsSessionResult(listOf(navigationRoute2))

        assertNull(RouteCompatibilityCache.getFor(directionsRouteMock1))

        val result = RouteCompatibilityCache.getFor(directionsRouteMock2)

        assertEquals(navigationRoute2, result)
    }

    @Test
    fun `null returned if both caches missed`() {
        val directionsRouteMock2: DirectionsRoute = mockk()
        val navigationRoute2: NavigationRoute = mockk {
            every { directionsRoute } returns directionsRouteMock2
        }
        RouteCompatibilityCache.setDirectionsSessionResult(listOf(navigationRoute2))

        val directionsRouteMock1: DirectionsRoute = mockk()
        val navigationRoute1: NavigationRoute = mockk {
            every { directionsRoute } returns directionsRouteMock1
        }
        RouteCompatibilityCache.cacheCreationResult(listOf(navigationRoute1))

        assertNull(RouteCompatibilityCache.getFor(mockk()))
    }

    @Test
    fun `both caches accessible if additional routes created after directions session update`() {
        val directionsRouteMock2: DirectionsRoute = mockk()
        val navigationRoute2: NavigationRoute = mockk {
            every { directionsRoute } returns directionsRouteMock2
        }
        RouteCompatibilityCache.setDirectionsSessionResult(listOf(navigationRoute2))

        val directionsRouteMock1: DirectionsRoute = mockk()
        val navigationRoute1: NavigationRoute = mockk {
            every { directionsRoute } returns directionsRouteMock1
        }
        RouteCompatibilityCache.cacheCreationResult(listOf(navigationRoute1))

        assertEquals(navigationRoute1, RouteCompatibilityCache.getFor(directionsRouteMock1))
        assertEquals(navigationRoute2, RouteCompatibilityCache.getFor(directionsRouteMock2))
    }

    @Test
    fun `cache size does not exceed maximum`() {
        val route1 = mockk<NavigationRoute> {
            every { directionsRoute } returns mockk()
        }
        val route2 = mockk<NavigationRoute> {
            every { directionsRoute } returns mockk()
        }
        val route3 = mockk<NavigationRoute> {
            every { directionsRoute } returns mockk()
        }
        val route4 = mockk<NavigationRoute> {
            every { directionsRoute } returns mockk()
        }

        RouteCompatibilityCache.setDirectionsSessionResult(listOf(route1, route2, route3, route4))

        assertNull(RouteCompatibilityCache.getFor(route1.directionsRoute))
        assertEquals(route2, RouteCompatibilityCache.getFor(route2.directionsRoute))
        assertEquals(route3, RouteCompatibilityCache.getFor(route3.directionsRoute))
        assertEquals(route4, RouteCompatibilityCache.getFor(route4.directionsRoute))
    }
}
