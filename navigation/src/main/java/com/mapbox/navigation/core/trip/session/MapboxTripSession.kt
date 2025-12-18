package com.mapbox.navigation.core.trip.session

import androidx.annotation.MainThread
import androidx.annotation.VisibleForTesting
import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.api.directions.v5.models.VoiceInstructions
import com.mapbox.bindgen.Expected
import com.mapbox.common.location.Location
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.internal.factory.RoadFactory
import com.mapbox.navigation.base.internal.factory.RoadObjectFactory.getUpdatedObjectsAhead
import com.mapbox.navigation.base.internal.factory.TripNotificationStateFactory.buildTripNotificationState
import com.mapbox.navigation.base.internal.performance.PerformanceTracker
import com.mapbox.navigation.base.internal.route.refreshNativePeer
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.trip.model.RouteLegProgress
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.model.roadobject.UpcomingRoadObject
import com.mapbox.navigation.core.SetRoutes
import com.mapbox.navigation.core.directions.session.DirectionsSession
import com.mapbox.navigation.core.internal.utils.initialLegIndex
import com.mapbox.navigation.core.internal.utils.mapToReason
import com.mapbox.navigation.core.navigator.getCurrentBannerInstructions
import com.mapbox.navigation.core.navigator.getLocationMatcherResult
import com.mapbox.navigation.core.navigator.getRouteProgressFrom
import com.mapbox.navigation.core.navigator.getTripStatusFrom
import com.mapbox.navigation.core.navigator.toFixLocation
import com.mapbox.navigation.core.navigator.toLocation
import com.mapbox.navigation.core.navigator.toLocations
import com.mapbox.navigation.core.reroute.RerouteController
import com.mapbox.navigation.core.reroute.RerouteController.RerouteStateObserver
import com.mapbox.navigation.core.reroute.RerouteState
import com.mapbox.navigation.core.trip.service.TripService
import com.mapbox.navigation.core.trip.session.eh.EHorizonObserver
import com.mapbox.navigation.core.trip.session.eh.EHorizonSubscriptionManager
import com.mapbox.navigation.navigator.internal.MapboxNativeNavigator
import com.mapbox.navigation.navigator.internal.TripStatus
import com.mapbox.navigation.navigator.internal.utils.calculateRemainingWaypoints
import com.mapbox.navigation.navigator.internal.utils.getCurrentLegDestination
import com.mapbox.navigation.utils.internal.JobControl
import com.mapbox.navigation.utils.internal.ThreadController
import com.mapbox.navigation.utils.internal.ifNonNull
import com.mapbox.navigation.utils.internal.logD
import com.mapbox.navigation.utils.internal.logE
import com.mapbox.navigation.utils.internal.logI
import com.mapbox.navigation.utils.internal.logW
import com.mapbox.navigator.FallbackVersionsObserver
import com.mapbox.navigator.NavigationStatus
import com.mapbox.navigator.NavigationStatusOrigin
import com.mapbox.navigator.NavigatorObserver
import com.mapbox.navigator.RouteAlternative
import com.mapbox.navigator.RouteState
import com.mapbox.navigator.RoutesChangeInfo
import com.mapbox.navigator.SetRoutesReason
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.TimeSource
import kotlin.time.minus

/**
 * Default implementation of [TripSession]
 *
 * @param tripService TripService
 * @param directionsSession Directions session
 * @param tripSessionLocationEngine the location engine
 * @param navigator Native navigator
 * @param threadController controller for main/io jobs
 * @param repeatRerouteAfterOffRouteDelaySeconds max delay in seconds before repeating reroute after
 * off-route event (when RerouteState.FetchingRoute is not invoked after off-route event)
 */
