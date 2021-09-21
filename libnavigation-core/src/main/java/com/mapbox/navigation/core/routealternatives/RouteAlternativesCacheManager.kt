package com.mapbox.navigation.core.routealternatives

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.navigation.core.trip.session.NavigationSession
import com.mapbox.navigation.core.trip.session.NavigationSessionState
import com.mapbox.navigation.core.trip.session.NavigationSessionStateObserver
import java.util.concurrent.CopyOnWriteArrayList

internal class RouteAlternativesCacheManager(
    navigationSession: NavigationSession,
) {
    private val cachedAlternatives = CopyOnWriteArrayList<DirectionsRoute>()

    private val sessionStateObserver = NavigationSessionStateObserver { state ->
        when (state) {
            is NavigationSessionState.ActiveGuidance -> Unit
            is NavigationSessionState.FreeDrive,
            NavigationSessionState.Idle -> {
                cachedAlternatives.clear()
            }
        }
    }

    init {
        navigationSession.registerNavigationSessionStateObserver(sessionStateObserver)
    }

    fun push(alternatives: List<DirectionsRoute>) {
        cachedAlternatives.addAll(alternatives)
    }

    fun areAlternatives(routes: List<DirectionsRoute>): Boolean =
        routes.map { it in cachedAlternatives }.any { it }
}
