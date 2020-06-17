package com.mapbox.navigation.core

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.trip.session.TripSessionState
import com.mapbox.navigation.core.trip.session.TripSessionStateObserver
import java.util.concurrent.CopyOnWriteArraySet

internal class NavigationSession :
    RoutesObserver,
    TripSessionStateObserver {

    private val stateObservers = CopyOnWriteArraySet<NavigationSessionStateObserver>()

    private var state = State.IDLE
        set(value) {
            if (field == value) {
                return
            }
            field = value

            stateObservers.forEach { it.onNavigationSessionStateChanged(value) }
        }

    private var hasRoutes = false
        set(value) {
            if (field != value) {
                field = value
                updateState()
            }
        }

    private var isDriving = false
        set(value) {
            if (field != value) {
                field = value
                updateState()
            }
        }

    private fun updateState() {
        state = when {
            hasRoutes && isDriving -> State.ACTIVE_GUIDANCE
            isDriving -> State.FREE_DRIVE
            else -> State.IDLE
        }
    }

    internal fun registerNavigationSessionStateObserver(navigationSessionStateObserver: NavigationSessionStateObserver) {
        stateObservers.add(navigationSessionStateObserver)
        navigationSessionStateObserver.onNavigationSessionStateChanged(state)
    }

    internal fun unregisterNavigationSessionStateObserver(navigationSessionStateObserver: NavigationSessionStateObserver) {
        stateObservers.remove(navigationSessionStateObserver)
    }

    internal fun unregisterAllNavigationSessionStateObservers() {
        stateObservers.clear()
    }

    override fun onRoutesChanged(routes: List<DirectionsRoute>) {
        hasRoutes = routes.isNotEmpty()
    }

    override fun onSessionStateChanged(tripSessionState: TripSessionState) {
        isDriving = when (tripSessionState) {
            TripSessionState.STARTED -> true
            TripSessionState.STOPPED -> false
        }
    }

    internal enum class State {
        IDLE,
        FREE_DRIVE,
        ACTIVE_GUIDANCE
    }
}
