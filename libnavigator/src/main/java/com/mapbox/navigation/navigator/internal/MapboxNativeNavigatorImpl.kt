package com.mapbox.navigation.navigator.internal

import android.location.Location
import com.mapbox.api.directions.v5.models.BannerComponents
import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.api.directions.v5.models.BannerText
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.LegStep
import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.api.directions.v5.models.VoiceInstructions
import com.mapbox.base.common.logger.Logger
import com.mapbox.geojson.Geometry
import com.mapbox.geojson.Point
import com.mapbox.geojson.gson.GeometryGeoJson
import com.mapbox.geojson.utils.PolylineUtils
import com.mapbox.navigation.base.options.DeviceProfile
import com.mapbox.navigation.base.trip.model.ElectronicHorizon
import com.mapbox.navigation.base.trip.model.RouteLegProgress
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.model.RouteProgressState
import com.mapbox.navigation.base.trip.model.RouteStepProgress
import com.mapbox.navigation.navigator.ifNonNull
import com.mapbox.navigation.navigator.toFixLocation
import com.mapbox.navigation.navigator.toLocation
import com.mapbox.navigator.BannerComponent
import com.mapbox.navigator.BannerInstruction
import com.mapbox.navigator.BannerSection
import com.mapbox.navigator.ElectronicHorizonOutput
import com.mapbox.navigator.NavigationStatus
import com.mapbox.navigator.Navigator
import com.mapbox.navigator.NavigatorConfig
import com.mapbox.navigator.RouteState
import com.mapbox.navigator.RouterParams
import com.mapbox.navigator.RouterResult
import com.mapbox.navigator.SensorData
import com.mapbox.navigator.VoiceInstruction
import java.util.Date
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Default implementation of [MapboxNativeNavigator] interface.
 */
object MapboxNativeNavigatorImpl : MapboxNativeNavigator {

    private const val ONE_INDEX = 1
    private const val ONE_SECOND_IN_MILLISECONDS = 1000.0
    private const val FIRST_BANNER_INSTRUCTION = 0
    private const val GRID_SIZE = 0.0025f
    private const val BUFFER_DILATION: Short = 1
    private const val TWO_LEGS: Short = 2
    private const val PRIMARY_ROUTE_INDEX = 0

    private var navigator: Navigator? = null
    private var route: DirectionsRoute? = null
    private var routeBufferGeoJson: Geometry? = null
    private val mutex = Mutex()

    // Route following

    /**
     * Create or reset resources. This must be called before calling any
     * functions within [MapboxNativeNavigatorImpl]
     */
    override fun create(deviceProfile: DeviceProfile, logger: Logger?): MapboxNativeNavigator {
        navigator = NavigatorLoader.createNavigator(deviceProfile, logger)
        route = null
        routeBufferGeoJson = null
        return this
    }

    /**
     * Passes in the current raw location of the user.
     *
     * @param rawLocation The current raw [Location] of user.
     *
     * @return true if the raw location was usable, false if not.
     */
    override suspend fun updateLocation(rawLocation: Location, date: Date): Boolean {
        mutex.withLock {
            return navigator!!.updateLocation(rawLocation.toFixLocation(date))
        }
    }

    /**
     * Passes in the current sensor data of the user.
     *
     * @param sensorData The current sensor data of user.
     *
     * @return true if the sensor data was usable, false if not.
     */
    override fun updateSensorData(sensorData: SensorData): Boolean {
        return navigator!!.updateSensorData(sensorData)
    }

    /**
     * Gets the status as an offset in time from the last fixed location. This
     * allows the caller to get predicted statuses in the future along the route if
     * the device is unable to get fixed locations. Poor reception would be one reason.
     *
     * This method uses previous fixes to snap the user's location to the route
     * and verify that the user is still on the route. This method also determines
     * if an instruction needs to be called out for the user.
     *
     * @param date the point in time to receive the status for.
     *
     * @return the last [TripStatus] as a result of fixed location updates. If the timestamp
     * is earlier than a previous call, the last status will be returned. The function does not support re-winding time.
     */
    override suspend fun getStatus(date: Date): TripStatus {
        mutex.withLock {
            val status = navigator!!.getStatus(date)
            return TripStatus(
                status.location.toLocation(),
                status.key_points.map { it.toLocation() },
                status.getRouteProgress(),
                status.routeState == RouteState.OFF_ROUTE
            )
        }
    }

    // Routing

    /**
     * Sets the route path for the navigator to process.
     * Returns initialized route state if no errors occurred.
     * Otherwise, it returns a invalid route state.
     *
     * @param route [DirectionsRoute] to follow.
     * @param legIndex Which leg to follow
     *
     * @return a [NavigationStatus] route state if no errors occurred.
     * Otherwise, it returns a invalid route state.
     */
    override suspend fun setRoute(
        route: DirectionsRoute?,
        legIndex: Int
    ): NavigationStatus {
        mutex.withLock {
            MapboxNativeNavigatorImpl.route = route
            val result = navigator!!.setRoute(route?.toJson()
                ?: "{}", PRIMARY_ROUTE_INDEX, legIndex)
            navigator!!.getRouteBufferGeoJson(GRID_SIZE, BUFFER_DILATION)?.also {
                routeBufferGeoJson = GeometryGeoJson.fromJson(it)
            }
            return result
        }
    }

