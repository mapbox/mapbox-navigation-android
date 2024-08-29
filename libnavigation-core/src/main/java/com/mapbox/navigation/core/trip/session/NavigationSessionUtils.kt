package com.mapbox.navigation.core.trip.session

import com.mapbox.navigation.core.internal.HistoryRecordingSessionState
import com.mapbox.navigation.core.telemetry.navObtainUniversalSessionId

internal object NavigationSessionUtils {

    fun isDriving(tripSessionState: TripSessionState): Boolean = when (tripSessionState) {
        TripSessionState.STARTED -> true
        TripSessionState.STOPPED -> false
    }

    fun getNewNavigationSessionState(
        isDriving: Boolean,
        hasRoutes: Boolean,
    ): NavigationSessionState = getNewState(
        isDriving,
        hasRoutes,
        { NavigationSessionState.Idle },
        { NavigationSessionState.FreeDrive(it) },
        { NavigationSessionState.ActiveGuidance(it) },
    )

    fun getNewHistoryRecordingSessionState(
        isDriving: Boolean,
        hasRoutes: Boolean,
    ): HistoryRecordingSessionState = getNewState(
        isDriving,
        hasRoutes,
        { HistoryRecordingSessionState.Idle },
        { HistoryRecordingSessionState.FreeDrive(it) },
        { HistoryRecordingSessionState.ActiveGuidance(it) },
    )

    private fun <T> getNewState(
        isDriving: Boolean,
        hasRoutes: Boolean,
        idleProvider: () -> T,
        freeDriveProvider: (String) -> T,
        activeGuidanceProvider: (String) -> T,
    ): T = when {
        hasRoutes && isDriving -> {
            activeGuidanceProvider(navObtainUniversalSessionId())
        }
        isDriving -> {
            freeDriveProvider(navObtainUniversalSessionId())
        }
        else -> {
            idleProvider()
        }
    }
}
