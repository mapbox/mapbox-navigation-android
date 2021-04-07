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
import com.mapbox.geojson.Geometry
import com.mapbox.geojson.LineString
import com.mapbox.geojson.utils.PolylineUtils
import com.mapbox.navigation.base.speed.model.SpeedLimit
import com.mapbox.navigation.base.trip.model.RouteLegProgress
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.model.RouteProgressState
import com.mapbox.navigation.base.trip.model.RouteStepProgress
import com.mapbox.navigation.base.trip.model.roadobject.RoadObject
import com.mapbox.navigation.base.trip.model.roadobject.RoadObjectGeometry
import com.mapbox.navigation.base.trip.model.roadobject.UpcomingRoadObject
import com.mapbox.navigation.core.navigator.GeometryType.ENTRANCE_POINT
import com.mapbox.navigation.core.navigator.GeometryType.EXIT_POINT
import com.mapbox.navigation.core.navigator.GeometryType.POINT_OR_LINE
import com.mapbox.navigation.core.navigator.MappedRoadObject.EntranceAndExit
import com.mapbox.navigation.core.navigator.MappedRoadObject.SingleObject
import com.mapbox.navigation.core.trip.model.roadobject.border.CountryBorderCrossing
import com.mapbox.navigation.core.trip.model.roadobject.border.CountryBorderCrossingAdminInfo
import com.mapbox.navigation.core.trip.model.roadobject.border.CountryBorderCrossingInfo
import com.mapbox.navigation.core.trip.model.roadobject.bridge.BridgeEntrance
import com.mapbox.navigation.core.trip.model.roadobject.bridge.BridgeExit
import com.mapbox.navigation.core.trip.model.roadobject.custom.Custom
import com.mapbox.navigation.core.trip.model.roadobject.incident.Incident
import com.mapbox.navigation.core.trip.model.roadobject.incident.IncidentCongestion
import com.mapbox.navigation.core.trip.model.roadobject.incident.IncidentImpact
import com.mapbox.navigation.core.trip.model.roadobject.incident.IncidentInfo
import com.mapbox.navigation.core.trip.model.roadobject.incident.IncidentType
import com.mapbox.navigation.core.trip.model.roadobject.restrictedarea.RestrictedAreaEntrance
import com.mapbox.navigation.core.trip.model.roadobject.restrictedarea.RestrictedAreaExit
import com.mapbox.navigation.core.trip.model.roadobject.reststop.RestStop
import com.mapbox.navigation.core.trip.model.roadobject.reststop.RestStopType
import com.mapbox.navigation.core.trip.model.roadobject.tollcollection.TollCollection
import com.mapbox.navigation.core.trip.model.roadobject.tollcollection.TollCollectionType.TOLL_BOOTH
import com.mapbox.navigation.core.trip.model.roadobject.tollcollection.TollCollectionType.TOLL_GANTRY
import com.mapbox.navigation.core.trip.model.roadobject.tunnel.TunnelEntrance
import com.mapbox.navigation.core.trip.model.roadobject.tunnel.TunnelExit
import com.mapbox.navigation.core.trip.model.roadobject.tunnel.TunnelInfo
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
import com.mapbox.navigator.RoadObjectMetadata
import com.mapbox.navigator.RoadObjectType
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

