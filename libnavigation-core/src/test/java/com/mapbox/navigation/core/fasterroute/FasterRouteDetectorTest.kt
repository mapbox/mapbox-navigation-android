package com.mapbox.navigation.core.fasterroute

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.navigation.base.trip.model.RouteProgress
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FasterRouteDetectorTest {

    private val routeComparator: RouteComparator = mockk {
        every { isRouteDescriptionDifferent(any(), any()) } returns true
    }

    private val fasterRouteDetector = FasterRouteDetector(routeComparator)

    @Test
    fun shouldDetectWhenRouteIsFaster() = runBlocking {
        every { routeComparator.isRouteDescriptionDifferent(any(), any()) } returns true
        val newRoute: DirectionsRoute = mockk()
        every { newRoute.duration() } returns 402.6
        val routeProgress: RouteProgress = mockk()
        every { routeProgress.durationRemaining } returns 797.447

        val isFasterRoute = fasterRouteDetector.isRouteFaster(newRoute, routeProgress)

        assertTrue(isFasterRoute)
    }

    @Test
    fun shouldDetectWhenRouteIsFasterOnlyIfDifferent() = runBlocking {
        every { routeComparator.isRouteDescriptionDifferent(any(), any()) } returns false
        val newRoute: DirectionsRoute = mockk()
        every { newRoute.duration() } returns 402.6
        val routeProgress: RouteProgress = mockk()
        every { routeProgress.durationRemaining } returns 797.447

        val isFasterRoute = fasterRouteDetector.isRouteFaster(newRoute, routeProgress)

        assertFalse(isFasterRoute)
    }

    @Test
    fun shouldDetectWhenRouteIsSlower() = runBlocking {
        val newRoute: DirectionsRoute = mockk()
        every { newRoute.duration() } returns 512.2
        val routeProgress: RouteProgress = mockk()
        every { routeProgress.durationRemaining } returns 450.501

        val isFasterRoute = fasterRouteDetector.isRouteFaster(newRoute, routeProgress)

        assertFalse(isFasterRoute)
    }

    @Test
    fun shouldNotAllowSlightlyFasterRoutes() = runBlocking {
        val newRoute: DirectionsRoute = mockk()
        every { newRoute.duration() } returns 634.7
        val routeProgress: RouteProgress = mockk()
        every { routeProgress.durationRemaining } returns 695.811

        val isFasterRoute = fasterRouteDetector.isRouteFaster(newRoute, routeProgress)

        assertFalse(isFasterRoute)
    }
}
