package com.mapbox.navigation.core.trip.session

import android.hardware.SensorEvent
import android.location.Location
import android.os.Looper
import android.util.Log
import com.mapbox.android.core.location.LocationEngineCallback
import com.mapbox.android.core.location.LocationEngineResult
import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.VoiceInstructions
import com.mapbox.base.common.logger.Logger
import com.mapbox.base.common.logger.model.Message
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.trip.model.RouteLegProgress
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.model.roadobject.UpcomingRoadObject
import com.mapbox.navigation.core.internal.utils.isSameRoute
import com.mapbox.navigation.core.internal.utils.isSameUuid
import com.mapbox.navigation.core.navigator.getMapMatcherResult
import com.mapbox.navigation.core.navigator.getRouteInitInfo
import com.mapbox.navigation.core.navigator.getRouteProgressFrom
import com.mapbox.navigation.core.navigator.toFixLocation
import com.mapbox.navigation.core.navigator.toLocation
import com.mapbox.navigation.core.navigator.toLocations
import com.mapbox.navigation.core.sensors.SensorMapper
import com.mapbox.navigation.core.trip.service.TripService
import com.mapbox.navigation.core.trip.session.eh.EHorizonObserver
import com.mapbox.navigation.core.trip.session.eh.EHorizonSubscriptionManager
import com.mapbox.navigation.navigator.internal.MapboxNativeNavigator
import com.mapbox.navigation.navigator.internal.MapboxNativeNavigatorImpl
import com.mapbox.navigation.navigator.internal.TripStatus
import com.mapbox.navigation.utils.internal.JobControl
import com.mapbox.navigation.utils.internal.ThreadController
import com.mapbox.navigation.utils.internal.ifNonNull
import com.mapbox.navigator.NavigationStatus
import com.mapbox.navigator.RouteState
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CopyOnWriteArraySet

/**
 * Default implementation of [TripSession]
 *
 * @param tripService TripService
 * @param navigationOptions the navigator options
 * For more information see [MapboxNativeNavigator.getStatus]. Unit is milliseconds
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

    companion object {
        @Volatile
        internal var UNCONDITIONAL_STATUS_POLLING_PATIENCE = 2000L

        @Volatile
        internal var UNCONDITIONAL_STATUS_POLLING_INTERVAL = 1000L
    }

    private var updateNavigatorStatusDataJobs: MutableList<Job> = CopyOnWriteArrayList()

    override var route: DirectionsRoute? = null
        set(value) {
            val isSameUuid = value?.isSameUuid(field) ?: false
            val isSameRoute = value?.isSameRoute(field) ?: false
            field = value
            if (value == null) {
                roadObjects = emptyList()
                routeProgress = null
            }
            cancelOngoingUpdateNavigatorStatusDataJobs()
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
                updateDataFromNavigatorStatus()
            }
            isOffRoute = false
            invalidateLatestBannerInstructionEvent()
        }

    private fun cancelOngoingUpdateNavigatorStatusDataJobs() {
        updateNavigatorStatusDataJobs.forEach {
            it.cancel()
        }
    }

    private val ioJobController: JobControl = threadController.getIOScopeAndRootJob()
    private val mainJobController: JobControl = threadController.getMainScopeAndRootJob()
    private var unconditionalStatusPollingJob: Job? = null

    private val locationObservers = CopyOnWriteArraySet<LocationObserver>()
    private val routeProgressObservers = CopyOnWriteArraySet<RouteProgressObserver>()
    private val offRouteObservers = CopyOnWriteArraySet<OffRouteObserver>()
    private val stateObservers = CopyOnWriteArraySet<TripSessionStateObserver>()
    private val bannerInstructionsObservers = CopyOnWriteArraySet<BannerInstructionsObserver>()
    private val voiceInstructionsObservers = CopyOnWriteArraySet<VoiceInstructionsObserver>()
    private val roadObjectsOnRouteObservers =
        CopyOnWriteArraySet<RoadObjectsOnRouteObserver>()
    private val mapMatcherResultObservers = CopyOnWriteArraySet<MapMatcherResultObserver>()

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
        tripService.startService()
        startLocationUpdates()
        state = TripSessionState.STARTED
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
        tripService.stopService()
        stopLocationUpdates()
        ioJobController.job.cancelChildren()
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
        updateNavigatorStatusDataJobs.clear()
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
        unconditionalStatusPollingJob?.cancel()
        if (state != TripSessionState.STARTED) return

        this.rawLocation = rawLocation
        locationObservers.forEach { it.onRawLocationChanged(rawLocation) }
        mainJobController.scope.launch {
            navigator.updateLocation(rawLocation.toFixLocation())
            updateDataFromNavigatorStatus()
        }

        unconditionalStatusPollingJob = ioJobController.scope.launch {
            delay(UNCONDITIONAL_STATUS_POLLING_PATIENCE)
            while (isActive) {
                mainJobController.scope.launch {
                    updateDataFromNavigatorStatus()
                }
                delay(UNCONDITIONAL_STATUS_POLLING_INTERVAL)
            }
        }
    }

    private fun updateDataFromNavigatorStatus() {
        val updateNavigatorStatusDataJob = mainJobController.scope.launch {
            if (state != TripSessionState.STARTED) {
                return@launch
            }

            val status = getNavigatorStatus()
            if (!isActive) {
                return@launch
            }
            val enhancedLocation = status.navigationStatus.location.toLocation()
            val keyPoints = status.navigationStatus.key_points.toLocations()
            updateEnhancedLocation(enhancedLocation, keyPoints)
            if (!isActive) {
                return@launch
            }
            updateMapMatcherResult(status.getMapMatcherResult(enhancedLocation, keyPoints))
            if (!isActive) {
                return@launch
            }
            val remainingWaypoints = ifNonNull(status.route?.routeOptions()?.coordinates()?.size) {
                it - status.navigationStatus.nextWaypointIndex
            } ?: 0
            val routeProgress = getRouteProgressFrom(
                status.route,
                status.routeBufferGeoJson,
                status.navigationStatus,
                remainingWaypoints
            )
            updateRouteProgress(routeProgress)
            if (!isActive) {
                return@launch
            }
            isOffRoute = status.navigationStatus.routeState == RouteState.OFF_ROUTE
        }
        updateNavigatorStatusDataJob.invokeOnCompletion {
            updateNavigatorStatusDataJobs.remove(updateNavigatorStatusDataJob)
        }
        updateNavigatorStatusDataJobs.add(updateNavigatorStatusDataJob)
    }

    private suspend fun getNavigatorStatus(): TripStatus {
        return navigator.getStatus(navigationOptions.navigatorPredictionMillis)
    }

    private fun updateEnhancedLocation(location: Location, keyPoints: List<Location>) {
        enhancedLocation = location
        locationObservers.forEach { it.onEnhancedLocationChanged(location, keyPoints) }
    }

    private fun updateMapMatcherResult(mapMatcherResult: MapMatcherResult) {
        this.mapMatcherResult = mapMatcherResult
        mapMatcherResultObservers.forEach { it.onNewMapMatcherResult(mapMatcherResult) }
    }

    private fun updateRouteProgress(progress: RouteProgress?) {
        routeProgress = progress
        Log.i("kyle_debug", "kyle_debug updateRouteProgress ${routeProgress?.durationRemaining}")
        tripService.updateNotification(progress)
        progress?.let { progress ->
            routeProgressObservers.forEach { it.onRouteProgressChanged(progress) }
            if (bannerInstructionEvent.isOccurring(progress)) {
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
