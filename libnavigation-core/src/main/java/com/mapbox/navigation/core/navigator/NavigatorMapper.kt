@file:JvmName("NavigatorMapper")

package com.mapbox.navigation.core.navigator

import android.location.Location
import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.api.directions.v5.models.LegStep
import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.api.directions.v5.models.VoiceInstructions
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.internal.factory.RouteLegProgressFactory.buildRouteLegProgressObject
import com.mapbox.navigation.base.internal.factory.RouteProgressFactory.buildRouteProgressObject
import com.mapbox.navigation.base.internal.factory.RouteStepProgressFactory.buildRouteStepProgressObject
import com.mapbox.navigation.base.road.model.Road
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.speed.model.SpeedLimit
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.model.RouteProgressState
import com.mapbox.navigation.base.trip.model.roadobject.UpcomingRoadObject
import com.mapbox.navigation.base.utils.DecodeUtils.stepGeometryToPoints
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.navigator.internal.TripStatus
import com.mapbox.navigation.utils.internal.ifNonNull
import com.mapbox.navigation.utils.internal.logW
import com.mapbox.navigator.NavigationStatus
import com.mapbox.navigator.Navigator
import com.mapbox.navigator.RouteState
import com.mapbox.navigator.SpeedLimitSign
import com.mapbox.navigator.SpeedLimitUnit
import com.mapbox.navigator.VoiceInstruction

private const val ONE_INDEX = 1
private const val ONE_SECOND_IN_MILLISECONDS = 1000.0
private const val LOG_CATEGORY = "NavigatorMapper"

/**
 * Builds [RouteProgress] object based on [NavigationStatus] returned by [Navigator]
 */
internal fun getRouteProgressFrom(
    route: NavigationRoute?,
    status: NavigationStatus,
    remainingWaypoints: Int,
    bannerInstructions: BannerInstructions?,
    instructionIndex: Int?,
    lastVoiceInstruction: VoiceInstructions?,
    upcomingRoadObjects: List<UpcomingRoadObject>,
): RouteProgress? {
    return status.getRouteProgress(
        route,
        remainingWaypoints,
        bannerInstructions,
        instructionIndex,
        lastVoiceInstruction,
        upcomingRoadObjects,
    )
}

internal fun NavigationStatus.getTripStatusFrom(
    route: NavigationRoute?
): TripStatus =
    TripStatus(
        route,
        this
    )

@OptIn(ExperimentalMapboxNavigationAPI::class)
private fun NavigationStatus.getRouteProgress(
    route: NavigationRoute?,
    remainingWaypoints: Int,
    bannerInstructions: BannerInstructions?,
    instructionIndex: Int?,
    lastVoiceInstruction: VoiceInstructions?,
    upcomingRoadObjects: List<UpcomingRoadObject>,
): RouteProgress? {
    if (routeState == RouteState.INVALID) {
        return null
    }
    route?.let {
        val upcomingStepIndex = stepIndex + ONE_INDEX

        var currentLegStep: LegStep? = null
        var stepPoints: List<Point>? = null
        var stepDistanceRemaining = 0f
        var stepDistanceTraveled = 0f
        var stepFractionTraveled = 0f
        var stepDurationRemaining = 0.0

        var currentLeg: RouteLeg? = null
        var routeLegProgressDistanceRemaining = 0f
        var routeLegProgressDistanceTraveled = 0f
        var routeLegProgressDurationRemaining = 0.0
        var routeLegProgressFractionTraveled = 0f
        var routeLegProgressUpcomingStep: LegStep? = null

        var routeProgressCurrentState: RouteProgressState = RouteProgressState.INITIALIZED
        var routeProgressUpcomingStepPoints: List<Point>? = null
        var routeProgressDistanceRemaining = 0f
        var routeProgressDistanceTraveled = 0f
        var routeProgressDurationRemaining = 0.0
        var routeProgressFractionTraveled = 0f

        ifNonNull(route.directionsRoute.legs(), activeGuidanceInfo) { legs, activeGuidanceInfo ->
            if (legIndex < legs.size) {
                currentLeg = legs[legIndex]

                routeLegProgressDistanceTraveled =
                    activeGuidanceInfo.legProgress.distanceTraveled.toFloat()
                routeLegProgressFractionTraveled =
                    activeGuidanceInfo.legProgress.fractionTraveled.toFloat()
                routeLegProgressDistanceRemaining =
                    activeGuidanceInfo.legProgress.remainingDistance.toFloat()
                routeLegProgressDurationRemaining =
                    activeGuidanceInfo.legProgress.remainingDuration / ONE_SECOND_IN_MILLISECONDS

                routeProgressDistanceTraveled =
                    activeGuidanceInfo.routeProgress.distanceTraveled.toFloat()
                routeProgressDistanceRemaining =
                    activeGuidanceInfo.routeProgress.remainingDistance.toFloat()
                routeProgressDurationRemaining =
                    activeGuidanceInfo.routeProgress.remainingDuration / ONE_SECOND_IN_MILLISECONDS
                routeProgressFractionTraveled =
                    activeGuidanceInfo.routeProgress.fractionTraveled.toFloat()
            }

            ifNonNull(currentLeg?.steps()) { steps ->
                if (stepIndex < steps.size) {
                    currentLegStep = steps[stepIndex].also { legStep ->
                        stepPoints = legStep.geometry()?.let {
                            route.directionsRoute.stepGeometryToPoints(legStep)
                        }
                        routeProgressCurrentState = routeState.convertState()
                    }

                    stepDistanceTraveled =
                        activeGuidanceInfo.stepProgress.distanceTraveled.toFloat()
                    stepFractionTraveled =
                        activeGuidanceInfo.stepProgress.fractionTraveled.toFloat()
                }

                if (upcomingStepIndex < steps.size) {
                    val upcomingStep = steps[upcomingStepIndex]
                    routeLegProgressUpcomingStep = upcomingStep

                    upcomingStep.geometry()?.let {
                        routeProgressUpcomingStepPoints =
                            route.directionsRoute.stepGeometryToPoints(upcomingStep)
                    }
                }

                stepDistanceRemaining = activeGuidanceInfo.stepProgress.remainingDistance.toFloat()
                stepDurationRemaining =
                    activeGuidanceInfo.stepProgress.remainingDuration / ONE_SECOND_IN_MILLISECONDS
            }
        }

        val routeStepProgress = buildRouteStepProgressObject(
            stepIndex,
            intersectionIndex,
            instructionIndex,
            currentLegStep,
            stepPoints,
            stepDistanceRemaining,
            stepDistanceTraveled,
            stepFractionTraveled,
            stepDurationRemaining
        )

        val routeLegProgress = buildRouteLegProgressObject(
            legIndex,
            currentLeg,
            routeLegProgressDistanceTraveled,
            routeLegProgressDistanceRemaining,
            routeLegProgressDurationRemaining,
            routeLegProgressFractionTraveled,
            routeStepProgress,
            routeLegProgressUpcomingStep,
            shapeIndex
        )

        return buildRouteProgressObject(
            route,
            bannerInstructions,
            voiceInstruction?.mapToDirectionsApi() ?: lastVoiceInstruction,
            routeProgressCurrentState,
            routeLegProgress,
            routeProgressUpcomingStepPoints,
            inTunnel,
            routeProgressDistanceRemaining,
            routeProgressDistanceTraveled,
            routeProgressDurationRemaining,
            routeProgressFractionTraveled,
            remainingWaypoints,
            upcomingRoadObjects,
            stale,
            locatedAlternativeRouteId,
            geometryIndex,
        )
    }
    return null
}

