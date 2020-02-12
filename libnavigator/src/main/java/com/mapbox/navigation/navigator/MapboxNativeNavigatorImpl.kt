package com.mapbox.navigation.navigator

import android.location.Location
import com.mapbox.api.directions.v5.models.BannerComponents
import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.api.directions.v5.models.BannerText
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.LegStep
import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.api.directions.v5.models.VoiceInstructions
import com.mapbox.geojson.Geometry
import com.mapbox.geojson.Point
import com.mapbox.geojson.gson.GeometryGeoJson
import com.mapbox.geojson.utils.PolylineUtils
import com.mapbox.navigation.base.extensions.ifNonNull
import com.mapbox.navigation.base.trip.model.RouteLegProgress
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.model.RouteProgressState
import com.mapbox.navigation.base.trip.model.RouteStepProgress
import com.mapbox.navigator.BannerComponent
import com.mapbox.navigator.BannerInstruction
import com.mapbox.navigator.BannerSection
import com.mapbox.navigator.HttpInterface
import com.mapbox.navigator.NavigationStatus
import com.mapbox.navigator.Navigator
import com.mapbox.navigator.NavigatorConfig
import com.mapbox.navigator.RouteState
import com.mapbox.navigator.RouterParams
import com.mapbox.navigator.RouterResult
import com.mapbox.navigator.VoiceInstruction
import java.util.Date
import kotlin.math.roundToLong

object MapboxNativeNavigatorImpl : MapboxNativeNavigator {

    // Order matters! https://kotlinlang.org/docs/reference/classes.html#constructors
    init {
        System.loadLibrary("navigator-android")
    }

    private const val ONE_INDEX = 1
    private const val ONE_SECOND_IN_MILLISECONDS = 1000.0
    private const val FIRST_BANNER_INSTRUCTION = 0
    private const val GRID_SIZE = 0.0025f
    private const val BUFFER_DILATION: Short = 1
    private const val TWO_LEGS: Short = 2

    private val navigator: Navigator = Navigator()
    private var route: DirectionsRoute? = null
    private var routeBufferGeoJson: Geometry? = null

    // Route following

    override fun updateLocation(rawLocation: Location): Boolean =
        navigator.updateLocation(rawLocation.toFixLocation(Date()))

    override fun getStatus(date: Date): TripStatus {
        val status = navigator.getStatus(date)
        return TripStatus(
            status.location.toLocation(),
            status.key_points.map { it.toLocation() },
            status.getRouteProgress(),
            status.routeState == RouteState.OFFROUTE
        )
    }

    // Routing

    override fun setRoute(
        route: DirectionsRoute,
        routeIndex: Int,
        legIndex: Int
    ): NavigationStatus {
        this.route = route
        val result = navigator.setRoute(route.toJson(), routeIndex, legIndex)
        navigator.getRouteBufferGeoJson(GRID_SIZE, BUFFER_DILATION)?.also {
            routeBufferGeoJson = GeometryGeoJson.fromJson(it)
        }
        return result
    }

    override fun updateAnnotations(
        legAnnotationJson: String,
        routeIndex: Int,
        legIndex: Int
    ): Boolean = navigator.updateAnnotations(legAnnotationJson, routeIndex, legIndex)

    override fun getBannerInstruction(index: Int): BannerInstruction? =
        navigator.getBannerInstruction(index)

    override fun getRouteGeometryWithBuffer(gridSize: Float, bufferDilation: Short): String? =
        navigator.getRouteBufferGeoJson(gridSize, bufferDilation)

    override fun updateLegIndex(routeIndex: Int, legIndex: Int): NavigationStatus =
        navigator.changeRouteLeg(routeIndex, legIndex)

    // Free Drive

    override fun getElectronicHorizon(request: String): RouterResult =
        navigator.getElectronicHorizon(request)

    // Offline

    override fun cacheLastRoute() {
        navigator.cacheLastRoute()
    }

