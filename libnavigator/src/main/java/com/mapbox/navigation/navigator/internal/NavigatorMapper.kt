package com.mapbox.navigation.navigator.internal

import com.mapbox.api.directions.v5.models.BannerComponents
import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.api.directions.v5.models.BannerText
import com.mapbox.api.directions.v5.models.DirectionsRoute
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
import com.mapbox.navigation.base.trip.model.alert.RestStopAlert
import com.mapbox.navigation.base.trip.model.alert.RestStopType
import com.mapbox.navigation.base.trip.model.alert.RestrictedAreaAlert
import com.mapbox.navigation.base.trip.model.alert.RouteAlert
import com.mapbox.navigation.base.trip.model.alert.RouteAlertGeometry
import com.mapbox.navigation.base.trip.model.alert.TollCollectionAlert
import com.mapbox.navigation.base.trip.model.alert.TollCollectionType
import com.mapbox.navigation.base.trip.model.alert.TunnelEntranceAlert
import com.mapbox.navigation.base.trip.model.alert.UpcomingRouteAlert
import com.mapbox.navigation.utils.internal.ifNonNull
import com.mapbox.navigator.BannerComponent
import com.mapbox.navigator.BannerInstruction
import com.mapbox.navigator.BannerSection
import com.mapbox.navigator.NavigationStatus
import com.mapbox.navigator.Navigator
import com.mapbox.navigator.PassiveManeuver
import com.mapbox.navigator.PassiveManeuverAdminInfo
import com.mapbox.navigator.PassiveManeuverServiceAreaInfo
import com.mapbox.navigator.PassiveManeuverServiceAreaType
import com.mapbox.navigator.PassiveManeuverTollCollectionInfo
import com.mapbox.navigator.PassiveManeuverTollCollectionType
import com.mapbox.navigator.PassiveManeuverType
import com.mapbox.navigator.RouteInfo
import com.mapbox.navigator.RouteState
import com.mapbox.navigator.UpcomingPassiveManeuver
import com.mapbox.navigator.VoiceInstruction

private val SUPPORTED_PASSIVE_MANEUVERS = arrayOf(
    PassiveManeuverType.KTUNNEL_ENTRANCE,
    PassiveManeuverType.KBORDER_CROSSING,
    PassiveManeuverType.KTOLL_COLLECTION_POINT,
    PassiveManeuverType.KSERVICE_AREA,
    PassiveManeuverType.KRESTRICTED_AREA
)

internal class NavigatorMapper {

    fun getRouteInitInfo(routeInfo: RouteInfo?) = routeInfo.toRouteInitInfo()

    /**
     * Builds [RouteProgress] object based on [NavigationStatus] returned by [Navigator]
     */
    fun getRouteProgress(
        directionsRoute: DirectionsRoute?,
        routeBufferGeoJson: Geometry?,
        status: NavigationStatus
    ): RouteProgress? {
        return status.getRouteProgress(directionsRoute, routeBufferGeoJson)
    }

