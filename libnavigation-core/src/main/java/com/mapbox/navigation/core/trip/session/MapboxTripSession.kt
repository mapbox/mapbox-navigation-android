package com.mapbox.navigation.core.trip.session

import android.location.Location
import android.util.Log
import androidx.annotation.VisibleForTesting
import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.api.directions.v5.models.VoiceInstructions
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.internal.factory.RoadFactory
import com.mapbox.navigation.base.internal.factory.TripNotificationStateFactory.buildTripNotificationState
import com.mapbox.navigation.base.internal.route.RouteCompatibilityCache
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.trip.model.RouteLegProgress
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.model.roadobject.UpcomingRoadObject
import com.mapbox.navigation.core.directions.session.RoutesExtra
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.directions.session.RoutesUpdatedResult
import com.mapbox.navigation.core.navigator.getLocationMatcherResult
import com.mapbox.navigation.core.navigator.getRouteInitInfo
import com.mapbox.navigation.core.navigator.getRouteProgressFrom
import com.mapbox.navigation.core.navigator.getTripStatusFrom
import com.mapbox.navigation.core.navigator.mapToDirectionsApi
import com.mapbox.navigation.core.navigator.toFixLocation
import com.mapbox.navigation.core.navigator.toLocation
import com.mapbox.navigation.core.navigator.toLocations
import com.mapbox.navigation.core.trip.service.TripService
import com.mapbox.navigation.core.trip.session.eh.EHorizonObserver
import com.mapbox.navigation.core.trip.session.eh.EHorizonSubscriptionManager
import com.mapbox.navigation.navigator.internal.MapboxNativeNavigator
import com.mapbox.navigation.navigator.internal.MapboxNativeNavigatorImpl
import com.mapbox.navigation.navigator.internal.TripStatus
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
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import java.util.concurrent.CopyOnWriteArraySet
import kotlin.math.max

