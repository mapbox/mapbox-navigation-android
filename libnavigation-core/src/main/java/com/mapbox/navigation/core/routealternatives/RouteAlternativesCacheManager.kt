package com.mapbox.navigation.core.routealternatives

import androidx.annotation.VisibleForTesting
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.navigation.core.trip.session.NavigationSession
import com.mapbox.navigation.core.trip.session.NavigationSessionState
import com.mapbox.navigation.core.trip.session.NavigationSessionStateObserver
import java.util.concurrent.ConcurrentLinkedQueue

internal class RouteAlternativesCacheManager(
    navigationSession: NavigationSession,
) {
    private val cachedAlternatives = ConcurrentLinkedQueue<List<DirectionsRoute>>()

    private val sessionStateObserver = NavigationSessionStateObserver { state ->
        when (state) {
            is NavigationSessionState.ActiveGuidance -> Unit
            is NavigationSessionState.FreeDrive,
            NavigationSessionState.Idle -> {
                cachedAlternatives.clear()
            }
        }
    }

    @VisibleForTesting
    internal companion object {
        internal const val CACHE_SIZE = 3
    }

    init {
        navigationSession.registerNavigationSessionStateObserver(sessionStateObserver)
    }

    fun push(alternatives: List<DirectionsRoute>) {
        if (cachedAlternatives.size == CACHE_SIZE) {
            cachedAlternatives.remove()
        }
        cachedAlternatives.add(alternatives)
    }

    fun areAlternatives(routes: List<DirectionsRoute>): Boolean =
        cachedAlternatives.any { cachedAlternative ->
            routes.any { cachedAlternative.contains(it) }
        }
}
