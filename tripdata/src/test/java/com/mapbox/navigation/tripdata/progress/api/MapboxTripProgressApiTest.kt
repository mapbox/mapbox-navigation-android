package com.mapbox.navigation.tripdata.progress.api

import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.navigation.base.internal.extensions.isLegWaypoint
import com.mapbox.navigation.base.internal.route.Waypoint
import com.mapbox.navigation.base.internal.utils.internalWaypoints
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.model.RouteProgressState
import com.mapbox.navigation.tripdata.progress.TripProgressProcessor
import com.mapbox.navigation.tripdata.progress.model.TripProgressUpdateFormatter
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.TimeZone

class MapboxTripProgressApiTest {

    @Before
    fun `set up`() {
        mockkStatic(NavigationRoute::internalWaypoints, Waypoint::isLegWaypoint)
    }

    @After
    fun `tear down`() {
        unmockkAll()
    }

    @Test
    fun `trip details with route progress`() {
        val progressFormatter = mockk<TripProgressUpdateFormatter>()
        val expectedEta = System.currentTimeMillis() + 600000
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
            every { navigationRoute } returns mockk {
                every { internalWaypoints() } returns listOf(
                    mockWaypoint(timeZoneId = "America/Los_Angeles"),
                    mockWaypoint(timeZoneId = "America/Phoenix", isLegWaypoint = false),
                    mockWaypoint(timeZoneId = "America/Chicago"),
                )
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
        assertEquals(expectedEta.toDouble(), result.estimatedTimeToArrival.toDouble(), 1000.0)
        assertEquals(TimeZone.getTimeZone("America/Chicago"), result.arrivalTimeZone)
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
            "RouteLeg duration cannot be null",
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
            "RouteLeg distance cannot be null",
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
            every { internalWaypoints() } returns listOf(
                mockWaypoint(timeZoneId = "America/Los_Angeles"),
                mockWaypoint(timeZoneId = "America/Phoenix", isLegWaypoint = false),
                mockWaypoint(timeZoneId = "America/Chicago"),
            )
        }
        val expectedTotalEta = System.currentTimeMillis() + route.directionsRoute.duration() * 1000

        val result = MapboxTripProgressApi(
            progressFormatter,
            TripProgressProcessor(),
        ).getTripDetails(route)

        assertEquals(progressFormatter, result.value?.formatter)
        assertEquals(route.directionsRoute.duration(), result.value!!.totalTime, 0.0)
        assertEquals(route.directionsRoute.distance(), result.value!!.totalDistance, 0.0)
        assertEquals(
            expectedTotalEta,
            result.value!!.totalEstimatedTimeToArrival.toDouble(),
            1000.0,
        )
        assertEquals(TimeZone.getTimeZone("America/Chicago"), result.value!!.arrivalTimeZone)
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
            every { internalWaypoints() } returns listOf(
                mockWaypoint(timeZoneId = "America/Los_Angeles"),
                mockWaypoint(timeZoneId = "America/Phoenix"),
                mockWaypoint(timeZoneId = "America/Chicago", isLegWaypoint = false),
                mockWaypoint(timeZoneId = "America/New_York"),
            )
        }
        val expectedEta1 = System.currentTimeMillis() + leg1.duration()!! * 1000
        val expectedEta2 = expectedEta1 + leg2.duration()!! * 1000
        val expectedTotalEta = System.currentTimeMillis() + route.directionsRoute.duration() * 1000

        val result = MapboxTripProgressApi(
            progressFormatter,
            TripProgressProcessor(),
        ).getTripDetails(route)

        assertEquals(progressFormatter, result.value!!.formatter)
        // For 1st route leg
        assertEquals(
            expectedEta1,
            result.value!!.routeLegTripDetail[0].estimatedTimeToArrival.toDouble(),
            1000.0,
        )
        assertEquals(leg1.duration()!!, result.value!!.routeLegTripDetail[0].legTime, 0.0)
        assertEquals(leg1.distance()!!, result.value!!.routeLegTripDetail[0].legDistance, 0.0)
        assertEquals(
            TimeZone.getTimeZone("America/Phoenix"),
            result.value!!.routeLegTripDetail[0].arrivalTimeZone,
        )
        // For 2nd route leg
        assertEquals(
            expectedEta2,
            result.value!!.routeLegTripDetail[1].estimatedTimeToArrival.toDouble(),
            1000.0,
        )
        assertEquals(leg2.duration()!!, result.value!!.routeLegTripDetail[1].legTime, 0.0)
        assertEquals(leg2.distance()!!, result.value!!.routeLegTripDetail[1].legDistance, 0.0)
        assertEquals(
            TimeZone.getTimeZone("America/New_York"),
            result.value!!.routeLegTripDetail[1].arrivalTimeZone,
        )
        // For complete route
        assertEquals(
            expectedTotalEta,
            result.value!!.totalEstimatedTimeToArrival.toDouble(),
            1000.0,
        )
        assertEquals(route.directionsRoute.duration(), result.value!!.totalTime, 0.0)
        assertEquals(route.directionsRoute.distance(), result.value!!.totalDistance, 0.0)
        assertEquals(TimeZone.getTimeZone("America/New_York"), result.value!!.arrivalTimeZone)
    }

    private fun mockWaypoint(timeZoneId: String, isLegWaypoint: Boolean = true): Waypoint {
        return mockk<Waypoint> {
            every { timeZone } returns mockk {
                every { toJavaTimeZone() } returns TimeZone.getTimeZone(timeZoneId)
                every { isLegWaypoint() } returns isLegWaypoint
            }
        }
    }
}