private val SUPPORTED_ROAD_OBJECTS = arrayOf(
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
    info?.toTunnelInfo()

internal fun getTollCollectionType(info: TollCollectionInfo?): Int? = info?.toTollCollectionType()

internal fun getRestStopType(info: ServiceAreaInfo?): Int? = info?.toRestStopType()

internal fun getRoadObject(
    metadata: RoadObjectMetadata,
    geometry: RoadObjectGeometry
): RoadObject {
    return when (metadata.type) {
        RoadObjectType.INCIDENT -> {
            buildIncident(geometry, metadata.incident!!.toIncidentInfo())
        }
        RoadObjectType.TOLL_COLLECTION_POINT ->
            buildTollCollection(
                geometry,
                metadata.tollCollectionInfo!!.toTollCollectionType()
            )
        RoadObjectType.BORDER_CROSSING ->
            buildBorderCrossing(
                geometry,
                metadata.borderCrossingInfo!!.from.toBorderCrossingAdminInfo(),
                metadata.borderCrossingInfo!!.to.toBorderCrossingAdminInfo()
            )
        RoadObjectType.TUNNEL_ENTRANCE ->
            buildTunnelEntrance(geometry, metadata.tunnelInfo!!.toTunnelInfo())
        RoadObjectType.TUNNEL_EXIT ->
            buildTunnelExit(geometry, metadata.tunnelInfo!!.toTunnelInfo())
        RoadObjectType.RESTRICTED_AREA_ENTRANCE ->
            buildRestrictedAreaEntrance(geometry)
        RoadObjectType.RESTRICTED_AREA_EXIT ->
            buildRestrictedAreaExit(geometry)
        RoadObjectType.SERVICE_AREA ->
            buildRestStop(geometry, metadata.serviceAreaInfo!!.toRestStopType())

        RoadObjectType.BRIDGE_ENTRANCE -> buildBridgeEntrance(geometry)
        RoadObjectType.BRIDGE_EXIT -> buildBridgeExit(geometry)
        RoadObjectType.CUSTOM -> buildCustom(geometry)
        else -> throw IllegalArgumentException("unsupported type: ${metadata.type}")
    }
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
                        if (stale) {
                            routeProgressBuilder.currentState(RouteProgressState.LOCATION_STALE)
                        } else {
                            routeProgressBuilder.currentState(it)
                        }

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

        routeProgressBuilder.upcomingRoadObjects(upcomingRouteAlerts.toUpcomingRoadObjects())

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
        RouteState.UNCERTAIN -> RouteProgressState.ROUTE_UNCERTAIN
    }
}

private fun RouteInfo?.toRouteInitInfo(): RouteInitInfo? {
    return if (this != null) {
        RouteInitInfo(
            alerts
                .filter { SUPPORTED_ROAD_OBJECTS.contains(it.type) }
                .map {
                    mutableListOf<RoadObject>().apply {
                        when (val result = it.toRoadObject()) {
                            is SingleObject -> {
                                add(result.roadObject)
                            }
                            is EntranceAndExit -> {
                                add(result.entrance)
                                add(result.exit)
                            }
                        }
                    }
                }
                .flatten()
        )
    } else null
}

private fun List<com.mapbox.navigator.UpcomingRouteAlert>.toUpcomingRoadObjects():
    List<UpcomingRoadObject> {
    return this
        .filter { SUPPORTED_ROAD_OBJECTS.contains(it.alert.type) }
        .map { upcomingAlert ->
            mutableListOf<UpcomingRoadObject>().apply {
                when (val result = upcomingAlert.alert.toRoadObject()) {
                    is SingleObject -> {
                        add(
                            UpcomingRoadObject.Builder(
                                result.roadObject,
                                upcomingAlert.distanceToStart
                            ).build()
                        )
                    }
                    is EntranceAndExit -> {
                        add(
                            UpcomingRoadObject.Builder(
                                result.entrance,
                                upcomingAlert.distanceToStart
                            ).build()
                        )
                        add(
                            UpcomingRoadObject.Builder(
                                result.exit,
                                upcomingAlert.distanceToStart + result.entranceExitDistance
                            ).build()
                        )
                    }
                }
            }
        }
        .flatten()
}

