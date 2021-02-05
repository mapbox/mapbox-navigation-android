package com.mapbox.navigation.navigator.internal

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
import com.mapbox.navigation.base.trip.model.RouteLegProgress
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.model.RouteProgressState
import com.mapbox.navigation.base.trip.model.RouteStepProgress
import com.mapbox.navigation.base.trip.model.alert.CountryBorderCrossingAdminInfo
import com.mapbox.navigation.base.trip.model.alert.CountryBorderCrossingAlert
import com.mapbox.navigation.base.trip.model.alert.IncidentAlert
import com.mapbox.navigation.base.trip.model.alert.IncidentCongestion
import com.mapbox.navigation.base.trip.model.alert.IncidentImpact
import com.mapbox.navigation.base.trip.model.alert.IncidentInfo
import com.mapbox.navigation.base.trip.model.alert.IncidentType
import com.mapbox.navigation.base.trip.model.alert.RestStopAlert
import com.mapbox.navigation.base.trip.model.alert.RestStopType
import com.mapbox.navigation.base.trip.model.alert.RestrictedAreaAlert
import com.mapbox.navigation.base.trip.model.alert.RouteAlert
import com.mapbox.navigation.base.trip.model.alert.RouteAlertGeometry
import com.mapbox.navigation.base.trip.model.alert.TollCollectionAlert
import com.mapbox.navigation.base.trip.model.alert.TollCollectionType
import com.mapbox.navigation.base.trip.model.alert.TunnelEntranceAlert
import com.mapbox.navigation.base.trip.model.alert.TunnelInfo
import com.mapbox.navigation.base.trip.model.alert.UpcomingRouteAlert
import com.mapbox.navigation.utils.internal.ifNonNull
import com.mapbox.navigator.AdminInfo
import com.mapbox.navigator.BannerComponent
import com.mapbox.navigator.BannerInstruction
import com.mapbox.navigator.BannerSection
import com.mapbox.navigator.NavigationStatus
import com.mapbox.navigator.Navigator
import com.mapbox.navigator.RouteAlertType
import com.mapbox.navigator.RouteInfo
import com.mapbox.navigator.RouteState
import com.mapbox.navigator.ServiceAreaInfo
import com.mapbox.navigator.ServiceAreaType
import com.mapbox.navigator.TollCollectionInfo
import com.mapbox.navigator.VoiceInstruction

private val SUPPORTED_ROUTE_ALERTS = arrayOf(
    RouteAlertType.TUNNEL_ENTRANCE,
    RouteAlertType.BORDER_CROSSING,
    RouteAlertType.TOLL_COLLECTION_POINT,
    RouteAlertType.SERVICE_AREA,
    RouteAlertType.RESTRICTED_AREA,
    RouteAlertType.INCIDENT
)

class NavigatorMapper internal constructor() {

    fun getRouteInitInfo(routeInfo: RouteInfo?) = routeInfo.toRouteInitInfo()

    /**
     * Builds [RouteProgress] object based on [NavigationStatus] returned by [Navigator]
     */
    fun getRouteProgress(
        directionsRoute: DirectionsRoute?,
        routeBufferGeoJson: Geometry?,
        status: NavigationStatus,
        remainingWaypoints: Int
    ): RouteProgress? {
        return status.getRouteProgress(directionsRoute, routeBufferGeoJson, remainingWaypoints)
    }

    fun toIncidentInfo(info: com.mapbox.navigator.IncidentInfo?): IncidentInfo? =
        info?.run {
            IncidentInfo.Builder(id)
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
                    .from(alert.borderCrossingInfo?.from.toBorderCrossingAdminInfo())
                    .to(alert.borderCrossingInfo?.to.toBorderCrossingAdminInfo())
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
                    .info(toIncidentInfo(alert.incidentInfo))
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

    companion object {

        private const val ONE_INDEX = 1
        private const val ONE_SECOND_IN_MILLISECONDS = 1000.0
        private const val FIRST_BANNER_INSTRUCTION = 0
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
            com.mapbox.navigator.TollCollectionType.TOLL_BOOTH -> TollCollectionType.TollBooth
            com.mapbox.navigator.TollCollectionType.TOLL_GANTRY -> TollCollectionType.TollGantry
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

    private fun com.mapbox.navigator.IncidentType.toIncidentType(): Int =
        when (this) {
            com.mapbox.navigator.IncidentType.ACCIDENT -> IncidentType.ACCIDENT
            com.mapbox.navigator.IncidentType.CONGESTION -> IncidentType.CONGESTION
            com.mapbox.navigator.IncidentType.CONSTRUCTION -> IncidentType.CONSTRUCTION
            com.mapbox.navigator.IncidentType.DISABLED_VEHICLE -> IncidentType.DISABLED_VEHICLE
            com.mapbox.navigator.IncidentType.LANE_RESTRICTION -> IncidentType.LANE_RESTRICTION
            com.mapbox.navigator.IncidentType.MASS_TRANSIT -> IncidentType.MASS_TRANSIT
            com.mapbox.navigator.IncidentType.MISCELLANEOUS -> IncidentType.MISCELLANEOUS
            com.mapbox.navigator.IncidentType.OTHER_NEWS -> IncidentType.OTHER_NEWS
            com.mapbox.navigator.IncidentType.PLANNED_EVENT -> IncidentType.PLANNED_EVENT
            com.mapbox.navigator.IncidentType.ROAD_CLOSURE -> IncidentType.ROAD_CLOSURE
            com.mapbox.navigator.IncidentType.ROAD_HAZARD -> IncidentType.ROAD_HAZARD
            com.mapbox.navigator.IncidentType.WEATHER -> IncidentType.WEATHER
        }

    private fun com.mapbox.navigator.IncidentCongestion?.toIncidentCongestion() =
        ifNonNull(this) { congestion ->
            IncidentCongestion.Builder().value(congestion.value).build()
        }

    @Incident.IncidentType
    private fun com.mapbox.navigator.IncidentImpact.toIncidentImpact(): String =
        when (this) {
            com.mapbox.navigator.IncidentImpact.UNKNOWN -> IncidentImpact.UNKNOWN
            com.mapbox.navigator.IncidentImpact.CRITICAL -> IncidentImpact.CRITICAL
            com.mapbox.navigator.IncidentImpact.MAJOR -> IncidentImpact.MAJOR
            com.mapbox.navigator.IncidentImpact.MINOR -> IncidentImpact.MINOR
            com.mapbox.navigator.IncidentImpact.LOW -> IncidentImpact.LOW
        }
}
