package com.mapbox.navigation.core.fasterroute

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.navigation.base.trip.model.RouteProgress
import io.mockk.every
import io.mockk.mockk
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FasterRouteDetectorWHATTest {

    @Test
    fun shouldDetectWhenRouteIsFaster() {
        val newRoute: DirectionsRoute = mockk()
        every { newRoute.duration() } returns TimeUnit.MINUTES.toSeconds(5).toDouble()
        val routeProgress: RouteProgress = mockk()
        every { routeProgress.durationRemaining() } returns TimeUnit.MINUTES.toSeconds(10)

        val isFasterRoute = FasterRouteDetectorWHAT.isRouteFaster(newRoute, routeProgress)

        assertTrue(newRoute.duration()!! < routeProgress.durationRemaining())
        assertTrue(isFasterRoute)
    }

    @Test
    fun shouldDetectWhenRouteIsFaster2() {
        val newRoute: DirectionsRoute = mockk()
        every { newRoute.duration() } returns TimeUnit.MINUTES.toSeconds(5).toDouble()
        val routeProgress: RouteProgress = mockk()
        every { routeProgress.durationRemaining() } returns TimeUnit.MINUTES.toSeconds(10)

        val isFasterRoute = FasterRouteDetectorWHAT.testSameThing(newRoute, routeProgress)

        assertNotNull(newRoute.duration())
        assertTrue(isFasterRoute)
    }

    @Test
    fun shouldDefaultToFalseWhenDurationIsNull() {
        val newRoute: DirectionsRoute = mockk()
        every { newRoute.duration() } returns null
        val routeProgress: RouteProgress = mockk()
        every { routeProgress.durationRemaining() } returns TimeUnit.MINUTES.toSeconds(10)

        val isFasterRoute = FasterRouteDetectorWHAT.isRouteFaster(newRoute, routeProgress)

        assertFalse(isFasterRoute)
    }

    @Test
    fun shouldNotAllowSlightlyFasterRoutes() {
        val newRoute: DirectionsRoute = mockk()
        every { newRoute.duration() } returns TimeUnit.MINUTES.toSeconds(59).toDouble()
        val routeProgress: RouteProgress = mockk()
        every { routeProgress.durationRemaining() } returns TimeUnit.MINUTES.toSeconds(60)

        val isFasterRoute = FasterRouteDetectorWHAT.isRouteFaster(newRoute, routeProgress)

        assertFalse(isFasterRoute)
    }
}