private fun com.mapbox.navigator.RouteAlert.toRoadObject(): MappedRoadObject {
    return when (type) {
        // RouteAlert with type TUNNEL_ENTRANCE contains coordinates of tunnel's start and end,
        // so map to TunnelEntrance + TunnelExit
        RouteAlertType.TUNNEL_ENTRANCE -> EntranceAndExit(
            entrance = buildTunnelEntrance(
                getObjectGeometry(ENTRANCE_POINT),
                tunnelInfo!!.toTunnelInfo(),
                distance
            ),
            exit = buildTunnelExit(
                getObjectGeometry(EXIT_POINT),
                tunnelInfo!!.toTunnelInfo(),
                length?.let { it + distance } ?: distance
            ),
            entranceExitDistance = length ?: 0.0
        )
        RouteAlertType.BORDER_CROSSING -> SingleObject(
            buildBorderCrossing(
                getObjectGeometry(),
                borderCrossingInfo!!.from.toBorderCrossingAdminInfo(),
                borderCrossingInfo!!.to.toBorderCrossingAdminInfo(),
                distance
            )
        )
        RouteAlertType.TOLL_COLLECTION_POINT -> SingleObject(
            buildTollCollection(
                getObjectGeometry(),
                tollCollectionInfo!!.toTollCollectionType(),
                distance,
            )
        )
        RouteAlertType.SERVICE_AREA -> SingleObject(
            buildRestStop(getObjectGeometry(), serviceAreaInfo!!.toRestStopType(), distance)
        )
        // RouteAlert with type RESTRICTED_AREA contains coordinates of area's start and end,
        // so map to RestrictedAreaEntrance + RestrictedAreaExit
        RouteAlertType.RESTRICTED_AREA -> EntranceAndExit(
            entrance = buildRestrictedAreaEntrance(
                getObjectGeometry(ENTRANCE_POINT),
                distance
            ),
            exit = buildRestrictedAreaExit(
                getObjectGeometry(EXIT_POINT),
                length?.let { it + distance } ?: distance
            ),
            entranceExitDistance = length ?: 0.0
        )
        RouteAlertType.INCIDENT -> SingleObject(
            buildIncident(getObjectGeometry(), incidentInfo!!.toIncidentInfo(), distance)
        )
        else -> throw IllegalArgumentException("not supported type: $type")
    }
}

private fun com.mapbox.navigator.RouteAlert.getObjectGeometry(
    geometryType: GeometryType = POINT_OR_LINE
): RoadObjectGeometry {
    val shape = when (geometryType) {
        POINT_OR_LINE -> {
            if (length != null) {
                LineString.fromLngLats(listOf(beginCoordinate, endCoordinate))
            } else {
                beginCoordinate
            }
        }
        ENTRANCE_POINT -> beginCoordinate
        EXIT_POINT -> endCoordinate
    }

    return RoadObjectGeometry.Builder(
        length,
        shape,
        beginGeometryIndex,
        endGeometryIndex,
    ).build()
}

private fun com.mapbox.navigator.TunnelInfo.toTunnelInfo() =
    TunnelInfo.Builder(name = this.name).build()

private fun AdminInfo.toBorderCrossingAdminInfo() =
    CountryBorderCrossingAdminInfo.Builder(
        code = iso_3166_1,
        codeAlpha3 = iso_3166_1_alpha3
    ).build()

private fun TollCollectionInfo.toTollCollectionType() =
    when (type) {
        TollCollectionType.TOLL_BOOTH -> TOLL_BOOTH
        TollCollectionType.TOLL_GANTRY -> TOLL_GANTRY
    }

private fun ServiceAreaInfo.toRestStopType() =
    when (type) {
        ServiceAreaType.REST_AREA -> RestStopType.REST_AREA
        ServiceAreaType.SERVICE_AREA -> RestStopType.SERVICE_AREA
    }

private fun com.mapbox.navigator.IncidentInfo.toIncidentInfo(): IncidentInfo =
    IncidentInfo.Builder(id)
        .type(type.toIncidentType())
        .impact(impact.toIncidentImpact())
        .congestion(congestion?.toIncidentCongestion())
        .isClosed(roadClosed)
        .creationTime(creationTime)
        .startTime(startTime)
        .endTime(endTime)
        .description(description)
        .subType(subType)
        .subTypeDescription(subTypeDescription)
        .alertcCodes(alertcCodes)
        .build()

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

