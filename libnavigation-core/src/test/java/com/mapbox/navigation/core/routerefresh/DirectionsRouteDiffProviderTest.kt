package com.mapbox.navigation.core.routerefresh

import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.mapbox.api.directions.v5.models.DirectionsWaypoint
import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.testing.factories.createClosure
import com.mapbox.navigation.testing.factories.createDirectionsRoute
import com.mapbox.navigation.testing.factories.createIncident
import com.mapbox.navigation.testing.factories.createMaxSpeed
import com.mapbox.navigation.testing.factories.createNavigationRoute
import com.mapbox.navigation.testing.factories.createRouteLeg
import com.mapbox.navigation.testing.factories.createRouteLegAnnotation
import com.mapbox.navigation.testing.factories.createRouteOptions
import org.junit.Assert.assertEquals
import org.junit.Test

class DirectionsRouteDiffProviderTest {

    private val routeDiffProvider = DirectionsRouteDiffProvider()

    @Test
    fun buildRouteDiffs_oldLegsAreNullWaypointsDidNotChange() {
        val waypoint1 = createTestWaypoint(chargeAtArrival = 80)
        val oldRoute = createTestNavigationRoute(
            null,
            waypointsPerRoute = true,
            waypoints = listOf(waypoint1)
        )
        val newRoute = createTestNavigationRoute(
            listOf(
                createTestLeg(57.14, 14.28, 85.71, 120, "low", 42, 89, 34, 22)
            ),
            waypointsPerRoute = true,
            waypoints = listOf(waypoint1)
        )
        assertEquals(
            emptyList<String>(),
            routeDiffProvider.buildRouteDiffs(oldRoute, newRoute, currentLegIndex = 0),
        )
    }

    @Test
    fun buildRouteDiffs_oldLegsAreNullWaypointsChanged() {
        val waypoint1 = createTestWaypoint(chargeAtArrival = 80)
        val waypoint2 = createTestWaypoint(chargeAtArrival = 75)
        val oldRoute = createTestNavigationRoute(
            null,
            waypointsPerRoute = true,
            waypoints = listOf(waypoint1)
        )
        val newRoute = createTestNavigationRoute(
            listOf(
                createTestLeg(57.14, 14.28, 85.71, 120, "low", 42, 89, 34, 22)
            ),
            waypointsPerRoute = true,
            waypoints = listOf(waypoint2)
        )
        assertEquals(
            listOf(
                "Updated waypoints at route testDiff#0",
            ),
            routeDiffProvider.buildRouteDiffs(oldRoute, newRoute, currentLegIndex = 0),
        )
    }

    @Test
    fun buildRouteDiffs_newLegsAreNullWaypointsDidNotChange() {
        val waypoint1 = createTestWaypoint(chargeAtArrival = 80)
        val oldRoute = createTestNavigationRoute(
            listOf(
                createTestLeg(57.14, 14.28, 85.71, 120, "low", 42, 89, 34, 22)
            ),
            waypointsPerRoute = true,
            waypoints = listOf(waypoint1)
        )
        val newRoute = createTestNavigationRoute(
            null,
            waypointsPerRoute = true,
            waypoints = listOf(waypoint1)
        )
        assertEquals(
            emptyList<String>(),
            routeDiffProvider.buildRouteDiffs(oldRoute, newRoute, currentLegIndex = 0),
        )
    }

    @Test
    fun buildRouteDiffs_newLegsAreNullWaypointsChanged() {
        val waypoint1 = createTestWaypoint(chargeAtArrival = 80)
        val waypoint2 = createTestWaypoint(chargeAtArrival = 75)
        val oldRoute = createTestNavigationRoute(
            listOf(
                createTestLeg(57.14, 14.28, 85.71, 120, "low", 42, 89, 34, 22)
            ),
            waypointsPerRoute = true,
            waypoints = listOf(waypoint1)
        )
        val newRoute = createTestNavigationRoute(
            null,
            waypointsPerRoute = true,
            waypoints = listOf(waypoint2)
        )
        assertEquals(
            listOf(
                "Updated waypoints at route testDiff#0",
            ),
            routeDiffProvider.buildRouteDiffs(oldRoute, newRoute, currentLegIndex = 0),
        )
    }

    @Test
    fun buildRouteDiffs_oldRouteHasMoreLegs() {
        val oldRoute = createTestNavigationRoute(
            listOf(
                createTestLeg(142.8, 28.57, 71.42, 120, "unknown", 42, 89, 34, 22),
                createTestLeg(57.14, 8.571, 42.85, 90, "low", 71, 90, 34, 22),
            ),
            waypointsPerRoute = true,
            waypoints = emptyList()
        )
        val newRoute = createTestNavigationRoute(
            listOf(
                createTestLeg(57.14, 14.28, 85.71, 120, "low", 42, 89, 34, 22),
            ),
            waypointsPerRoute = true,
            waypoints = emptyList()
        )

        assertEquals(
            listOf(
                "Updated distance, duration, speed, congestion at route testDiff#0 leg 0",
            ),
            routeDiffProvider.buildRouteDiffs(oldRoute, newRoute, currentLegIndex = 0),
        )
    }

