package com.mapbox.navigation.core.telemetry

import com.mapbox.android.telemetry.TelemetryUtils
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.base.common.logger.model.Message
import com.mapbox.base.common.logger.model.Tag
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.telemetry.events.AppMetadata
import com.mapbox.navigation.core.telemetry.events.FeedbackEvent
import com.mapbox.navigation.core.telemetry.events.NavigationEvent
import com.mapbox.navigation.utils.internal.ifNonNull
import com.mapbox.navigation.utils.internal.logD

/**
 * Populate base Navigation Event
 *
 * @param sdkIdentifier sdk identifier
 * @param originalRoute original route (optional)
 * @param routeProgress route progress (optional)
 * @param lastLocation last location
 * @param locationEngineNameExternal location engine name
 * @param percentTimeInPortrait [ApplicationLifecycleMonitor.obtainPortraitPercentage]
 * @param percentTimeInForeground [ApplicationLifecycleMonitor.obtainForegroundPercentage]
 * @param navigatorSessionIdentifier nav session id (identifier of instance of MapboxNavigation)
 * @param driverModeId id of driver mode (FreeDrive or ActiveGuidance)
 * @param driverMode one of [FeedbackEvent.DriverMode]
 * @param driverModeStartTime driver mode start time.
 * Use [TelemetryUtils.generateCreateDateFormatted]
 * @param rerouteCount reroute count
 * @param eventVersion events version [MapboxNavigationTelemetry.EVENT_VERSION]
 * @param appMetadata use [MapboxNavigationTelemetry.createAppMetadata]
 */
internal fun NavigationEvent.populate(
    sdkIdentifier: String,
    originalRoute: DirectionsRoute?,
    routeProgress: RouteProgress?,
    lastLocation: Point?,
    locationEngineNameExternal: String?,
    percentTimeInPortrait: Int?,
    percentTimeInForeground: Int?,
    navigatorSessionIdentifier: String?,
    driverModeId: String?,
    @FeedbackEvent.DriverMode driverMode: String?,
    driverModeStartTime: String?,
    rerouteCount: Int?,
    eventVersion: Int,
    appMetadata: AppMetadata?,
) {
    logD(Tag("MbxNavigationTelemetry"), Message("populateNavigationEvent"))

    this.sdkIdentifier = sdkIdentifier

    ifNonNull(routeProgress) { routeProgressNonNull ->
        stepIndex = routeProgressNonNull.currentLegProgress?.currentStepProgress?.stepIndex ?: 0

        distanceRemaining = routeProgressNonNull.distanceRemaining.toInt()
        durationRemaining = routeProgressNonNull.durationRemaining.toInt()
        distanceCompleted = routeProgressNonNull.distanceTraveled.toInt()

        routeProgressNonNull.route.let {
            geometry = it.geometry()
            profile = it.routeOptions()?.profile()
            requestIdentifier = it.requestUuid()
            stepCount = obtainStepCount(it)
            legIndex = it.routeIndex()?.toInt() ?: 0
            legCount = it.legs()?.size ?: 0

            absoluteDistanceToDestination = obtainAbsoluteDistance(
                lastLocation,
                obtainRouteDestination(it)
            )
            estimatedDistance = it.distance().toInt()
            estimatedDuration = it.duration().toInt()
            totalStepCount = obtainStepCount(it)
        }
    }

    ifNonNull(originalRoute) { orininalRouteNonNull ->
        originalStepCount = obtainStepCount(orininalRouteNonNull)
        originalEstimatedDistance = orininalRouteNonNull.distance().toInt()
        originalEstimatedDuration = orininalRouteNonNull.duration().toInt()
        originalRequestIdentifier = orininalRouteNonNull.requestUuid()
        originalGeometry = orininalRouteNonNull.geometry()
    }

    locationEngine = locationEngineNameExternal
    tripIdentifier = navObtainUniversalTelemetryTripId()
    lat = lastLocation?.latitude() ?: 0.0
    lng = lastLocation?.longitude() ?: 0.0
    this.simulation = locationEngineNameExternal == MapboxNavigationTelemetry.MOCK_PROVIDER
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
