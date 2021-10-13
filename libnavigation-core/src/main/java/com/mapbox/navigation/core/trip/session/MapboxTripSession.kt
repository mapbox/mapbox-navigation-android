package com.mapbox.navigation.core.trip.session

import android.hardware.SensorEvent
import android.location.Location
import androidx.annotation.VisibleForTesting
import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.VoiceInstructions
import com.mapbox.base.common.logger.Logger
import com.mapbox.base.common.logger.model.Message
import com.mapbox.base.common.logger.model.Tag
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.internal.factory.RoadFactory
import com.mapbox.navigation.base.internal.factory.TripNotificationStateFactory.buildTripNotificationState
import com.mapbox.navigation.base.trip.model.RouteLegProgress
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.model.roadobject.UpcomingRoadObject
import com.mapbox.navigation.core.directions.session.RoutesExtra
import com.mapbox.navigation.core.navigator.getLocationMatcherResult
import com.mapbox.navigation.core.navigator.getRouteInitInfo
import com.mapbox.navigation.core.navigator.getRouteProgressFrom
import com.mapbox.navigation.core.navigator.getTripStatusFrom
import com.mapbox.navigation.core.navigator.mapToDirectionsApi
import com.mapbox.navigation.core.navigator.toFixLocation
import com.mapbox.navigation.core.navigator.toLocation
import com.mapbox.navigation.core.navigator.toLocations
import com.mapbox.navigation.core.sensors.SensorMapper
import com.mapbox.navigation.core.trip.service.TripService
import com.mapbox.navigation.core.trip.session.eh.EHorizonObserver
import com.mapbox.navigation.core.trip.session.eh.EHorizonSubscriptionManager
import com.mapbox.navigation.navigator.internal.MapboxNativeNavigator
import com.mapbox.navigation.navigator.internal.MapboxNativeNavigatorImpl
import com.mapbox.navigation.utils.internal.JobControl
import com.mapbox.navigation.utils.internal.ThreadController
import com.mapbox.navigation.utils.internal.ifNonNull
import com.mapbox.navigation.utils.internal.logW
import com.mapbox.navigator.BannerInstruction
import com.mapbox.navigator.FallbackVersionsObserver
import com.mapbox.navigator.NavigationStatus
import com.mapbox.navigator.NavigationStatusOrigin
import com.mapbox.navigator.NavigatorObserver
import com.mapbox.navigator.RouteState
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import java.util.concurrent.CopyOnWriteArraySet

/**
 * Default implementation of [TripSession]
 *
 * @param tripService TripService
 * @param tripSessionLocationEngine the location engine
 * @param navigator Native navigator
 * @param threadController controller for main/io jobs
 * @param logger interface for logging any events
 */