    override fun configureRouter(routerParams: RouterParams, httpClient: HttpInterface?): Long =
        navigator.configureRouter(routerParams, httpClient)

    override fun getRoute(url: String): RouterResult = navigator.getRoute(url)

    override fun unpackTiles(tarPath: String, destinationPath: String): Long =
        navigator.unpackTiles(tarPath, destinationPath)

    override fun removeTiles(tilePath: String, southwest: Point, northeast: Point): Long =
        navigator.removeTiles(tilePath, southwest, northeast)

    // History traces

    override fun getHistory(): String = navigator.history

    override fun toggleHistory(isEnabled: Boolean) {
        navigator.toggleHistory(isEnabled)
    }

    override fun addHistoryEvent(eventType: String, eventJsonProperties: String) {
        navigator.pushHistory(eventType, eventJsonProperties)
    }

    // Configuration

    override fun getConfig(): NavigatorConfig = navigator.config

    override fun setConfig(config: NavigatorConfig?) {
        navigator.setConfig(config)
    }

    // Other

    override fun getVoiceInstruction(index: Int): VoiceInstruction? =
        navigator.getVoiceInstruction(index)

    private fun NavigationStatus.getRouteProgress(): RouteProgress {
        val upcomingStepIndex = stepIndex + ONE_INDEX

        val routeProgressBuilder = RouteProgress.Builder()
        val legProgressBuilder = RouteLegProgress.Builder()
        val stepProgressBuilder = RouteStepProgress.Builder()

        ifNonNull(route?.legs()) { legs ->
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
                var routeDurationRemaining = remainingLegDuration
                if (legs.size >= TWO_LEGS) {
                    for (i in legIndex + ONE_INDEX until legs.size) {
                        routeDistanceRemaining += legs[i].distance()?.toFloat() ?: 0f
                        routeDurationRemaining += legs[i].duration()?.toLong() ?: 0L
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
                    stepProgressBuilder.fractionTraveled(distanceTraveled / currentStep.distance().toFloat())

                    routeState.convertState()?.also {
                        routeProgressBuilder.currentState(it)

                        var bannerInstructions = bannerInstruction?.mapToDirectionsApi(currentStep)
                        if (it == RouteProgressState.ROUTE_INITIALIZED) {
                            bannerInstructions =
                                getBannerInstruction(FIRST_BANNER_INSTRUCTION)?.mapToDirectionsApi(
                                    currentStep
                                )
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
        stepProgressBuilder.durationRemaining((remainingStepDuration / ONE_SECOND_IN_MILLISECONDS).roundToLong())

        legProgressBuilder.currentStepProgress(stepProgressBuilder.build())
        legProgressBuilder.distanceRemaining(remainingLegDistance)
        legProgressBuilder.durationRemaining((remainingLegDuration / ONE_SECOND_IN_MILLISECONDS).roundToLong())

        routeProgressBuilder.currentLegProgress(legProgressBuilder.build())

        routeProgressBuilder.inTunnel(inTunnel)
        routeProgressBuilder.routeGeometryWithBuffer(routeBufferGeoJson)

        routeProgressBuilder.voiceInstructions(voiceInstruction?.mapToDirectionsApi())

        ifNonNull(route) {
            routeProgressBuilder.route(it)
        }

        return routeProgressBuilder.build()
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
}

private fun RouteState.convertState(): RouteProgressState? {
    return when (this) {
        RouteState.INVALID -> RouteProgressState.ROUTE_INVALID
        RouteState.INITIALIZED -> RouteProgressState.ROUTE_INITIALIZED
        RouteState.TRACKING -> RouteProgressState.LOCATION_TRACKING
        RouteState.COMPLETE -> RouteProgressState.ROUTE_ARRIVED
        RouteState.OFFROUTE -> null // send in a callback instead
        RouteState.STALE -> RouteProgressState.LOCATION_STALE
        RouteState.UNCERTAIN -> RouteProgressState.ROUTE_UNCERTAIN
    }
}
