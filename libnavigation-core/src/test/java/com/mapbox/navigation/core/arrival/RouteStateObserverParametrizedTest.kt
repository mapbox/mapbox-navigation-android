package com.mapbox.navigation.core.arrival

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.trip.model.RouteLegProgress
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.model.RouteProgressState
import com.mapbox.navigation.core.trip.session.TripSession
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class RouteStateObserverParametrizedTest(
    private val routeProgressState: RouteProgressState,
    private val nextLegStart: Boolean,
    private val finalDestinationArrival: Boolean
) {

    companion object {

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() = listOf(
            arrayOf(RouteProgressState.ROUTE_INVALID, false, false),
            arrayOf(RouteProgressState.ROUTE_UNCERTAIN, false, false),
            arrayOf(RouteProgressState.OFF_ROUTE, false, false),
            arrayOf(RouteProgressState.ROUTE_INITIALIZED, false, false),
            arrayOf(RouteProgressState.LOCATION_STALE, false, false),
            arrayOf(RouteProgressState.ROUTE_COMPLETE, true, true),
            arrayOf(RouteProgressState.LOCATION_TRACKING, true, true)
        )
    }

    private val tripSession: TripSession = mockk()
    private val arrivalObserver: ArrivalObserver = mockk {
        every { onWaypointArrival(any()) } returns Unit
        every { onNextRouteLegStart(any()) } returns Unit
        every { onFinalDestinationArrival(any()) } returns Unit
    }
    private val arrivalProgressObserver = ArrivalProgressObserver(tripSession)

    @Before
    fun setup() {
        every { tripSession.getRouteProgress() } returns null
        arrivalProgressObserver.registerObserver(arrivalObserver)
    }

    @Test
    fun `next leg start triggered`() {
        val onNextRouteLegStartCalls = slot<RouteLegProgress>()
        val onFinalDestinationArrivalCalls = slot<RouteProgress>()
        val customArrivalController: ArrivalController = mockk {
            every { navigateNextRouteLeg(capture(onNextRouteLegStartCalls)) } returns false
            every { arrivalOptions() } returns mockk {
                every { arrivalInSeconds } returns null
                every { arrivalInMeters } returns 10.0
            }
        }

        arrivalProgressObserver.attach(customArrivalController)
        arrivalProgressObserver.onRouteProgressChanged(
            mockk {
                every { currentState } returns routeProgressState
                every { route } returns mockk {
                    mockMultipleLegs()
                }
                every { currentLegProgress } returns mockk {
                    every { routeLeg } returns mockk()
                    every { legIndex } returns 1
                    every { durationRemaining } returns 2.0
                    every { distanceRemaining } returns 8.0f
                }
            }
        )

        Assert.assertEquals(nextLegStart, onNextRouteLegStartCalls.isCaptured)
        Assert.assertFalse(onFinalDestinationArrivalCalls.isCaptured)
    }

    @Test
    fun `final destination arrival triggered`() {
        val onNextRouteLegStartCalls = slot<RouteLegProgress>()
        val onFinalDestinationArrivalCalls = slot<RouteProgress>()
        val customArrivalController: ArrivalController = mockk {
            every { navigateNextRouteLeg(capture(onNextRouteLegStartCalls)) } returns false
            every { arrivalOptions() } returns mockk {
                every { arrivalInSeconds } returns null
                every { arrivalInMeters } returns 10.0
            }
        }
        every {
            arrivalObserver.onFinalDestinationArrival(capture(onFinalDestinationArrivalCalls))
        } returns Unit

        arrivalProgressObserver.attach(customArrivalController)
        arrivalProgressObserver.onRouteProgressChanged(
            mockk {
                every { currentState } returns routeProgressState
                every { route } returns mockk {
                    mockMultipleLegs()
                    every { durationRemaining } returns 2.0
                    every { distanceRemaining } returns 8.0f
                }
                every { currentLegProgress } returns mockk {
                    every { routeLeg } returns mockk()
                    every { legIndex } returns 2
                    every { durationRemaining } returns 2.0
                    every { distanceRemaining } returns 8.0f
                }
            }
        )

        Assert.assertFalse(onNextRouteLegStartCalls.isCaptured)
        Assert.assertEquals(finalDestinationArrival, onFinalDestinationArrivalCalls.isCaptured)
    }

    private fun DirectionsRoute.mockMultipleLegs() {
        every { routeOptions() } returns mockk {
            every { coordinates() } returns listOf(
                Point.fromLngLat(-122.444359, 37.736351),
                Point.fromLngLat(-122.444481, 37.735916),
                Point.fromLngLat(-122.444275, 37.735595),
                Point.fromLngLat(-122.444375, 37.736141)
            )
        }
        every { legs() } returns listOf(
            mockk(),
            mockk(),
            mockk() // This route has three legs
        )
    }
}
