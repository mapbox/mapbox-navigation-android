package com.mapbox.navigation.core.trip.session

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.navigation.core.trip.session.NavigationSessionState.ActiveGuidance
import com.mapbox.navigation.core.trip.session.NavigationSessionState.FreeDrive
import com.mapbox.navigation.core.trip.session.NavigationSessionState.Idle
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class NavigationSessionTest {

    private val route: DirectionsRoute = mockk()
    private val stateObserver: NavigationSessionStateObserver = mockk()
    private val navigationSessionStateSlot = slot<NavigationSessionState>()

    @Before
    fun setup() {
        every {
            stateObserver.onNavigationSessionStateChanged(capture(navigationSessionStateSlot))
        } just runs
    }

    @Test
    fun stateObserverImmediateIdle() {
        val navigationSession = NavigationSession()

        navigationSession.registerNavigationSessionStateObserver(stateObserver)

        verify(exactly = 1) {
            stateObserver.onNavigationSessionStateChanged(
                Idle
            )
        }
    }

    @Test
    fun stateObserverImmediateActiveGuidance() {
        val routes = mutableListOf<DirectionsRoute>()
        val navigationSession = NavigationSession()
        routes.add(route)
        navigationSession.onRoutesChanged(routes)
        navigationSession.onSessionStateChanged(TripSessionState.STARTED)

        navigationSession.registerNavigationSessionStateObserver(stateObserver)

        verify(exactly = 1) {
            stateObserver.onNavigationSessionStateChanged(
                ActiveGuidance(navigationSessionStateSlot.captured.sessionId)
            )
        }
    }

    @Test
    fun stateObserverImmediateActiveFreeDrive() {
        val navigationSession = NavigationSession()
        navigationSession.onSessionStateChanged(TripSessionState.STARTED)

        navigationSession.registerNavigationSessionStateObserver(stateObserver)

        verify(exactly = 1) {
            stateObserver.onNavigationSessionStateChanged(
                FreeDrive(navigationSessionStateSlot.captured.sessionId)
            )
        }
    }

    @Test
    fun stateObserverIdle() {
        val navigationSession = NavigationSession()
        navigationSession.registerNavigationSessionStateObserver(stateObserver)

        navigationSession.onSessionStateChanged(TripSessionState.STARTED)
        navigationSession.onSessionStateChanged(TripSessionState.STOPPED)

        verify(exactly = 2) {
            stateObserver.onNavigationSessionStateChanged(
                Idle
            )
        }
    }

    @Test
    fun stateObserverActiveGuidance() {
        val routes = mutableListOf<DirectionsRoute>()
        val navigationSession = NavigationSession()
        navigationSession.registerNavigationSessionStateObserver(stateObserver)

        routes.add(route)
        navigationSession.onRoutesChanged(routes)
        navigationSession.onSessionStateChanged(TripSessionState.STARTED)

        verify(exactly = 1) {
            stateObserver.onNavigationSessionStateChanged(
                ActiveGuidance(navigationSessionStateSlot.captured.sessionId)
            )
        }
    }

    @Test
    fun stateObserverFreeDrive() {
        val navigationSession = NavigationSession()
        navigationSession.registerNavigationSessionStateObserver(stateObserver)

        navigationSession.onSessionStateChanged(TripSessionState.STARTED)

        verify(exactly = 1) {
            stateObserver.onNavigationSessionStateChanged(
                FreeDrive(navigationSessionStateSlot.captured.sessionId)
            )
        }
    }

    @Test
    fun stateObserverUnregisterIdle() {
        val navigationSession = NavigationSession()
        navigationSession.registerNavigationSessionStateObserver(stateObserver)
        clearMocks(stateObserver)
        navigationSession.unregisterNavigationSessionStateObserver(stateObserver)

        navigationSession.onSessionStateChanged(TripSessionState.STOPPED)

        verify(exactly = 0) {
            stateObserver.onNavigationSessionStateChanged(
                Idle
            )
        }
    }

    @Test
    fun stateObserverUnregisterActiveGuidance() {
        val routes = mutableListOf<DirectionsRoute>()
        val navigationSession = NavigationSession()
        navigationSession.registerNavigationSessionStateObserver(stateObserver)
        clearMocks(stateObserver)
        navigationSession.unregisterNavigationSessionStateObserver(stateObserver)

        routes.add(route)
        navigationSession.onRoutesChanged(routes)
        navigationSession.onSessionStateChanged(TripSessionState.STARTED)

        verify(exactly = 0) {
            stateObserver.onNavigationSessionStateChanged(
                ActiveGuidance(navigationSessionStateSlot.captured.sessionId)
            )
        }
    }

    @Test
    fun stateObserverUnregisterFreeDrive() {
        val navigationSession = NavigationSession()
        navigationSession.registerNavigationSessionStateObserver(stateObserver)
        clearMocks(stateObserver)
        navigationSession.unregisterNavigationSessionStateObserver(stateObserver)

        navigationSession.onSessionStateChanged(TripSessionState.STARTED)

        verify(exactly = 0) {
            stateObserver.onNavigationSessionStateChanged(
                FreeDrive(navigationSessionStateSlot.captured.sessionId)
            )
        }
    }

    @Test
    fun unregisterAllStateObservers() {
        val navigationSession = NavigationSession()
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
        val navigationSession = NavigationSession()
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
        val navigationSession = NavigationSession()
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
        val routes = mutableListOf<DirectionsRoute>()
        val navigationSession = NavigationSession()
        navigationSession.registerNavigationSessionStateObserver(mockedStateObserver)

        routes.add(route)
        navigationSession.onRoutesChanged(routes)
        navigationSession.onSessionStateChanged(TripSessionState.STARTED)

        assertTrue(navigationSessionStateSlot.captured.sessionId.isNotEmpty())
    }

    @Test
    fun navigationSessionStateHoldsSessionId() {
        val mockedStateObserver: NavigationSessionStateObserver = mockk()
        val navigationSessionStateSlots = mutableListOf<NavigationSessionState>()

        every {
            mockedStateObserver.onNavigationSessionStateChanged(
                capture(
                    navigationSessionStateSlots
                )
            )
        } just runs
        val routes = mutableListOf<DirectionsRoute>()
        val navigationSession = NavigationSession()
        navigationSession.registerNavigationSessionStateObserver(mockedStateObserver)

        navigationSession.onSessionStateChanged(TripSessionState.STARTED)
        val one = navigationSessionStateSlots[1].sessionId
        navigationSession.onSessionStateChanged(TripSessionState.STOPPED)
        routes.add(mockk())
        navigationSession.onRoutesChanged(routes)
        navigationSession.onSessionStateChanged(TripSessionState.STARTED)
        routes.clear()
        navigationSession.onRoutesChanged(emptyList())
        val two = navigationSessionStateSlots[1].sessionId

        assertEquals(one, two)
    }
}