    /**
     * Updates annotations so that subsequent calls to getStatus will
     * reflect the most current annotations for the route.
     *
     * @param legAnnotationJson A string containing the json/pbf annotations
     * @param routeIndex Which route to apply the annotation update to
     * @param legIndex Which leg to apply the annotation update to
     *
     * @return True if the annotations could be updated false if not (wrong number of annotations)
     */
    override fun updateAnnotations(
        legAnnotationJson: String,
        routeIndex: Int,
        legIndex: Int
    ): Boolean = navigator!!.updateAnnotations(legAnnotationJson, routeIndex, legIndex)

    /**
     * Gets the banner at a specific step index in the route. If there is no
     * banner at the specified index method return *null*.
     *
     * @param index Which step you want to get [BannerInstruction] for
     *
     * @return [BannerInstruction] for step index you passed
     */
    override fun getBannerInstruction(index: Int): BannerInstruction? =
        navigator!!.getBannerInstruction(index)

    /**
     * Gets a polygon around the currently loaded route. The method uses a bitmap approach
     * in which you specify a grid size (pixel size) and a dilation (how many pixels) to
     * expand the initial grid cells that are intersected by the route.
     *
     * @param gridSize the size of the individual grid cells
     * @param bufferDilation the number of pixels to dilate the initial intersection by it can
     * be thought of as controlling the halo thickness around the route
     *
     * @return a geojson as [String] representing the route buffer polygon
     */
    override fun getRouteGeometryWithBuffer(gridSize: Float, bufferDilation: Short): String? =
        navigator!!.getRouteBufferGeoJson(gridSize, bufferDilation)

    /**
     * Follows a new leg of the already loaded directions.
     * Returns an initialized navigation status if no errors occurred
     * otherwise, it returns an invalid navigation status state.
     *
     * @param legIndex new leg index
     *
     * @return an initialized [NavigationStatus] if no errors, invalid otherwise
     */
    override fun updateLegIndex(legIndex: Int): NavigationStatus =
        navigator!!.changeRouteLeg(PRIMARY_ROUTE_INDEX, legIndex)

    // Free Drive

    /**
     * Toggles Electronic Horizon on or off.
     *
     * @param isEnabled set this to true to turn on Electronic Horizon and false to turn it off
     */
    override fun toggleElectronicHorizon(isEnabled: Boolean) {
        when (isEnabled) {
            true -> navigator!!.enableElectronicHorizon()
            false -> navigator!!.disableElectronicHorizon()
        }
    }

    // Offline

    /**
     * Caches tiles around the last set route
     */
    override fun cacheLastRoute() {
        navigator!!.cacheLastRoute()
    }

    /**
     * Configures routers for getting routes offline.
     *
     * @param routerParams Optional [RouterParams] object which contains router configurations for
     * getting routes offline.
     *
     * @return number of tiles founded in the directory
     */
    override fun configureRouter(routerParams: RouterParams): Long =
        navigator!!.configureRouter(routerParams)

    /**
     * Uses valhalla and local tile data to generate mapbox-directions-api-like json.
     *
     * @param url the directions-based uri used when hitting the http service
     * @return a [RouterResult] object with the json and a success/fail boolean
     */
    override fun getRoute(url: String): RouterResult = navigator!!.getRoute(url)

    /**
     * Passes in an input path to the tar file and output path.
     *
     * @param tarPath The path to the packed tiles.
     * @param destinationPath The path to the unpacked files.
     *
     * @return the number of unpacked tiles
     */
    override fun unpackTiles(tarPath: String, destinationPath: String): Long =
        navigator!!.unpackTiles(tarPath, destinationPath)

    /**
     * Removes tiles wholly within the supplied bounding box. If the tile is not
     * contained completely within the bounding box, it will remain in the cache.
     * After removing files from the cache, any routers should be reconfigured
     * to synchronize their in-memory cache with the disk.
     *
     * @param tilePath The path to the tiles.
     * @param southwest The lower left coord of the bounding box.
     * @param northeast The upper right coord of the bounding box.
     *
     * @return the number of tiles removed
     */
    override fun removeTiles(tilePath: String, southwest: Point, northeast: Point): Long =
        navigator!!.removeTiles(tilePath, southwest, northeast)

    // History traces

    /**
     * Gets the history of state-changing calls to the navigator. This can be used to
     * replay a sequence of events for the purpose of bug fixing.
     *
     * @return a json representing the series of events that happened since the last time
     * the history was toggled on.
     */
    override fun getHistory(): String = navigator!!.history

