@file:OptIn(ExperimentalMapboxNavigationAPI::class)

package com.mapbox.navigation.base.route

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.DirectionsWaypoint
import com.mapbox.api.directions.v5.models.utils.toHashCode
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.internal.route.operations.RouteOperations
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.navigation.testing.factories.createDirectionsRoute
import com.mapbox.navigation.testing.factories.createNavigationRoute
import com.mapbox.navigation.testing.factories.createRouteOptions
import com.mapbox.navigation.testing.factories.createWaypoint
import com.mapbox.navigator.RouteInterface
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test

class NavigationRouteHashCodeTest {

    @get:Rule
    val loggerRule = LoggingFrontendTestRule()

    @Test
    fun `hash code combines id, directions route and waypoints hashes`() {
        val route = createNavigationRoute()

        var expected = route.id.hashCode()
        expected = 31 * expected + route.directionsRoute.hashCode().toLong().toHashCode()
        expected = 31 * expected + route.waypoints.hashCode()

        assertEquals(expected, route.hashCode())
    }

    @Test
    fun `repeated hash code calls hash the content only once`() {
        val waypoints = HashCountingList(listOf(createWaypoint()))
        val route = createRoute(waypoints = waypoints)

        repeat(3) { route.hashCode() }

        assertEquals(1, waypoints.hashCodeInvocations)
    }

    @Test
    fun `equal routes are equal and share a hash code`() {
        val route1 = createNavigationRoute()
        val route2 = createNavigationRoute()

        assertEquals(route1, route2)
        assertEquals(route1.hashCode(), route2.hashCode())
    }

    @Test
    fun `routes with the same id and different directions routes are not equal`() {
        val route1 = createNavigationRoute(
            directionsRoute = createDirectionsRoute(distance = 5.0),
        )
        val route2 = createNavigationRoute(
            directionsRoute = createDirectionsRoute(distance = 6.0),
        )

        assertEquals(route1.id, route2.id)
        assertNotEquals(route1, route2)
    }

    @Test
    fun `routes with the same id and different waypoints are not equal`() {
        val route1 = createNavigationRoute(
            responseWaypoints = listOf(createWaypoint(name = "first")),
        )
        val route2 = createNavigationRoute(
            responseWaypoints = listOf(createWaypoint(name = "second")),
        )

        assertEquals(route1.id, route2.id)
        assertNotEquals(route1, route2)
    }

    @Test
    fun `comparisons hash the waypoints once and reuse the cached value`() {
        val waypoints1 = HashCountingList(listOf(createWaypoint(name = "first")))
        val waypoints2 = HashCountingList(listOf(createWaypoint(name = "second")))
        val route1 = createRoute(waypoints = waypoints1)
        val route2 = createRoute(waypoints = waypoints2)

        repeat(3) { assertNotEquals(route1, route2) }

        assertEquals(1, waypoints1.hashCodeInvocations)
        assertEquals(1, waypoints2.hashCodeInvocations)
    }

    @Test
    fun `comparisons decide by hash and never walk the waypoints field by field`() {
        val waypoints1 = HashCountingList(listOf(createWaypoint(name = "first")))
        val waypoints2 = HashCountingList(listOf(createWaypoint(name = "second")))
        val route1 = createRoute(waypoints = waypoints1)
        val route2 = createRoute(waypoints = waypoints2)

        repeat(3) { assertNotEquals(route1, route2) }

        assertEquals(0, waypoints1.equalsInvocations)
        assertEquals(0, waypoints2.equalsInvocations)
    }

    @Test
    fun `comparing an instance with itself does not hash the content`() {
        val waypoints = HashCountingList(listOf(createWaypoint()))
        val route = createNavigationRoute(responseWaypoints = waypoints)

        assertEquals(route, route)

        assertEquals(0, waypoints.hashCodeInvocations)
    }

    @Test
    fun `routes with different ids are not equal without hashing the content`() {
        val waypoints1 = HashCountingList(listOf(createWaypoint()))
        val waypoints2 = HashCountingList(listOf(createWaypoint()))
        val route1 = createRoute(waypoints = waypoints1, id = "test-uuid#0")
        val route2 = createRoute(waypoints = waypoints2, id = "test-uuid#1")

        assertNotEquals(route1, route2)

        assertEquals(0, waypoints1.hashCodeInvocations)
        assertEquals(0, waypoints2.hashCodeInvocations)
    }

    private companion object {

        fun createRoute(
            directionsRoute: DirectionsRoute = createDirectionsRoute(),
            waypoints: List<DirectionsWaypoint>? = listOf(createWaypoint()),
            id: String = "test-uuid#0",
        ): NavigationRoute = NavigationRoute(
            directionsRoute = directionsRoute,
            waypoints = waypoints,
            routeOptions = createRouteOptions(),
            nativeRoute = mockk<RouteInterface>(relaxed = true) {
                every { routeId } returns id
            },
            expirationTimeElapsedSeconds = null,
            responseOriginAPI = ResponseOriginAPI.DIRECTIONS_API,
            operations = mockk<RouteOperations>(),
        )

        class HashCountingList<T>(
            private val delegate: List<T>,
        ) : List<T> by delegate {

            var hashCodeInvocations = 0
                private set

            var equalsInvocations = 0
                private set

            override fun hashCode(): Int {
                hashCodeInvocations++
                return delegate.hashCode()
            }

            override fun equals(other: Any?): Boolean {
                equalsInvocations++
                return delegate == other
            }
        }
    }
}
