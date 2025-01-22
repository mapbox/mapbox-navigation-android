package com.mapbox.navigation.core.trip.session

import com.mapbox.common.TelemetrySystemUtils
import com.mapbox.navigation.core.internal.HistoryRecordingSessionState

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
            activeGuidanceProvider(telemetryUuid())
        }
        isDriving -> {
            freeDriveProvider(telemetryUuid())
        }
        else -> {
            idleProvider()
        }
    }

    private fun telemetryUuid(): String {
        return TelemetrySystemUtils.obtainUniversalUniqueIdentifier()
    }
}
