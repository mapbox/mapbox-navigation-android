package com.mapbox.navigation.core.trip.session

import android.hardware.SensorEvent
import android.location.Location
import android.os.Looper
import com.mapbox.android.core.location.LocationEngineCallback
import com.mapbox.android.core.location.LocationEngineResult
import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.VoiceInstructions
import com.mapbox.base.common.logger.Logger
import com.mapbox.base.common.logger.model.Message
import com.mapbox.navigation.base.internal.factory.TripNotificationStateFactory.buildTripNotificationState
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.trip.model.RouteLegProgress
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.model.roadobject.UpcomingRoadObject
import com.mapbox.navigation.core.internal.utils.isSameRoute
import com.mapbox.navigation.core.internal.utils.isSameUuid
import com.mapbox.navigation.core.navigator.getMapMatcherResult
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
import com.mapbox.navigator.BannerInstruction
import com.mapbox.navigator.FallbackVersionsObserver
import com.mapbox.navigator.NavigationStatus
import com.mapbox.navigator.NavigationStatusOrigin
import com.mapbox.navigator.NavigatorObserver
import com.mapbox.navigator.RouteState
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import java.util.concurrent.CopyOnWriteArraySet

/**
 * Default implementation of [TripSession]
 *
 * @param tripService TripService
 * @param navigationOptions the navigator options
 * @param navigator Native navigator
 * @param threadController controller for main/io jobs
 * @param logger interface for logging any events
 *
 * @property route should be set to start routing
 */