@IncidentImpact.Impact
private fun com.mapbox.navigator.IncidentImpact.toIncidentImpact(): String =
    when (this) {
        com.mapbox.navigator.IncidentImpact.UNKNOWN -> IncidentImpact.UNKNOWN
        com.mapbox.navigator.IncidentImpact.CRITICAL -> IncidentImpact.CRITICAL
        com.mapbox.navigator.IncidentImpact.MAJOR -> IncidentImpact.MAJOR
        com.mapbox.navigator.IncidentImpact.MINOR -> IncidentImpact.MINOR
        com.mapbox.navigator.IncidentImpact.LOW -> IncidentImpact.LOW
    }

private fun buildTunnelEntrance(
    geometry: RoadObjectGeometry,
    tunnelInfo: TunnelInfo,
    distance: Double? = null,
) =
    TunnelEntrance.Builder(geometry, tunnelInfo)
        .distanceFromStartOfRoute(distance)
        .build()

private fun buildTunnelExit(
    geometry: RoadObjectGeometry,
    tunnelInfo: TunnelInfo,
    distance: Double? = null,
) =
    TunnelExit.Builder(geometry, tunnelInfo)
        .distanceFromStartOfRoute(distance)
        .build()

private fun buildBorderCrossing(
    geometry: RoadObjectGeometry,
    from: CountryBorderCrossingAdminInfo,
    to: CountryBorderCrossingAdminInfo,
    distance: Double? = null,
) = CountryBorderCrossing.Builder(geometry, CountryBorderCrossingInfo.Builder(from, to).build())
    .distanceFromStartOfRoute(distance)
    .build()

private fun buildTollCollection(
    geometry: RoadObjectGeometry,
    type: Int,
    distance: Double? = null,
) =
    TollCollection.Builder(geometry, type)
        .distanceFromStartOfRoute(distance)
        .build()

private fun buildRestStop(
    geometry: RoadObjectGeometry,
    type: Int,
    distance: Double? = null,
) =
    RestStop.Builder(geometry, type)
        .distanceFromStartOfRoute(distance)
        .build()

private fun buildRestrictedAreaEntrance(
    geometry: RoadObjectGeometry,
    distance: Double? = null
) =
    RestrictedAreaEntrance.Builder(geometry)
        .distanceFromStartOfRoute(distance)
        .build()

private fun buildRestrictedAreaExit(geometry: RoadObjectGeometry, distance: Double? = null) =
    RestrictedAreaExit.Builder(geometry)
        .distanceFromStartOfRoute(distance)
        .build()

private fun buildIncident(
    geometry: RoadObjectGeometry,
    info: IncidentInfo,
    distance: Double? = null,
) = Incident.Builder(geometry, info)
    .distanceFromStartOfRoute(distance)
    .build()

private fun buildBridgeEntrance(
    geometry: RoadObjectGeometry,
    distance: Double? = null
) = BridgeEntrance.Builder(geometry)
    .distanceFromStartOfRoute(distance)
    .build()

private fun buildBridgeExit(
    geometry: RoadObjectGeometry,
    distance: Double? = null
) = BridgeExit.Builder(geometry)
    .distanceFromStartOfRoute(distance)
    .build()

private fun buildCustom(
    geometry: RoadObjectGeometry,
    distance: Double? = null
) = Custom.Builder(geometry)
    .distanceFromStartOfRoute(distance)
    .build()

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

private sealed class MappedRoadObject {
    internal data class SingleObject(val roadObject: RoadObject) : MappedRoadObject()
    internal data class EntranceAndExit(
        val entrance: RoadObject,
        val exit: RoadObject,
        val entranceExitDistance: Double
    ) : MappedRoadObject()
}

private enum class GeometryType {
    // Used to split line-like object (Tunnel, RestrictedArea) into a pair of entrance-exit objects.
    // Object will be an entrance Point with extra data (length, geometry indices).
    ENTRANCE_POINT,

    // Used to split line-like object (Tunnel, RestrictedArea) into a pair of entrance-exit objects.
    // Object will be an exit Point with extra data (length, geometry indices).
    EXIT_POINT,

    // Used to map object to a Point or a LineString depending on the length.
    // If length is null, we will get a Point without extra data (length, geometry indices).
    POINT_OR_LINE,
}
