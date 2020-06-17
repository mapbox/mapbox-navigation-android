package com.mapbox.navigation.core

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.navigation.core.trip.session.TripSessionState
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class NavigationSessionTest {

    private val route: DirectionsRoute = mockk()
    private val stateObserver: NavigationSessionStateObserver = mockk(relaxUnitFun = true)

    @Test
    fun stateObserverImmediateIdle() {
        val navigationSession = NavigationSession()

        navigationSession.registerNavigationSessionStateObserver(stateObserver)

        verify(exactly = 1) {
            stateObserver.onNavigationSessionStateChanged(NavigationSession.State.IDLE)
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
            stateObserver.onNavigationSessionStateChanged(NavigationSession.State.ACTIVE_GUIDANCE)
        }
    }

    @Test
    fun stateObserverImmediateActiveFreeDrive() {
        val navigationSession = NavigationSession()
        navigationSession.onSessionStateChanged(TripSessionState.STARTED)

        navigationSession.registerNavigationSessionStateObserver(stateObserver)

        verify(exactly = 1) {
            stateObserver.onNavigationSessionStateChanged(NavigationSession.State.FREE_DRIVE)
        }
    }

    @Test
    fun stateObserverIdle() {
        val navigationSession = NavigationSession()
        navigationSession.registerNavigationSessionStateObserver(stateObserver)

        navigationSession.onSessionStateChanged(TripSessionState.STARTED)
        navigationSession.onSessionStateChanged(TripSessionState.STOPPED)

        verify(exactly = 2) {
            stateObserver.onNavigationSessionStateChanged(NavigationSession.State.IDLE)
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
            stateObserver.onNavigationSessionStateChanged(NavigationSession.State.ACTIVE_GUIDANCE)
        }
    }

    @Test
    fun stateObserverFreeDrive() {
        val navigationSession = NavigationSession()
        navigationSession.registerNavigationSessionStateObserver(stateObserver)

        navigationSession.onSessionStateChanged(TripSessionState.STARTED)

        verify(exactly = 1) {
            stateObserver.onNavigationSessionStateChanged(NavigationSession.State.FREE_DRIVE)
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
            stateObserver.onNavigationSessionStateChanged(NavigationSession.State.IDLE)
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
            stateObserver.onNavigationSessionStateChanged(NavigationSession.State.ACTIVE_GUIDANCE)
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
            stateObserver.onNavigationSessionStateChanged(NavigationSession.State.FREE_DRIVE)
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
}
