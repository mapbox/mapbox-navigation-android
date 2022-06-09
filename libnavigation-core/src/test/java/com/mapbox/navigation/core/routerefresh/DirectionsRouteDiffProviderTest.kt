package com.mapbox.navigation.core.routerefresh

import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.testing.factories.createDirectionsRoute
import com.mapbox.navigation.testing.factories.createIncident
import com.mapbox.navigation.testing.factories.createMaxSpeed
import com.mapbox.navigation.testing.factories.createNavigationRoute
import com.mapbox.navigation.testing.factories.createRouteLeg
import com.mapbox.navigation.testing.factories.createRouteLegAnnotation
import org.junit.Assert.assertEquals
import org.junit.Test

class DirectionsRouteDiffProviderTest {

    private val routeDiffProvider = DirectionsRouteDiffProvider()

    @Test
    fun buildRouteDiffs() {
        val oldRoute = createTestNavigationRoute(
            createTestLeg(57.14, 8.571, 42.85, 90, "low", 71),
            createTestLeg(142.8, 28.57, 71.42, 120, "unknown", 42),
            createTestLeg(85.71, 42.85, 57.14, 90, "unknown", 14),
            createTestLeg(71.42, 14.28, 85.71, 90, "low", 57),
            createTestLeg(42.85, 57.14, 28.57, 120, "low", 28),
            createRouteLeg(),
        )
        val newRoute = createTestNavigationRoute(
            createRouteLeg(annotation = null),
            createTestLeg(57.14, 14.28, 85.71, 120, "low", 42),
            createTestLeg(85.71, 42.85, 57.14, 90, "unknown", 14),
            createTestLeg(71.42, 28.57, 42.85, 120, "unknown", 57),
            createTestLeg(142.8, 57.14, 28.57, 90, "low", 71),
            createRouteLeg(incidents = listOf(createIncident())),
        )

        assertEquals(
            routeDiffProvider.buildRouteDiffs(oldRoute, newRoute, currentLegIndex = 1),
            listOf(
                "Updated distance, duration, speed, congestion at leg 1",
                "Updated duration, speed, maxSpeed, congestion at leg 3",
                "Updated distance, maxSpeed, congestionNumeric at leg 4",
                "Updated incidents at leg 5",
            ),
        )
    }

    private fun createTestNavigationRoute(vararg legs: RouteLeg): NavigationRoute {
        return createNavigationRoute(
            createDirectionsRoute(
                legs = legs.toList()
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
    ): RouteLeg {
        return createRouteLeg(
            createRouteLegAnnotation(
                congestion = listOf(congestion),
                congestionNumeric = listOf(congestionNumeric),
                maxSpeed = listOf(createMaxSpeed(speed = maxSpeed)),
                distance = listOf(distance),
                duration = listOf(duration),
                speed = listOf(speed)
            )
        )
    }
}
