package com.mapbox.navigation.core.trip.session

import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.history.MapboxHistoryRecorder
import com.mapbox.navigation.core.telemetry.navObtainUniversalSessionId
import com.mapbox.navigation.core.trip.session.NavigationSessionState.ActiveGuidance
import com.mapbox.navigation.core.trip.session.NavigationSessionState.FreeDrive
import com.mapbox.navigation.core.trip.session.NavigationSessionState.Idle
import java.util.concurrent.CopyOnWriteArraySet

internal class NavigationSession : TripSessionStateObserver {

    private val stateObservers = CopyOnWriteArraySet<NavigationSessionStateObserver>()

    internal var state: NavigationSessionState = Idle
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
        state = getNewState(hasRoutes, isDriving)
    }

    private fun getNewState(hasRoutes: Boolean, isDriving: Boolean): NavigationSessionState {
        return  when {
            hasRoutes && isDriving -> {
                ActiveGuidance(navObtainUniversalSessionId())
            }
            isDriving -> {
                FreeDrive(navObtainUniversalSessionId())
            }
            else -> {
                Idle
            }
        }
    }

    private fun getHasRoutesValue(routes: List<NavigationRoute>): Boolean = routes.isNotEmpty()

    internal fun registerNavigationSessionStateObserver(
        navigationSessionStateObserver: NavigationSessionStateObserver
    ) {
        stateObservers.add(navigationSessionStateObserver)
        navigationSessionStateObserver.onNavigationSessionStateChanged(state)
    }

    internal fun unregisterNavigationSessionStateObserver(
        navigationSessionStateObserver: NavigationSessionStateObserver
    ) {
        stateObservers.remove(navigationSessionStateObserver)
    }

    internal fun unregisterAllNavigationSessionStateObservers() {
        stateObservers.clear()
    }

    fun setRoutes(routes: List<NavigationRoute>) {
        hasRoutes = getHasRoutesValue(routes)
    }

    fun setRoutesPreview(routes: List<NavigationRoute>): NavigationSessionState {
        return getNewState(getHasRoutesValue(routes), isDriving)
    }

    override fun onSessionStateChanged(tripSessionState: TripSessionState) {
        isDriving = when (tripSessionState) {
            TripSessionState.STARTED -> true
            TripSessionState.STOPPED -> false
        }
    }
}

/**
 * Contains the various states that can occur during a navigation.
 *
 * The [MapboxNavigation] implementation can enter into the following session states:
 * - [Idle]
 * - [FreeDrive]
 * - [ActiveGuidance]
 *
 * The SDK starts off in an [Idle] state.
 * Whenever the [MapboxNavigation.startTripSession] is called, the SDK will enter the [FreeDrive] state.
 * If the session is stopped, the SDK will enter the [Idle] state.
 * If the SDK is in an [Idle] state, it stays in this same state even when a primary route is available.
 * If the SDK is already in the [FreeDrive] mode or entering it whenever a primary route is available,
 * the SDK will enter the [ActiveGuidance] mode instead.
 * When the routes are manually cleared, the SDK automatically fall back to either [Idle] or [FreeDrive] state.
 * When transitioning across states of a trip session the [sessionId] will change (empty when [Idle]).
 */
sealed class NavigationSessionState {

    /**
     * Random session UUID.
     * This is generated internally based on the current state within a trip session.
     * I.e. will change when transitioning across states of a trip session. Empty when [Idle].
     *
     * Useful to use it in combination with the [MapboxHistoryRecorder].
     *
     * @see [TripSessionState]
     */
    abstract val sessionId: String

    /**
     * Idle state
     */
    object Idle : NavigationSessionState() {
        override val sessionId = ""
    }

    /**
     * Free Drive state
     */
    data class FreeDrive internal constructor(
        override val sessionId: String
    ) : NavigationSessionState()

    /**
     * Active Guidance state
     */
    data class ActiveGuidance internal constructor(
        override val sessionId: String
    ) : NavigationSessionState()
}
