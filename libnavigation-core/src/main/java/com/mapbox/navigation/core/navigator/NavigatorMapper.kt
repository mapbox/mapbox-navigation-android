@file:JvmName("NavigatorMapper")

package com.mapbox.navigation.core.navigator

import android.location.Location
import com.mapbox.api.directions.v5.models.BannerComponents
import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.api.directions.v5.models.BannerText
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.Incident
import com.mapbox.api.directions.v5.models.LegStep
import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.api.directions.v5.models.VoiceInstructions
import com.mapbox.geojson.Geometry
import com.mapbox.geojson.utils.PolylineUtils
import com.mapbox.navigation.base.speed.model.SpeedLimit
import com.mapbox.navigation.base.trip.model.RouteLegProgress
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.model.RouteProgressState
import com.mapbox.navigation.base.trip.model.RouteStepProgress
import com.mapbox.navigation.base.trip.model.alert.RouteAlert
import com.mapbox.navigation.base.trip.model.alert.RouteAlertGeometry
import com.mapbox.navigation.base.trip.model.alert.UpcomingRouteAlert
import com.mapbox.navigation.core.trip.model.alert.CountryBorderCrossingAdminInfo
import com.mapbox.navigation.core.trip.model.alert.CountryBorderCrossingAlert
import com.mapbox.navigation.core.trip.model.alert.CountryBorderCrossingInfo
import com.mapbox.navigation.core.trip.model.alert.IncidentAlert
import com.mapbox.navigation.core.trip.model.alert.IncidentCongestion
import com.mapbox.navigation.core.trip.model.alert.IncidentImpact.CRITICAL
import com.mapbox.navigation.core.trip.model.alert.IncidentImpact.LOW
import com.mapbox.navigation.core.trip.model.alert.IncidentImpact.MAJOR
import com.mapbox.navigation.core.trip.model.alert.IncidentImpact.MINOR
import com.mapbox.navigation.core.trip.model.alert.IncidentImpact.UNKNOWN
import com.mapbox.navigation.core.trip.model.alert.IncidentInfo
import com.mapbox.navigation.core.trip.model.alert.IncidentType.ACCIDENT
import com.mapbox.navigation.core.trip.model.alert.IncidentType.CONGESTION
import com.mapbox.navigation.core.trip.model.alert.IncidentType.CONSTRUCTION
import com.mapbox.navigation.core.trip.model.alert.IncidentType.DISABLED_VEHICLE
import com.mapbox.navigation.core.trip.model.alert.IncidentType.LANE_RESTRICTION
import com.mapbox.navigation.core.trip.model.alert.IncidentType.MASS_TRANSIT
import com.mapbox.navigation.core.trip.model.alert.IncidentType.MISCELLANEOUS
import com.mapbox.navigation.core.trip.model.alert.IncidentType.OTHER_NEWS
import com.mapbox.navigation.core.trip.model.alert.IncidentType.PLANNED_EVENT
import com.mapbox.navigation.core.trip.model.alert.IncidentType.ROAD_CLOSURE
import com.mapbox.navigation.core.trip.model.alert.IncidentType.ROAD_HAZARD
import com.mapbox.navigation.core.trip.model.alert.IncidentType.WEATHER
import com.mapbox.navigation.core.trip.model.alert.RestStopAlert
import com.mapbox.navigation.core.trip.model.alert.RestStopType
import com.mapbox.navigation.core.trip.model.alert.RestrictedAreaAlert
import com.mapbox.navigation.core.trip.model.alert.TollCollectionAlert
import com.mapbox.navigation.core.trip.model.alert.TollCollectionType.TollBooth
import com.mapbox.navigation.core.trip.model.alert.TollCollectionType.TollGantry
import com.mapbox.navigation.core.trip.model.alert.TunnelEntranceAlert
import com.mapbox.navigation.core.trip.model.alert.TunnelInfo
import com.mapbox.navigation.core.trip.session.MapMatcherResult
import com.mapbox.navigation.navigator.internal.MapboxNativeNavigatorImpl
import com.mapbox.navigation.navigator.internal.TripStatus
import com.mapbox.navigation.utils.internal.ifNonNull
import com.mapbox.navigator.AdminInfo
import com.mapbox.navigator.BannerComponent
import com.mapbox.navigator.BannerInstruction
import com.mapbox.navigator.BannerSection
import com.mapbox.navigator.BorderCrossingInfo
import com.mapbox.navigator.NavigationStatus
import com.mapbox.navigator.Navigator
import com.mapbox.navigator.RouteAlertType
import com.mapbox.navigator.RouteInfo
import com.mapbox.navigator.RouteState
import com.mapbox.navigator.ServiceAreaInfo
import com.mapbox.navigator.ServiceAreaType
import com.mapbox.navigator.SpeedLimitSign
import com.mapbox.navigator.SpeedLimitUnit
import com.mapbox.navigator.TollCollectionInfo
import com.mapbox.navigator.TollCollectionType
import com.mapbox.navigator.VoiceInstruction

