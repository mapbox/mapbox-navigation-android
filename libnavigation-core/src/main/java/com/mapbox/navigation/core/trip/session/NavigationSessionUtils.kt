package com.mapbox.navigation.core.trip.session

import com.mapbox.navigation.core.telemetry.navObtainUniversalSessionId

internal object NavigationSessionUtils {

    fun isDriving(tripSessionState: TripSessionState): Boolean = when (tripSessionState) {
        TripSessionState.STARTED -> true
        TripSessionState.STOPPED -> false
    }

    fun getNewState(
        isDriving: Boolean,
        hasRoutes: Boolean
    ): NavigationSessionState = when {
        hasRoutes && isDriving -> {
            NavigationSessionState.ActiveGuidance(navObtainUniversalSessionId())
        }
        isDriving -> {
            NavigationSessionState.FreeDrive(navObtainUniversalSessionId())
        }
        else -> {
            NavigationSessionState.Idle
        }
    }
}