internal class MapboxTripSession(
    val tripService: TripService,
    private val tripSessionLocationEngine: TripSessionLocationEngine,
    private val navigator: MapboxNativeNavigator = MapboxNativeNavigatorImpl,
    private val threadController: ThreadController,
    private val eHorizonSubscriptionManager: EHorizonSubscriptionManager
) {

    private val routesObservers = CopyOnWriteArraySet<RoutesObserver>()

    var routes: List<NavigationRoute> = emptyList()
        private set

    private var routesUpdateReason: String = RoutesExtra.ROUTES_UPDATE_REASON_CLEAN_UP

    var initialLegIndex = 0
        private set

    private var updatePrimaryRouteJob: Job? = null
    private var updateLegIndexJob: Job? = null
    private var updateRouteProgressJob: Job? = null

    @VisibleForTesting
    internal var primaryRoute: NavigationRoute? = null

    private companion object {
        private const val LOG_CATEGORY = "MapboxTripSession"
        private const val INDEX_OF_INITIAL_LEG_TARGET = 1
    }

    suspend fun setRoutes(
        routes: List<NavigationRoute>,
        legIndex: Int = 0,
        @RoutesExtra.RoutesUpdateReason reason: String,
        onFinished: (() -> Unit)? = null,
    ) {
        if (this.routes.isEmpty() && routes.isEmpty()) {
            return
        }
        Log.e("lp_test", "setRoutes")
        val updateJobs: List<Job> = when (reason) {
            RoutesExtra.ROUTES_UPDATE_REASON_CLEAN_UP,
            RoutesExtra.ROUTES_UPDATE_REASON_NEW,
            RoutesExtra.ROUTES_UPDATE_REASON_REROUTE -> {
                isOffRoute = false
                invalidateLatestInstructions()
                roadObjects = emptyList()
                routeProgress = null
                updateLegIndexJob?.cancel()
                updateRouteProgressJob?.cancel()

                val updateRouteJob = threadController.getMainScopeAndRootJob().scope.launch(
                    Dispatchers.Main.immediate
                ) {
                    val newPrimaryRoute = routes.firstOrNull()
                    navigator.setPrimaryRoute(
                        if (newPrimaryRoute != null) {
                            Pair(newPrimaryRoute, legIndex)
                        } else {
                            null
                        }
                    )?.let {
                        roadObjects = getRouteInitInfo(it)?.roadObjects ?: emptyList()
                    }
                    this@MapboxTripSession.primaryRoute = newPrimaryRoute
                }
                updatePrimaryRouteJob = updateRouteJob
                val updateAlternativesJob =
                    threadController.getMainScopeAndRootJob().scope.launch(Dispatchers.Main.immediate) {
                        Log.e("lp_test", "starting alternatives update")
                        navigator.setAlternativeRoutes(routes.drop(1))
                        Log.e("lp_test", "finished alternatives update")
                    }

                listOf(
                    updateRouteJob,
                    updateAlternativesJob
                )
            }
            RoutesExtra.ROUTES_UPDATE_REASON_ALTERNATIVE -> {
                listOf(
                    threadController.getMainScopeAndRootJob().scope.launch(Dispatchers.Main.immediate) {
                        navigator.setAlternativeRoutes(routes.drop(1))
                    }
                )
            }
            RoutesExtra.ROUTES_UPDATE_REASON_REFRESH -> {
                listOf(
                    threadController.getMainScopeAndRootJob().scope.launch(Dispatchers.Main.immediate) {
                        if (routes.isNotEmpty()) {
                            navigator.updateAnnotations(routes.first())
                            this@MapboxTripSession.primaryRoute = routes.first()
                        } else {
                            logW("Cannot refresh route. Route can't be null", LOG_CATEGORY)
                        }
                    }
                )
            }
            else -> throw IllegalArgumentException("Unsupported route update reason: $reason")
        }

        updateJobs.forEach {
            it.join()
        }

        Log.e("lp_test", "finished setRoutes")
        this@MapboxTripSession.initialLegIndex = initialLegIndex
        RouteCompatibilityCache.setDirectionsSessionResult(routes)
        this@MapboxTripSession.routes = routes
        this@MapboxTripSession.routesUpdateReason = reason
        routesObservers.forEach {
            it.onRoutesChanged(
                RoutesUpdatedResult(
                    routes,
                    routesUpdateReason
                )
            )
        }
        onFinished?.invoke()
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
    private var lastVoiceInstruction: VoiceInstructions? = null

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
    var zLevel: Int? = null
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

    var locationMatcherResult: LocationMatcherResult? = null
        private set

    private val nativeFallbackVersionsObserver =
        object : FallbackVersionsObserver {
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
    fun getRawLocation() = rawLocation

    /**
     * Provide route progress
     */
    fun getRouteProgress() = routeProgress

    /**
     * Current [MapboxTripSession] state
     */
    fun getState(): TripSessionState = state

    /**
     * Start MapboxTripSession
     */
    fun start(withTripService: Boolean, withReplayEnabled: Boolean) {
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
        this.rawLocation = rawLocation
        locationObservers.forEach { it.onNewRawLocation(rawLocation) }
        mainJobController.scope.launch(start = CoroutineStart.UNDISPATCHED) {
            navigator.updateLocation(rawLocation.toFixLocation())
        }
    }

    /**
     * Returns if the MapboxTripSession is running a foreground service
     */
    fun isRunningWithForegroundService(): Boolean {
        return tripService.hasServiceStarted()
    }

    @OptIn(ExperimentalMapboxNavigationAPI::class)
    private val navigatorObserver = object : NavigatorObserver {
        override fun onStatus(origin: NavigationStatusOrigin, status: NavigationStatus) {
            val tripStatus = status.getTripStatusFrom(primaryRoute)
            val enhancedLocation = tripStatus.navigationStatus.location.toLocation()
            val keyPoints = tripStatus.navigationStatus.keyPoints.toLocations()
            val road = RoadFactory.buildRoadObject(tripStatus.navigationStatus)
            updateLocationMatcherResult(
                tripStatus.getLocationMatcherResult(enhancedLocation, keyPoints, road)
            )
            zLevel = status.layer

            // we should skip RouteProgress, BannerInstructions, isOffRoute state updates while
            // setting a new route
            if (updatePrimaryRouteJob?.isActive == true) {
                return
            }

            updateRouteProgressJob?.cancel()
            updateRouteProgressJob = mainJobController.scope.launch(Dispatchers.Main.immediate) {
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
                val remainingWaypoints = calculateRemainingWaypoints(tripStatus)
                val routeProgress = getRouteProgressFrom(
                    tripStatus.route,
                    tripStatus.navigationStatus,
                    remainingWaypoints,
                    bannerInstructionEvent.latestBannerInstructions,
                    bannerInstructionEvent.latestInstructionIndex,
                    lastVoiceInstruction
                )
                updateRouteProgress(routeProgress, triggerObserver)
                triggerVoiceInstructionEvent(routeProgress, status)
                isOffRoute = tripStatus.navigationStatus.routeState == RouteState.OFF_ROUTE
            }
        }

        private fun calculateRemainingWaypoints(tripStatus: TripStatus): Int {
            val routeCoordinates = tripStatus.route?.routeOptions?.coordinatesList()
            return if (routeCoordinates != null) {
                val waypointsCount = routeCoordinates.size
                val nextWaypointIndex = normalizeNextWaypointIndex(
                    tripStatus.navigationStatus.nextWaypointIndex
                )
                return waypointsCount - nextWaypointIndex
            } else 0
        }

        /**
         * On the Android side, we always start navigation from the current position.
         * So we expect that the next waypoint index will not be less than 1.
         * But the native part considers the origin as a usual waypoint.
         * It can return the next waypoint index 0. Be careful, this case isn't easy to reproduce.
         *
         * For example, nextWaypointIndex=0 leads to an incorrect rerouting.
         * We don't want to get to an initial position even it hasn't been reached yet.
         */
        private fun normalizeNextWaypointIndex(nextWaypointIndex: Int) = max(
            INDEX_OF_INITIAL_LEG_TARGET,
            nextWaypointIndex
        )
    }

    /**
     * Stop MapboxTripSession
     */
    fun stop() {
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
     * Registers [RoutesObserver]. Updated on each change of [routes]
     */
    fun registerRoutesObserver(routesObserver: RoutesObserver) {
        routesObservers.add(routesObserver)
        if (routes.isNotEmpty()) {
            routesObserver.onRoutesChanged(RoutesUpdatedResult(routes, routesUpdateReason))
        }
    }

    /**
     * Unregisters [RoutesObserver]
     */
    fun unregisterRoutesObserver(routesObserver: RoutesObserver) {
        routesObservers.remove(routesObserver)
    }

    /**
     * Unregisters all [RoutesObserver]
     */
    fun unregisterAllRoutesObservers() {
        routesObservers.clear()
    }

    /**
     * Register [LocationObserver] to receive location updates
     */
    fun registerLocationObserver(locationObserver: LocationObserver) {
        locationObservers.add(locationObserver)
        rawLocation?.let { locationObserver.onNewRawLocation(it) }
        locationMatcherResult?.let { locationObserver.onNewLocationMatcherResult(it) }
    }

    /**
     * Unregister [LocationObserver]
     */
    fun unregisterLocationObserver(locationObserver: LocationObserver) {
        locationObservers.remove(locationObserver)
    }

    /**
     * Unregister all [LocationObserver]
     *
     * @see [registerLocationObserver]
     */
    fun unregisterAllLocationObservers() {
        locationObservers.clear()
    }

    /**
     * Register [RouteProgressObserver] to receive information about routing's state
     * like [BannerInstructions], [RouteLegProgress], etc.
     *
     * @see [RouteProgress]
     */
    fun registerRouteProgressObserver(routeProgressObserver: RouteProgressObserver) {
        routeProgressObservers.add(routeProgressObserver)
        routeProgress?.let { routeProgressObserver.onRouteProgressChanged(it) }
    }

    /**
     * Unregister [RouteProgressObserver]
     */
    fun unregisterRouteProgressObserver(routeProgressObserver: RouteProgressObserver) {
        routeProgressObservers.remove(routeProgressObserver)
    }

    /**
     * Unregister all [RouteProgressObserver]
     *
     * @see [registerRouteProgressObserver]
     */
    fun unregisterAllRouteProgressObservers() {
        routeProgressObservers.clear()
    }

    /**
     * Register [OffRouteObserver] to receive notification about off-route events
     */
    fun registerOffRouteObserver(offRouteObserver: OffRouteObserver) {
        offRouteObservers.add(offRouteObserver)
        offRouteObserver.onOffRouteStateChanged(isOffRoute)
    }

    /**
     * Unregister [OffRouteObserver]
     */
    fun unregisterOffRouteObserver(offRouteObserver: OffRouteObserver) {
        offRouteObservers.remove(offRouteObserver)
    }

    /**
     * Unregister all [OffRouteObserver]
     *
     * @see [registerOffRouteObserver]
     */
    fun unregisterAllOffRouteObservers() {
        offRouteObservers.clear()
    }

    /**
     * Register [TripSessionStateObserver] to receive current TripSession's state
     *
     * @see [TripSessionState]
     */
    fun registerStateObserver(stateObserver: TripSessionStateObserver) {
        stateObservers.add(stateObserver)
        stateObserver.onSessionStateChanged(state)
    }

    /**
     * Unregister [TripSessionStateObserver]
     */
    fun unregisterStateObserver(stateObserver: TripSessionStateObserver) {
        stateObservers.remove(stateObserver)
    }

    /**
     * Unregister all [TripSessionStateObserver]
     *
     * @see [registerStateObserver]
     */
    fun unregisterAllStateObservers() {
        stateObservers.clear()
    }

    /**
     * Register [BannerInstructionsObserver]
     */
    fun registerBannerInstructionsObserver(
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
    fun unregisterBannerInstructionsObserver(
        bannerInstructionsObserver: BannerInstructionsObserver
    ) {
        bannerInstructionsObservers.remove(bannerInstructionsObserver)
    }

    /**
     * Unregister all [BannerInstructionsObserver]
     *
     * @see [registerBannerInstructionsObserver]
     */
    fun unregisterAllBannerInstructionsObservers() {
        bannerInstructionsObservers.clear()
    }

    /**
     * Register [VoiceInstructionsObserver]
     */
    fun registerVoiceInstructionsObserver(
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
    fun unregisterVoiceInstructionsObserver(
        voiceInstructionsObserver: VoiceInstructionsObserver
    ) {
        voiceInstructionsObservers.remove(voiceInstructionsObserver)
    }

    /**
     * Unregister all [VoiceInstructionsObserver]
     *
     * @see [registerVoiceInstructionsObserver]
     */
    fun unregisterAllVoiceInstructionsObservers() {
        voiceInstructionsObservers.clear()
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
    fun updateLegIndex(legIndex: Int, callback: LegIndexUpdatedCallback) {
        var legIndexUpdated = false
        updateLegIndexJob = mainJobController.scope.launch {
            try {
                legIndexUpdated = navigator.updateLegIndex(legIndex)
                if (legIndexUpdated) {
                    invalidateLatestInstructions()
                }
            } finally {
                callback.onLegIndexUpdatedCallback(legIndexUpdated)
            }
        }
    }

    fun registerRoadObjectsOnRouteObserver(
        roadObjectsOnRouteObserver: RoadObjectsOnRouteObserver
    ) {
        roadObjectsOnRouteObservers.add(roadObjectsOnRouteObserver)
        roadObjectsOnRouteObserver.onNewRoadObjectsOnTheRoute(roadObjects)
    }

    fun unregisterRoadObjectsOnRouteObserver(
        roadObjectsOnRouteObserver: RoadObjectsOnRouteObserver
    ) {
        roadObjectsOnRouteObservers.remove(roadObjectsOnRouteObserver)
    }

    fun unregisterAllRoadObjectsOnRouteObservers() {
        roadObjectsOnRouteObservers.clear()
    }

    fun registerEHorizonObserver(eHorizonObserver: EHorizonObserver) {
        eHorizonSubscriptionManager.registerObserver(eHorizonObserver)
    }

    fun unregisterEHorizonObserver(eHorizonObserver: EHorizonObserver) {
        eHorizonSubscriptionManager.unregisterObserver(eHorizonObserver)
    }

    fun unregisterAllEHorizonObservers() {
        eHorizonSubscriptionManager.unregisterAllObservers()
    }

    fun registerFallbackVersionsObserver(
        fallbackVersionsObserver: FallbackVersionsObserver
    ) {
        if (fallbackVersionsObservers.isEmpty()) {
            navigator.setFallbackVersionsObserver(nativeFallbackVersionsObserver)
        }
        fallbackVersionsObservers.add(fallbackVersionsObserver)
    }

    fun unregisterFallbackVersionsObserver(
        fallbackVersionsObserver: FallbackVersionsObserver
    ) {
        fallbackVersionsObservers.remove(fallbackVersionsObserver)
        if (fallbackVersionsObservers.isEmpty()) {
            navigator.setFallbackVersionsObserver(null)
        }
    }

    fun unregisterAllFallbackVersionsObservers() {
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
        }
    }

    private fun triggerVoiceInstructionEvent(progress: RouteProgress?, status: NavigationStatus) {
        val voiceInstructions = progress?.voiceInstructions
        val navigatorTriggeredNewInstruction = status.voiceInstruction != null
        if (voiceInstructions != null && navigatorTriggeredNewInstruction) {
            voiceInstructionsObservers.forEach {
                it.onNewVoiceInstructions(voiceInstructions)
            }
            lastVoiceInstruction = progress.voiceInstructions
        }
    }

    private fun checkLatestValidBannerInstructionEvent(
        action: (BannerInstructions) -> Unit
    ) {
        ifNonNull(bannerInstructionEvent.latestBannerInstructions) {
            action(it)
        }
    }

    private fun invalidateLatestInstructions() {
        bannerInstructionEvent.invalidateLatestBannerInstructions()
        lastVoiceInstruction = null
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