    /**
     * Toggles the recording of history on or off.
     * Toggling will reset all history calls [getHistory] first before toggling to retain a copy.
     *
     * @param isEnabled set this to true to turn on history recording and false to turn it off
     */
    override fun toggleHistory(isEnabled: Boolean) {
        navigator!!.toggleHistory(isEnabled)
    }

    /**
     * Adds a custom event to the navigator's history. This can be useful to log things that
     * happen during navigation that are specific to your application.
     *
     * @param eventType the event type in the events log for your custom event
     * @param eventJsonProperties the json to attach to the "properties" key of the event
     */
    override fun addHistoryEvent(eventType: String, eventJsonProperties: String) {
        navigator!!.pushHistory(eventType, eventJsonProperties)
    }

    // Configuration

    /**
     * Gets the current configuration used for navigation.
     *
     * @return the [NavigatorConfig] used for navigation.
     */
    override fun getConfig(): NavigatorConfig = navigator!!.config

    /**
     * Updates the configuration used for navigation. Passing null resets the config.
     *
     * @param config the new [NavigatorConfig]
     */
    override fun setConfig(config: NavigatorConfig?) {
        navigator!!.setConfig(config)
    }

    // Other

    /**
     * Gets the voice instruction at a specific step index in the route. If there is no
     * voice instruction at the specified index, *null* is returned.
     *
     * @param index Which step you want to get [VoiceInstruction] for
     *
     * @return [VoiceInstruction] for step index you passed
     */
    override fun getVoiceInstruction(index: Int): VoiceInstruction? =
        navigator!!.getVoiceInstruction(index)

    /**
     * Builds [RouteProgress] object based on [NavigationStatus] returned by [Navigator]
     */
    private fun NavigationStatus.getRouteProgress(): RouteProgress {
        val upcomingStepIndex = stepIndex + ONE_INDEX

        val routeProgressBuilder = RouteProgress.Builder()
        val legProgressBuilder = RouteLegProgress.Builder()
        val stepProgressBuilder = RouteStepProgress.Builder()

        val electronicHorizon = electronicHorizon.mapToElectronicHorizon()
        routeProgressBuilder.eHorizon(electronicHorizon)

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
                    ifNonNull(currentStep.bannerInstructions()) {
                        stepProgressBuilder.guidanceViewURL(getGuidanceViewUrl(it))
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
        stepProgressBuilder.durationRemaining(remainingStepDuration / ONE_SECOND_IN_MILLISECONDS)

        legProgressBuilder.currentStepProgress(stepProgressBuilder.build())
        legProgressBuilder.distanceRemaining(remainingLegDistance)
        legProgressBuilder.durationRemaining(remainingLegDuration / ONE_SECOND_IN_MILLISECONDS)

        routeProgressBuilder.currentLegProgress(legProgressBuilder.build())

        routeProgressBuilder.inTunnel(inTunnel)
        routeProgressBuilder.routeGeometryWithBuffer(routeBufferGeoJson)

        routeProgressBuilder.voiceInstructions(voiceInstruction?.mapToDirectionsApi())

        ifNonNull(route) {
            routeProgressBuilder.route(it)
        }

        return routeProgressBuilder.build()
    }

    private fun getGuidanceViewUrl(bannerInstructions: List<BannerInstructions>): String? {
        bannerInstructions.forEach {
            ifNonNull(it.view()) { bannerView ->
                return bannerView.components()?.firstOrNull()?.imageUrl()
            }
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

    private fun ElectronicHorizonOutput.mapToElectronicHorizon(): ElectronicHorizon {
        val eHorizonBuilder = ElectronicHorizon.Builder()
            .routeIndex(this.routeIndex)
            .legIndex(this.legIndex)
            .legDistanceRemaining(this.remainingLegDistance)
            .legDurationRemaining(this.remainingLegDuration / ONE_SECOND_IN_MILLISECONDS)
            .stepIndex(this.stepIndex)
            .stepDistanceRemaining(this.remainingStepDistance)
            .stepDurationRemaining(this.remainingStepDuration / ONE_SECOND_IN_MILLISECONDS)
            .shapeIndex(this.shapeIndex)
            .intersectionIndex(this.intersectionIndex)
        this.horizon?.let {
            val eHorizonRoute = DirectionsResponse.fromJson(it)
            eHorizonBuilder.horizon(eHorizonRoute)
        }
        return eHorizonBuilder.build()
    }
}

private fun RouteState.convertState(): RouteProgressState? {
    return when (this) {
        RouteState.INVALID -> RouteProgressState.ROUTE_INVALID
        RouteState.INITIALIZED -> RouteProgressState.ROUTE_INITIALIZED
        RouteState.TRACKING -> RouteProgressState.LOCATION_TRACKING
        RouteState.COMPLETE -> RouteProgressState.ROUTE_ARRIVED
        RouteState.OFF_ROUTE -> null // send in a callback instead
        RouteState.STALE -> RouteProgressState.LOCATION_STALE
        RouteState.UNCERTAIN -> RouteProgressState.ROUTE_UNCERTAIN
    }
}
