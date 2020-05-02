package com.mapbox.navigation.core.stops

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
        every { onStopArrival(any()) } returns Unit
        every { onRouteArrival(any()) } returns Unit
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
            every { currentState() } returns RouteProgressState.LOCATION_TRACKING
            every { route() } returns mockk {
                every { legs() } returns listOf(
                    mockk(), mockk(), mockk() // This route has three legs
                )
                every { routeIndex() } returns null
                every { currentLegProgress() } returns mockk {
                    every { legIndex() } returns 0
                    every { durationRemaining() } returns 0.0
                    every { distanceRemaining() } returns 0.0f
                }
            }
        }

        arrivalProgressObserver.onRouteProgressChanged(routeProgress)
    }

    @Test
    fun `should notify stop arrival when arrived at waypoint`() {
        val onStopArrivalCalls = slot<RouteLegProgress>()
        val onRouteArrivalCalls = slot<RouteProgress>()
        val customArrivalController: ArrivalController = mockk {
            every { navigateNextRouteLeg(capture(onStopArrivalCalls)) } returns false
            every { arrivalOptions() } returns mockk {
                every { arrivalInSeconds } returns null
                every { arrivalInMeters } returns 10.0
            }
        }

        arrivalProgressObserver.attach(customArrivalController)
        arrivalProgressObserver.onRouteProgressChanged(mockk {
            every { currentState() } returns RouteProgressState.ROUTE_ARRIVED
            every { route() } returns mockk {
                every { legs() } returns listOf(mockk(), mockk(), mockk())
            }
            every { currentLegProgress() } returns mockk {
                every { legIndex() } returns 1
                every { durationRemaining() } returns 2.0
                every { distanceRemaining() } returns 8.0f
            }
        })

        assertTrue(onStopArrivalCalls.isCaptured)
        assertFalse(onRouteArrivalCalls.isCaptured)
    }

    @Test
    fun `should notify route arrival when arrived at last stop`() {
        val onStopArrivalCalls = slot<RouteLegProgress>()
        val onRouteArrivalCalls = slot<RouteProgress>()
        val customArrivalController: ArrivalController = mockk {
            every { navigateNextRouteLeg(capture(onStopArrivalCalls)) } returns false
            every { arrivalOptions() } returns mockk {
                every { arrivalInSeconds } returns null
                every { arrivalInMeters } returns 10.0
            }
        }
        every { arrivalObserver.onRouteArrival(capture(onRouteArrivalCalls)) } returns Unit

        arrivalProgressObserver.attach(customArrivalController)
        arrivalProgressObserver.onRouteProgressChanged(mockk {
            every { currentState() } returns RouteProgressState.ROUTE_ARRIVED
            every { route() } returns mockk {
                every { legs() } returns listOf(mockk(), mockk(), mockk())
            }
            every { currentLegProgress() } returns mockk {
                every { legIndex() } returns 2
                every { durationRemaining() } returns 2.0
                every { distanceRemaining() } returns 8.0f
            }
        })

        assertFalse(onStopArrivalCalls.isCaptured)
        assertTrue(onRouteArrivalCalls.isCaptured)
    }

    @Test
    fun `should notify route arrival when arrived at last stop only once`() {
        val onStopArrivalCalls = slot<RouteLegProgress>()
        val onRouteArrivalCalls = mutableListOf<RouteProgress>()
        val customArrivalController: ArrivalController = mockk {
            every { navigateNextRouteLeg(capture(onStopArrivalCalls)) } returns false
            every { arrivalOptions() } returns mockk {
                every { arrivalInSeconds } returns null
                every { arrivalInMeters } returns 10.0
            }
        }
        every { arrivalObserver.onRouteArrival(capture(onRouteArrivalCalls)) } returns Unit
        val routeProgress: RouteProgress = mockk {
            every { currentState() } returns RouteProgressState.ROUTE_ARRIVED
            every { route() } returns mockk {
                every { legs() } returns listOf(mockk(), mockk(), mockk())
            }
            every { currentLegProgress() } returns mockk {
                every { legIndex() } returns 2
                every { durationRemaining() } returns 2.0
                every { distanceRemaining() } returns 8.0f
            }
        }

        arrivalProgressObserver.attach(customArrivalController)
        arrivalProgressObserver.onRouteProgressChanged(routeProgress)
        arrivalProgressObserver.onRouteProgressChanged(routeProgress)
        arrivalProgressObserver.onRouteProgressChanged(routeProgress)

        assertEquals(1, onRouteArrivalCalls.size)
        assertFalse(onStopArrivalCalls.isCaptured)
        verify(exactly = 1) { arrivalObserver.onRouteArrival(any()) }
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
            every { currentState() } returns RouteProgressState.LOCATION_TRACKING
            every { currentLegProgress() } returns mockk {
                every { durationRemaining() } returns 1.0
                every { distanceRemaining() } returns 15.0f
            }
        })

        assertTrue(onArrivalCalls.isCaptured)
        assertEquals(onArrivalCalls.captured.durationRemaining(), 1.0, 0.001)
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
            every { currentState() } returns RouteProgressState.LOCATION_TRACKING
            every { currentLegProgress() } returns mockk {
                every { durationRemaining() } returns 2.0
                every { distanceRemaining() } returns 8.0f
            }
        })

        assertEquals(onArrivalCalls.captured.distanceRemaining(), 8.0f, 0.001f)
        verify(exactly = 0) { arrivalObserver.onRouteArrival(any()) }
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
            every { currentState() } returns RouteProgressState.LOCATION_TRACKING
            every { currentLegProgress() } returns mockk {
                every { durationRemaining() } returns 1.0
                every { distanceRemaining() } returns 15.0f
            }
        }
        every { tripSession.getRouteProgress() } returns routeProgress

        arrivalProgressObserver.attach(customArrivalController)
        arrivalProgressObserver.onRouteProgressChanged(routeProgress)

        assertTrue(onArrivalCalls.isCaptured)
    }

    @Test
    fun `should not navigate to next stop before arrival`() {
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
            every { currentState() } returns RouteProgressState.LOCATION_TRACKING
            every { currentLegProgress() } returns mockk {
                every { durationRemaining() } returns 360.0
                every { distanceRemaining() } returns 80.0f
            }
        })

        assertFalse(onArrivalCalls.isCaptured)
    }

    @Test
    fun `should navigate to next stop automatically by default`() {
        every { tripSession.getRouteProgress() } returns mockk {
            every { route() } returns mockk {
                every { legs() } returns listOf(
                    mockk(), mockk(), mockk() // This route has three legs
                )
                every { routeIndex() } returns "0"
                every { currentLegProgress() } returns mockk {
                    every { legIndex() } returns 1
                }
            }
        }
        every { tripSession.updateLegIndex(2) } returns mockk {
            every { legIndex } returns 2
        }

        arrivalProgressObserver.onRouteProgressChanged(mockk {
            every { currentState() } returns RouteProgressState.LOCATION_TRACKING
            every { currentLegProgress() } returns mockk {
                every { durationRemaining() } returns 0.0
                every { distanceRemaining() } returns 0.0f
            }
        })

        verify { tripSession.updateLegIndex(2) }
    }

    @Test
    fun `should navigate to next stop automatically using options`() {
        val testNavigateNextRouteLeg = true
        val routeProgress: RouteProgress = mockk {
            every { currentState() } returns RouteProgressState.LOCATION_TRACKING
            every { route() } returns mockk {
                every { legs() } returns listOf(
                    mockk(), mockk(), mockk() // This route has three legs
                )
                every { routeIndex() } returns "0"
                every { currentLegProgress() } returns mockk {
                    every { legIndex() } returns 0
                    every { durationRemaining() } returns 0.0
                    every { distanceRemaining() } returns 0.0f
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
    fun `should not navigate to next stop automatically using options`() {
        val testNavigateNextRouteLeg = false
        every { tripSession.getRouteProgress() } returns mockk {
            every { currentState() } returns RouteProgressState.LOCATION_TRACKING
            every { route() } returns mockk {
                every { legs() } returns listOf(
                    mockk(), mockk(), mockk() // This route has three legs
                )
                every { routeIndex() } returns "0"
                every { currentLegProgress() } returns mockk {
                    every { legIndex() } returns 0
                    every { durationRemaining() } returns 0.0
                    every { distanceRemaining() } returns 0.0f
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
            every { currentState() } returns RouteProgressState.LOCATION_TRACKING
            every { route() } returns mockk {
                every { legs() } returns listOf(
                    mockk(), mockk(), mockk() // This route has three legs
                )
                every { routeIndex() } returns "0"
                every { currentLegProgress() } returns mockk {
                    every { legIndex() } returns 1
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
            every { currentState() } returns RouteProgressState.LOCATION_TRACKING
            every { route() } returns mockk {
                every { legs() } returns listOf(
                    mockk(), mockk(), mockk() // This route has three legs
                )
                every { routeIndex() } returns "0"
                every { currentLegProgress() } returns mockk {
                    every { legIndex() } returns 2
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
    fun `navigateNextRouteLeg should notify stop arrival`() {
        val onStopArrivalCalls = slot<RouteLegProgress>()
        val onRouteArrivalCalls = slot<RouteProgress>()
        every { arrivalObserver.onStopArrival(capture(onStopArrivalCalls)) } returns Unit
        every { arrivalObserver.onRouteArrival(capture(onRouteArrivalCalls)) } returns Unit
        every { tripSession.getRouteProgress() } returns mockk {
            every { currentState() } returns RouteProgressState.LOCATION_TRACKING
            every { route() } returns mockk {
                every { legs() } returns listOf(
                    mockk(), mockk(), mockk() // This route has three legs
                )
                every { routeIndex() } returns "0"
                every { currentLegProgress() } returns mockk {
                    every { legIndex() } returns 1
                }
            }
        }
        every { tripSession.updateLegIndex(2) } returns mockk {
            every { legIndex } returns 2
        }

        arrivalProgressObserver.navigateNextRouteLeg()

        assertTrue(onStopArrivalCalls.isCaptured)
        assertFalse(onRouteArrivalCalls.isCaptured)
    }
}
