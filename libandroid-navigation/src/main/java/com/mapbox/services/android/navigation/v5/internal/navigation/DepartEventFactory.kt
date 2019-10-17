package com.mapbox.services.android.navigation.v5.internal.navigation

import com.mapbox.services.android.navigation.v5.internal.location.MetricsLocation
import com.mapbox.services.android.navigation.v5.internal.navigation.metrics.SessionState
import com.mapbox.services.android.navigation.v5.internal.navigation.routeprogress.MetricsRouteProgress
import java.util.Date

internal class DepartEventFactory(private val departEventHandler: DepartEventHandler) {

    companion object {
        private const val INITIAL_LEG_INDEX = -1
    }

    private var currentLegIndex = INITIAL_LEG_INDEX

    fun send(
        sessionState: SessionState,
        routeProgress: MetricsRouteProgress,
        location: MetricsLocation
    ): SessionState {
        val checkedSessionState = checkResetForNewLeg(sessionState, routeProgress)
        this.currentLegIndex = routeProgress.legIndex
        return if (isValidDeparture(checkedSessionState, routeProgress)) {
            sendToHandler(checkedSessionState, routeProgress, location)
        } else {
            checkedSessionState
        }
    }

    fun reset() {
        currentLegIndex = INITIAL_LEG_INDEX
    }

    private fun checkResetForNewLeg(
        sessionState: SessionState,
        routeProgress: MetricsRouteProgress
    ): SessionState {
        if (shouldResetDepartureDate(routeProgress)) {
            sessionState.startTimestamp = null
        }
        return sessionState
    }

    private fun shouldResetDepartureDate(routeProgress: MetricsRouteProgress): Boolean =
        currentLegIndex != routeProgress.legIndex

    private fun isValidDeparture(
        sessionState: SessionState,
        routeProgress: MetricsRouteProgress
    ): Boolean =
        sessionState.startTimestamp == null && routeProgress.distanceTraveled > 0

    private fun sendToHandler(
        sessionState: SessionState,
        routeProgress: MetricsRouteProgress,
        location: MetricsLocation
    ): SessionState {
        sessionState.startTimestamp = Date()
        departEventHandler.send(sessionState, routeProgress, location)
        return sessionState
    }
}