    @Test
    fun buildRouteDiffs_newRouteHasMoreLegs() {
        val oldRoute = createTestNavigationRoute(
            listOf(
                createTestLeg(57.14, 14.28, 85.71, 120, "low", 42, 89, 34, 22),
            ),
            waypointsPerRoute = true,
            waypoints = emptyList()
        )
        val newRoute = createTestNavigationRoute(
            listOf(
                createTestLeg(142.8, 28.57, 71.42, 120, "unknown", 42, 89, 34, 22),
                createTestLeg(57.14, 8.571, 42.85, 90, "low", 71, 90, 34, 22),
            ),
            waypointsPerRoute = true,
            waypoints = emptyList()
        )

        assertEquals(
            listOf(
                "Updated distance, duration, speed, congestion at route testDiff#0 leg 0",
            ),
            routeDiffProvider.buildRouteDiffs(oldRoute, newRoute, currentLegIndex = 0),
        )
    }

    @Test
    fun buildRouteDiffs() {
        val waypoint1 = createTestWaypoint(chargeAtArrival = 80)
        val waypoint2 = createTestWaypoint(chargeAtArrival = 81)
        val oldRoute = createTestNavigationRoute(
            listOf(
                createTestLeg(57.14, 8.571, 42.85, 90, "low", 71, 90, 34, 22),
                createTestLeg(142.8, 28.57, 71.42, 120, "unknown", 42, 89, 34, 22),
                createTestLeg(85.71, 42.85, 57.14, 90, "unknown", 14, 87, 34, 22),
                createTestLeg(71.42, 14.28, 85.71, 90, "low", 57, 85, 34, 22),
                createTestLeg(42.85, 57.14, 28.57, 120, "low", 28, 80, 34, 22),
                createRouteLeg(),
            ),
            waypointsPerRoute = true,
            waypoints = listOf(waypoint1)
        )
        val newRoute = createTestNavigationRoute(
            listOf(
                createRouteLeg(annotation = null),
                createTestLeg(57.14, 14.28, 85.71, 120, "low", 42, 89, 34, 22),
                createTestLeg(85.71, 42.85, 57.14, 90, "unknown", 14, 87, 34, 22),
                createTestLeg(71.42, 28.57, 42.85, 120, "unknown", 57, 84, 37, 22),
                createTestLeg(142.8, 57.14, 28.57, 90, "low", 71, 79, 34, 62),
                createRouteLeg(
                    incidents = listOf(createIncident()),
                    closures = listOf(createClosure())
                ),
            ),
            waypointsPerRoute = true,
            waypoints = listOf(waypoint2)
        )

        assertEquals(
            listOf(
                "Updated distance, duration, speed, congestion at route testDiff#0 leg 1",
                "Updated duration, speed, maxSpeed, congestion, state_of_charge, freeflow_speed " +
                    "at route testDiff#0 leg 3",
                "Updated distance, maxSpeed, congestionNumeric, state_of_charge, current_speed " +
                    "at route testDiff#0 leg 4",
                "Updated incidents, closures at route testDiff#0 leg 5",
                "Updated waypoints at route testDiff#0",
            ),
            routeDiffProvider.buildRouteDiffs(oldRoute, newRoute, currentLegIndex = 1),
        )
    }

    private fun createTestWaypoint(chargeAtArrival: Int): DirectionsWaypoint =
        DirectionsWaypoint.builder()
            .distance(1.0)
            .name("name")
            .rawLocation(doubleArrayOf(1.1, 2.2))
            .unrecognizedJsonProperties(
                mapOf(
                    "metadata" to JsonObject().apply {
                        add("charge_at_arrival", JsonPrimitive(chargeAtArrival))
                    }
                )
            )
            .build()

    private fun createTestNavigationRoute(
        legs: List<RouteLeg>?,
        waypointsPerRoute: Boolean? = null,
        waypoints: List<DirectionsWaypoint> = emptyList()
    ): NavigationRoute {
        return createNavigationRoute(
            createDirectionsRoute(
                requestUuid = "testDiff",
                legs = legs,
                routeOptions = createRouteOptions(waypointsPerRoute = waypointsPerRoute),
                waypoints = waypoints
            )
        )
    }

    private fun createTestLeg(
        distance: Double,
        duration: Double,
        speed: Double,
        maxSpeed: Int,
        congestion: String,
        congestionNumeric: Int,
        stateOfCharge: Int,
        freeFlowSpeed: Int,
        currentSpeed: Int
    ): RouteLeg {
        return createRouteLeg(
            createRouteLegAnnotation(
                congestion = listOf(congestion),
                congestionNumeric = listOf(congestionNumeric),
                maxSpeed = listOf(createMaxSpeed(speed = maxSpeed)),
                distance = listOf(distance),
                duration = listOf(duration),
                speed = listOf(speed),
                stateOfCharge = listOf(stateOfCharge),
                freeFlowSpeed = listOf(freeFlowSpeed),
                currentSpeed = listOf(currentSpeed)
            ),
        )
    }
}
