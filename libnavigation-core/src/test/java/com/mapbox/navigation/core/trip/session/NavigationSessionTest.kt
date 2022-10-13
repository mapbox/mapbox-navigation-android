package com.mapbox.navigation.core.trip.session

import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.core.directions.session.RoutesExtra
import com.mapbox.navigation.core.directions.session.RoutesUpdatedResult
import com.mapbox.navigation.testing.factories.createNavigationRoutes
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import io.mockk.verifySequence
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class NavigationSessionTest {

    private val route: NavigationRoute = mockk {
        every { routeOptions } returns mockk()
        every { directionsRoute } returns mockk()
    }
    private val stateObserver: NavigationSessionStateObserver = mockk()
    private val navigationSessionStateSlot = slot<NavigationSessionState>()
    private val stateObserverV2: NavigationSessionStateObserverV2 = mockk()
    private val navigationSessionV2StateSlot = slot<NavigationSessionStateV2>()

    @Before
    fun setup() {
        every {
            stateObserver.onNavigationSessionStateChanged(capture(navigationSessionStateSlot))
        } just runs
        every {
            stateObserverV2.onNavigationSessionStateChanged(capture(navigationSessionV2StateSlot))
        } just runs
    }

    @Test
    fun stateObserverImmediateIdle() {
        val navigationSession = createNavigationSession()

        navigationSession.registerNavigationSessionStateObserver(stateObserver)
        navigationSession.registerNavigationSessionStateObserverV2(stateObserverV2)

        verify(exactly = 1) {
            stateObserver.onNavigationSessionStateChanged(
                NavigationSessionState.Idle
            )
        }
        verify(exactly = 1) {
            stateObserverV2.onNavigationSessionStateChanged(
                NavigationSessionStateV2.Idle
            )
        }
        assertEquals(
            navigationSessionStateSlot.captured.sessionId,
            navigationSessionV2StateSlot.captured.sessionId
        )
    }

    @Test
    fun stateObserverImmediateActiveGuidance() {
        val navigationSession = createNavigationSession()
        navigationSession.onRoutesChanged(
            RoutesUpdatedResult(createNavigationRoutes(), RoutesExtra.ROUTES_UPDATE_REASON_NEW)
        )
        navigationSession.onSessionStateChanged(TripSessionState.STARTED)

        navigationSession.registerNavigationSessionStateObserver(stateObserver)
        navigationSession.registerNavigationSessionStateObserverV2(stateObserverV2)

        verify(exactly = 1) {
            stateObserver.onNavigationSessionStateChanged(
                ofType<NavigationSessionState.ActiveGuidance>()
            )
        }
        verify(exactly = 1) {
            stateObserverV2.onNavigationSessionStateChanged(
                ofType<NavigationSessionStateV2.ActiveGuidance>()
            )
        }
        assertEquals(
            navigationSessionStateSlot.captured.sessionId,
            navigationSessionV2StateSlot.captured.sessionId
        )
    }

    @Test
    fun stateObserverImmediateActiveFreeDrive() {
        val navigationSession = createNavigationSession()
        navigationSession.onSessionStateChanged(TripSessionState.STARTED)

        navigationSession.registerNavigationSessionStateObserver(stateObserver)
        navigationSession.registerNavigationSessionStateObserverV2(stateObserverV2)

        verify(exactly = 1) {
            stateObserver.onNavigationSessionStateChanged(
                ofType<NavigationSessionState.FreeDrive>()
            )
        }
        verify(exactly = 1) {
            stateObserverV2.onNavigationSessionStateChanged(
                ofType<NavigationSessionStateV2.FreeDrive>()
            )
        }
        assertEquals(
            navigationSessionStateSlot.captured.sessionId,
            navigationSessionV2StateSlot.captured.sessionId
        )
    }

    @Test
    fun stateObserverIdle() {
        val navigationSession = createNavigationSession()
        navigationSession.registerNavigationSessionStateObserver(stateObserver)
        navigationSession.registerNavigationSessionStateObserverV2(stateObserverV2)

        navigationSession.onSessionStateChanged(TripSessionState.STARTED)
        navigationSession.onSessionStateChanged(TripSessionState.STOPPED)

        verify(exactly = 2) {
            stateObserver.onNavigationSessionStateChanged(
                NavigationSessionState.Idle
            )
        }
        verify(exactly = 2) {
            stateObserverV2.onNavigationSessionStateChanged(
                NavigationSessionStateV2.Idle
            )
        }
        assertEquals(
            navigationSessionStateSlot.captured.sessionId,
            navigationSessionV2StateSlot.captured.sessionId
        )
    }

    @Test
    fun stateObserverActiveGuidance() {
        val navigationSession = createNavigationSession()
        navigationSession.registerNavigationSessionStateObserver(stateObserver)
        navigationSession.registerNavigationSessionStateObserverV2(stateObserverV2)

        navigationSession.onRoutesChanged(
            RoutesUpdatedResult(createNavigationRoutes(), RoutesExtra.ROUTES_UPDATE_REASON_NEW)
        )
        navigationSession.onSessionStateChanged(TripSessionState.STARTED)

        verify(exactly = 1) {
            stateObserver.onNavigationSessionStateChanged(
                ofType<NavigationSessionState.ActiveGuidance>()
            )
        }
        verify(exactly = 1) {
            stateObserverV2.onNavigationSessionStateChanged(
                ofType<NavigationSessionStateV2.ActiveGuidance>()
            )
        }
        assertEquals(
            navigationSessionStateSlot.captured.sessionId,
            navigationSessionV2StateSlot.captured.sessionId
        )
    }

    @Test
    fun stateObserverFreeDrive() {
        val navigationSession = createNavigationSession()
        navigationSession.registerNavigationSessionStateObserver(stateObserver)
        navigationSession.registerNavigationSessionStateObserverV2(stateObserverV2)

        navigationSession.onSessionStateChanged(TripSessionState.STARTED)

        verify(exactly = 1) {
            stateObserver.onNavigationSessionStateChanged(
                ofType<NavigationSessionState.FreeDrive>()
            )
        }
        verify(exactly = 1) {
            stateObserverV2.onNavigationSessionStateChanged(
                ofType<NavigationSessionStateV2.FreeDrive>()
            )
        }
        assertEquals(
            navigationSessionStateSlot.captured.sessionId,
            navigationSessionV2StateSlot.captured.sessionId
        )
    }

    @Test
    fun stateObserverUnregisterIdle() {
        val navigationSession = createNavigationSession()
        navigationSession.onSessionStateChanged(TripSessionState.STARTED)
        navigationSession.registerNavigationSessionStateObserver(stateObserver)
        navigationSession.registerNavigationSessionStateObserverV2(stateObserverV2)

        navigationSession.unregisterNavigationSessionStateObserver(stateObserver)
        navigationSession.unregisterNavigationSessionStateObserverV2(stateObserverV2)
        navigationSession.onSessionStateChanged(TripSessionState.STOPPED)

        verify(exactly = 0) {
            stateObserver.onNavigationSessionStateChanged(
                NavigationSessionState.Idle
            )
        }
        verify(exactly = 0) {
            stateObserverV2.onNavigationSessionStateChanged(
                NavigationSessionStateV2.Idle
            )
        }
    }

    @Test
    fun stateObserverV2UnregisterIdle() {
        val navigationSession = createNavigationSession()
        navigationSession.onSessionStateChanged(TripSessionState.STARTED)
        navigationSession.registerNavigationSessionStateObserverV2(stateObserverV2)

        navigationSession.unregisterNavigationSessionStateObserverV2(stateObserverV2)
        navigationSession.onSessionStateChanged(TripSessionState.STOPPED)

        verify(exactly = 0) {
            stateObserverV2.onNavigationSessionStateChanged(
                NavigationSessionStateV2.Idle
            )
        }
    }

    @Test
    fun stateObserverUnregisterActiveGuidance() {
        val routes = mutableListOf<NavigationRoute>()
        val navigationSession = createNavigationSession()
        navigationSession.registerNavigationSessionStateObserver(stateObserver)
        clearMocks(stateObserver)
        navigationSession.unregisterNavigationSessionStateObserver(stateObserver)

        routes.add(route)
        navigationSession.onRoutesChanged(
            RoutesUpdatedResult(routes, RoutesExtra.ROUTES_UPDATE_REASON_NEW)
        )
        navigationSession.onSessionStateChanged(TripSessionState.STARTED)

        verify(exactly = 0) {
            stateObserver.onNavigationSessionStateChanged(
                NavigationSessionState.ActiveGuidance(
                    navigationSessionStateSlot.captured.sessionId
                )
            )
        }
    }

    @Test
    fun stateObserverUnregisterFreeDrive() {
        val navigationSession = createNavigationSession()
        navigationSession.registerNavigationSessionStateObserver(stateObserver)
        clearMocks(stateObserver)
        navigationSession.unregisterNavigationSessionStateObserver(stateObserver)

        navigationSession.onSessionStateChanged(TripSessionState.STARTED)

        verify(exactly = 0) {
            stateObserver.onNavigationSessionStateChanged(
                NavigationSessionState.FreeDrive(
                    navigationSessionStateSlot.captured.sessionId
                )
            )
        }
    }

    @Test
    fun unregisterAllStateObservers() {
        val navigationSession = createNavigationSession()
        navigationSession.registerNavigationSessionStateObserver(stateObserver)
        clearMocks(stateObserver)
        navigationSession.unregisterNavigationSessionStateObserver(stateObserver)

        navigationSession.unregisterAllNavigationSessionStateObservers()

        verify(exactly = 0) { stateObserver.onNavigationSessionStateChanged(any()) }
    }

    @Test
    fun emptySessionIdWhenIdle() {
        val mockedStateObserver: NavigationSessionStateObserver = mockk()
        val navigationSessionStateSlot = slot<NavigationSessionState>()
        every {
            mockedStateObserver.onNavigationSessionStateChanged(
                capture(
                    navigationSessionStateSlot
                )
            )
        } just runs
        val navigationSession = createNavigationSession()
        navigationSession.registerNavigationSessionStateObserver(mockedStateObserver)

        navigationSession.onSessionStateChanged(TripSessionState.STARTED)
        navigationSession.onSessionStateChanged(TripSessionState.STOPPED)

        assertTrue(navigationSessionStateSlot.captured.sessionId.isEmpty())
    }

    @Test
    fun nonEmptySessionIdWhenFreeDrive() {
        val mockedStateObserver: NavigationSessionStateObserver = mockk()
        val navigationSessionStateSlot = slot<NavigationSessionState>()
        every {
            mockedStateObserver.onNavigationSessionStateChanged(
                capture(
                    navigationSessionStateSlot
                )
            )
        } just runs
        val navigationSession = createNavigationSession()
        navigationSession.registerNavigationSessionStateObserver(mockedStateObserver)

        navigationSession.onSessionStateChanged(TripSessionState.STARTED)

        assertTrue(navigationSessionStateSlot.captured.sessionId.isNotEmpty())
    }

    @Test
    fun nonEmptySessionIdWhenActiveGuidance() {
        val mockedStateObserver: NavigationSessionStateObserver = mockk()
        val navigationSessionStateSlot = slot<NavigationSessionState>()
        every {
            mockedStateObserver.onNavigationSessionStateChanged(
                capture(
                    navigationSessionStateSlot
                )
            )
        } just runs
        val routes = mutableListOf<NavigationRoute>()
        val navigationSession = createNavigationSession()
        navigationSession.registerNavigationSessionStateObserver(mockedStateObserver)

        routes.add(route)
        navigationSession.onRoutesChanged(
            RoutesUpdatedResult(routes, RoutesExtra.ROUTES_UPDATE_REASON_NEW)
        )
        navigationSession.onSessionStateChanged(TripSessionState.STARTED)

        assertTrue(navigationSessionStateSlot.captured.sessionId.isNotEmpty())
    }

    @Test
    fun `start route preview`() {
        val navigationSession = createNavigationSession()
        navigationSession.registerNavigationSessionStateObserver(stateObserver)
        navigationSession.registerNavigationSessionStateObserverV2(stateObserverV2)
        clearMocks(stateObserver, stateObserverV2, answers = false)

        navigationSession.onRoutesChanged(
            RoutesUpdatedResult(createNavigationRoutes(), RoutesExtra.ROUTES_UPDATE_REASON_PREVIEW)
        )

        verify(exactly = 1) {
            stateObserver.onNavigationSessionStateChanged(
                ofType<NavigationSessionState.FreeDrive>()
            )
        }
        verify(exactly = 1) {
            stateObserverV2.onNavigationSessionStateChanged(
                ofType<NavigationSessionStateV2.RoutePreview>()
            )
        }
        assertEquals(
            navigationSessionStateSlot.captured.sessionId,
            navigationSessionV2StateSlot.captured.sessionId
        )
    }

    @Test
    fun `register observers in route preview state`() {
        val navigationSession = createNavigationSession()
        navigationSession.onRoutesChanged(
            RoutesUpdatedResult(createNavigationRoutes(), RoutesExtra.ROUTES_UPDATE_REASON_PREVIEW)
        )

        navigationSession.registerNavigationSessionStateObserver(stateObserver)
        navigationSession.registerNavigationSessionStateObserverV2(stateObserverV2)

        verify(exactly = 1) {
            stateObserver.onNavigationSessionStateChanged(
                ofType<NavigationSessionState.FreeDrive>()
            )
        }
        verify(exactly = 1) {
            stateObserverV2.onNavigationSessionStateChanged(
                ofType<NavigationSessionStateV2.RoutePreview>()
            )
        }
        assertEquals(
            navigationSessionStateSlot.captured.sessionId,
            navigationSessionV2StateSlot.captured.sessionId
        )
    }

    @Test
    fun `starting active guidance after route preview`() {
        val navigationSession = createNavigationSession()
        navigationSession.onSessionStateChanged(TripSessionState.STARTED)
        val testNavigationRoutes = createNavigationRoutes()
        navigationSession.onRoutesChanged(
            RoutesUpdatedResult(testNavigationRoutes, RoutesExtra.ROUTES_UPDATE_REASON_PREVIEW)
        )
        navigationSession.registerNavigationSessionStateObserver(stateObserver)
        navigationSession.registerNavigationSessionStateObserverV2(stateObserverV2)

        navigationSession.onRoutesChanged(
            RoutesUpdatedResult(testNavigationRoutes, RoutesExtra.ROUTES_UPDATE_REASON_NEW)
        )

        verifySequence {
            stateObserver.onNavigationSessionStateChanged(
                ofType<NavigationSessionState.FreeDrive>()
            )
            stateObserver.onNavigationSessionStateChanged(
                ofType<NavigationSessionState.ActiveGuidance>()
            )
        }
        verifySequence {
            stateObserverV2.onNavigationSessionStateChanged(
                ofType<NavigationSessionStateV2.RoutePreview>()
            )
            stateObserverV2.onNavigationSessionStateChanged(
                ofType<NavigationSessionStateV2.ActiveGuidance>()
            )
        }
        assertEquals(
            navigationSessionStateSlot.captured.sessionId,
            navigationSessionV2StateSlot.captured.sessionId
        )
    }

    private fun createNavigationSession() = NavigationSession()
}
