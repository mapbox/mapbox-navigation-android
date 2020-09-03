package com.mapbox.navigation.core.trip.session

import android.hardware.SensorEvent
import android.location.Location
import android.os.Looper
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineCallback
import com.mapbox.android.core.location.LocationEngineRequest
import com.mapbox.android.core.location.LocationEngineResult
import com.mapbox.api.directions.v5.models.BannerComponents
import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.VoiceInstructions
import com.mapbox.base.common.logger.Logger
import com.mapbox.base.common.logger.model.Message
import com.mapbox.navigation.base.trip.model.RouteLegProgress
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.sensors.SensorMapper
import com.mapbox.navigation.core.trip.service.TripService
import com.mapbox.navigation.navigator.internal.MapboxNativeNavigator
import com.mapbox.navigation.navigator.internal.MapboxNativeNavigatorImpl
import com.mapbox.navigation.navigator.internal.TripStatus
import com.mapbox.navigation.utils.internal.JobControl
import com.mapbox.navigation.utils.internal.ThreadController
import com.mapbox.navigation.utils.internal.ifNonNull
import com.mapbox.navigator.NavigationStatus
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
 * @param locationEngine LocationEngine
 * @param navigatorPredictionMillis millis for navigation status predictions
 * For more information see [MapboxNativeNavigator.getStatus]. Unit is milliseconds
 * @param navigator Native navigator
 * @param threadController controller for main/io jobs
 * @param logger interface for logging any events
 *
 * @property route should be set to start routing
 */
internal class MapboxTripSession(
    override val tripService: TripService,
    private val locationEngine: LocationEngine,
    private val navigatorPredictionMillis: Long,
    private val navigator: MapboxNativeNavigator = MapboxNativeNavigatorImpl,
    threadController: ThreadController = ThreadController,
    private val logger: Logger,
    private val accessToken: String?
) : TripSession {

    companion object {
        internal const val UNCONDITIONAL_STATUS_POLLING_PATIENCE = 2000L
        internal const val UNCONDITIONAL_STATUS_POLLING_INTERVAL = 1000L
        private const val LOCATION_POLLING_INTERVAL = 1000L
        private const val LOCATION_FASTEST_INTERVAL = 500L
        private val locationEngineRequest: LocationEngineRequest = LocationEngineRequest
            .Builder(LOCATION_POLLING_INTERVAL)
            .setFastestInterval(LOCATION_FASTEST_INTERVAL)
            .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
            .build()
    }

    private var updateNavigatorStatusDataJobs: MutableList<Job> = CopyOnWriteArrayList()

    override var route: DirectionsRoute? = null
        set(value) {
            field = value
            cancelOngoingUpdateNavigatorStatusDataJobs()
            mainJobController.scope.launch {
                navigator.setRoute(value)
                if (state == TripSessionState.STARTED) {
                    updateDataFromNavigatorStatus()
                }
            }
            isOffRoute = false
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

    private val bannerInstructionEvent = BannerInstructionEvent()
    private val voiceInstructionEvent = VoiceInstructionEvent()

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
        locationEngine.requestLocationUpdates(
            locationEngineRequest,
            locationEngineCallback,
            Looper.getMainLooper()
        )
        locationEngine.getLastLocation(locationEngineCallback)
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
        locationEngine.removeLocationUpdates(locationEngineCallback)
    }

    private fun reset() {
        rawLocation = null
        enhancedLocation = null
        routeProgress = null
        isOffRoute = false
        updateNavigatorStatusDataJobs.clear()
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
        routeProgress?.let {
            checkBannerInstructionEvent(it) { bannerInstruction ->
                bannerInstructionsObserver.onNewBannerInstructions(bannerInstruction)
            }
        }
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
     * Unregister [BannerInstructionsObserver]
     */
    override fun unregisterBannerInstructionsObserver(
        bannerInstructionsObserver: BannerInstructionsObserver
    ) {
        bannerInstructionsObservers.remove(bannerInstructionsObserver)
    }

    /**
     * Register [VoiceInstructionsObserver]
     */
    override fun registerVoiceInstructionsObserver(
        voiceInstructionsObserver: VoiceInstructionsObserver
    ) {
        voiceInstructionsObservers.add(voiceInstructionsObserver)
        routeProgress?.let {
            checkVoiceInstructionEvent(it) { voiceInstruction ->
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
        return navigator.updateLegIndex(legIndex)
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
        this.rawLocation = rawLocation
        locationObservers.forEach { it.onRawLocationChanged(rawLocation) }
        mainJobController.scope.launch {
            navigator.updateLocation(rawLocation)
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
            val status = getNavigatorStatus()
            if (!isActive) {
                return@launch
            }
            updateEnhancedLocation(status.enhancedLocation, status.keyPoints)
            if (!isActive) {
                return@launch
            }
            updateRouteProgress(status.routeProgress)
            if (!isActive) {
                return@launch
            }
            isOffRoute = status.offRoute
        }
        updateNavigatorStatusDataJob.invokeOnCompletion {
            updateNavigatorStatusDataJobs.remove(updateNavigatorStatusDataJob)
        }
        updateNavigatorStatusDataJobs.add(updateNavigatorStatusDataJob)
    }

    private suspend fun getNavigatorStatus(): TripStatus {
        return navigator.getStatus(navigatorPredictionMillis)
    }

    private fun updateEnhancedLocation(location: Location, keyPoints: List<Location>) {
        enhancedLocation = location
        locationObservers.forEach { it.onEnhancedLocationChanged(location, keyPoints) }
    }

    private fun updateRouteProgress(progress: RouteProgress?) {
        routeProgress = progress
        tripService.updateNotification(progress)
        progress?.let {
            routeProgressObservers.forEach { it.onRouteProgressChanged(progress) }
            checkBannerInstructionEvent(progress) { bannerInstruction ->
                bannerInstructionsObservers.forEach {
                    it.onNewBannerInstructions(bannerInstruction)
                }
            }
            checkVoiceInstructionEvent(progress) { voiceInstruction ->
                voiceInstructionsObservers.forEach {
                    it.onNewVoiceInstructions(voiceInstruction)
                }
            }
        }
    }

    private fun checkBannerInstructionEvent(
        progress: RouteProgress,
        action: (BannerInstructions) -> Unit
    ) {
        if (bannerInstructionEvent.isOccurring(progress)) {
            ifNonNull(bannerInstructionEvent.bannerInstructions) { bannerInstructions ->
                val bannerView = bannerInstructions.view()
                val bannerComponents = bannerView?.components()
                ifNonNull(bannerComponents) { components ->
                    components.forEachIndexed { index, component ->
                        component.takeIf { it.type() == BannerComponents.GUIDANCE_VIEW }?.let { c ->
                            components[index] =
                                c.toBuilder()
                                    .imageUrl(c.imageUrl()?.plus("&access_token=$accessToken"))
                                    .build()
                        }
                    }
                }
                action(bannerInstructions)
            }
        }
    }

    private fun checkVoiceInstructionEvent(
        progress: RouteProgress,
        action: (VoiceInstructions) -> Unit
    ) {
        if (voiceInstructionEvent.isOccurring(progress)) {
            action(voiceInstructionEvent.voiceInstructions)
        }
    }
}