private val SUPPORTED_ROUTE_ALERTS = arrayOf(
    RouteAlertType.TUNNEL_ENTRANCE,
    RouteAlertType.BORDER_CROSSING,
    RouteAlertType.TOLL_COLLECTION_POINT,
    RouteAlertType.SERVICE_AREA,
    RouteAlertType.RESTRICTED_AREA,
    RouteAlertType.INCIDENT
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
    routeBufferGeoJson: Geometry?,
    status: NavigationStatus,
    remainingWaypoints: Int
): RouteProgress? {
    return status.getRouteProgress(directionsRoute, routeBufferGeoJson, remainingWaypoints)
}

internal fun getIncidentInfo(info: com.mapbox.navigator.IncidentInfo?) = info?.toIncidentInfo()

internal fun getBorderCrossingInfo(info: BorderCrossingInfo?): CountryBorderCrossingInfo? {
    return info?.let {
        CountryBorderCrossingInfo.Builder(
            it.from.toBorderCrossingAdminInfo(),
            it.to.toBorderCrossingAdminInfo()
        ).build()
    }
}

internal fun getTunnelInfo(info: com.mapbox.navigator.TunnelInfo?): TunnelInfo? =
    info.toTunnelInfo()

internal fun getTollCollectionType(info: TollCollectionInfo?): Int? = info.toTollCollectionType()

internal fun getRestStopType(info: ServiceAreaInfo?): Int? = info.toRestStopType()

private fun NavigationStatus.getRouteProgress(
    route: DirectionsRoute?,
    routeBufferGeoJson: Geometry?,
    remainingWaypoints: Int
): RouteProgress? {
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
                        if (it == RouteProgressState.ROUTE_INITIALIZED) {
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
        routeProgressBuilder.routeGeometryWithBuffer(routeBufferGeoJson)

        routeProgressBuilder.voiceInstructions(voiceInstruction?.mapToDirectionsApi())

        routeProgressBuilder.upcomingRouteAlerts(upcomingRouteAlerts.toUpcomingRouteAlerts())

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
        RouteState.INVALID -> RouteProgressState.ROUTE_INVALID
        RouteState.INITIALIZED -> RouteProgressState.ROUTE_INITIALIZED
        RouteState.TRACKING -> RouteProgressState.LOCATION_TRACKING
        RouteState.COMPLETE -> RouteProgressState.ROUTE_COMPLETE
        RouteState.OFF_ROUTE -> RouteProgressState.OFF_ROUTE
        RouteState.STALE -> RouteProgressState.LOCATION_STALE
        RouteState.UNCERTAIN -> RouteProgressState.ROUTE_UNCERTAIN
    }
}

private fun RouteInfo?.toRouteInitInfo(): RouteInitInfo? {
    return if (this != null) {
        RouteInitInfo(
            alerts
                .filter { SUPPORTED_ROUTE_ALERTS.contains(it.type) }
                .map { it.toRouteAlert() }
        )
    } else null
}

private fun List<com.mapbox.navigator.UpcomingRouteAlert>.toUpcomingRouteAlerts():
    List<UpcomingRouteAlert> {
        return this
            .filter { SUPPORTED_ROUTE_ALERTS.contains(it.alert.type) }
            .map {
                UpcomingRouteAlert.Builder(it.alert.toRouteAlert(), it.distanceToStart).build()
            }
    }

private fun com.mapbox.navigator.RouteAlert.toRouteAlert(): RouteAlert {
    val alert = this
    return when (alert.type) {
        RouteAlertType.TUNNEL_ENTRANCE -> {
            TunnelEntranceAlert.Builder(
                alert.beginCoordinate,
                alert.distance
            )
                .alertGeometry(alert.getAlertGeometry())
                .info(alert.tunnelInfo?.toTunnelInfo())
                .build()
        }
        RouteAlertType.BORDER_CROSSING -> {
            CountryBorderCrossingAlert.Builder(
                alert.beginCoordinate,
                alert.distance
            )
                .alertGeometry(alert.getAlertGeometry())
                .countryBorderCrossingInfo(
                    CountryBorderCrossingInfo.Builder(
                        alert.borderCrossingInfo?.from.toBorderCrossingAdminInfo(),
                        alert.borderCrossingInfo?.to.toBorderCrossingAdminInfo()
                    ).build()
                )
                .build()
        }
        RouteAlertType.TOLL_COLLECTION_POINT -> {
            TollCollectionAlert.Builder(
                alert.beginCoordinate,
                alert.distance
            )
                .alertGeometry(alert.getAlertGeometry())
                .apply {
                    val type = alert.tollCollectionInfo.toTollCollectionType()
                    if (type != null) {
                        tollCollectionType(type)
                    }
                }
                .build()
        }
        RouteAlertType.SERVICE_AREA -> {
            RestStopAlert.Builder(
                alert.beginCoordinate,
                alert.distance
            )
                .alertGeometry(alert.getAlertGeometry())
                .apply {
                    val type = alert.serviceAreaInfo.toRestStopType()
                    if (type != null) {
                        restStopType(type)
                    }
                }
                .build()
        }
        RouteAlertType.RESTRICTED_AREA -> {
            RestrictedAreaAlert.Builder(
                alert.beginCoordinate,
                alert.distance
            )
                .alertGeometry(alert.getAlertGeometry())
                .build()
        }
        RouteAlertType.INCIDENT -> {
            IncidentAlert.Builder(
                alert.beginCoordinate,
                alert.distance
            )
                .info(alert.incidentInfo?.toIncidentInfo())
                .alertGeometry(alert.getAlertGeometry())
                .build()
        }
        else -> throw IllegalArgumentException("not supported type: ${alert.type}")
    }
}

