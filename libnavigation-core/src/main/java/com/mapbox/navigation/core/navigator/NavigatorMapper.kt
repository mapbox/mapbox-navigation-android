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
import com.mapbox.geojson.utils.PolylineUtils
import com.mapbox.navigation.base.internal.factory.RoadObjectInstanceFactory
import com.mapbox.navigation.base.speed.model.SpeedLimit
import com.mapbox.navigation.base.trip.model.RouteLegProgress
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.model.RouteProgressState
import com.mapbox.navigation.base.trip.model.RouteStepProgress
import com.mapbox.navigation.base.trip.model.roadobject.UpcomingRoadObject
import com.mapbox.navigation.core.trip.session.MapMatcherResult
import com.mapbox.navigation.navigator.internal.MapboxNativeNavigatorImpl
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
private const val FIRST_BANNER_INSTRUCTION = 0

internal fun getRouteInitInfo(routeInfo: RouteInfo?) = routeInfo.toRouteInitInfo()

/**
 * Builds [RouteProgress] object based on [NavigationStatus] returned by [Navigator]
 */
internal fun getRouteProgressFrom(
    directionsRoute: DirectionsRoute?,
    status: NavigationStatus,
    remainingWaypoints: Int
): RouteProgress? {
    return status.getRouteProgress(directionsRoute, remainingWaypoints)
}

internal fun NavigationStatus.getTripStatusFrom(
    route: DirectionsRoute?,
): TripStatus =
    TripStatus(
        route,
        this
    )

private fun NavigationStatus.getRouteProgress(
    route: DirectionsRoute?,
    remainingWaypoints: Int
): RouteProgress? {
    if (routeState == RouteState.INVALID) {
        return null
    }
    route?.let {
        val upcomingStepIndex = stepIndex + ONE_INDEX

        val routeProgressBuilder = RouteProgress.Builder(route)
        val legProgressBuilder = RouteLegProgress.Builder()
        val stepProgressBuilder = RouteStepProgress.Builder()

        ifNonNull(route.legs(), activeGuidanceInfo) { legs, activeGuidanceInfo ->
            var currentLeg: RouteLeg? = null
            if (legIndex < legs.size) {
                currentLeg = legs[legIndex]
                legProgressBuilder.legIndex(legIndex)
                legProgressBuilder.routeLeg(currentLeg)

                val distanceTraveled = activeGuidanceInfo.legProgress.distanceTraveled
                legProgressBuilder.distanceTraveled(distanceTraveled.toFloat())
                legProgressBuilder.fractionTraveled(
                    activeGuidanceInfo.legProgress.fractionTraveled.toFloat()
                )

                val legDistanceRemaining = activeGuidanceInfo.legProgress.remainingDistance
                val legDurationRemaining =
                    activeGuidanceInfo.legProgress.remainingDuration /
                        ONE_SECOND_IN_MILLISECONDS
                legProgressBuilder.distanceRemaining(legDistanceRemaining.toFloat())
                legProgressBuilder.durationRemaining(legDurationRemaining)

                val routeDistanceRemaining = activeGuidanceInfo.routeProgress.remainingDistance
                val routeDurationRemaining =
                    activeGuidanceInfo.routeProgress.remainingDuration /
                        ONE_SECOND_IN_MILLISECONDS
                routeProgressBuilder.distanceRemaining(routeDistanceRemaining.toFloat())
                routeProgressBuilder.durationRemaining(routeDurationRemaining)

                val routeDistanceTraveled = activeGuidanceInfo.routeProgress.distanceTraveled
                routeProgressBuilder.distanceTraveled(routeDistanceTraveled.toFloat())
                routeProgressBuilder.fractionTraveled(
                    activeGuidanceInfo.routeProgress.fractionTraveled.toFloat()
                )

                routeProgressBuilder.remainingWaypoints(remainingWaypoints)
            }

            ifNonNull(currentLeg?.steps()) { steps ->
                val currentStep: LegStep?
                if (stepIndex < steps.size) {
                    currentStep = steps[stepIndex]
                    stepProgressBuilder.stepIndex(stepIndex)
                    stepProgressBuilder.step(currentStep)
                    stepProgressBuilder.intersectionIndex(intersectionIndex)

                    val stepGeometry = currentStep.geometry()
                    stepGeometry?.let {
                        stepProgressBuilder.stepPoints(
                            PolylineUtils.decode(
                                stepGeometry, /* todo add core dependency PRECISION_6*/
                                6
                            )
                        )
                    }

                    val distanceTraveled = activeGuidanceInfo.stepProgress.distanceTraveled
                    stepProgressBuilder.distanceTraveled(distanceTraveled.toFloat())
                    stepProgressBuilder.fractionTraveled(
                        activeGuidanceInfo.stepProgress.fractionTraveled.toFloat()
                    )

                    routeState.convertState().let {
                        routeProgressBuilder.currentState(it)

                        var bannerInstructions =
                            bannerInstruction?.mapToDirectionsApi(currentStep)
                        if (it == RouteProgressState.INITIALIZED) {
                            bannerInstructions =
                                MapboxNativeNavigatorImpl.getBannerInstruction(
                                    FIRST_BANNER_INSTRUCTION
                                )
                                    ?.mapToDirectionsApi(currentStep)
                        }
                        routeProgressBuilder.bannerInstructions(bannerInstructions)
                    }
                }

                if (upcomingStepIndex < steps.size) {
                    val upcomingStep = steps[upcomingStepIndex]
                    legProgressBuilder.upcomingStep(upcomingStep)

                    val stepGeometry = upcomingStep.geometry()
                    stepGeometry?.let {
                        routeProgressBuilder.upcomingStepPoints(
                            PolylineUtils.decode(
                                stepGeometry, /* todo add core dependency PRECISION_6*/
                                6
                            )
                        )
                    }
                }

                val stepDistanceRemaining = activeGuidanceInfo.stepProgress.remainingDistance
                val stepDurationRemaining =
                    activeGuidanceInfo.stepProgress.remainingDuration /
                        ONE_SECOND_IN_MILLISECONDS

                stepProgressBuilder.distanceRemaining(stepDistanceRemaining.toFloat())
                stepProgressBuilder.durationRemaining(stepDurationRemaining)
            }
        }

        legProgressBuilder.currentStepProgress(stepProgressBuilder.build())

        routeProgressBuilder.currentLegProgress(legProgressBuilder.build())

        routeProgressBuilder.inTunnel(inTunnel)

        routeProgressBuilder.voiceInstructions(voiceInstruction?.mapToDirectionsApi())

        routeProgressBuilder.upcomingRoadObjects(upcomingRouteAlerts.toUpcomingRoadObjects())

        routeProgressBuilder.stale(stale)

        return routeProgressBuilder.build()
    }
    return null
}

private fun BannerInstruction.mapToDirectionsApi(currentStep: LegStep): BannerInstructions {
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

private fun RouteState.convertState(): RouteProgressState {
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
            RoadObjectInstanceFactory.buildUpcomingRoadObject(
                RoadObjectInstanceFactory.buildRoadObject(it.roadObject),
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
