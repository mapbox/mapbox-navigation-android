package com.mapbox.navigation.core.directions.session

import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.core.SetRoutes
import com.mapbox.navigation.core.trip.session.NativeSetRouteValue
import com.mapbox.navigator.RouteAlternative
import com.mapbox.navigator.RouteInterface
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class UtilsTest {

    private val invalidRouteReason = "Route is invalid for navigation"

    @Test
    fun emptyRoutesEmptyProcessedRoutes() {
        val expected = DirectionsSessionRoutes(emptyList(), emptyList(), SetRoutes.CleanUp)

        val actual = Utils.createDirectionsSessionRoutes(
            emptyList(),
            NativeSetRouteValue(emptyList(), emptyList()),
            SetRoutes.CleanUp,
        )

        assertEquals(expected, actual)
    }

    @Test
    fun emptyRoutesNonEmptyProcessedRoutes() {
        val primaryRoute = mockk<NavigationRoute>(relaxed = true)
        val expected = DirectionsSessionRoutes(
            listOf(primaryRoute),
            emptyList(),
            SetRoutes.NewRoutes(0),
        )

        val actual = Utils.createDirectionsSessionRoutes(
            emptyList(),
            NativeSetRouteValue(
                listOf(primaryRoute, mockk(relaxed = true)),
                listOf(mockk(relaxed = true)),
            ),
            SetRoutes.NewRoutes(0),
        )

        assertEquals(expected, actual)
    }

    @Test
    fun singleValidRoute() {
        val primaryRoute = mockk<NavigationRoute>(relaxed = true)
        val expected = DirectionsSessionRoutes(
            listOf(primaryRoute),
            emptyList(),
            SetRoutes.NewRoutes(0),
        )

        val actual = Utils.createDirectionsSessionRoutes(
            listOf(primaryRoute),
            NativeSetRouteValue(
                listOf(primaryRoute, mockk(relaxed = true)),
                emptyList(),
            ),
            SetRoutes.NewRoutes(0),
        )

        assertEquals(expected, actual)
    }

    @Test
    fun singleInvalidRoute() {
        val primaryRoute = route("id#0")
        val expected = DirectionsSessionRoutes(
            listOf(primaryRoute),
            emptyList(),
            SetRoutes.NewRoutes(0),
        )

        val actual = Utils.createDirectionsSessionRoutes(
            listOf(primaryRoute),
            NativeSetRouteValue(
                listOf(primaryRoute, mockk(relaxed = true)),
                listOf(alternativeWithId("id#0")),
            ),
            SetRoutes.NewRoutes(0),
        )

        assertEquals(expected, actual)
    }

    @Test
    fun multipleRoutesAllValid() {
        val primaryRoute = route("id#0")
        val alternative1 = route("id#1")
        val alternative2 = route("id#2")
        val expected = DirectionsSessionRoutes(
            listOf(primaryRoute, alternative1, alternative2),
            emptyList(),
            SetRoutes.NewRoutes(0),
        )

        val actual = Utils.createDirectionsSessionRoutes(
            listOf(primaryRoute, alternative1, alternative2),
            NativeSetRouteValue(
                listOf(primaryRoute, alternative1, alternative2),
                listOf(alternativeWithId("id#1"), alternativeWithId("id#2")),
            ),
            SetRoutes.NewRoutes(0),
        )

        assertEquals(expected, actual)
    }

    @Test
    fun multipleRoutesSomeValid() {
        val primaryRoute = route("id#0")
        val alternative1 = route("id#1")
        val alternative2 = route("id#2")
        val expected = DirectionsSessionRoutes(
            listOf(primaryRoute, alternative2),
            listOf(IgnoredRoute(alternative1, invalidRouteReason)),
            SetRoutes.NewRoutes(0),
        )

        val actual = Utils.createDirectionsSessionRoutes(
            listOf(primaryRoute, alternative1, alternative2),
            NativeSetRouteValue(
                listOf(primaryRoute, alternative1, alternative2),
                listOf(alternativeWithId("id#2")),
            ),
            SetRoutes.NewRoutes(0),
        )

        assertEquals(expected, actual)
    }

    @Test
    fun multipleRoutesAllInvalid() {
        val primaryRoute = route("id#0")
        val alternative1 = route("id#1")
        val alternative2 = route("id#2")
        val expected = DirectionsSessionRoutes(
            listOf(primaryRoute),
            listOf(
                IgnoredRoute(alternative1, invalidRouteReason),
                IgnoredRoute(alternative2, invalidRouteReason),
            ),
            SetRoutes.NewRoutes(0),
        )

        val actual = Utils.createDirectionsSessionRoutes(
            listOf(primaryRoute, alternative1, alternative2),
            NativeSetRouteValue(
                listOf(primaryRoute, alternative1, alternative2),
                emptyList(),
            ),
            SetRoutes.NewRoutes(0),
        )

        assertEquals(expected, actual)
    }

    private fun route(routeId: String): NavigationRoute {
        return mockk(relaxed = true) { every { id } returns routeId }
    }

    private fun alternativeWithId(routeId: String): RouteAlternative {
        val mockRoute = mockk<RouteInterface> {
            every { getRouteId() } returns routeId
        }
        return mockk(relaxed = true) {
            every { route } returns mockRoute
        }
    }
}
