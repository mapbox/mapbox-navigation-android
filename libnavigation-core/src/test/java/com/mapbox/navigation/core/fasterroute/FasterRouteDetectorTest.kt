package com.mapbox.navigation.core.fasterroute

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.navigation.base.trip.model.RouteProgress
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkObject
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FasterRouteDetectorTest {

    @Before
    fun setup() {
        unmockkObject(FasterRouteDetector)
    }

    @Test
    fun shouldDetectWhenRouteIsFaster() {
        val newRoute: DirectionsRoute = mockk()
        every { newRoute.duration() } returns 402.6
        val routeProgress: RouteProgress = mockk()
        every { routeProgress.durationRemaining() } returns 797447

        val isFasterRoute = FasterRouteDetector.isRouteFaster(newRoute, routeProgress)

        assertTrue(isFasterRoute)
    }

    @Test
    fun shouldDetectWhenRouteIsSlower() {
        val newRoute: DirectionsRoute = mockk()
        every { newRoute.duration() } returns 512.2
        val routeProgress: RouteProgress = mockk()
        every { routeProgress.durationRemaining() } returns 450501

        val isFasterRoute = FasterRouteDetector.isRouteFaster(newRoute, routeProgress)

        assertFalse(isFasterRoute)
    }

    @Test
    fun shouldDefaultToFalseWhenDurationIsNull() {
        val newRoute: DirectionsRoute = mockk()
        every { newRoute.duration() } returns null
        val routeProgress: RouteProgress = mockk()
        every { routeProgress.durationRemaining() } returns 727228

        val isFasterRoute = FasterRouteDetector.isRouteFaster(newRoute, routeProgress)

        assertFalse(isFasterRoute)
    }

    @Test
    fun shouldNotAllowSlightlyFasterRoutes() {
        val newRoute: DirectionsRoute = mockk()
        every { newRoute.duration() } returns 634.7
        val routeProgress: RouteProgress = mockk()
        every { routeProgress.durationRemaining() } returns 695811

        val isFasterRoute = FasterRouteDetector.isRouteFaster(newRoute, routeProgress)

        assertFalse(isFasterRoute)
    }
}
