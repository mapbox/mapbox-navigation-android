package com.mapbox.navigation.core.fasterroute

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.navigation.base.trip.model.RouteProgress
import io.mockk.every
import io.mockk.mockk
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FasterRouteDetectorTest {

    @Test
    fun shouldDetectWhenRouteIsFaster() {
        val newRoute: DirectionsRoute = mockk()
        every { newRoute.duration() } returns TimeUnit.MINUTES.toSeconds(5).toDouble()
        val routeProgress: RouteProgress = mockk()
        every { routeProgress.durationRemaining() } returns TimeUnit.MINUTES.toSeconds(10)

        val isFasterRoute = FasterRouteDetector.isRouteFaster(newRoute, routeProgress)

        assertTrue(newRoute.duration()!! < routeProgress.durationRemaining())
        assertTrue(isFasterRoute)
    }

    @Test
    fun shouldDefaultToFalseWhenDurationIsNull() {
        val newRoute: DirectionsRoute = mockk()
        every { newRoute.duration() } returns null
        val routeProgress: RouteProgress = mockk()
        every { routeProgress.durationRemaining() } returns TimeUnit.MINUTES.toSeconds(10)

        val isFasterRoute = FasterRouteDetector.isRouteFaster(newRoute, routeProgress)

        assertFalse(isFasterRoute)
    }

    @Test
    fun shouldNotAllowSlightlyFasterRoutes() {
        val newRoute: DirectionsRoute = mockk()
        every { newRoute.duration() } returns TimeUnit.MINUTES.toSeconds(49).toDouble()
        val routeProgress: RouteProgress = mockk()
        every { routeProgress.durationRemaining() } returns TimeUnit.MINUTES.toSeconds(50)

        val isFasterRoute = FasterRouteDetector.isRouteFaster(newRoute, routeProgress)

        assertFalse(isFasterRoute)
    }
}
