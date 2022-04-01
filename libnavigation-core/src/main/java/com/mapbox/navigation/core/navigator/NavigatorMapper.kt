@file:JvmName("NavigatorMapper")

package com.mapbox.navigation.core.navigator

import android.location.Location
import com.mapbox.api.directions.v5.models.BannerComponents
import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.api.directions.v5.models.BannerText
import com.mapbox.api.directions.v5.models.BannerView
import com.mapbox.api.directions.v5.models.LegStep
import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.api.directions.v5.models.VoiceInstructions
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.internal.extensions.toMapboxShield
import com.mapbox.navigation.base.internal.factory.RoadObjectFactory
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
import com.mapbox.navigator.BannerComponent
import com.mapbox.navigator.BannerInstruction
import com.mapbox.navigator.BannerSection
import com.mapbox.navigator.NavigationStatus
import com.mapbox.navigator.Navigator
import com.mapbox.navigator.RoadObjectType
import com.mapbox.navigator.RouteInfo
import com.mapbox.navigator.RouteState
import com.mapbox.navigator.SpeedLimitSign
import com.mapbox.navigator.SpeedLimitUnit
import com.mapbox.navigator.VoiceInstruction

private val SUPPORTED_ROAD_OBJECTS = arrayOf(
    RoadObjectType.INCIDENT,
    RoadObjectType.TOLL_COLLECTION_POINT,
    RoadObjectType.BORDER_CROSSING,
    RoadObjectType.TUNNEL,
    RoadObjectType.RESTRICTED_AREA,
    RoadObjectType.SERVICE_AREA,
    RoadObjectType.BRIDGE,
    RoadObjectType.CUSTOM,
    RoadObjectType.RAILWAY_CROSSING,
)

private const val ONE_INDEX = 1
private const val ONE_SECOND_IN_MILLISECONDS = 1000.0

internal fun getRouteInitInfo(routeInfo: RouteInfo?) = routeInfo.toRouteInitInfo()

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
): RouteProgress? {
    return status.getRouteProgress(
        route,
        remainingWaypoints,
        bannerInstructions,
        instructionIndex,
        lastVoiceInstruction,
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
            routeLegProgressUpcomingStep
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
            upcomingRouteAlerts.toUpcomingRoadObjects(),
            stale,
            locatedAlternativeRouteId,
        )
    }
    return null
}

internal fun BannerInstruction.mapToDirectionsApi(): BannerInstructions {
    return BannerInstructions.builder()
        .distanceAlongGeometry(this.remainingStepDistance.toDouble())
        .primary(this.primary.mapToDirectionsApi())
        .secondary(this.secondary?.mapToDirectionsApi())
        .sub(this.sub?.mapToDirectionsApi())
        .view(this.view?.mapViewToDirectionsApi())
        .build()
}

private fun BannerSection.mapToDirectionsApi(): BannerText {
    return BannerText.builder()
        .components(this.components?.mapToDirectionsApi())
        .degrees(this.degrees?.toDouble())
        .drivingSide(this.drivingSide)
        .modifier(this.modifier)
        .text(this.text)
        .type(this.type)
        .build()
}

private fun BannerSection.mapViewToDirectionsApi(): BannerView {
    return BannerView.builder()
        .components(components?.mapToDirectionsApi())
        .text(text)
        .type(type)
        .modifier(modifier)
        .build()
}

private fun MutableList<BannerComponent>.mapToDirectionsApi(): MutableList<BannerComponents> {
    val components = mutableListOf<BannerComponents>()
    this.forEach {
        components.add(
            BannerComponents.builder()
                .abbreviation(it.abbr)
                .abbreviationPriority(it.abbrPriority)
                .active(it.active)
                .directions(it.directions)
                .imageBaseUrl(it.imageBaseUrl)
                .imageUrl(it.imageURL)
                .text(it.text)
                .type(it.type)
                .subType(it.subType?.name?.lowercase())
                .mapboxShield(it.shield?.toMapboxShield())
                .build()
        )
    }
    return components
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

private fun RouteInfo?.toRouteInitInfo(): RouteInitInfo? {
    return this?.let {
        RouteInitInfo(alerts.toUpcomingRoadObjects())
    }
}

private fun List<com.mapbox.navigator.UpcomingRouteAlert>.toUpcomingRoadObjects():
    List<UpcomingRoadObject> {
    return this
        .filter { SUPPORTED_ROAD_OBJECTS.contains(it.roadObject.type) }
        .map {
            RoadObjectFactory.buildUpcomingRoadObject(
                RoadObjectFactory.buildRoadObject(it.roadObject),
                it.distanceToStart,
                null
            )
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
        navigationStatus.isFallback
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
