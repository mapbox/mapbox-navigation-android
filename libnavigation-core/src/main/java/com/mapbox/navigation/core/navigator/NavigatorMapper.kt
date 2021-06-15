@file:JvmName("NavigatorMapper")

package com.mapbox.navigation.core.navigator

import android.location.Location
import com.mapbox.api.directions.v5.models.BannerComponents
import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.api.directions.v5.models.BannerText
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.LegStep
import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.api.directions.v5.models.VoiceInstructions
import com.mapbox.geojson.Point
import com.mapbox.geojson.utils.PolylineUtils
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.internal.factory.RoadObjectFactory
import com.mapbox.navigation.base.internal.factory.RouteLegProgressFactory.buildRouteLegProgressObject
import com.mapbox.navigation.base.internal.factory.RouteProgressFactory.buildRouteProgressObject
import com.mapbox.navigation.base.internal.factory.RouteStepProgressFactory.buildRouteStepProgressObject
import com.mapbox.navigation.base.speed.model.SpeedLimit
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.model.RouteProgressState
import com.mapbox.navigation.base.trip.model.roadobject.UpcomingRoadObject
import com.mapbox.navigation.core.trip.session.MapMatcherResult
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
)

private const val ONE_INDEX = 1
private const val ONE_SECOND_IN_MILLISECONDS = 1000.0

internal fun getRouteInitInfo(routeInfo: RouteInfo?) = routeInfo.toRouteInitInfo()

/**
 * Builds [RouteProgress] object based on [NavigationStatus] returned by [Navigator]
 */
internal fun getRouteProgressFrom(
    directionsRoute: DirectionsRoute?,
    status: NavigationStatus,
    remainingWaypoints: Int,
    bannerInstructions: BannerInstructions?,
    instructionIndex: Int?
): RouteProgress? {
    return status.getRouteProgress(
        directionsRoute,
        remainingWaypoints,
        bannerInstructions,
        instructionIndex
    )
}

internal fun NavigationStatus.getTripStatusFrom(
    route: DirectionsRoute?,
): TripStatus =
    TripStatus(
        route,
        this
    )

@OptIn(ExperimentalMapboxNavigationAPI::class)
private fun NavigationStatus.getRouteProgress(
    route: DirectionsRoute?,
    remainingWaypoints: Int,
    bannerInstructions: BannerInstructions?,
    instructionIndex: Int?
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
        var routeLegProgressDistanceRemaining: Float = 0f
        var routeLegProgressDurationRemaining: Double = 0.0
        var routeLegProgressFractionTraveled: Float = 0f
        var routeLegProgressUpcomingStep: LegStep? = null

        var routeProgressCurrentState: RouteProgressState = RouteProgressState.INITIALIZED
        var routeProgressUpcomingStepPoints: List<Point>? = null
        var routeProgressDistanceRemaining: Float = 0f
        var routeProgressDistanceTraveled: Float = 0f
        var routeProgressDurationRemaining: Double = 0.0
        var routeProgressFractionTraveled: Float = 0f

        ifNonNull(route.legs(), activeGuidanceInfo) { legs, activeGuidanceInfo ->
            if (legIndex < legs.size) {
                currentLeg = legs[legIndex]

                routeProgressDistanceTraveled =
                    activeGuidanceInfo.legProgress.distanceTraveled.toFloat()
                routeLegProgressFractionTraveled =
                    activeGuidanceInfo.legProgress.fractionTraveled.toFloat()

                routeLegProgressDistanceRemaining =
                    activeGuidanceInfo.legProgress.remainingDistance.toFloat()
                routeLegProgressDurationRemaining =
                    activeGuidanceInfo.legProgress.remainingDuration / ONE_SECOND_IN_MILLISECONDS

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
                        stepPoints = ifNonNull(legStep.geometry()) { geometry ->
                            PolylineUtils.decode(
                                geometry, /* todo add core dependency PRECISION_6*/
                                6
                            )
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

                    val stepGeometry = upcomingStep.geometry()
                    stepGeometry?.let {
                        routeProgressUpcomingStepPoints = PolylineUtils.decode(
                            stepGeometry, /* todo add core dependency PRECISION_6*/
                            6
                        )
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
            routeProgressDistanceTraveled,
            routeLegProgressDistanceRemaining,
            routeLegProgressDurationRemaining,
            routeLegProgressFractionTraveled,
            routeStepProgress,
            routeLegProgressUpcomingStep
        )

        return buildRouteProgressObject(
            route,
            bannerInstructions,
            voiceInstruction?.mapToDirectionsApi(),
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
            stale
        )
    }
    return null
}

internal fun BannerInstruction.mapToDirectionsApi(currentStep: LegStep): BannerInstructions {
    return BannerInstructions.builder()
        .distanceAlongGeometry(this.remainingStepDistance.toDouble())
        .primary(this.primary.mapToDirectionsApi())
        .secondary(this.secondary?.mapToDirectionsApi())
        .sub(this.sub?.mapToDirectionsApi())
        .view(currentStep.bannerInstructions()?.get(this.index)?.view())
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

private fun MutableList<BannerComponent>.mapToDirectionsApi(): MutableList<BannerComponents>? {
    val components = mutableListOf<BannerComponents>()
    this.forEach {
        components.add(
            BannerComponents.builder()
                .abbreviation(it.abbr)
                .abbreviationPriority(it.abbrPriority)
                .active(it.active)
                .directions(it.directions)
                .imageBaseUrl(it.imageBaseurl)
                .text(it.text)
                .type(it.type)
                .build()
        )
    }
    return components
}

private fun VoiceInstruction.mapToDirectionsApi(): VoiceInstructions? {
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

internal fun TripStatus.getMapMatcherResult(
    enhancedLocation: Location,
    keyPoints: List<Location>
): MapMatcherResult {
    return MapMatcherResult(
        enhancedLocation,
        keyPoints,
        navigationStatus.offRoadProba > 0.5,
        navigationStatus.offRoadProba,
        navigationStatus.mapMatcherOutput.isTeleport,
        navigationStatus.prepareSpeedLimit(),
        navigationStatus.mapMatcherOutput.matches.firstOrNull()?.proba ?: 0f
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