private fun com.mapbox.navigator.RouteAlert.getAlertGeometry(): RouteAlertGeometry? = ifNonNull(
    this.length,
    this.beginCoordinate,
    this.beginGeometryIndex,
    this.endCoordinate,
    this.endGeometryIndex
) { length,
    beginCoordinate,
    beginGeometryIndex,
    endCoordinate,
    endGeometryIndex ->
    RouteAlertGeometry.Builder(
        length = length,
        startCoordinate = beginCoordinate,
        startGeometryIndex = beginGeometryIndex,
        endCoordinate = endCoordinate,
        endGeometryIndex = endGeometryIndex,
    ).build()
}

private fun com.mapbox.navigator.TunnelInfo?.toTunnelInfo() = ifNonNull(
    this?.name,
) { name ->
    TunnelInfo.Builder(
        name = name
    ).build()
}

private fun AdminInfo?.toBorderCrossingAdminInfo() = ifNonNull(
    this?.iso_3166_1,
    this?.iso_3166_1_alpha3,
) { countryCode, countryCodeAlpha3 ->
    CountryBorderCrossingAdminInfo.Builder(
        code = countryCode,
        codeAlpha3 = countryCodeAlpha3
    ).build()
}

private fun TollCollectionInfo?.toTollCollectionType() = ifNonNull(
    this?.type
) { type ->
    when (type) {
        TollCollectionType.TOLL_BOOTH -> TollBooth
        TollCollectionType.TOLL_GANTRY -> TollGantry
    }
}

private fun ServiceAreaInfo?.toRestStopType() = ifNonNull(
    this?.type
) { type ->
    when (type) {
        ServiceAreaType.REST_AREA -> RestStopType.RestArea
        ServiceAreaType.SERVICE_AREA -> RestStopType.ServiceArea
    }
}

private fun com.mapbox.navigator.IncidentInfo.toIncidentInfo(): IncidentInfo? =
    ifNonNull(this) { info ->
        IncidentInfo.Builder(info.id)
            .type(info.type.toIncidentType())
            .impact(info.impact.toIncidentImpact())
            .congestion(info.congestion?.toIncidentCongestion())
            .isClosed(info.roadClosed)
            .creationTime(info.creationTime)
            .startTime(info.startTime)
            .endTime(info.endTime)
            .description(info.description)
            .subType(info.subType)
            .subTypeDescription(info.subTypeDescription)
            .alertcCodes(info.alertcCodes)
            .build()
    }

private fun com.mapbox.navigator.IncidentType.toIncidentType(): Int =
    when (this) {
        com.mapbox.navigator.IncidentType.ACCIDENT -> ACCIDENT
        com.mapbox.navigator.IncidentType.CONGESTION -> CONGESTION
        com.mapbox.navigator.IncidentType.CONSTRUCTION -> CONSTRUCTION
        com.mapbox.navigator.IncidentType.DISABLED_VEHICLE -> DISABLED_VEHICLE
        com.mapbox.navigator.IncidentType.LANE_RESTRICTION -> LANE_RESTRICTION
        com.mapbox.navigator.IncidentType.MASS_TRANSIT -> MASS_TRANSIT
        com.mapbox.navigator.IncidentType.MISCELLANEOUS -> MISCELLANEOUS
        com.mapbox.navigator.IncidentType.OTHER_NEWS -> OTHER_NEWS
        com.mapbox.navigator.IncidentType.PLANNED_EVENT -> PLANNED_EVENT
        com.mapbox.navigator.IncidentType.ROAD_CLOSURE -> ROAD_CLOSURE
        com.mapbox.navigator.IncidentType.ROAD_HAZARD -> ROAD_HAZARD
        com.mapbox.navigator.IncidentType.WEATHER -> WEATHER
    }

private fun com.mapbox.navigator.IncidentCongestion?.toIncidentCongestion() =
    ifNonNull(this) { congestion ->
        IncidentCongestion.Builder().value(congestion.value).build()
    }

@Incident.IncidentType
private fun com.mapbox.navigator.IncidentImpact.toIncidentImpact(): String =
    when (this) {
        com.mapbox.navigator.IncidentImpact.UNKNOWN -> UNKNOWN
        com.mapbox.navigator.IncidentImpact.CRITICAL -> CRITICAL
        com.mapbox.navigator.IncidentImpact.MAJOR -> MAJOR
        com.mapbox.navigator.IncidentImpact.MINOR -> MINOR
        com.mapbox.navigator.IncidentImpact.LOW -> LOW
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
        navigationStatus.map_matcher_output.isTeleport,
        navigationStatus.prepareSpeedLimit(),
        navigationStatus.map_matcher_output.matches.firstOrNull()?.proba ?: 0f
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
