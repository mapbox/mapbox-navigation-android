package com.mapbox.navigation.core.arrival

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.trip.model.RouteLegProgress
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.model.RouteProgressState
import com.mapbox.navigation.core.trip.session.LegIndexUpdatedCallback
import com.mapbox.navigation.core.trip.session.TripSession
import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ArrivalProgressObserverTest {

    private val tripSession: TripSession = mockk()
    private val arrivalObserver: ArrivalObserver = mockk {
        every { onWaypointArrival(any()) } returns Unit
        every { onNextRouteLegStart(any()) } returns Unit
        every { onFinalDestinationArrival(any()) } returns Unit
    }
    private val arrivalProgressObserver = ArrivalProgressObserver(tripSession)
    private val legIndexUpdatedCallback: LegIndexUpdatedCallback = mockk(relaxed = true)

    @Before
    fun setup() {
        every { tripSession.getRouteProgress() } returns null
        arrivalProgressObserver.registerObserver(arrivalObserver)
    }

    @Test
    fun `should not crash with null route progress values`() {
        val routeProgress: RouteProgress = mockk {
            every { stale } returns false
            every { currentState } returns RouteProgressState.TRACKING
            every { route } returns mockk {
                mockMultipleLegs()
                every { routeIndex() } returns null
                every { currentLegProgress } returns mockk {
                    every { routeLeg } returns mockk()
                    every { legIndex } returns 0
                    every { durationRemaining } returns 0.0
                    every { distanceRemaining } returns 0.0f
                }
            }
        }

        arrivalProgressObserver.onRouteProgressChanged(routeProgress)
    }

    @Test
    fun `should do nothing when currentLegProgress is null`() {
        val routeProgress: RouteProgress = mockk {
            every { stale } returns false
            every { currentState } returns RouteProgressState.TRACKING
            every { route } returns mockk {
                every { currentLegProgress } returns null
            }
        }

        arrivalProgressObserver.onRouteProgressChanged(routeProgress)

        verify { arrivalObserver wasNot Called }
        verify { tripSession wasNot Called }
    }

    @Test
    fun `should not navigateNextRouteLeg when currentLegProgress is null`() {
        every { tripSession.getRouteProgress() } returns mockk {
            every { stale } returns false
            every { currentState } returns RouteProgressState.TRACKING
            every { navigationRoute } returns mockk {
                every { directionsRoute } returns mockk {
                    mockMultipleLegs()
                }
            }
            every { currentLegProgress } returns null
        }

        arrivalProgressObserver.navigateNextRouteLeg(legIndexUpdatedCallback)

        verify(exactly = 1) { legIndexUpdatedCallback.onLegIndexUpdatedCallback(false) }
    }

    @Test
    fun `should not navigateNextRouteLeg when route legs is empty`() {
        every { tripSession.getRouteProgress() } returns mockk {
            every { stale } returns false
            every { currentState } returns RouteProgressState.TRACKING
            every { navigationRoute } returns mockk {
                every { directionsRoute } returns mockk {
                    every { legs() } returns emptyList()
                }
            }
            every { currentLegProgress } returns mockk {
                every { legIndex } returns 0
            }
        }

        arrivalProgressObserver.navigateNextRouteLeg(legIndexUpdatedCallback)

        verify(exactly = 1) { legIndexUpdatedCallback.onLegIndexUpdatedCallback(false) }
        verify { arrivalObserver wasNot Called }
    }

    @Test
    fun `should notify next leg start when the next leg started`() {
        val onNextRouteLegStartCalls = slot<RouteLegProgress>()
        val onFinalDestinationArrivalCalls = slot<RouteProgress>()
        val customArrivalController: ArrivalController = mockk {
            every { navigateNextRouteLeg(capture(onNextRouteLegStartCalls)) } returns false
        }

        arrivalProgressObserver.attach(customArrivalController)
        arrivalProgressObserver.onRouteProgressChanged(
            mockk {
                every { stale } returns false
                every { currentState } returns RouteProgressState.COMPLETE
                every { navigationRoute } returns mockk {
                    every { directionsRoute } returns mockk {
                        mockMultipleLegs()
                    }
                }
                every { currentLegProgress } returns mockk {
                    every { routeLeg } returns mockk()
                    every { legIndex } returns 1
                    every { durationRemaining } returns 2.0
                    every { distanceRemaining } returns 8.0f
                }
            },
        )

        assertTrue(onNextRouteLegStartCalls.isCaptured)
        assertFalse(onFinalDestinationArrivalCalls.isCaptured)
    }

    @Test
    fun `should notify final destination arrival when arrived at last waypoint`() {
        val onNextRouteLegStartCalls = slot<RouteLegProgress>()
        val onFinalDestinationArrivalCalls = slot<RouteProgress>()
        val customArrivalController: ArrivalController = mockk {
            every { navigateNextRouteLeg(capture(onNextRouteLegStartCalls)) } returns false
        }
        every {
            arrivalObserver.onFinalDestinationArrival(capture(onFinalDestinationArrivalCalls))
        } returns Unit

        arrivalProgressObserver.attach(customArrivalController)
        arrivalProgressObserver.onRouteProgressChanged(
            mockk {
                every { stale } returns false
                every { currentState } returns RouteProgressState.COMPLETE
                every { navigationRoute } returns mockk {
                    every { directionsRoute } returns mockk {
                        mockMultipleLegs()
                        every { durationRemaining } returns 2.0
                        every { distanceRemaining } returns 8.0f
                    }
                }
                every { currentLegProgress } returns mockk {
                    every { routeLeg } returns mockk()
                    every { legIndex } returns 2
                    every { durationRemaining } returns 2.0
                    every { distanceRemaining } returns 8.0f
                }
            },
        )

        assertFalse(onNextRouteLegStartCalls.isCaptured)
        assertTrue(onFinalDestinationArrivalCalls.isCaptured)
    }

    @Test
    fun `should notify final destination arrival when arrived at last waypoint only once`() {
        val onNextRouteLegStartCalls = slot<RouteLegProgress>()
        val onFinalDestinationArrivalCalls = mutableListOf<RouteProgress>()
        val customArrivalController: ArrivalController = mockk {
            every { navigateNextRouteLeg(capture(onNextRouteLegStartCalls)) } returns false
        }
        every {
            arrivalObserver.onFinalDestinationArrival(capture(onFinalDestinationArrivalCalls))
        } returns Unit
        val routeProgress: RouteProgress = mockk {
            every { stale } returns false
            every { currentState } returns RouteProgressState.COMPLETE
            every { navigationRoute } returns mockk {
                every { directionsRoute } returns mockk {
                    mockMultipleLegs()
                    every { durationRemaining } returns 2.0
                    every { distanceRemaining } returns 8.0f
                }
            }
            every { currentLegProgress } returns mockk {
                every { routeLeg } returns mockk()
                every { legIndex } returns 2
                every { durationRemaining } returns 2.0
                every { distanceRemaining } returns 8.0f
            }
        }

        arrivalProgressObserver.attach(customArrivalController)
        arrivalProgressObserver.onRouteProgressChanged(routeProgress)
        arrivalProgressObserver.onRouteProgressChanged(routeProgress)
        arrivalProgressObserver.onRouteProgressChanged(routeProgress)

        assertEquals(1, onFinalDestinationArrivalCalls.size)
        assertFalse(onNextRouteLegStartCalls.isCaptured)
        verify(exactly = 1) { arrivalObserver.onFinalDestinationArrival(any()) }
    }

    @Test
    fun `should notify observers with time option`() {
        val onArrivalCalls = slot<RouteLegProgress>()
        val customArrivalController: ArrivalController = mockk {
            every { navigateNextRouteLeg(capture(onArrivalCalls)) } returns false
        }

        arrivalProgressObserver.attach(customArrivalController)
        arrivalProgressObserver.onRouteProgressChanged(
            mockk {
                every { stale } returns false
                every { currentState } returns RouteProgressState.COMPLETE
                every { currentLegProgress } returns mockk {
                    every { routeLeg } returns mockk()
                    every { durationRemaining } returns 1.0
                    every { distanceRemaining } returns 15.0f
                    every { legIndex } returns 0
                }
                every { navigationRoute } returns mockk {
                    every { directionsRoute } returns mockk {
                        mockMultipleLegs()
                    }
                }
            },
        )

        assertTrue(onArrivalCalls.isCaptured)
        assertEquals(onArrivalCalls.captured.durationRemaining, 1.0, 0.001)
    }

    @Test
    fun `should notify arrival if arrived on attach`() {
        val onArrivalCalls = slot<RouteLegProgress>()
        val customArrivalController: ArrivalController = mockk {
            every { navigateNextRouteLeg(capture(onArrivalCalls)) } returns false
        }
        val routeProgress: RouteProgress = mockk {
            every { stale } returns false
            every { currentState } returns RouteProgressState.COMPLETE
            every { currentLegProgress } returns mockk {
                every { routeLeg } returns mockk()
                every { durationRemaining } returns 1.0
                every { distanceRemaining } returns 15.0f
                every { legIndex } returns 0
            }
            every { navigationRoute } returns mockk {
                every { directionsRoute } returns mockk {
                    mockMultipleLegs()
                }
            }
        }
        every { tripSession.getRouteProgress() } returns routeProgress

        arrivalProgressObserver.attach(customArrivalController)
        arrivalProgressObserver.onRouteProgressChanged(routeProgress)

        assertTrue(onArrivalCalls.isCaptured)
    }

    @Test
    fun `should notify onWaypointArrival only once for early arrival`() {
        val onNextRouteLegStartCalls = slot<RouteLegProgress>()
        val onWaypointArrivalCalls = mutableListOf<RouteProgress>()
        val customArrivalController: ArrivalController = mockk {
            every { navigateNextRouteLeg(capture(onNextRouteLegStartCalls)) } returns false
        }
        every {
            arrivalObserver.onWaypointArrival(capture(onWaypointArrivalCalls))
        } returns Unit
        val routeProgress: RouteProgress = mockk {
            every { stale } returns false
            every { currentState } returns RouteProgressState.COMPLETE
            every { navigationRoute } returns mockk {
                every { directionsRoute } returns mockk {
                    mockMultipleLegs()
                }
            }
            every { currentLegProgress } returns mockk {
                every { routeLeg } returns mockk()
                every { legIndex } returns 1
                every { durationRemaining } returns 2.0
                every { distanceRemaining } returns 8.0f
            }
        }

        arrivalProgressObserver.attach(customArrivalController)
        arrivalProgressObserver.onRouteProgressChanged(routeProgress)
        arrivalProgressObserver.onRouteProgressChanged(routeProgress)
        arrivalProgressObserver.onRouteProgressChanged(routeProgress)

        assertEquals(1, onWaypointArrivalCalls.size)
        verify(exactly = 0) { arrivalObserver.onFinalDestinationArrival(any()) }
    }

    @Test
    fun `should not navigate to next route leg before arrival`() {
        val onArrivalCalls = slot<RouteLegProgress>()
        val customArrivalController: ArrivalController = mockk {
            every { navigateNextRouteLeg(capture(onArrivalCalls)) } returns false
        }

        arrivalProgressObserver.attach(customArrivalController)
        arrivalProgressObserver.onRouteProgressChanged(
            mockk {
                every { stale } returns false
                every { currentState } returns RouteProgressState.TRACKING
                every { currentLegProgress } returns mockk {
                    every { routeLeg } returns mockk()
                    every { durationRemaining } returns 360.0
                    every { distanceRemaining } returns 80.0f
                    every { legIndex } returns 0
                }
                every { navigationRoute } returns mockk {
                    every { directionsRoute } returns mockk {
                        mockMultipleLegs()
                    }
                }
            },
        )

        assertFalse(onArrivalCalls.isCaptured)
        verify { arrivalObserver wasNot Called }
    }

    @Test
    fun `should navigate to next waypoint automatically when navigateNextRouteLeg is true`() {
        val testNavigateNextRouteLeg = true
        val routeProgress: RouteProgress = mockk {
            every { stale } returns false
            every { currentState } returns RouteProgressState.COMPLETE
            every { navigationRoute } returns mockk {
                every { directionsRoute } returns mockk {
                    mockMultipleLegs()
                    every { routeIndex() } returns "0"
                    every { currentLegProgress } returns mockk {
                        every { routeLeg } returns mockk()
                        every { legIndex } returns 0
                        every { durationRemaining } returns 0.0
                        every { distanceRemaining } returns 0.0f
                    }
                }
            }
        }
        every { tripSession.getRouteProgress() } returns routeProgress
        val customArrivalController: ArrivalController = mockk {
            every { navigateNextRouteLeg(any()) } returns testNavigateNextRouteLeg
        }

        val tripSessionCallbackSlot = slot<LegIndexUpdatedCallback>()
        every { tripSession.updateLegIndex(1, capture(tripSessionCallbackSlot)) } answers {
            tripSessionCallbackSlot.captured.onLegIndexUpdatedCallback(true)
        }

        arrivalProgressObserver.attach(customArrivalController)
        arrivalProgressObserver.onRouteProgressChanged(routeProgress)

        verify(exactly = 1) { tripSession.updateLegIndex(1, any()) }
    }

    @Test
    fun `should not navigate to next waypoint when navigateNextRouteLeg is false`() {
        val testNavigateNextRouteLeg = false
        every { tripSession.getRouteProgress() } returns mockk {
            every { stale } returns false
            every { currentState } returns RouteProgressState.COMPLETE
            every { navigationRoute } returns mockk {
                every { directionsRoute } returns mockk {
                    mockMultipleLegs()
                    every { routeIndex() } returns "0"
                    every { currentLegProgress } returns mockk {
                        every { legIndex } returns 0
                        every { durationRemaining } returns 0.0
                        every { distanceRemaining } returns 0.0f
                    }
                }
            }
        }
        val customArrivalController: ArrivalController = mockk {
            every { navigateNextRouteLeg(any()) } returns testNavigateNextRouteLeg
        }

        arrivalProgressObserver.attach(customArrivalController)

        verify(exactly = 0) { tripSession.updateLegIndex(any(), any()) }
        verify { arrivalObserver wasNot Called }
    }

    @Test
    fun `navigateNextRouteLeg should return true when route leg is updated`() {
        every { tripSession.getRouteProgress() } returns mockk {
            every { stale } returns false
            every { currentState } returns RouteProgressState.TRACKING
            every { navigationRoute } returns mockk {
                every { directionsRoute } returns mockk {
                    mockMultipleLegs()
                    every { routeIndex() } returns "0"
                    every { currentLegProgress } returns mockk {
                        every { legIndex } returns 1
                    }
                }
            }
        }
        val tripSessionCallbackSlot = slot<LegIndexUpdatedCallback>()
        every { tripSession.updateLegIndex(2, capture(tripSessionCallbackSlot)) } answers {
            tripSessionCallbackSlot.captured.onLegIndexUpdatedCallback(true)
        }

        arrivalProgressObserver.navigateNextRouteLeg(legIndexUpdatedCallback)

        verify(exactly = 1) { legIndexUpdatedCallback.onLegIndexUpdatedCallback(true) }
        verify(exactly = 1) { arrivalObserver.onNextRouteLegStart(any()) }
    }

    @Test
    fun `navigateNextRouteLeg should return false when route leg is not updated`() {
        every { tripSession.getRouteProgress() } returns mockk {
            every { stale } returns false
            every { currentState } returns RouteProgressState.TRACKING
            every { navigationRoute } returns mockk {
                every { directionsRoute } returns mockk {
                    mockMultipleLegs()
                    every { routeIndex() } returns "0"
                    every { currentLegProgress } returns mockk {
                        every { legIndex } returns 2
                    }
                }
            }
        }
        val tripSessionCallbackSlot = slot<LegIndexUpdatedCallback>()
        every { tripSession.updateLegIndex(3, capture(tripSessionCallbackSlot)) } answers {
            tripSessionCallbackSlot.captured.onLegIndexUpdatedCallback(false)
        }

        arrivalProgressObserver.navigateNextRouteLeg(legIndexUpdatedCallback)

        verify(exactly = 1) { legIndexUpdatedCallback.onLegIndexUpdatedCallback(false) }
        verify { arrivalObserver wasNot Called }
    }

    @Test
    fun `navigateNextRouteLeg should notify next route leg start`() {
        val onNavigateNextRouteLegCalls = slot<RouteLegProgress>()
        val onFinalDestinationArrivalCalls = slot<RouteProgress>()
        every {
            arrivalObserver.onNextRouteLegStart(capture(onNavigateNextRouteLegCalls))
        } returns Unit
        every {
            arrivalObserver.onFinalDestinationArrival(capture(onFinalDestinationArrivalCalls))
        } returns Unit
        every { tripSession.getRouteProgress() } returns mockk {
            every { stale } returns false
            every { currentState } returns RouteProgressState.TRACKING
            every { navigationRoute } returns mockk {
                every { directionsRoute } returns mockk {
                    mockMultipleLegs()
                    every { routeIndex() } returns "0"
                    every { currentLegProgress } returns mockk {
                        every { legIndex } returns 1
                    }
                }
            }
        }
        val tripSessionCallbackSlot = slot<LegIndexUpdatedCallback>()
        every { tripSession.updateLegIndex(2, capture(tripSessionCallbackSlot)) } answers {
            tripSessionCallbackSlot.captured.onLegIndexUpdatedCallback(true)
        }

        arrivalProgressObserver.navigateNextRouteLeg(mockk(relaxed = true))

        assertTrue(onNavigateNextRouteLegCalls.isCaptured)
        assertFalse(onFinalDestinationArrivalCalls.isCaptured)
    }

    private fun DirectionsRoute.mockMultipleLegs() {
        every { routeOptions() } returns mockk {
            every { coordinatesList() } returns listOf(
                Point.fromLngLat(-122.444359, 37.736351),
                Point.fromLngLat(-122.444481, 37.735916),
                Point.fromLngLat(-122.444275, 37.735595),
                Point.fromLngLat(-122.444375, 37.736141),
            )
        }
        every { legs() } returns listOf(
            mockk(),
            mockk(),
            mockk(), // This route has three legs
        )
    }
}
