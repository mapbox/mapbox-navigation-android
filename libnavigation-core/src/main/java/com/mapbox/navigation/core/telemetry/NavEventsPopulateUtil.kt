package com.mapbox.navigation.core.telemetry

import com.mapbox.geojson.Point
import com.mapbox.navigation.core.telemetry.events.AppMetadata
import com.mapbox.navigation.core.telemetry.events.FeedbackEvent
import com.mapbox.navigation.core.telemetry.events.MetricsDirectionsRoute
import com.mapbox.navigation.core.telemetry.events.MetricsRouteProgress
import com.mapbox.navigation.core.telemetry.events.NavigationEvent
import com.mapbox.navigation.utils.internal.logD

/**
 * Populate base Navigation Event
 *
 * @param sdkIdentifier sdk identifier
 * @param originalRoute original route (optional)
 * @param routeProgress route progress (optional)
 * @param lastLocation last location
 * @param locationEngineNameExternal location engine name
 * @param isSimulation whether the location is being simulated
 * @param percentTimeInPortrait [ApplicationLifecycleMonitor.obtainPortraitPercentage]
 * @param percentTimeInForeground [ApplicationLifecycleMonitor.obtainForegroundPercentage]
 * @param navigatorSessionIdentifier nav session id (identifier of instance of MapboxNavigation)
 * @param driverModeId id of driver mode (FreeDrive or ActiveGuidance)
 * @param driverMode one of [FeedbackEvent.DriverMode]
 * @param driverModeStartTime driver mode start time.
 * Use [TelemetryUtils.generateCreateDateFormatted]
 * @param rerouteCount reroute count
 * @param distanceTraveled accumulated for the session
 * @param eventVersion events version [MapboxNavigationTelemetry.EVENT_VERSION]
 * @param appMetadata use [MapboxNavigationTelemetry.createAppMetadata]
 */
internal fun NavigationEvent.populate(
    sdkIdentifier: String,
    originalRoute: MetricsDirectionsRoute,
    routeProgress: MetricsRouteProgress,
    lastLocation: Point?,
    locationEngineNameExternal: String?,
    isSimulation: Boolean,
    percentTimeInPortrait: Int?,
    percentTimeInForeground: Int?,
    navigatorSessionIdentifier: String?,
    driverModeId: String?,
    @FeedbackEvent.DriverMode driverMode: String?,
    driverModeStartTime: String?,
    rerouteCount: Int?,
    distanceTraveled: Int,
    eventVersion: Int,
    appMetadata: AppMetadata?,
) {
    logD("populateNavigationEvent", "MapboxNavigationTelemetry")

    this.sdkIdentifier = sdkIdentifier

    stepIndex = routeProgress.stepIndex

    distanceRemaining = routeProgress.distanceRemaining
    durationRemaining = routeProgress.durationRemaining
    distanceCompleted = distanceTraveled

    geometry = routeProgress.directionsRouteGeometry
    profile = routeProgress.directionsRouteProfile
    requestIdentifier = routeProgress.directionsRouteRequestIdentifier
    stepCount = routeProgress.directionsRouteStepCount
    legIndex = routeProgress.directionsRouteIndex
    legCount = routeProgress.legCount

    absoluteDistanceToDestination =
        obtainAbsoluteDistance(lastLocation, routeProgress.directionsRouteDestination)
    estimatedDistance = routeProgress.directionsRouteDistance
    estimatedDuration = routeProgress.directionsRouteDuration
    totalStepCount = routeProgress.directionsRouteStepCount

    originalStepCount = originalRoute.stepCount
    originalEstimatedDistance = originalRoute.distance
    originalEstimatedDuration = originalRoute.duration
    originalRequestIdentifier = originalRoute.requestIdentifier
    originalGeometry = originalRoute.geometry

    locationEngine = locationEngineNameExternal
    tripIdentifier = navObtainUniversalTelemetryTripId()
    lat = lastLocation?.latitude() ?: 0.0
    lng = lastLocation?.longitude() ?: 0.0
    this.simulation = isSimulation
    this.percentTimeInPortrait = percentTimeInPortrait ?: 100
    this.percentTimeInForeground = percentTimeInForeground ?: 100

    this.navigatorSessionIdentifier = navigatorSessionIdentifier

    this.sessionIdentifier = driverModeId
    this.startTimestamp = driverModeStartTime
    this.driverMode = driverMode
    rerouteCount?.let { this.rerouteCount = it }

    this.eventVersion = eventVersion
    this.appMetadata = appMetadata
}