internal fun NavigationStatus.getCurrentBannerInstructions(
    currentRoute: NavigationRoute?
): BannerInstructions? {
    return ifNonNull(currentRoute, bannerInstruction) { route, nativeBanner ->
        route.directionsRoute.legs()?.let { legs ->
            if (legs.size > 0) {
                val currentLeg = legs[legIndex]
                currentLeg.steps()?.let { steps ->
                    if (steps.size > 0) {
                        val currentStep = steps[stepIndex]
                        currentStep.bannerInstructions()?.let { banners ->
                            banners[nativeBanner.index]
                                .toBuilder()
                                .distanceAlongGeometry(
                                    nativeBanner.remainingStepDistance.toDouble()
                                )
                                .build()
                        }
                    } else {
                        logW("Steps cannot be null or empty", LOG_CATEGORY)
                        null
                    }
                }
            } else {
                logW("Legs cannot be null or empty", LOG_CATEGORY)
                null
            }
        }
    }
}

internal fun VoiceInstruction.mapToDirectionsApi(): VoiceInstructions? {
    return VoiceInstructions.builder()
        .announcement(this.announcement)
        .distanceAlongGeometry(this.remainingStepDistance.toDouble())
        .ssmlAnnouncement(this.ssmlAnnouncement)
        .build()
}

internal fun RouteState.convertState(): RouteProgressState {
    return when (this) {
        RouteState.INVALID ->
            throw IllegalArgumentException("invalid route progress state not supported")
        RouteState.INITIALIZED -> RouteProgressState.INITIALIZED
        RouteState.TRACKING -> RouteProgressState.TRACKING
        RouteState.COMPLETE -> RouteProgressState.COMPLETE
        RouteState.OFF_ROUTE -> RouteProgressState.OFF_ROUTE
        RouteState.UNCERTAIN -> RouteProgressState.UNCERTAIN
    }
}

@OptIn(ExperimentalMapboxNavigationAPI::class)
internal fun TripStatus.getLocationMatcherResult(
    enhancedLocation: Location,
    keyPoints: List<Location>,
    road: Road
): LocationMatcherResult {
    return LocationMatcherResult(
        enhancedLocation,
        keyPoints,
        navigationStatus.offRoadProba > 0.5,
        navigationStatus.offRoadProba,
        navigationStatus.mapMatcherOutput.isTeleport,
        navigationStatus.prepareSpeedLimit(),
        navigationStatus.mapMatcherOutput.matches.firstOrNull()?.proba ?: 0f,
        navigationStatus.layer,
        road,
        navigationStatus.isFallback,
        navigationStatus.inTunnel,
    )
}

internal fun NavigationStatus.prepareSpeedLimit(): SpeedLimit? {
    return ifNonNull(speedLimit) { limit ->
        val speedLimitUnit = when (limit.localeUnit) {
            SpeedLimitUnit.KILOMETRES_PER_HOUR ->
                com.mapbox.navigation.base.speed.model.SpeedLimitUnit.KILOMETRES_PER_HOUR
            else -> com.mapbox.navigation.base.speed.model.SpeedLimitUnit.MILES_PER_HOUR
        }
        val speedLimitSign = when (limit.localeSign) {
            SpeedLimitSign.MUTCD -> com.mapbox.navigation.base.speed.model.SpeedLimitSign.MUTCD
            else -> com.mapbox.navigation.base.speed.model.SpeedLimitSign.VIENNA
        }
        SpeedLimit(
            limit.speedKmph,
            speedLimitUnit,
            speedLimitSign
        )
    }
}