internal class MapboxTripSession(
    override val tripService: TripService,
    private val navigationOptions: NavigationOptions,
    private val navigator: MapboxNativeNavigator = MapboxNativeNavigatorImpl,
    private val threadController: ThreadController = ThreadController,
    private val logger: Logger,
    private val eHorizonSubscriptionManager: EHorizonSubscriptionManager,
) : TripSession {

    override var route: DirectionsRoute? = null
        set(value) {
            val isSameUuid = value?.isSameUuid(field) ?: false
            val isSameRoute = value?.isSameRoute(field) ?: false
            field = value
            if (value == null) {
                roadObjects = emptyList()
                routeProgress = null
            }
            val updateRouteJob = threadController.getMainScopeAndRootJob().scope.launch {
                when {
                    isSameUuid && isSameRoute && value != null -> {
                        navigator.updateAnnotations(value)
                    }
                    else -> {
                        navigator.setRoute(value)?.let {
                            roadObjects = getRouteInitInfo(it)?.roadObjects ?: emptyList()
                        }
                    }
                }
            }
            mainJobController.scope.launch {
                updateRouteJob.join()
            }
            isOffRoute = false
            invalidateLatestBannerInstructionEvent()
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
    private val mapMatcherResultObservers = CopyOnWriteArraySet<MapMatcherResultObserver>()
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
    private var enhancedLocation: Location? = null
    private var routeProgress: RouteProgress? = null
    private var roadObjects: List<UpcomingRoadObject> = emptyList()
        set(value) {
            if (field == value) {
                return
            }
            field = value
            roadObjectsOnRouteObservers.forEach { it.onNewRoadObjectsOnTheRoute(value) }
        }
    private var mapMatcherResult: MapMatcherResult? = null

    private val nativeFallbackVersionsObserver =
        object : com.mapbox.navigator.FallbackVersionsObserver() {
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
     * Return enhanced location
     */
    override fun getEnhancedLocation() = enhancedLocation

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
    override fun start() {
        if (state == TripSessionState.STARTED) {
            return
        }
        navigator.addNavigatorObserver(navigatorObserver)
        tripService.startService()
        startLocationUpdates()
        state = TripSessionState.STARTED
    }

    private val navigatorObserver = object : NavigatorObserver() {
        override fun onStatus(origin: NavigationStatusOrigin, status: NavigationStatus) {
            val tripStatus = status.getTripStatusFrom(route)
            val enhancedLocation = tripStatus.navigationStatus.location.toLocation()
            val keyPoints = tripStatus.navigationStatus.keyPoints.toLocations()
            updateEnhancedLocation(enhancedLocation, keyPoints)
            updateMapMatcherResult(
                tripStatus.getMapMatcherResult(enhancedLocation, keyPoints)
            )
            val remainingWaypoints =
                ifNonNull(tripStatus.route?.routeOptions()?.coordinatesList()?.size) {
                    it - tripStatus.navigationStatus.nextWaypointIndex
                } ?: 0

            var triggerObserver = false
            if (tripStatus.navigationStatus.routeState != RouteState.INVALID) {
                route?.let { route ->
                    ifNonNull(route.legs()) { legs ->
                        val currentLeg = legs[tripStatus.navigationStatus.legIndex]
                        ifNonNull(currentLeg?.steps()) { steps ->
                            val currentStep = steps[tripStatus.navigationStatus.stepIndex]
                            val nativeBannerInstruction: BannerInstruction? =
                                tripStatus.navigationStatus.bannerInstruction.let {
                                    if (it == null &&
                                        bannerInstructionEvent.latestBannerInstructions == null
                                    ) {
                                        // workaround for a remaining issues in
                                        // github.com/mapbox/mapbox-navigation-native/issues/3466
                                        // and
                                        // github.com/mapbox/mapbox-navigation-android/issues/4727
                                        MapboxNativeNavigatorImpl.getBannerInstruction(
                                            status.stepIndex
                                        )
                                    } else {
                                        it
                                    }
                                }
                            val bannerInstructions: BannerInstructions? =
                                nativeBannerInstruction?.mapToDirectionsApi(currentStep)
                            triggerObserver = bannerInstructionEvent.isOccurring(
                                bannerInstructions,
                                nativeBannerInstruction?.index
                            )
                        }
                    }
                }
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

    private fun startLocationUpdates() {
        navigationOptions.locationEngine.requestLocationUpdates(
            navigationOptions.locationEngineRequest,
            locationEngineCallback,
            Looper.getMainLooper()
        )
        navigationOptions.locationEngine.getLastLocation(locationEngineCallback)
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
        stopLocationUpdates()
        mainJobController.job.cancelChildren()
        reset()
        state = TripSessionState.STOPPED
    }

    private fun stopLocationUpdates() {
        navigationOptions.locationEngine.removeLocationUpdates(locationEngineCallback)
    }

    private fun reset() {
        mapMatcherResult = null
        rawLocation = null
        enhancedLocation = null
        routeProgress = null
        isOffRoute = false
        eHorizonSubscriptionManager.reset()
    }

    /**
     * Register [LocationObserver] to receive location updates
     */
    override fun registerLocationObserver(locationObserver: LocationObserver) {
        locationObservers.add(locationObserver)
        rawLocation?.let { locationObserver.onRawLocationChanged(it) }
        enhancedLocation?.let { locationObserver.onEnhancedLocationChanged(it, emptyList()) }
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
    override fun updateSensorEvent(sensorEvent: SensorEvent) {
        SensorMapper.toSensorData(sensorEvent, logger)?.let { sensorData ->
            navigator.updateSensorData(sensorData)
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
    override fun updateLegIndex(legIndex: Int): Boolean {
        invalidateLatestBannerInstructionEvent()
        return navigator.updateLegIndex(legIndex)
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

    override fun registerMapMatcherResultObserver(
        mapMatcherResultObserver: MapMatcherResultObserver
    ) {
        mapMatcherResultObservers.add(mapMatcherResultObserver)
        mapMatcherResult?.let {
            mapMatcherResultObserver.onNewMapMatcherResult(it)
        }
    }

    override fun unregisterMapMatcherResultObserver(
        mapMatcherResultObserver: MapMatcherResultObserver
    ) {
        mapMatcherResultObservers.remove(mapMatcherResultObserver)
    }

    override fun unregisterAllMapMatcherResultObservers() {
        mapMatcherResultObservers.clear()
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

    private var locationEngineCallback = object : LocationEngineCallback<LocationEngineResult> {
        override fun onSuccess(result: LocationEngineResult?) {
            result?.locations?.lastOrNull()?.let {
                updateRawLocation(it)
            }
        }

        override fun onFailure(exception: Exception) {
            logger.d(
                msg = Message("location on failure"),
                tr = exception
            )
        }
    }

    private fun updateRawLocation(rawLocation: Location) {
        if (state != TripSessionState.STARTED) return

        this.rawLocation = rawLocation
        locationObservers.forEach { it.onRawLocationChanged(rawLocation) }
        mainJobController.scope.launch {
            navigator.updateLocation(rawLocation.toFixLocation())
        }
    }

    private fun updateEnhancedLocation(location: Location, keyPoints: List<Location>) {
        enhancedLocation = location
        locationObservers.forEach { it.onEnhancedLocationChanged(location, keyPoints) }
    }

    private fun updateMapMatcherResult(mapMatcherResult: MapMatcherResult) {
        this.mapMatcherResult = mapMatcherResult
        mapMatcherResultObservers.forEach { it.onNewMapMatcherResult(mapMatcherResult) }
    }

    private fun updateRouteProgress(
        progress: RouteProgress?,
        shouldTriggerBannerInstructionsObserver: Boolean
    ) {
        routeProgress = progress
        tripService.updateNotification(buildTripNotificationState(progress))
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
