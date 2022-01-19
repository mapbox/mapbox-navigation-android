package com.mapbox.navigation.core.routerefresh

import com.mapbox.api.directions.v5.models.MaxSpeed
import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.api.directions.v5.models.SpeedLimit
import com.mapbox.navigation.base.route.NavigationRoute
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class DirectionsRouteDiffProviderTest {

    private val routeDiffProvider = DirectionsRouteDiffProvider()

    @Test
    fun buildRouteDiffs() {
        val oldRoute = createNavigationRoute(
            createRouteLeg(57.14, 8.571, 42.85, 90, "low", 71),
            createRouteLeg(142.8, 28.57, 71.42, 120, "unknown", 42),
            createRouteLeg(85.71, 42.85, 57.14, 90, "unknown", 14),
            createRouteLeg(71.42, 14.28, 85.71, 90, "low", 57),
            createRouteLeg(42.85, 57.14, 28.57, 120, "low", 28),
        )
        val newRoute = createNavigationRoute(
            mockk {
                every { annotation() } returns null
            },
            createRouteLeg(57.14, 14.28, 85.71, 120, "low", 42),
            createRouteLeg(85.71, 42.85, 57.14, 90, "unknown", 14),
            createRouteLeg(71.42, 28.57, 42.85, 120, "unknown", 57),
            createRouteLeg(142.8, 57.14, 28.57, 90, "low", 71),
        )

        assertEquals(
            routeDiffProvider.buildRouteDiffs(oldRoute, newRoute, currentLegIndex = 1),
            listOf(
                "Updated distance, duration, speed, congestion at leg 1",
                "Updated duration, speed, maxSpeed, congestion at leg 3",
                "Updated distance, maxSpeed, congestionNumeric at leg 4",
            ),
        )
    }

    private fun createNavigationRoute(vararg legs: RouteLeg): NavigationRoute {
        return mockk {
            every { directionsRoute } returns mockk {
                every { legs() } returns legs.asList()
            }
        }
    }

    private fun createRouteLeg(
        distance: Double,
        duration: Double,
        speed: Double,
        maxSpeed: Int,
        congestion: String,
        congestionNumeric: Int,
    ): RouteLeg {
        return mockk {
            every { annotation() } returns mockk {
                every { distance() } returns listOf(distance)
                every { duration() } returns listOf(duration)
                every { speed() } returns listOf(speed)
                every { maxspeed() } returns listOf(createMaxSpeed(maxSpeed))
                every { congestion() } returns listOf(congestion)
                every { congestionNumeric() } returns listOf(congestionNumeric)
            }
        }
    }

    private fun createMaxSpeed(speed: Int): MaxSpeed {
        return MaxSpeed.builder().speed(speed).unit(SpeedLimit.KMPH).build()
    }
}
