package com.mapbox.navigation.core.fasterroute

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.navigation.base.trip.model.RouteProgress
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FasterRouteDetectorTest {

    private val fasterRouteDetector = FasterRouteDetector()

    @Test
    fun shouldDetectWhenRouteIsFaster() {
        val newRoute: DirectionsRoute = mockk()
        every { newRoute.duration() } returns 402.6
        val routeProgress: RouteProgress = mockk()
        every { routeProgress.durationRemaining() } returns 797.447

        val isFasterRoute = fasterRouteDetector.isRouteFaster(newRoute, routeProgress)

        assertTrue(isFasterRoute)
    }

    @Test
    fun shouldDetectWhenRouteIsSlower() {
        val newRoute: DirectionsRoute = mockk()
        every { newRoute.duration() } returns 512.2
        val routeProgress: RouteProgress = mockk()
        every { routeProgress.durationRemaining() } returns 450.501

        val isFasterRoute = fasterRouteDetector.isRouteFaster(newRoute, routeProgress)

        assertFalse(isFasterRoute)
    }

    @Test
    fun shouldDefaultToFalseWhenDurationIsNull() {
        val newRoute: DirectionsRoute = mockk()
        every { newRoute.duration() } returns null
        val routeProgress: RouteProgress = mockk()
        every { routeProgress.durationRemaining() } returns 727.228

        val isFasterRoute = fasterRouteDetector.isRouteFaster(newRoute, routeProgress)

        assertFalse(isFasterRoute)
    }

    @Test
    fun shouldNotAllowSlightlyFasterRoutes() {
        val newRoute: DirectionsRoute = mockk()
        every { newRoute.duration() } returns 634.7
        val routeProgress: RouteProgress = mockk()
        every { routeProgress.durationRemaining() } returns 695.811

        val isFasterRoute = fasterRouteDetector.isRouteFaster(newRoute, routeProgress)

        assertFalse(isFasterRoute)
    }
}
