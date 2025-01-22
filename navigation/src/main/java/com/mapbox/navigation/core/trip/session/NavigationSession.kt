package com.mapbox.navigation.core.trip.session

import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.directions.session.RoutesUpdatedResult
import com.mapbox.navigation.core.history.MapboxHistoryRecorder
import com.mapbox.navigation.core.trip.session.NavigationSessionState.ActiveGuidance
import com.mapbox.navigation.core.trip.session.NavigationSessionState.FreeDrive
import com.mapbox.navigation.core.trip.session.NavigationSessionState.Idle
import java.util.concurrent.CopyOnWriteArraySet

internal class NavigationSession : RoutesObserver, TripSessionStateObserver {

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
        state = NavigationSessionUtils.getNewNavigationSessionState(
            isDriving = isDriving,
            hasRoutes = hasRoutes,
        )
    }

    internal fun registerNavigationSessionStateObserver(
        navigationSessionStateObserver: NavigationSessionStateObserver,
    ) {
        stateObservers.add(navigationSessionStateObserver)
        navigationSessionStateObserver.onNavigationSessionStateChanged(state)
    }

    internal fun unregisterNavigationSessionStateObserver(
        navigationSessionStateObserver: NavigationSessionStateObserver,
    ) {
        stateObservers.remove(navigationSessionStateObserver)
    }

    internal fun unregisterAllNavigationSessionStateObservers() {
        stateObservers.clear()
    }

    override fun onRoutesChanged(result: RoutesUpdatedResult) {
        hasRoutes = result.navigationRoutes.isNotEmpty()
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
    class FreeDrive internal constructor(
        override val sessionId: String,
    ) : NavigationSessionState() {

        /**
         * Indicates whether some other object is "equal to" this one.
         */
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as FreeDrive

            return sessionId == other.sessionId
        }

        /**
         * Returns a hash code value for the object.
         */
        override fun hashCode(): Int {
            return sessionId.hashCode()
        }

        /**
         * Returns a string representation of the object.
         */
        override fun toString(): String {
            return "FreeDrive(sessionId='$sessionId')"
        }
    }

    /**
     * Active Guidance state
     */
    class ActiveGuidance internal constructor(
        override val sessionId: String,
    ) : NavigationSessionState() {

        /**
         * Indicates whether some other object is "equal to" this one.
         */
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as ActiveGuidance

            return sessionId == other.sessionId
        }

        /**
         * Returns a hash code value for the object.
         */
        override fun hashCode(): Int {
            return sessionId.hashCode()
        }

        /**
         * Returns a string representation of the object.
         */
        override fun toString(): String {
            return "ActiveGuidance(sessionId='$sessionId')"
        }
    }
}