@MainThread
internal class MapboxTripSession(
    override val tripService: TripService,
    private val directionsSession: DirectionsSession,
    private val tripSessionLocationEngine: TripSessionLocationEngine,
    private val navigator: MapboxNativeNavigator,
    private val threadController: ThreadController,
    private val eHorizonSubscriptionManager: EHorizonSubscriptionManager,
    private val repeatRerouteAfterOffRouteDelaySeconds: Int = -1,
) : TripSession {

    private companion object {
        private const val LOG_CATEGORY = "MapboxTripSession"
    }

    private var updateLegIndexJob: Job? = null

    @VisibleForTesting
    internal val isUpdatingRoute = AtomicBoolean(false)

    @VisibleForTesting
    internal var primaryRoute: NavigationRoute? = null

    init {
        registerSetRoutesObservers()
    }

    override suspend fun setRoutes(
        routes: List<NavigationRoute>,
        setRoutes: SetRoutes,
    ): NativeSetRouteResult {
        fun logMessage(suffix: String): () -> String = {
            "routes update (reason: ${setRoutes.mapToReason()}, " +
                "route IDs: ${routes.map { it.id }}) $suffix"
        }
        logI(LOG_CATEGORY, logMessage("- starting"))

        val result = when (setRoutes) {
            is SetRoutes.CleanUp -> {
                setRouteToNativeNavigator(
                    routes,
                    setRoutes.initialLegIndex(),
                    SetRoutesReason.CLEAN_UP,
                )
            }
            is SetRoutes.NewRoutes -> {
                setRouteToNativeNavigator(
                    routes,
                    setRoutes.initialLegIndex(),
                    SetRoutesReason.NEW_ROUTE,
                )
            }
            is SetRoutes.Reroute -> {
                setRouteToNativeNavigator(
                    routes,
                    setRoutes.initialLegIndex(),
                    SetRoutesReason.REROUTE,
                )
            }
            is SetRoutes.Alternatives -> {
                NativeSetRouteValue(
                    routes = routes,
                    nativeAlternatives = navigator.setAlternativeRoutes(routes.drop(1)),
                )
            }
            is SetRoutes.Reorder -> {
                setRouteToNativeNavigator(
                    routes,
                    setRoutes.initialLegIndex(),
                    SetRoutesReason.ALTERNATIVE,
                )
            }
            is SetRoutes.RefreshRoutes -> {
                if (routes.isNotEmpty()) {
                    val primaryRoute = routes.first()
                    lateinit var refreshRouteResult: Expected<String, List<RouteAlternative>>
                    var lastSavedResultValue: Expected<String, List<RouteAlternative>>? = null
                    // primary route must be refreshed at the very end,
                    // because after it is refreshed,
                    // statuses that correspond to the refreshed route will start coming
                    for (route in routes.drop(1) + primaryRoute) {
                        refreshRouteResult = navigator.refreshRoute(route)
                        if (refreshRouteResult.isValue) {
                            lastSavedResultValue = refreshRouteResult
                        }
                    }
                    // The latest result contains the most actual cumulated data.
                    // TODO API change request NN-110
                    (lastSavedResultValue ?: refreshRouteResult).fold(
                        { NativeSetRouteError(it) },
                        { value ->
                            val refreshedPrimaryRoute = primaryRoute.refreshNativePeer()
                            this@MapboxTripSession.primaryRoute = refreshedPrimaryRoute
                            roadObjects = refreshedPrimaryRoute.upcomingRoadObjects
                            val refreshedRoutes = routes
                                .drop(1)
                                .toMutableList().apply {
                                    add(0, refreshedPrimaryRoute)
                                }
                            NativeSetRouteValue(
                                routes = refreshedRoutes,
                                nativeAlternatives = value,
                            )
                        },
                    ).also {
                        logD(
                            "routes update (route IDs: ${routes.map { it.id }}) - refresh finished",
                            LOG_CATEGORY,
                        )
                    }
                } else {
                    with("Cannot refresh route. Route can't be null") {
                        logW(this, LOG_CATEGORY)
                        NativeSetRouteError(this)
                    }
                }
            }
        }
        logI(LOG_CATEGORY, logMessage("- finished"))
        return result
    }

    private suspend fun setRouteToNativeNavigator(
        routes: List<NavigationRoute>,
        legIndex: Int,
        reason: SetRoutesReason,
    ): NativeSetRouteResult = updateRouteTransaction {
        logD(
            "native routes update (route IDs: ${routes.map { it.id }}) - starting",
            LOG_CATEGORY,
        )
        val newPrimaryRoute = routes.firstOrNull()
        return@updateRouteTransaction navigator.setRoutes(
            newPrimaryRoute,
            legIndex,
            routes.drop(1),
            reason,
        ).onValue {
            updateLegIndexJob?.cancel()
            this@MapboxTripSession.primaryRoute = newPrimaryRoute
            roadObjects = newPrimaryRoute?.upcomingRoadObjects ?: emptyList()
            isOffRoute = false
            invalidateLatestInstructions(
                bannerInstructionEvent.latestInstructionWrapper,
                lastVoiceInstruction,
            )
            routeProgress = null
        }.mapValue {
            it.alternatives
        }.fold({ NativeSetRouteError(it) }, { NativeSetRouteValue(routes, it) }).also {
            logD(
                "native routes update (route IDs: ${routes.map { it.id }}) - finished",
                LOG_CATEGORY,
            )
        }
    }

    private val mainJobController: JobControl = threadController.getMainScopeAndRootJob()

    private val locationObservers = CopyOnWriteArraySet<LocationObserver>()
    private val routeProgressObservers = CopyOnWriteArraySet<RouteProgressObserver>()
    private val offRouteObservers = CopyOnWriteArraySet<OffRouteObserver>()
    private val stateObservers = CopyOnWriteArraySet<TripSessionStateObserver>()
    private val bannerInstructionsObservers = CopyOnWriteArraySet<BannerInstructionsObserver>()
    private val voiceInstructionsObservers = CopyOnWriteArraySet<VoiceInstructionsObserver>()
    private val fallbackVersionsObservers = CopyOnWriteArraySet<FallbackVersionsObserver>()

    private val bannerInstructionEvent = BannerInstructionEvent()

    @VisibleForTesting
    internal var lastVoiceInstruction: VoiceInstructions? = null

    private var state: TripSessionState = TripSessionState.STOPPED
        set(value) {
            if (field == value) {
                return
            }
            field = value
            stateObservers.forEach { it.onSessionStateChanged(value) }
        }

    override var isOffRoute: Boolean = false
        private set(value) {
            if (field == value) {
                return
            }
            field = value
            offRouteObservers.forEach { it.onOffRouteStateChanged(value) }
        }

    private var offRouteObserverForReroute: OffRouteObserver? = null

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
        }

    override var locationMatcherResult: LocationMatcherResult? = null
        private set

    private var rerouteInvocationHandler: RerouteInvocationHandler? = null

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

    // worker thread
    private val onRawLocationUpdate: (Location) -> Unit = { rawLocation ->
        val locationHash = rawLocation.hashCode()
        mainJobController.scope.launch {
            this@MapboxTripSession.rawLocation = rawLocation
            locationObservers.forEach { it.onNewRawLocation(rawLocation) }
        }
        mainJobController.scope.launch(start = CoroutineStart.UNDISPATCHED) {
            val monotonicStart = System.nanoTime()
            navigator.updateLocation(rawLocation.toFixLocation())
            val diffMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - monotonicStart)
            logD(
                "updateRawLocation; system elapsed time: ${System.nanoTime()}; " +
                    "location ($locationHash) elapsed time: ${rawLocation.monotonicTimestamp}," +
                    "notify NN for $diffMillis ms",
                LOG_CATEGORY,
            )
        }
    }

    /**
     * Helper class that handles the reroute invocation logic based on the off-route state changes.
     * It uses the [RerouteController] to monitor the reroute state and ensure that reroutes are
     * triggered appropriately when the user goes off-route.
     *
     * Rely on the fact that all triggerRerouteIfRequired() should be called from the same thread.
     * It allows to build state machine based on variables without additional locks.
     */
    private class RerouteInvocationHandler(
        val tripSession: MapboxTripSession,
        val rerouteController: RerouteController,
        val repeatRerouteAfterOffRouteDelaySeconds: Int,
    ) {

        private companion object {
            private const val LOG_CATEGORY = "RerouteInvocationHandler"
        }

        private var rerouteInvoked = false

        @OptIn(ExperimentalTime::class)
        private var startTimeMark = TimeSource.Monotonic.markNow()

        private val rerouteStateObserver = RerouteStateObserver {
            if (it == RerouteState.FetchingRoute) {
                rerouteInvoked = true
            }
        }

        fun triggerRerouteIfRequired(tripStatus: TripStatus) {
            if (repeatRerouteAfterOffRouteDelaySeconds == -1) {
                if (tripSession.isOffRoute != tripStatus.isOffRoute) {
                    tripSession.offRouteObserverForReroute?.onOffRouteStateChanged(
                        tripStatus.isOffRoute,
                    )
                }
            } else {
                when {
                    // Send signal via callback to MapboxNavigation for the reroute logic
                    !tripSession.isOffRoute && tripStatus.isOffRoute -> {
                        logI("Trigger off-route observer for re-route", LOG_CATEGORY)
                        resetState()
                        rerouteController.registerRerouteStateObserver(rerouteStateObserver)
                        tripSession.offRouteObserverForReroute?.onOffRouteStateChanged(true)
                    }

                    // Checks if the reroute was invoked the last time the user was off route
                    tripSession.isOffRoute && tripStatus.isOffRoute -> {
                        if (rerouteInvoked) {
                            logI(
                                "Re-route was invoked (catch RerouteState.FetchingRoute)",
                                LOG_CATEGORY,
                            )
                            rerouteController.unregisterRerouteStateObserver(rerouteStateObserver)
                            resetState()
                        } else {
                            if (exceedDelay()) {
                                logI(
                                    "Re-route wasn't invoked " +
                                        "(missing RerouteState.FetchingRoute), " +
                                        "repeating off-route observer call for re-route",
                                    LOG_CATEGORY,
                                )
                                resetState()
                                tripSession.offRouteObserverForReroute?.onOffRouteStateChanged(true)
                            }
                        }
                    }

                    // Resets the state
                    !tripSession.isOffRoute -> {
                        resetState()
                        rerouteController.unregisterRerouteStateObserver(rerouteStateObserver)
                    }
                }
            }
        }

        @OptIn(ExperimentalTime::class)
        private fun exceedDelay() =
            startTimeMark.elapsedNow() >= repeatRerouteAfterOffRouteDelaySeconds.seconds

        @OptIn(ExperimentalTime::class)
        private fun resetState() {
            rerouteInvoked = false
            startTimeMark = TimeSource.Monotonic.markNow()
        }

        fun finish() {
            rerouteController.unregisterRerouteStateObserver(rerouteStateObserver)
        }
    }

    init {
        navigator.addNativeNavigatorRecreationObserver {
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
        if (state != TripSessionState.STARTED) {
            logI(LOG_CATEGORY) { "Start trip session, replay enabled: $withReplayEnabled" }
            navigator.addNavigatorObserver(navigatorObserver)
            navigator.startNavigationSession()
            if (withTripService) {
                tripService.startService()
            }
            state = TripSessionState.STARTED
        }
        tripSessionLocationEngine.startLocationUpdates(withReplayEnabled, onRawLocationUpdate)
    }

    /**
     * Returns if the MapboxTripSession is running a foreground service
     */
    override fun isRunningWithForegroundService(): Boolean {
        return tripService.hasServiceStarted()
    }

    private val navigatorObserver = object : NavigatorObserver {
        override fun onStatus(origin: NavigationStatusOrigin, status: NavigationStatus) {
            try {
                PerformanceTracker.trackPerformanceSync("NavigatorObserver#onStatus") {
                    processNativeStatus(status)
                }
            } catch (error: Throwable) {
                logE(LOG_CATEGORY) {
                    "Error processing native status update: origin=$origin, status=$status.\n" +
                        "Error: $error\n" +
                        "MapboxTripSession state: " +
                        "isUpdatingRoute=${isUpdatingRoute.get()}, primaryRoute=${primaryRoute?.id}"
                }
                throw NativeStatusProcessingError(error)
            }
        }

        override fun onRoutesChanged(info: RoutesChangeInfo) {
            // no-op NAVAND-5180
        }
    }

    /**
     * Stop MapboxTripSession
     */
    override fun stop() {
        if (state == TripSessionState.STOPPED) {
            return
        }
        logI(LOG_CATEGORY) { "Stop trip session" }
        navigator.stopNavigationSession()
        navigator.removeNavigatorObserver(navigatorObserver)
        tripService.stopService()
        tripSessionLocationEngine.stopLocationUpdates()
        mainJobController.job.cancelChildren()
        reset()
        state = TripSessionState.STOPPED
    }

    private fun reset() {
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
        bannerInstructionsObserver: BannerInstructionsObserver,
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
        bannerInstructionsObserver: BannerInstructionsObserver,
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
        voiceInstructionsObserver: VoiceInstructionsObserver,
    ) {
        voiceInstructionsObservers.add(voiceInstructionsObserver)
    }

    /**
     * Unregister [VoiceInstructionsObserver]
     */
    override fun unregisterVoiceInstructionsObserver(
        voiceInstructionsObserver: VoiceInstructionsObserver,
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
                fun msg(state: String, append: String = ""): () -> String = {
                    "update to new leg $state. Leg index: $legIndex, route id: " +
                        "${primaryRoute?.id} + $append"
                }
                logD(LOG_CATEGORY, msg("started"))
                val latestInstructionWrapper = bannerInstructionEvent.latestInstructionWrapper
                val lastVoiceInstruction = lastVoiceInstruction
                legIndexUpdated = navigator.updateLegIndex(legIndex)
                if (legIndexUpdated) {
                    invalidateLatestInstructions(latestInstructionWrapper, lastVoiceInstruction)
                }
                logD(
                    LOG_CATEGORY,
                    msg(
                        "finished",
                        "(is leg updated: $legIndexUpdated; " +
                            "latestInstructionWrapper: [$latestInstructionWrapper]; " +
                            "lastVoiceInstruction: [$lastVoiceInstruction])",
                    ),
                )
            } finally {
                callback.onLegIndexUpdatedCallback(legIndexUpdated)
            }
        }
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
        fallbackVersionsObserver: FallbackVersionsObserver,
    ) {
        if (fallbackVersionsObservers.isEmpty()) {
            navigator.setFallbackVersionsObserver(nativeFallbackVersionsObserver)
        }
        fallbackVersionsObservers.add(fallbackVersionsObserver)
    }

    override fun unregisterFallbackVersionsObserver(
        fallbackVersionsObserver: FallbackVersionsObserver,
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

    override fun setOffRouteObserverForReroute(
        offRouteObserver: OffRouteObserver,
        rerouteController: RerouteController,
    ) {
        offRouteObserverForReroute = offRouteObserver
        rerouteInvocationHandler = RerouteInvocationHandler(
            this,
            rerouteController,
            repeatRerouteAfterOffRouteDelaySeconds,
        )
    }

    override fun resetOffRouteObserverForReroute() {
        offRouteObserverForReroute = null
        rerouteInvocationHandler?.finish()
        rerouteInvocationHandler = null
    }

    private fun updateLocationMatcherResult(locationMatcherResult: LocationMatcherResult) {
        PerformanceTracker.trackPerformanceSync("MapboxTripSession#updateLocationMatcherResult") {
            this.locationMatcherResult = locationMatcherResult
            locationObservers.forEach { it.onNewLocationMatcherResult(locationMatcherResult) }
        }
    }

    @OptIn(ExperimentalMapboxNavigationAPI::class)
    private fun processNativeStatus(status: NavigationStatus) {
        logD(LOG_CATEGORY) {
            "navigatorObserver#onStatus; " +
                "fixLocation elapsed time: ${status.location.monotonicTimestampNanoseconds}, " +
                "state: ${status.routeState};" +
                "instructions: " +
                "banner idx [${status.bannerInstruction?.index}], " +
                "voice idx [${status.voiceInstruction?.index}]"
        }

        val tripStatus = status.getTripStatusFrom(primaryRoute)
        val locationMatcherResult = PerformanceTracker.trackPerformanceSync(
            "MapboxTripSession#processNativeStatus-prepare-location-matcher-result",
        ) {
            val enhancedLocation = tripStatus.navigationStatus.location.toLocation()
            val keyPoints = tripStatus.navigationStatus.keyPoints.toLocations()
            val road = RoadFactory.buildRoadObject(tripStatus.navigationStatus)
            tripStatus.getLocationMatcherResult(
                enhancedLocation,
                keyPoints,
                road,
            )
        }
        updateLocationMatcherResult(locationMatcherResult)
        zLevel = status.layer

        // we should skip RouteProgress, BannerInstructions, isOffRoute state updates while
        // setting a new route
        if (isUpdatingRoute.get()) {
            logD("route progress update dropped - updating routes", LOG_CATEGORY)
            return
        }

        var triggerObserver = false
        if (tripStatus.navigationStatus.routeState != RouteState.INVALID) {
            val nativeBannerInstruction = tripStatus.navigationStatus.bannerInstruction
            val bannerInstructions =
                tripStatus.navigationStatus.getCurrentBannerInstructions(primaryRoute)
            triggerObserver = bannerInstructionEvent.isOccurring(
                bannerInstructions,
                nativeBannerInstruction?.index,
            )
        }
        val remainingWaypoints = tripStatus.calculateRemainingWaypoints()
        val latestBannerInstructionsWrapper = bannerInstructionEvent.latestInstructionWrapper
        val upcomingRoadObjects =
            PerformanceTracker.trackPerformanceSync(
                "MapboxTripSession#processNativeStatus-getUpdatedObjectsAhead",
            ) {
                roadObjects.getUpdatedObjectsAhead(
                    tripStatus.navigationStatus.upcomingRouteAlertUpdates,
                )
            }
        val routeProgress =
            PerformanceTracker.trackPerformanceSync(
                "MapboxTripSession#processNativeStatus-create-route-progress",
            ) {
                tripStatus.route?.let {
                    val currentLegDestination = tripStatus.getCurrentLegDestination(it)
                    getRouteProgressFrom(
                        it,
                        tripStatus.navigationStatus,
                        remainingWaypoints,
                        latestBannerInstructionsWrapper?.latestBannerInstructions,
                        latestBannerInstructionsWrapper?.latestInstructionIndex,
                        lastVoiceInstruction,
                        upcomingRoadObjects,
                        currentLegDestination,
                    ).also { routeProgress ->
                        if (routeProgress == null) {
                            logD(
                                "route progress update dropped - " +
                                    "currentPrimaryRoute ID: ${primaryRoute?.id}; " +
                                    "currentState: ${status.routeState}",
                                LOG_CATEGORY,
                            )
                        }
                    }
                }
            }
        updateRouteProgress(routeProgress, triggerObserver)
        triggerVoiceInstructionEvent(routeProgress, status)
        rerouteInvocationHandler?.triggerRerouteIfRequired(tripStatus)
        isOffRoute = tripStatus.isOffRoute
    }

    private fun updateRouteProgress(
        progress: RouteProgress?,
        shouldTriggerBannerInstructionsObserver: Boolean,
    ) {
        routeProgress = progress
        PerformanceTracker.trackPerformanceSync(
            "MapboxTripSession#updateRouteProgress-update-notification",
        ) {
            tripService.updateNotification(buildTripNotificationState(progress))
        }
        progress?.let { progress ->
            logD(
                "dispatching progress update; state: ${progress.currentState}",
                LOG_CATEGORY,
            )
            PerformanceTracker.trackPerformanceSync(
                "MapboxTripSession#updateRouteProgress-dispatch-route-progress-update",
            ) {
                routeProgressObservers.forEach { it.onRouteProgressChanged(progress) }
            }
            if (shouldTriggerBannerInstructionsObserver) {
                checkBannerInstructionEvent { bannerInstruction ->
                    PerformanceTracker.trackPerformanceSync(
                        "MapboxTripSession#updateRouteProgress-dispatch-banner-instruction",
                    ) {
                        bannerInstructionsObservers.forEach {
                            it.onNewBannerInstructions(bannerInstruction)
                        }
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
        action: (BannerInstructions) -> Unit,
    ) {
        ifNonNull(bannerInstructionEvent.latestBannerInstructions) {
            action(it)
        }
    }

    /**
     * Invalidate latest banner and voice instruction. To get the latest banner instruction wrapper call
     * [BannerInstructionEvent.latestInstructionWrapper], to get the latest voice instruction
     * call [lastVoiceInstruction]
     */
    private fun invalidateLatestInstructions(
        latestInstructionWrapper: BannerInstructionEvent.LatestInstructionWrapper?,
        voiceInstruction: VoiceInstructions?,
    ) {
        bannerInstructionEvent.invalidateLatestBannerInstructions(latestInstructionWrapper)
        if (lastVoiceInstruction == voiceInstruction) {
            lastVoiceInstruction = null
        }
    }

    private fun checkBannerInstructionEvent(
        action: (BannerInstructions) -> Unit,
    ) {
        ifNonNull(bannerInstructionEvent.bannerInstructions) { bannerInstructions ->
            action(bannerInstructions)
        }
    }

    private fun checkVoiceInstructionEvent(
        currentVoiceInstructions: VoiceInstructions?,
        action: (VoiceInstructions) -> Unit,
    ) {
        ifNonNull(currentVoiceInstructions) { voiceInstructions ->
            action(voiceInstructions)
        }
    }

    /**
     * Executes a route update transaction, setting the `isUpdatingRoute` flag to true
     * Works alongside with the observers registered in [registerSetRoutesObservers].
     * This ensures that during the execution of the provided function, any incoming
     * route progress updates are ignored, preventing potential race conditions
     */
    private inline fun <T> updateRouteTransaction(func: () -> T): T {
        isUpdatingRoute.set(true)
        return try {
            func()
        } finally {
            isUpdatingRoute.set(false)
        }
    }

    /**
     * Registers an observer to detect an upcoming routes change even before `setRoutes` is called.
     * This helps handle race conditions by setting `isUpdatingRoute` early, allowing the session to
     * ignore route progress updates that may arrive from the navigator during the route update process.
     */
    private fun registerSetRoutesObservers() {
        directionsSession.registerSetNavigationRoutesStartedObserver {
            isUpdatingRoute.set(true)
        }

        directionsSession.registerSetNavigationRoutesFinishedObserver {
            isUpdatingRoute.set(false)
        }
    }
}

private val TripStatus.isOffRoute: Boolean
    get() = navigationStatus.routeState == RouteState.OFF_ROUTE

private class NativeStatusProcessingError(cause: Throwable) :
    Throwable("Error processing native status update", cause)
