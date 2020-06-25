package com.mapbox.navigation.core.arrival

import com.mapbox.navigation.base.trip.model.RouteLegProgress
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.model.RouteProgressState
import com.mapbox.navigation.core.trip.session.TripSession
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

internal class ArrivalProgressObserverTest {

    private val tripSession: TripSession = mockk()
    private val arrivalObserver: ArrivalObserver = mockk {
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
    fun `should not crash with null route progress values`() {
        val routeProgress: RouteProgress = mockk {
            every { currentState } returns RouteProgressState.LOCATION_TRACKING
            every { route } returns mockk {
                every { legs() } returns listOf(
                    mockk(), mockk(), mockk() // This route has three legs
                )
                every { routeIndex() } returns null
                every { currentLegProgress } returns mockk {
                    every { legIndex } returns 0
                    every { durationRemaining } returns 0.0
                    every { distanceRemaining } returns 0.0f
                }
            }
        }

        arrivalProgressObserver.onRouteProgressChanged(routeProgress)
    }

    @Test
    fun `should notify next leg start when the next leg started`() {
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
        arrivalProgressObserver.onRouteProgressChanged(mockk {
            every { currentState } returns RouteProgressState.ROUTE_COMPLETE
            every { route } returns mockk {
                every { legs() } returns listOf(mockk(), mockk(), mockk())
            }
            every { currentLegProgress } returns mockk {
                every { legIndex } returns 1
                every { durationRemaining } returns 2.0
                every { distanceRemaining } returns 8.0f
            }
        })

        assertTrue(onNextRouteLegStartCalls.isCaptured)
        assertFalse(onFinalDestinationArrivalCalls.isCaptured)
    }

    @Test
    fun `should notify final destination arrival when arrived at last waypoint`() {
        val onNextRouteLegStartCalls = slot<RouteLegProgress>()
        val onFinalDestinationArrivalCalls = slot<RouteProgress>()
        val customArrivalController: ArrivalController = mockk {
            every { navigateNextRouteLeg(capture(onNextRouteLegStartCalls)) } returns false
            every { arrivalOptions() } returns mockk {
                every { arrivalInSeconds } returns null
                every { arrivalInMeters } returns 10.0
            }
        }
        every { arrivalObserver.onFinalDestinationArrival(capture(onFinalDestinationArrivalCalls)) } returns Unit

        arrivalProgressObserver.attach(customArrivalController)
        arrivalProgressObserver.onRouteProgressChanged(mockk {
            every { currentState } returns RouteProgressState.ROUTE_COMPLETE
            every { route } returns mockk {
                every { legs() } returns listOf(mockk(), mockk(), mockk())
            }
            every { currentLegProgress } returns mockk {
                every { legIndex } returns 2
                every { durationRemaining } returns 2.0
                every { distanceRemaining } returns 8.0f
            }
        })

        assertFalse(onNextRouteLegStartCalls.isCaptured)
        assertTrue(onFinalDestinationArrivalCalls.isCaptured)
    }

    @Test
    fun `should notify final destination arrival when arrived at last waypoint only once`() {
        val onNextRouteLegStartCalls = slot<RouteLegProgress>()
        val onFinalDestinationArrivalCalls = mutableListOf<RouteProgress>()
        val customArrivalController: ArrivalController = mockk {
            every { navigateNextRouteLeg(capture(onNextRouteLegStartCalls)) } returns false
            every { arrivalOptions() } returns mockk {
                every { arrivalInSeconds } returns null
                every { arrivalInMeters } returns 10.0
            }
        }
        every { arrivalObserver.onFinalDestinationArrival(capture(onFinalDestinationArrivalCalls)) } returns Unit
        val routeProgress: RouteProgress = mockk {
            every { currentState } returns RouteProgressState.ROUTE_COMPLETE
            every { route } returns mockk {
                every { legs() } returns listOf(mockk(), mockk(), mockk())
            }
            every { currentLegProgress } returns mockk {
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
            every { arrivalOptions() } returns mockk {
                every { arrivalInSeconds } returns 5.0
                every { arrivalInMeters } returns null
            }
        }

        arrivalProgressObserver.attach(customArrivalController)
        arrivalProgressObserver.onRouteProgressChanged(mockk {
            every { currentState } returns RouteProgressState.LOCATION_TRACKING
            every { currentLegProgress } returns mockk {
                every { durationRemaining } returns 1.0
                every { distanceRemaining } returns 15.0f
                every { legIndex } returns 0
            }
            every { route } returns mockk {
                every { legs() } returns listOf(
                    mockk(), mockk(), mockk() // This route has three legs
                )
            }
        })

        assertTrue(onArrivalCalls.isCaptured)
        assertEquals(onArrivalCalls.captured.durationRemaining, 1.0, 0.001)
    }

    @Test
    fun `should notify observers with distance option`() {
        val onArrivalCalls = slot<RouteLegProgress>()
        val customArrivalController: ArrivalController = mockk {
            every { navigateNextRouteLeg(capture(onArrivalCalls)) } returns false
            every { arrivalOptions() } returns mockk {
                every { arrivalInSeconds } returns null
                every { arrivalInMeters } returns 10.0
            }
        }

        arrivalProgressObserver.attach(customArrivalController)
        arrivalProgressObserver.onRouteProgressChanged(mockk {
            every { currentState } returns RouteProgressState.LOCATION_TRACKING
            every { currentLegProgress } returns mockk {
                every { durationRemaining } returns 2.0
                every { distanceRemaining } returns 8.0f
                every { legIndex } returns 0
            }
            every { route } returns mockk {
                every { legs() } returns listOf(
                    mockk(), mockk(), mockk() // This route has three legs
                )
            }
        })

        assertEquals(onArrivalCalls.captured.distanceRemaining, 8.0f, 0.001f)
        verify(exactly = 0) { arrivalObserver.onFinalDestinationArrival(any()) }
    }

    @Test
    fun `should notify arrival if arrived on attach`() {
        val onArrivalCalls = slot<RouteLegProgress>()
        val customArrivalController: ArrivalController = mockk {
            every { navigateNextRouteLeg(capture(onArrivalCalls)) } returns false
            every { arrivalOptions() } returns mockk {
                every { arrivalInSeconds } returns 5.0
                every { arrivalInMeters } returns null
            }
        }
        val routeProgress: RouteProgress = mockk {
            every { currentState } returns RouteProgressState.LOCATION_TRACKING
            every { currentLegProgress } returns mockk {
                every { durationRemaining } returns 1.0
                every { distanceRemaining } returns 15.0f
                every { legIndex } returns 0
            }
            every { route } returns mockk {
                every { legs() } returns listOf(
                    mockk(), mockk(), mockk() // This route has three legs
                )
            }
        }
        every { tripSession.getRouteProgress() } returns routeProgress

        arrivalProgressObserver.attach(customArrivalController)
        arrivalProgressObserver.onRouteProgressChanged(routeProgress)

        assertTrue(onArrivalCalls.isCaptured)
    }

    @Test
    fun `should not navigate to next route leg before arrival`() {
        val onArrivalCalls = slot<RouteLegProgress>()
        val customArrivalController: ArrivalController = mockk {
            every { navigateNextRouteLeg(capture(onArrivalCalls)) } returns false
            every { arrivalOptions() } returns mockk {
                every { arrivalInMeters } returns 10.0
                every { arrivalInSeconds } returns 5.0
            }
        }

        arrivalProgressObserver.attach(customArrivalController)
        arrivalProgressObserver.onRouteProgressChanged(mockk {
            every { currentState } returns RouteProgressState.LOCATION_TRACKING
            every { currentLegProgress } returns mockk {
                every { durationRemaining } returns 360.0
                every { distanceRemaining } returns 80.0f
                every { legIndex } returns 0
            }
            every { route } returns mockk {
                every { legs() } returns listOf(
                    mockk(), mockk(), mockk() // This route has three legs
                )
            }
        })

        assertFalse(onArrivalCalls.isCaptured)
    }

    @Test
    fun `should navigate to next waypoint automatically by default`() {
        val routeProgress: RouteProgress = mockk {
            every { route } returns mockk {
                every { currentState } returns RouteProgressState.LOCATION_TRACKING
                every { legs() } returns listOf(
                    mockk(), mockk(), mockk() // This route has three legs
                )
                every { routeIndex() } returns "0"
                every { currentLegProgress } returns mockk {
                    every { durationRemaining } returns 0.0
                    every { distanceRemaining } returns 0.0f
                    every { legIndex } returns 1
                }
            }
        }
        every { tripSession.getRouteProgress() } returns routeProgress
        every { tripSession.updateLegIndex(2) } returns mockk {
            every { legIndex } returns 2
        }

        arrivalProgressObserver.onRouteProgressChanged(routeProgress)

        verify { tripSession.updateLegIndex(2) }
    }

    @Test
    fun `should navigate to next waypoint automatically using options`() {
        val testNavigateNextRouteLeg = true
        val routeProgress: RouteProgress = mockk {
            every { currentState } returns RouteProgressState.LOCATION_TRACKING
            every { route } returns mockk {
                every { legs() } returns listOf(
                    mockk(), mockk(), mockk() // This route has three legs
                )
                every { routeIndex() } returns "0"
                every { currentLegProgress } returns mockk {
                    every { legIndex } returns 0
                    every { durationRemaining } returns 0.0
                    every { distanceRemaining } returns 0.0f
                }
            }
        }
        every { tripSession.getRouteProgress() } returns routeProgress
        val customArrivalController: ArrivalController = mockk {
            every { navigateNextRouteLeg(any()) } returns testNavigateNextRouteLeg
            every { arrivalOptions() } returns mockk {
                every { arrivalInSeconds } returns 0.0
                every { arrivalInMeters } returns null
            }
        }
        every { tripSession.updateLegIndex(any()) } returns mockk {
            every { legIndex } returns 1
        }

        arrivalProgressObserver.attach(customArrivalController)
        arrivalProgressObserver.onRouteProgressChanged(routeProgress)

        verify(exactly = 1) { tripSession.updateLegIndex(1) }
    }

    @Test
    fun `should not navigate to next waypoint automatically using options`() {
        val testNavigateNextRouteLeg = false
        every { tripSession.getRouteProgress() } returns mockk {
            every { currentState } returns RouteProgressState.LOCATION_TRACKING
            every { route } returns mockk {
                every { legs() } returns listOf(
                    mockk(), mockk(), mockk() // This route has three legs
                )
                every { routeIndex() } returns "0"
                every { currentLegProgress } returns mockk {
                    every { legIndex } returns 0
                    every { durationRemaining } returns 0.0
                    every { distanceRemaining } returns 0.0f
                }
            }
        }
        val customArrivalController: ArrivalController = mockk {
            every { navigateNextRouteLeg(any()) } returns testNavigateNextRouteLeg
            every { arrivalOptions() } returns mockk {
                every { arrivalInSeconds } returns 0.0
                every { arrivalInMeters } returns null
            }
        }

        arrivalProgressObserver.attach(customArrivalController)

        verify(exactly = 0) { tripSession.updateLegIndex(any()) }
    }

    @Test
    fun `navigateNextRouteLeg should return true when route leg is updated`() {
        every { tripSession.getRouteProgress() } returns mockk {
            every { currentState } returns RouteProgressState.LOCATION_TRACKING
            every { route } returns mockk {
                every { legs() } returns listOf(
                    mockk(), mockk(), mockk() // This route has three legs
                )
                every { routeIndex() } returns "0"
                every { currentLegProgress } returns mockk {
                    every { legIndex } returns 1
                }
            }
        }
        every { tripSession.updateLegIndex(2) } returns mockk {
            every { legIndex } returns 2
        }

        val didNavigate = arrivalProgressObserver.navigateNextRouteLeg()

        assertTrue(didNavigate)
    }

    @Test
    fun `navigateNextRouteLeg should return false when route leg is not updated`() {
        every { tripSession.getRouteProgress() } returns mockk {
            every { currentState } returns RouteProgressState.LOCATION_TRACKING
            every { route } returns mockk {
                every { legs() } returns listOf(
                    mockk(), mockk(), mockk() // This route has three legs
                )
                every { routeIndex() } returns "0"
                every { currentLegProgress } returns mockk {
                    every { legIndex } returns 2
                }
            }
        }
        every { tripSession.updateLegIndex(3) } returns mockk {
            every { legIndex } returns 2
        }

        val didNavigate = arrivalProgressObserver.navigateNextRouteLeg()

        assertFalse(didNavigate)
    }

    @Test
    fun `navigateNextRouteLeg should notify next route leg start`() {
        val onNavigateNextRouteLegCalls = slot<RouteLegProgress>()
        val onFinalDestinationArrivalCalls = slot<RouteProgress>()
        every { arrivalObserver.onNextRouteLegStart(capture(onNavigateNextRouteLegCalls)) } returns Unit
        every { arrivalObserver.onFinalDestinationArrival(capture(onFinalDestinationArrivalCalls)) } returns Unit
        every { tripSession.getRouteProgress() } returns mockk {
            every { currentState } returns RouteProgressState.LOCATION_TRACKING
            every { route } returns mockk {
                every { legs() } returns listOf(
                    mockk(), mockk(), mockk() // This route has three legs
                )
                every { routeIndex() } returns "0"
                every { currentLegProgress } returns mockk {
                    every { legIndex } returns 1
                }
            }
        }
        every { tripSession.updateLegIndex(2) } returns mockk {
            every { legIndex } returns 2
        }

        arrivalProgressObserver.navigateNextRouteLeg()

        assertTrue(onNavigateNextRouteLegCalls.isCaptured)
        assertFalse(onFinalDestinationArrivalCalls.isCaptured)
    }
}
