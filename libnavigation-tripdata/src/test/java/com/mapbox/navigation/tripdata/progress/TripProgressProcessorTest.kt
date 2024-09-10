package com.mapbox.navigation.tripdata.progress

import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.model.RouteProgressState
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class TripProgressProcessorTest {

    @Test
    fun `process trip details with route progress`() {
        val routeProgress = mockk<RouteProgress> {
            every { durationRemaining } returns 600.0
            every { distanceRemaining } returns 100f
            every { currentLegProgress } returns mockk {
                every { durationRemaining } returns 2.0
            }
            every { distanceTraveled } returns 50f
            every { route } returns mockk {
                every { currentState } returns RouteProgressState.TRACKING
            }
        }
        val expectedEta = Calendar.getInstance().also {
            it.add(Calendar.SECOND, 600)
        }

        val result =
            TripProgressProcessor().process(
                TripProgressAction.CalculateTripProgress(routeProgress),
            ) as TripProgressResult.RouteProgressCalculation

        assertEquals(
            expectedEta.timeInMillis.toDouble(),
            result.estimatedTimeToArrival.toDouble(),
            30000.0,
        )
        assertEquals(100.0, result.distanceRemaining, 0.0)
        assertEquals(2.0, result.currentLegTimeRemaining, 0.0)
        assertEquals(600.0, result.totalTimeRemaining, 0.0)
        assertEquals(0.3333333432674408, result.percentRouteTraveled, 0.0)
    }

    @Test
    fun `process trip details with route containing no legs`() {
        val route = mockk<NavigationRoute>(relaxed = true) {
            every { directionsRoute } returns mockk(relaxed = true) {
                every { legs() } returns null
                every { duration() } returns 100.0
                every { distance() } returns 200.0
            }
        }

        val result =
            TripProgressProcessor().process(
                TripProgressAction.CalculateTripDetails(route),
            ) as TripProgressResult.TripOverview.Failure

        assertEquals("Directions route should not have null RouteLegs", result.errorMessage)
    }

    @Test
    fun `process trip details with route containing null leg duration`() {
        val leg = mockk<RouteLeg>(relaxed = true) {
            every { duration() } returns null
            every { distance() } returns 100.0
        }
        val route = mockk<NavigationRoute>(relaxed = true) {
            every { directionsRoute } returns mockk(relaxed = true) {
                every { legs() } returns listOf(leg)
                every { duration() } returns 100.0
                every { distance() } returns 200.0
            }
        }

        val result =
            TripProgressProcessor().process(
                TripProgressAction.CalculateTripDetails(route),
            ) as TripProgressResult.TripOverview.Failure

        assertEquals(
            "RouteLeg duration and RouteLeg distance cannot be null",
            result.errorMessage,
        )
    }

    @Test
    fun `process trip details with route containing null leg distance`() {
        val leg = mockk<RouteLeg>(relaxed = true) {
            every { duration() } returns 100.0
            every { distance() } returns null
        }
        val route = mockk<NavigationRoute>(relaxed = true) {
            every { directionsRoute } returns mockk(relaxed = true) {
                every { legs() } returns listOf(leg)
                every { duration() } returns 100.0
                every { distance() } returns 200.0
            }
        }

        val result =
            TripProgressProcessor().process(
                TripProgressAction.CalculateTripDetails(route),
            ) as TripProgressResult.TripOverview.Failure

        assertEquals(
            "RouteLeg duration and RouteLeg distance cannot be null",
            result.errorMessage,
        )
    }

    @Test
    fun `process trip details with route containing one leg`() {
        val leg = mockk<RouteLeg>(relaxed = true) {
            every { duration() } returns 50.0
            every { distance() } returns 100.0
        }
        val route = mockk<NavigationRoute>(relaxed = true) {
            every { directionsRoute } returns mockk(relaxed = true) {
                every { legs() } returns listOf(leg)
                every { duration() } returns 100.0
                every { distance() } returns 200.0
            }
        }
        val expectedEta = Calendar.getInstance().also {
            it.add(Calendar.SECOND, route.directionsRoute.duration().toInt())
        }.timeInMillis

        val result =
            TripProgressProcessor().process(
                TripProgressAction.CalculateTripDetails(route),
            ) as TripProgressResult.TripOverview.Success

        assertEquals(expectedEta.toDouble(), result.totalEstimatedTimeToArrival.toDouble(), 30000.0)
        assertEquals(route.directionsRoute.duration(), result.totalTime, 0.0)
        assertEquals(route.directionsRoute.distance(), result.totalDistance, 0.0)
    }

    @Test
    fun `process trip details with route containing multiple legs`() {
        val leg1 = mockk<RouteLeg>(relaxed = true) {
            every { duration() } returns 50.0
            every { distance() } returns 100.0
        }
        val leg2 = mockk<RouteLeg>(relaxed = true) {
            every { duration() } returns 100.0
            every { distance() } returns 200.0
        }
        val route = mockk<NavigationRoute>(relaxed = true) {
            every { directionsRoute } returns mockk(relaxed = true) {
                every { legs() } returns listOf(leg1, leg2)
                every { duration() } returns 150.0
                every { distance() } returns 300.0
            }
        }
        val expectedEta1 = Calendar.getInstance().also {
            it.add(Calendar.SECOND, leg1.duration()!!.toInt())
        }.timeInMillis
        val expectedEta2 = Calendar.getInstance().also {
            it.add(Calendar.SECOND, leg2.duration()!!.toInt())
        }.timeInMillis
        val expectedEta = Calendar.getInstance().also {
            it.add(Calendar.SECOND, route.directionsRoute.duration().toInt())
        }.timeInMillis

        val result =
            TripProgressProcessor().process(
                TripProgressAction.CalculateTripDetails(route),
            ) as TripProgressResult.TripOverview.Success

        // For 1st route leg
        assertEquals(
            expectedEta1.toDouble(),
            result.routeLegTripDetail[0].estimatedTimeToArrival.toDouble(),
            30000.0,
        )
        assertEquals(leg1.duration()!!, result.routeLegTripDetail[0].legTime, 0.0)
        assertEquals(leg1.distance()!!, result.routeLegTripDetail[0].legDistance, 0.0)
        // For 2nd route leg
        assertEquals(
            expectedEta2.toDouble(),
            result.routeLegTripDetail[1].estimatedTimeToArrival.toDouble(),
            30000.0,
        )
        assertEquals(leg2.duration()!!, result.routeLegTripDetail[1].legTime, 0.0)
        assertEquals(leg2.distance()!!, result.routeLegTripDetail[1].legDistance, 0.0)
        // For complete route
        assertEquals(expectedEta.toDouble(), result.totalEstimatedTimeToArrival.toDouble(), 30000.0)
        assertEquals(route.directionsRoute.duration(), result.totalTime, 0.0)
        assertEquals(route.directionsRoute.distance(), result.totalDistance, 0.0)
    }
}
