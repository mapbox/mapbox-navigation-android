package com.mapbox.navigation.tripdata.progress.api

import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.model.RouteProgressState
import com.mapbox.navigation.tripdata.progress.TripProgressProcessor
import com.mapbox.navigation.tripdata.progress.model.TripProgressUpdateFormatter
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class MapboxTripProgressApiTest {

    @Test
    fun `trip details with route progress`() {
        val progressFormatter = mockk<TripProgressUpdateFormatter>()
        val expectedEta = Calendar.getInstance().also {
            it.add(Calendar.SECOND, 600)
        }
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

        val result = MapboxTripProgressApi(
            progressFormatter,
            TripProgressProcessor(),
        ).getTripProgress(routeProgress)

        assertEquals(progressFormatter, result.formatter)
        assertEquals(100.0, result.distanceRemaining, 0.0)
        assertEquals(2.0, result.currentLegTimeRemaining, 0.0)
        assertEquals(600.0, result.totalTimeRemaining, 0.0)
        assertEquals(0.3333333432674408, result.percentRouteTraveled, 0.0)
        assertEquals(
            expectedEta.timeInMillis.toDouble(),
            result.estimatedTimeToArrival.toDouble(),
            30000.0,
        )
    }

    @Test
    fun `trip details with no route leg`() {
        val progressFormatter = mockk<TripProgressUpdateFormatter>()
        val route = mockk<NavigationRoute>(relaxed = true) {
            every { directionsRoute } returns mockk(relaxed = true) {
                every { legs() } returns null
                every { duration() } returns 100.0
                every { distance() } returns 200.0
            }
        }

        val result = MapboxTripProgressApi(
            progressFormatter,
            TripProgressProcessor(),
        ).getTripDetails(route)

        assertEquals(
            "Directions route should not have null RouteLegs",
            result.error?.errorMessage,
        )
    }

    @Test
    fun `trip details with null route leg duration`() {
        val progressFormatter = mockk<TripProgressUpdateFormatter>()
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

        val result = MapboxTripProgressApi(
            progressFormatter,
            TripProgressProcessor(),
        ).getTripDetails(route)

        assertEquals(
            "RouteLeg duration and RouteLeg distance cannot be null",
            result.error?.errorMessage,
        )
    }

    @Test
    fun `trip details with null route leg distance`() {
        val progressFormatter = mockk<TripProgressUpdateFormatter>()
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

        val result = MapboxTripProgressApi(
            progressFormatter,
            TripProgressProcessor(),
        ).getTripDetails(route)

        assertEquals(
            "RouteLeg duration and RouteLeg distance cannot be null",
            result.error?.errorMessage,
        )
    }

    @Test
    fun `trip details with one route leg`() {
        val progressFormatter = mockk<TripProgressUpdateFormatter>()
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
        val expectedTotalEta = Calendar.getInstance().also {
            it.add(Calendar.SECOND, route.directionsRoute.duration().toInt())
        }.timeInMillis

        val result = MapboxTripProgressApi(
            progressFormatter,
            TripProgressProcessor(),
        ).getTripDetails(route)

        assertEquals(progressFormatter, result.value?.formatter)
        assertEquals(route.directionsRoute.duration(), result.value!!.totalTime, 0.0)
        assertEquals(route.directionsRoute.distance(), result.value!!.totalDistance, 0.0)
        assertEquals(
            expectedTotalEta.toDouble(),
            result.value!!.totalEstimatedTimeToArrival.toDouble(),
            30000.0,
        )
    }

    @Test
    fun `trip details with multiple route leg`() {
        val progressFormatter = mockk<TripProgressUpdateFormatter>()
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
                every { distance() } returns 300.0
                every { duration() } returns 150.0
            }
        }
        val expectedEta1 = Calendar.getInstance().also {
            it.add(Calendar.SECOND, leg1.duration()!!.toInt())
        }.timeInMillis
        val expectedEta2 = Calendar.getInstance().also {
            it.add(Calendar.SECOND, leg2.duration()!!.toInt())
        }.timeInMillis
        val expectedTotalEta = Calendar.getInstance().also {
            it.add(Calendar.SECOND, route.directionsRoute.duration().toInt())
        }.timeInMillis

        val result = MapboxTripProgressApi(
            progressFormatter,
            TripProgressProcessor(),
        ).getTripDetails(route)

        assertEquals(progressFormatter, result.value!!.formatter)
        // For 1st route leg
        assertEquals(
            expectedEta1.toDouble(),
            result.value!!.routeLegTripDetail[0].estimatedTimeToArrival.toDouble(),
            30000.0,
        )
        assertEquals(leg1.duration()!!, result.value!!.routeLegTripDetail[0].legTime, 0.0)
        assertEquals(leg1.distance()!!, result.value!!.routeLegTripDetail[0].legDistance, 0.0)
        // For 2nd route leg
        assertEquals(
            expectedEta2.toDouble(),
            result.value!!.routeLegTripDetail[1].estimatedTimeToArrival.toDouble(),
            30000.0,
        )
        assertEquals(leg2.duration()!!, result.value!!.routeLegTripDetail[1].legTime, 0.0)
        assertEquals(leg2.distance()!!, result.value!!.routeLegTripDetail[1].legDistance, 0.0)
        // For complete route
        assertEquals(
            expectedTotalEta.toDouble(),
            result.value!!.totalEstimatedTimeToArrival.toDouble(),
            30000.0,
        )
        assertEquals(route.directionsRoute.duration(), result.value!!.totalTime, 0.0)
        assertEquals(route.directionsRoute.distance(), result.value!!.totalDistance, 0.0)
    }
}
