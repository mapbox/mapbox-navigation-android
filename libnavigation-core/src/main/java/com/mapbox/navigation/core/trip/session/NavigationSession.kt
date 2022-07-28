package com.mapbox.navigation.core.trip.session

import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesExtra
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.directions.session.RoutesUpdatedResult
import com.mapbox.navigation.core.history.MapboxHistoryRecorder
import com.mapbox.navigation.core.trip.session.NavigationSessionState.ActiveGuidance
import com.mapbox.navigation.core.trip.session.NavigationSessionState.FreeDrive
import com.mapbox.navigation.core.trip.session.NavigationSessionState.Idle
import java.util.concurrent.CopyOnWriteArraySet

internal class NavigationSession : RoutesObserver, TripSessionStateObserver {

    private val stateObservers = CopyOnWriteArraySet<NavigationSessionStateObserver>()
    private val stateObserversV2 = CopyOnWriteArraySet<NavigationSessionStateObserverV2>()

    internal var state: NavigationSessionState = Idle
        set(value) {
            if (field == value) {
                return
            }
            field = value

            stateObservers.forEach { it.onNavigationSessionStateChanged(value) }
        }

    internal var stateV2: NavigationSessionStateV2 = NavigationSessionStateV2.Idle
        set(value) {
            if (field == value) {
                return
            }
            field = value

            stateObserversV2.forEach { it.onNavigationSessionStateChanged(value) }
        }

    private var hasRoutes = false

    private var isDriving = false
        set(value) {
            if (field != value) {
                field = value
                updateState()
            }
        }

    private var isPreview = false

    private fun updateState() {
        stateV2 = NavigationSessionUtils.getNewStateV2(isDriving, hasRoutes, isPreview)
        state = when (stateV2) {
            is NavigationSessionStateV2.ActiveGuidance -> ActiveGuidance(stateV2.sessionId)
            is NavigationSessionStateV2.FreeDrive -> FreeDrive(stateV2.sessionId)
            NavigationSessionStateV2.Idle -> Idle
            is NavigationSessionStateV2.RoutePreview -> FreeDrive(stateV2.sessionId)
        }
    }

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

    internal fun registerNavigationSessionStateObserverV2(
        navigationSessionStateObserver: NavigationSessionStateObserverV2
    ) {
        stateObserversV2.add(navigationSessionStateObserver)
        navigationSessionStateObserver.onNavigationSessionStateChanged(stateV2)
    }

    internal fun unregisterNavigationSessionStateObserverV2(
        navigationSessionStateObserver: NavigationSessionStateObserverV2
    ) {
        stateObserversV2.remove(navigationSessionStateObserver)
    }

    internal fun unregisterAllNavigationSessionStateObservers() {
        stateObservers.clear()
    }

    override fun onRoutesChanged(result: RoutesUpdatedResult) {
        val newIsPreview = result.reason == RoutesExtra.ROUTES_UPDATE_REASON_PREVIEW
        val newNasRoutes = result.navigationRoutes.isNotEmpty()
        if (newIsPreview != isPreview || newNasRoutes != hasRoutes) {
            isPreview = newIsPreview
            hasRoutes = newNasRoutes
            updateState()
        }
    }

    override fun onSessionStateChanged(tripSessionState: TripSessionState) {
        isDriving = NavigationSessionUtils.isDriving(tripSessionState)
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

sealed class NavigationSessionStateV2 {

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
    object Idle : NavigationSessionStateV2() {
        override val sessionId = ""
    }

    /**
     * Free Drive state
     */
    data class FreeDrive internal constructor(
        override val sessionId: String
    ) : NavigationSessionStateV2()

    /**
     * Active Guidance state
     */
    data class ActiveGuidance internal constructor(
        override val sessionId: String
    ) : NavigationSessionStateV2()

    data class RoutePreview internal constructor(
        override val sessionId: String
    ) : NavigationSessionStateV2()
}