    private fun NavigationStatus.getRouteProgress(
        route: DirectionsRoute?,
        routeBufferGeoJson: Geometry?
    ): RouteProgress? {
        route?.let {
            val upcomingStepIndex = stepIndex + ONE_INDEX

            val routeProgressBuilder = RouteProgress.Builder(route)
            val legProgressBuilder = RouteLegProgress.Builder()
            val stepProgressBuilder = RouteStepProgress.Builder()

            ifNonNull(route.legs()) { legs ->
                var currentLeg: RouteLeg? = null
                if (legIndex < legs.size) {
                    currentLeg = legs[legIndex]
                    legProgressBuilder.legIndex(legIndex)
                    legProgressBuilder.routeLeg(currentLeg)

                    // todo mapbox java issue - leg distance is nullable
                    val distanceTraveled =
                        (currentLeg.distance()?.toFloat() ?: 0f) - remainingLegDistance
                    legProgressBuilder.distanceTraveled(distanceTraveled)
                    legProgressBuilder.fractionTraveled(
                        distanceTraveled / (currentLeg.distance()?.toFloat() ?: 0f)
                    )

                    var routeDistanceRemaining = remainingLegDistance
                    var routeDurationRemaining = remainingLegDuration / ONE_SECOND_IN_MILLISECONDS
                    if (legs.size >= TWO_LEGS) {
                        for (i in legIndex + ONE_INDEX until legs.size) {
                            routeDistanceRemaining += legs[i].distance()?.toFloat() ?: 0f
                            routeDurationRemaining += legs[i].duration() ?: 0.0
                        }
                    }
                    routeProgressBuilder.distanceRemaining(routeDistanceRemaining)
                    routeProgressBuilder.durationRemaining(routeDurationRemaining)

                    var routeDistance = 0f
                    for (leg in legs) {
                        routeDistance += leg.distance()?.toFloat() ?: 0f
                    }
                    val routeDistanceTraveled = routeDistance - routeDistanceRemaining
                    routeProgressBuilder.distanceTraveled(routeDistanceTraveled)
                    routeProgressBuilder.fractionTraveled(routeDistanceTraveled / routeDistance)

                    routeProgressBuilder.remainingWaypoints(legs.size - (legIndex + 1))
                }

                ifNonNull(currentLeg?.steps()) { steps ->
                    val currentStep: LegStep?
                    if (stepIndex < steps.size) {
                        currentStep = steps[stepIndex]
                        stepProgressBuilder.stepIndex(stepIndex)
                        stepProgressBuilder.step(currentStep)

                        currentStep?.distance()
                        val stepGeometry = currentStep.geometry()
                        stepGeometry?.let {
                            stepProgressBuilder.stepPoints(
                                PolylineUtils.decode(
                                    stepGeometry, /* todo add core dependency PRECISION_6*/
                                    6
                                )
                            )
                        }

                        val distanceTraveled =
                            currentStep.distance().toFloat() - remainingStepDistance
                        stepProgressBuilder.distanceTraveled(distanceTraveled)
                        stepProgressBuilder.fractionTraveled(
                            distanceTraveled / currentStep.distance().toFloat()
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
                }
            }

            stepProgressBuilder.distanceRemaining(remainingStepDistance)
            stepProgressBuilder.durationRemaining(
                remainingStepDuration / ONE_SECOND_IN_MILLISECONDS
            )

            legProgressBuilder.currentStepProgress(stepProgressBuilder.build())
            legProgressBuilder.distanceRemaining(remainingLegDistance)
            legProgressBuilder.durationRemaining(remainingLegDuration / ONE_SECOND_IN_MILLISECONDS)

            routeProgressBuilder.currentLegProgress(legProgressBuilder.build())

            routeProgressBuilder.inTunnel(inTunnel)
            routeProgressBuilder.routeGeometryWithBuffer(routeBufferGeoJson)

            routeProgressBuilder.voiceInstructions(voiceInstruction?.mapToDirectionsApi())

            routeProgressBuilder.upcomingRouteAlerts(
                upcomingPassiveManeuvers.toUpcomingRouteAlerts()
            )

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
                passiveManeuvers
                    .filter { SUPPORTED_PASSIVE_MANEUVERS.contains(it.type) }
                    .map { it.toRouteAlert() }
            )
        } else null
    }

    private fun List<UpcomingPassiveManeuver>.toUpcomingRouteAlerts(): List<UpcomingRouteAlert> {
        return this
            .filter { SUPPORTED_PASSIVE_MANEUVERS.contains(it.maneuver.type) }
            .map {
                UpcomingRouteAlert.Builder(it.maneuver.toRouteAlert(), it.distanceToStart).build()
            }
    }

    private fun PassiveManeuver.toRouteAlert(): RouteAlert {
        val maneuver = this
        return when (maneuver.type) {
            PassiveManeuverType.KTUNNEL_ENTRANCE -> {
                TunnelEntranceAlert.Builder(
                    maneuver.beginCoordinate,
                    maneuver.distance
                )
                    .alertGeometry(maneuver.getAlertGeometry())
                    .build()
            }
            PassiveManeuverType.KBORDER_CROSSING -> {
                CountryBorderCrossingAlert.Builder(
                    maneuver.beginCoordinate,
                    maneuver.distance
                )
                    .alertGeometry(maneuver.getAlertGeometry())
                    .from(maneuver.borderCrossingInfo?.from.toCountryBorderCrossingAdminInfo())
                    .to(maneuver.borderCrossingInfo?.to.toCountryBorderCrossingAdminInfo())
                    .build()
            }
            PassiveManeuverType.KTOLL_COLLECTION_POINT -> {
                TollCollectionAlert.Builder(
                    maneuver.beginCoordinate,
                    maneuver.distance
                )
                    .alertGeometry(maneuver.getAlertGeometry())
                    .apply {
                        val type = maneuver.tollCollectionInfo.toTollCollectionType()
                        if (type != null) {
                            tollCollectionType(type)
                        }
                    }
                    .build()
            }
            PassiveManeuverType.KSERVICE_AREA -> {
                RestStopAlert.Builder(
                    maneuver.beginCoordinate,
                    maneuver.distance
                )
                    .alertGeometry(maneuver.getAlertGeometry())
                    .apply {
                        val type = maneuver.serviceAreaInfo.toRestStopType()
                        if (type != null) {
                            restStopType(type)
                        }
                    }
                    .build()
            }
            PassiveManeuverType.KRESTRICTED_AREA -> {
                RestrictedAreaAlert.Builder(
                    maneuver.beginCoordinate,
                    maneuver.distance
                )
                    .alertGeometry(maneuver.getAlertGeometry())
                    .build()
            }
            else -> throw IllegalArgumentException("not supported type: ${maneuver.type}")
        }
    }

    private fun PassiveManeuver.getAlertGeometry(): RouteAlertGeometry? = ifNonNull(
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
        private const val TWO_LEGS: Short = 2
    }

    private fun PassiveManeuverAdminInfo?.toCountryBorderCrossingAdminInfo() = ifNonNull(
        this?.iso_3166_1,
        this?.iso_3166_1_alpha3,
    ) { countryCode, CountryCodeAlpha3 ->
        CountryBorderCrossingAdminInfo.Builder(
            code = countryCode,
            codeAlpha3 = CountryCodeAlpha3
        ).build()
    }

    private fun PassiveManeuverTollCollectionInfo?.toTollCollectionType() = ifNonNull(
        this?.type
    ) { type ->
        when (type) {
            PassiveManeuverTollCollectionType.KTOLL_BOOTH -> TollCollectionType.TollBooth
            PassiveManeuverTollCollectionType.KTOLL_GANTRY -> TollCollectionType.TollGantry
        }
    }

    private fun PassiveManeuverServiceAreaInfo?.toRestStopType() = ifNonNull(
        this?.type
    ) { type ->
        when (type) {
            PassiveManeuverServiceAreaType.KREST_AREA -> RestStopType.RestArea
            PassiveManeuverServiceAreaType.KSERVICE_AREA -> RestStopType.ServiceArea
        }
    }
}