internal class MapboxTripSession(
    override val tripService: TripService,
    private val tripSessionLocationEngine: TripSessionLocationEngine,
    private val navigator: MapboxNativeNavigator = MapboxNativeNavigatorImpl,
    private val threadController: ThreadController = ThreadController,
    private val logger: Logger,
    private val eHorizonSubscriptionManager: EHorizonSubscriptionManager,
) : TripSession {

    private var updateRouteJob: Job? = null
    private var updateLegIndexJob: Job? = null
    private var updateRouteProgressJob: Job? = null

    @VisibleForTesting
    internal var routes: List<DirectionsRoute> = emptyList()

    private companion object {
        private val TAG = Tag("MbxTripSession")
    }

    override fun setRoutes(
        routes: List<DirectionsRoute>,
        legIndex: Int,
        @RoutesExtra.RoutesUpdateReason reason: String
    ) {

        isOffRoute = false
        invalidateLatestBannerInstructionEvent()
        roadObjects = emptyList()
        routeProgress = null

        updateRouteJob = when (reason) {
            RoutesExtra.ROUTES_UPDATE_REASON_CLEAN_UP,
            RoutesExtra.ROUTES_UPDATE_REASON_NEW,
            RoutesExtra.ROUTES_UPDATE_REASON_REROUTE,
            RoutesExtra.ROUTES_UPDATE_REASON_ALTERNATIVE -> {
                updateLegIndexJob?.cancel()
                updateRouteProgressJob?.cancel()

                threadController.getMainScopeAndRootJob().scope.launch {
                    navigator.setRoute(routes, legIndex)?.let {
                        roadObjects = getRouteInitInfo(it)?.roadObjects ?: emptyList()
                    }
                    this@MapboxTripSession.routes = routes
                }
            }
            RoutesExtra.ROUTES_UPDATE_REASON_REFRESH -> {
                threadController.getMainScopeAndRootJob().scope.launch {
                    if (routes.isNotEmpty()) {
                        navigator.updateAnnotations(
                            routes[MapboxNativeNavigatorImpl.PRIMARY_ROUTE_INDEX]
                        )
                        this@MapboxTripSession.routes = routes
                    } else {
                        logger.w(
                            TAG,
                            Message("Cannot refresh route. Route can't be null"),
                        )
                    }
                }
            }
            else -> null.also {
                logW(
                    TAG,
                    Message("Unsupported route update reason: $reason")
                )
            }
        }
    }

    private val mainJobController: JobControl = threadController.getMainScopeAndRootJob()

    private val locationObservers = CopyOnWriteArraySet<LocationObserver>()
    private val routeProgressObservers = CopyOnWriteArraySet<RouteProgressObserver>()
    private val offRouteObservers = CopyOnWriteArraySet<OffRouteObserver>()
    private val stateObservers = CopyOnWriteArraySet<TripSessionStateObserver>()
    private val bannerInstructionsObservers = CopyOnWriteArraySet<BannerInstructionsObserver>()
    private val voiceInstructionsObservers = CopyOnWriteArraySet<VoiceInstructionsObserver>()
    private val roadObjectsOnRouteObservers =
        CopyOnWriteArraySet<RoadObjectsOnRouteObserver>()
    private val fallbackVersionsObservers = CopyOnWriteArraySet<FallbackVersionsObserver>()

    private val bannerInstructionEvent = BannerInstructionEvent()

    private var state: TripSessionState = TripSessionState.STOPPED
        set(value) {
            if (field == value) {
                return
            }
            field = value
            stateObservers.forEach { it.onSessionStateChanged(value) }
        }

    private var isOffRoute: Boolean = false
        set(value) {
            if (field == value) {
                return
            }
            field = value
            offRouteObservers.forEach { it.onOffRouteStateChanged(value) }
        }

    private var rawLocation: Location? = null
    override var zLevel: Int? = null
        private set
    private var routeProgress: RouteProgress? = null
    private var roadObjects: List<UpcomingRoadObject> = emptyList()
        set(value) {
            if (field == value) {
                return
            }
            field = value
            roadObjectsOnRouteObservers.forEach { it.onNewRoadObjectsOnTheRoute(value) }
        }

    override var locationMatcherResult: LocationMatcherResult? = null
        private set

    private val nativeFallbackVersionsObserver =
        object : com.mapbox.navigator.FallbackVersionsObserver {
            override fun onFallbackVersionsFound(versions: MutableList<String>) {
                mainJobController.scope.launch {
                    fallbackVersionsObservers.forEach {
                        it.onFallbackVersionsFound(versions)
                    }
                }
            }

            override fun onCanReturnToLatest(version: String) {
                mainJobController.scope.launch {
                    fallbackVersionsObservers.forEach {
                        it.onCanReturnToLatest(version)
                    }
                }
            }
        }

    init {
        navigator.setNativeNavigatorRecreationObserver {
            if (fallbackVersionsObservers.isNotEmpty()) {
                navigator.setFallbackVersionsObserver(nativeFallbackVersionsObserver)
            }
            if (state == TripSessionState.STARTED) {
                navigator.addNavigatorObserver(navigatorObserver)
            }
        }
    }

    /**
     * Return raw location
     */
    override fun getRawLocation() = rawLocation

    /**
     * Provide route progress
     */
    override fun getRouteProgress() = routeProgress

    /**
     * Current [MapboxTripSession] state
     */
    override fun getState(): TripSessionState = state

    /**
     * Start MapboxTripSession
     */
    override fun start(withTripService: Boolean, withReplayEnabled: Boolean) {
        if (state == TripSessionState.STARTED) {
            return
        }
        navigator.addNavigatorObserver(navigatorObserver)
        if (withTripService) {
            tripService.startService()
        }
        tripSessionLocationEngine.startLocationUpdates(withReplayEnabled) {
            updateRawLocation(it)
        }
        state = TripSessionState.STARTED
    }

    private fun updateRawLocation(rawLocation: Location) {
        if (state != TripSessionState.STARTED) return

        this.rawLocation = rawLocation
        locationObservers.forEach { it.onNewRawLocation(rawLocation) }
        mainJobController.scope.launch {
            navigator.updateLocation(rawLocation.toFixLocation())
        }
    }

    /**
     * Returns if the MapboxTripSession is running a foreground service
     */
    override fun isRunningWithForegroundService(): Boolean {
        return tripService.hasServiceStarted()
    }

    @OptIn(ExperimentalMapboxNavigationAPI::class)
    private val navigatorObserver = object : NavigatorObserver {
        override fun onStatus(origin: NavigationStatusOrigin, status: NavigationStatus) {
            val tripStatus = status.getTripStatusFrom(
                routes,
                MapboxNativeNavigatorImpl.PRIMARY_ROUTE_INDEX
            )
            val enhancedLocation = tripStatus.navigationStatus.location.toLocation()
            val keyPoints = tripStatus.navigationStatus.keyPoints.toLocations()
            val road = RoadFactory.buildRoadObject(tripStatus.navigationStatus)
            updateLocationMatcherResult(
                tripStatus.getLocationMatcherResult(enhancedLocation, keyPoints, road)
            )
            zLevel = status.layer

            // we should skip RouteProgress, BannerInstructions, isOffRoute state updates while
            // setting a new route
            if (updateRouteJob?.isActive == true) {
                return
            }

            val remainingWaypoints =
                ifNonNull(tripStatus.route?.routeOptions()?.coordinatesList()?.size) {
                    it - tripStatus.navigationStatus.nextWaypointIndex
                } ?: 0

            updateRouteProgressJob?.cancel()
            updateRouteProgressJob = mainJobController.scope.launch {
                var triggerObserver = false
                if (tripStatus.navigationStatus.routeState != RouteState.INVALID) {
                    val nativeBannerInstruction: BannerInstruction? =
                        tripStatus.navigationStatus.bannerInstruction.let {
                            if (it == null &&
                                bannerInstructionEvent.latestBannerInstructions == null
                            ) {
                                // workaround for a remaining issues in
                                // github.com/mapbox/mapbox-navigation-native/issues/3466
                                MapboxNativeNavigatorImpl.getCurrentBannerInstruction()
                            } else {
                                it
                            }
                        }
                    val bannerInstructions: BannerInstructions? =
                        nativeBannerInstruction?.mapToDirectionsApi()
                    triggerObserver = bannerInstructionEvent.isOccurring(
                        bannerInstructions,
                        nativeBannerInstruction?.index
                    )
                }
                val routeProgress = getRouteProgressFrom(
                    tripStatus.route,
                    tripStatus.navigationStatus,
                    remainingWaypoints,
                    bannerInstructionEvent.latestBannerInstructions,
                    bannerInstructionEvent.latestInstructionIndex
                )
                updateRouteProgress(routeProgress, triggerObserver)

                isOffRoute = tripStatus.navigationStatus.routeState == RouteState.OFF_ROUTE
            }
        }
    }

    /**
     * Stop MapboxTripSession
     */
    override fun stop() {
        if (state == TripSessionState.STOPPED) {
            return
        }
        navigator.removeNavigatorObserver(navigatorObserver)
        tripService.stopService()
        tripSessionLocationEngine.stopLocationUpdates()
        mainJobController.job.cancelChildren()
        reset()
        state = TripSessionState.STOPPED
    }

    private fun reset() {
        updateRouteProgressJob?.cancel()
        updateLegIndexJob?.cancel()
        locationMatcherResult = null
        rawLocation = null
        zLevel = null
        routeProgress = null
        isOffRoute = false
        eHorizonSubscriptionManager.reset()
    }

    /**
     * Register [LocationObserver] to receive location updates
     */
    override fun registerLocationObserver(locationObserver: LocationObserver) {
        locationObservers.add(locationObserver)
        rawLocation?.let { locationObserver.onNewRawLocation(it) }
        locationMatcherResult?.let { locationObserver.onNewLocationMatcherResult(it) }
    }

    /**
     * Unregister [LocationObserver]
     */
    override fun unregisterLocationObserver(locationObserver: LocationObserver) {
        locationObservers.remove(locationObserver)
    }

    /**
     * Unregister all [LocationObserver]
     *
     * @see [registerLocationObserver]
     */
    override fun unregisterAllLocationObservers() {
        locationObservers.clear()
    }

    /**
     * Register [RouteProgressObserver] to receive information about routing's state
     * like [BannerInstructions], [RouteLegProgress], etc.
     *
     * @see [RouteProgress]
     */
    override fun registerRouteProgressObserver(routeProgressObserver: RouteProgressObserver) {
        routeProgressObservers.add(routeProgressObserver)
        routeProgress?.let { routeProgressObserver.onRouteProgressChanged(it) }
    }

    /**
     * Unregister [RouteProgressObserver]
     */
    override fun unregisterRouteProgressObserver(routeProgressObserver: RouteProgressObserver) {
        routeProgressObservers.remove(routeProgressObserver)
    }

    /**
     * Unregister all [RouteProgressObserver]
     *
     * @see [registerRouteProgressObserver]
     */
    override fun unregisterAllRouteProgressObservers() {
        routeProgressObservers.clear()
    }

    /**
     * Register [OffRouteObserver] to receive notification about off-route events
     */
    override fun registerOffRouteObserver(offRouteObserver: OffRouteObserver) {
        offRouteObservers.add(offRouteObserver)
        offRouteObserver.onOffRouteStateChanged(isOffRoute)
    }

    /**
     * Unregister [OffRouteObserver]
     */
    override fun unregisterOffRouteObserver(offRouteObserver: OffRouteObserver) {
        offRouteObservers.remove(offRouteObserver)
    }

    /**
     * Unregister all [OffRouteObserver]
     *
     * @see [registerOffRouteObserver]
     */
    override fun unregisterAllOffRouteObservers() {
        offRouteObservers.clear()
    }

    /**
     * Register [TripSessionStateObserver] to receive current TripSession's state
     *
     * @see [TripSessionState]
     */
    override fun registerStateObserver(stateObserver: TripSessionStateObserver) {
        stateObservers.add(stateObserver)
        stateObserver.onSessionStateChanged(state)
    }

    /**
     * Unregister [TripSessionStateObserver]
     */
    override fun unregisterStateObserver(stateObserver: TripSessionStateObserver) {
        stateObservers.remove(stateObserver)
    }

    /**
     * Unregister all [TripSessionStateObserver]
     *
     * @see [registerStateObserver]
     */
    override fun unregisterAllStateObservers() {
        stateObservers.clear()
    }

    /**
     * Register [BannerInstructionsObserver]
     */
    override fun registerBannerInstructionsObserver(
        bannerInstructionsObserver: BannerInstructionsObserver
    ) {
        bannerInstructionsObservers.add(bannerInstructionsObserver)
        checkLatestValidBannerInstructionEvent { bannerInstruction ->
            bannerInstructionsObserver.onNewBannerInstructions(bannerInstruction)
        }
    }

    /**
     * Unregister [BannerInstructionsObserver]
     */
    override fun unregisterBannerInstructionsObserver(
        bannerInstructionsObserver: BannerInstructionsObserver
    ) {
        bannerInstructionsObservers.remove(bannerInstructionsObserver)
    }

    /**
     * Unregister all [BannerInstructionsObserver]
     *
     * @see [registerBannerInstructionsObserver]
     */
    override fun unregisterAllBannerInstructionsObservers() {
        bannerInstructionsObservers.clear()
    }

    /**
     * Register [VoiceInstructionsObserver]
     */
    override fun registerVoiceInstructionsObserver(
        voiceInstructionsObserver: VoiceInstructionsObserver
    ) {
        voiceInstructionsObservers.add(voiceInstructionsObserver)
        routeProgress?.let {
            checkVoiceInstructionEvent(it.voiceInstructions) { voiceInstruction ->
                voiceInstructionsObserver.onNewVoiceInstructions(voiceInstruction)
            }
        }
    }

    /**
     * Unregister [VoiceInstructionsObserver]
     */
    override fun unregisterVoiceInstructionsObserver(
        voiceInstructionsObserver: VoiceInstructionsObserver
    ) {
        voiceInstructionsObservers.remove(voiceInstructionsObserver)
    }

    /**
     * Unregister all [VoiceInstructionsObserver]
     *
     * @see [registerVoiceInstructionsObserver]
     */
    override fun unregisterAllVoiceInstructionsObservers() {
        voiceInstructionsObservers.clear()
    }

    /**
     * Sensor event consumed by native
     */
    override fun updateSensorEvent(sensorEvent: SensorEvent, callback: SensorEventUpdatedCallback) {
        mainJobController.scope.launch {
            var sensorUpdated = false
            try {
                SensorMapper.toSensorData(sensorEvent, logger)?.let { it ->
                    sensorUpdated = navigator.updateSensorData(it)
                }
            } finally {
                callback.onSensorEventUpdated(sensorUpdated)
            }
        }
    }

    /**
     * Follows a new leg of the already loaded directions.
     * Returns an initialized navigation status if no errors occurred
     * otherwise, it returns an invalid navigation status state.
     *
     * @param legIndex new leg index
     *
     * @return an initialized [NavigationStatus] if no errors, invalid otherwise
     */
    override fun updateLegIndex(legIndex: Int, callback: LegIndexUpdatedCallback) {
        var legIndexUpdated = false
        updateLegIndexJob = mainJobController.scope.launch {
            try {
                legIndexUpdated = navigator.updateLegIndex(legIndex)
                if (legIndexUpdated) {
                    invalidateLatestBannerInstructionEvent()
                }
            } finally {
                callback.onLegIndexUpdatedCallback(legIndexUpdated)
            }
        }
    }

    override fun registerRoadObjectsOnRouteObserver(
        roadObjectsOnRouteObserver: RoadObjectsOnRouteObserver
    ) {
        roadObjectsOnRouteObservers.add(roadObjectsOnRouteObserver)
        roadObjectsOnRouteObserver.onNewRoadObjectsOnTheRoute(roadObjects)
    }

    override fun unregisterRoadObjectsOnRouteObserver(
        roadObjectsOnRouteObserver: RoadObjectsOnRouteObserver
    ) {
        roadObjectsOnRouteObservers.remove(roadObjectsOnRouteObserver)
    }

    override fun unregisterAllRoadObjectsOnRouteObservers() {
        roadObjectsOnRouteObservers.clear()
    }

    override fun registerEHorizonObserver(eHorizonObserver: EHorizonObserver) {
        eHorizonSubscriptionManager.registerObserver(eHorizonObserver)
    }

    override fun unregisterEHorizonObserver(eHorizonObserver: EHorizonObserver) {
        eHorizonSubscriptionManager.unregisterObserver(eHorizonObserver)
    }

    override fun unregisterAllEHorizonObservers() {
        eHorizonSubscriptionManager.unregisterAllObservers()
    }

    override fun registerFallbackVersionsObserver(
        fallbackVersionsObserver: FallbackVersionsObserver
    ) {
        if (fallbackVersionsObservers.isEmpty()) {
            navigator.setFallbackVersionsObserver(nativeFallbackVersionsObserver)
        }
        fallbackVersionsObservers.add(fallbackVersionsObserver)
    }

    override fun unregisterFallbackVersionsObserver(
        fallbackVersionsObserver: FallbackVersionsObserver
    ) {
        fallbackVersionsObservers.remove(fallbackVersionsObserver)
        if (fallbackVersionsObservers.isEmpty()) {
            navigator.setFallbackVersionsObserver(null)
        }
    }

    override fun unregisterAllFallbackVersionsObservers() {
        fallbackVersionsObservers.clear()
        navigator.setFallbackVersionsObserver(null)
    }

    private fun updateLocationMatcherResult(locationMatcherResult: LocationMatcherResult) {
        this.locationMatcherResult = locationMatcherResult
        locationObservers.forEach { it.onNewLocationMatcherResult(locationMatcherResult) }
    }

    private fun updateRouteProgress(
        progress: RouteProgress?,
        shouldTriggerBannerInstructionsObserver: Boolean
    ) {
        routeProgress = progress
        if (tripService.hasServiceStarted()) {
            tripService.updateNotification(buildTripNotificationState(progress))
        }
        progress?.let { progress ->
            routeProgressObservers.forEach { it.onRouteProgressChanged(progress) }
            if (shouldTriggerBannerInstructionsObserver) {
                checkBannerInstructionEvent { bannerInstruction ->
                    bannerInstructionsObservers.forEach {
                        it.onNewBannerInstructions(bannerInstruction)
                    }
                }
            }
            checkVoiceInstructionEvent(progress.voiceInstructions) { voiceInstruction ->
                voiceInstructionsObservers.forEach {
                    it.onNewVoiceInstructions(voiceInstruction)
                }
            }
        }
    }

    private fun checkLatestValidBannerInstructionEvent(
        action: (BannerInstructions) -> Unit
    ) {
        ifNonNull(bannerInstructionEvent.latestBannerInstructions) {
            action(it)
        }
    }

    private fun invalidateLatestBannerInstructionEvent() {
        bannerInstructionEvent.invalidateLatestBannerInstructions()
    }

    private fun checkBannerInstructionEvent(
        action: (BannerInstructions) -> Unit
    ) {
        ifNonNull(bannerInstructionEvent.bannerInstructions) { bannerInstructions ->
            action(bannerInstructions)
        }
    }

    private fun checkVoiceInstructionEvent(
        currentVoiceInstructions: VoiceInstructions?,
        action: (VoiceInstructions) -> Unit
    ) {
        ifNonNull(currentVoiceInstructions) { voiceInstructions ->
            action(voiceInstructions)
        }
    }
}
