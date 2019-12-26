package com.mapbox.services.android.navigation.v5.internal.navigation.metrics

import android.location.Location
import com.mapbox.android.telemetry.TelemetryUtils
import com.mapbox.services.android.navigation.v5.internal.navigation.routeprogress.MetricsRouteProgress
import com.mapbox.services.android.navigation.v5.utils.DistanceFormatter
import com.mapbox.services.android.navigation.v5.utils.extensions.ifNonNull
import java.util.Date

internal object NavigationEventFactory {

    internal const val EVENT_VERSION = 7

    fun buildNavigationDepartEvent(
        phoneState: PhoneState,
        sessionState: SessionState,
        metricProgress: MetricsRouteProgress,
        location: Location,
        sdkIdentifier: String
    ): NavigationDepartEvent = NavigationDepartEvent(phoneState).apply {
        setEvent(sessionState, metricProgress, location, sdkIdentifier, this)
    }

    fun buildNavigationCancelEvent(
        phoneState: PhoneState,
        sessionState: SessionState,
        metricProgress: MetricsRouteProgress,
        location: Location,
        sdkIdentifier: String
    ): NavigationCancelEvent = NavigationCancelEvent(phoneState).apply {
        setEvent(sessionState, metricProgress, location, sdkIdentifier, this)
        arrivalTimestamp = obtainArriveTimestamp(sessionState)
    }

    fun buildNavigationArriveEvent(
        phoneState: PhoneState,
        sessionState: SessionState,
        metricProgress: MetricsRouteProgress,
        location: Location,
        sdkIdentifier: String
    ): NavigationArriveEvent = NavigationArriveEvent(phoneState).apply {
        setEvent(sessionState, metricProgress, location, sdkIdentifier, this)
    }

    fun buildNavigationRerouteEvent(
        phoneState: PhoneState,
        sessionState: SessionState,
        metricProgress: MetricsRouteProgress,
        location: Location,
        sdkIdentifier: String,
        rerouteEvent: RerouteEvent
    ): NavigationRerouteEvent =
        NavigationRerouteEvent(phoneState, rerouteEvent, metricProgress).apply {
            setEvent(sessionState, metricProgress, location, sdkIdentifier, this)
            locationsBefore = convertToArray(sessionState.beforeEventLocations)
            locationsAfter = convertToArray(sessionState.afterEventLocations)
            secondsSinceLastReroute = sessionState.secondsSinceLastReroute
        }

    fun buildNavigationFeedbackEvent(
        phoneState: PhoneState,
        sessionState: SessionState,
        metricProgress: MetricsRouteProgress,
        location: Location,
        sdkIdentifier: String,
        feedbackDescription: String?,
        type: String?,
        feedbackScreenshot: String?,
        feedbackSource: String?
    ): NavigationFeedbackEvent =
        NavigationFeedbackEvent(phoneState, metricProgress).apply {
            setEvent(sessionState, metricProgress, location, sdkIdentifier, this)
            locationsBefore = convertToArray(sessionState.beforeEventLocations)
            locationsAfter = convertToArray(sessionState.afterEventLocations)
            description = feedbackDescription
            feedbackType = type
            screenshot = feedbackScreenshot
            source = feedbackSource
        }

    private fun setEvent(
        sessionState: SessionState,
        metricProgress: MetricsRouteProgress,
        location: Location,
        sdkIdentifier: String,
        navigationEvent: NavigationEvent
    ) {
        navigationEvent.absoluteDistanceToDestination =
            DistanceFormatter.calculateAbsoluteDistance(location, metricProgress)
        navigationEvent.distanceCompleted =
            (sessionState.eventRouteDistanceCompleted + metricProgress.distanceTraveled).toInt()
        navigationEvent.distanceRemaining = metricProgress.distanceRemaining
        navigationEvent.durationRemaining = metricProgress.durationRemaining
        navigationEvent.profile = metricProgress.directionsRouteProfile
        navigationEvent.legIndex = metricProgress.legIndex
        navigationEvent.legCount = metricProgress.legCount
        navigationEvent.stepIndex = metricProgress.stepIndex
        navigationEvent.stepCount = metricProgress.stepCount
        navigationEvent.estimatedDistance = metricProgress.directionsRouteDistance
        navigationEvent.estimatedDuration = metricProgress.directionsRouteDuration
        navigationEvent.startTimestamp = obtainStartTimestamp(sessionState)
        navigationEvent.eventVersion = EVENT_VERSION
        navigationEvent.sdkIdentifier = sdkIdentifier
        navigationEvent.sessionIdentifier = sessionState.sessionIdentifier
        navigationEvent.lat = location.latitude
        navigationEvent.lng = location.longitude
        navigationEvent.geometry = sessionState.currentGeometry()
        navigationEvent.simulation = sessionState.mockLocation
        navigationEvent.locationEngine = sessionState.locationEngineName
        navigationEvent.tripIdentifier = sessionState.tripIdentifier
        navigationEvent.rerouteCount = sessionState.rerouteCount
        navigationEvent.originalRequestIdentifier = sessionState.originalRequestIdentifier
        navigationEvent.requestIdentifier = sessionState.requestIdentifier
        navigationEvent.originalGeometry = sessionState.originalGeometry()
        navigationEvent.originalEstimatedDistance = sessionState.originalDistance()
        navigationEvent.originalEstimatedDuration = sessionState.originalDuration()
        navigationEvent.originalStepCount = sessionState.originalStepCount()
        navigationEvent.percentTimeInForeground = sessionState.percentInForeground
        navigationEvent.percentTimeInPortrait = sessionState.percentInPortrait
        navigationEvent.totalStepCount = sessionState.currentStepCount()
    }

    private fun obtainStartTimestamp(sessionState: SessionState): String {
        val date: Date = ifNonNull(sessionState.startTimestamp) {
            sessionState.startTimestamp
        } ?: Date()
        return TelemetryUtils.generateCreateDateFormatted(date)
    }

    private fun obtainArriveTimestamp(sessionState: SessionState): String {
        val date: Date = ifNonNull(sessionState.arrivalTimestamp) {
            sessionState.arrivalTimestamp
        } ?: Date()
        return TelemetryUtils.generateCreateDateFormatted(date)
    }

    private fun convertToArray(locationList: List<Location>?): Array<Location> =
        ifNonNull(locationList) {
            it.toTypedArray()
        } ?: arrayOf()
}
