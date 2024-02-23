package com.mapbox.navigation.core.telemetry

import android.app.Application
import android.content.Context
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.common.TelemetrySystemUtils.generateCreateDateFormatted
import com.mapbox.common.TurnstileEvent
import com.mapbox.common.UserSKUIdentifier
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.metrics.MetricEvent
import com.mapbox.navigation.base.metrics.MetricsReporter
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.trip.model.RouteLegProgress
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.model.RouteProgressState
import com.mapbox.navigation.core.BuildConfig
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.arrival.ArrivalObserver
import com.mapbox.navigation.core.directions.session.RoutesExtra
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.internal.telemetry.UserFeedback
import com.mapbox.navigation.core.internal.telemetry.UserFeedbackCallback
import com.mapbox.navigation.core.internal.telemetry.toTelemetryLocation
import com.mapbox.navigation.core.internal.telemetry.toTelemetryLocations
import com.mapbox.navigation.core.telemetry.events.AppMetadata
import com.mapbox.navigation.core.telemetry.events.FeedbackEvent
import com.mapbox.navigation.core.telemetry.events.FeedbackMetadata
import com.mapbox.navigation.core.telemetry.events.FeedbackMetadataWrapper
import com.mapbox.navigation.core.telemetry.events.FreeDriveEventType
import com.mapbox.navigation.core.telemetry.events.FreeDriveEventType.START
import com.mapbox.navigation.core.telemetry.events.FreeDriveEventType.STOP
import com.mapbox.navigation.core.telemetry.events.MetricsDirectionsRoute
import com.mapbox.navigation.core.telemetry.events.MetricsRouteProgress
import com.mapbox.navigation.core.telemetry.events.NavigationAppUserTurnstileEvent
import com.mapbox.navigation.core.telemetry.events.NavigationArriveEvent
import com.mapbox.navigation.core.telemetry.events.NavigationCancelEvent
import com.mapbox.navigation.core.telemetry.events.NavigationCustomEvent
import com.mapbox.navigation.core.telemetry.events.NavigationDepartEvent
import com.mapbox.navigation.core.telemetry.events.NavigationEvent
import com.mapbox.navigation.core.telemetry.events.NavigationFeedbackEvent
import com.mapbox.navigation.core.telemetry.events.NavigationFreeDriveEvent
import com.mapbox.navigation.core.telemetry.events.NavigationRerouteEvent
import com.mapbox.navigation.core.telemetry.events.NavigationStepData
import com.mapbox.navigation.core.telemetry.events.PhoneState
import com.mapbox.navigation.core.trip.session.NavigationSessionState
import com.mapbox.navigation.core.trip.session.NavigationSessionState.ActiveGuidance
import com.mapbox.navigation.core.trip.session.NavigationSessionState.FreeDrive
import com.mapbox.navigation.core.trip.session.NavigationSessionState.Idle
import com.mapbox.navigation.core.trip.session.NavigationSessionStateObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.metrics.MapboxMetricsReporter
import com.mapbox.navigation.utils.internal.Time
import com.mapbox.navigation.utils.internal.ifNonNull
import com.mapbox.navigation.utils.internal.logD
import com.mapbox.navigation.utils.internal.logW
import com.mapbox.navigation.utils.internal.toPoint
import java.util.Date
import java.util.concurrent.CopyOnWriteArraySet

/**
 * Session metadata when telemetry is on Pause.
 *
 * @param navigatorSessionIdentifier holds identifier between
 * [MapboxNavigationTelemetry.initialize]/[MapboxNavigationTelemetry.destroy]. Allows to
 * concatenate FreeDrive/ActiveGuidance/Feedback under one Telemetry session.
 */
private data class SessionMetadataOnPause(
    val navigatorSessionIdentifier: String,
)

/**
 * Session metadata when Telemetry is Running
 *
 * @param navigatorSessionIdentifier holds identifier between
 * [MapboxNavigationTelemetry.initialize]/[MapboxNavigationTelemetry.destroy]. Allows to
 * concatenate FreeDrive/ActiveGuidance/Feedback under one Telemetry session.
 * @param driverModeId random id of **FreeDrive** or **ActiveGuidance** mode
 * @param driverModeStartTime time of start of the driver mode (**FreeDrive** or
 * **ActiveGuidance**)
 * @param telemetryNavSessionState telemetry is running under one of [TelemetryNavSessionState].
 * It transforms into [FeedbackEvent.DriverMode] in telemetry events.
 * @param dynamicValues dynamic values for ActiveGuidance mode.
 */
private data class SessionMetadata(
    val navigatorSessionIdentifier: String,
    var driverModeId: String,
    val driverModeStartTime: Date = Date(),
    val telemetryNavSessionState: TelemetryNavSessionState,
    val dynamicValues: DynamicSessionValues = DynamicSessionValues()
)

/**
 * Dynamic session values. Relevant for ActiveGuidance only.
 *
 * @param rerouteCount count of reroutes for particular route.
 * @param timeOfReroute time of reroute. Unit is **time in millis**.
 * @param timeSinceLastReroute time since last reroute. Unit is **millis**.
 * @param driverModeArrivalTime arrival time of driver mode
 * @param currentDistanceTraveled for the active session
 */
private data class DynamicSessionValues(
    var rerouteCount: Int = 0,
    var timeOfReroute: Long = 0L,
    var timeSinceLastReroute: Int = 0,
    var driverModeArrivalTime: Date? = null,
    var currentDistanceTraveled: Int = 0,
    var accumulatedDistanceTraveled: Int = 0,
) {
    fun reset() {
        rerouteCount = 0
        timeOfReroute = 0
        timeSinceLastReroute = 0
        driverModeArrivalTime = null
        currentDistanceTraveled = 0
        accumulatedDistanceTraveled = 0
    }

    fun accumulateDistanceTraveled(distance: Int) {
        accumulatedDistanceTraveled += distance
    }

    fun resetCurrentDistanceTraveled() {
        currentDistanceTraveled = 0
    }
}

/**
 * Telemetry nav session state. [NavigationSessionState] without [NavigationSessionState.Idle]
 */
private enum class TelemetryNavSessionState {
    TRIP,
    FREE_DRIVE,
}

private const val LOG_TELEMETRY_IS_NOT_RUNNING = "Telemetry is not running"
private const val LOG_TELEMETRY_NO_ROUTE_OR_ROUTE_PROGRESS = "no route or route progress"
private const val SDK_IDENTIFIER = "mapbox-navigation-android"

/**
 * The one and only Telemetry class. This class handles all telemetry events.
 * Event List:
- appUserTurnstile
- navigation.depart
- navigation.feedback
- navigation.reroute
- navigation.arrive
- navigation.cancel
The class must be initialized before any telemetry events are reported.
Attempting to use telemetry before initialization is called will throw an exception.
Initialization may be called multiple times, the call is idempotent.
The class has two public methods, postUserFeedback() and initialize().

TODO(NAVAND-1820) refactor this class. It's hard to test because of statics.
 */
internal object MapboxNavigationTelemetry : SdkTelemetry {
    internal const val LOG_CATEGORY = "MapboxNavigationTelemetry"

    private const val ONE_SECOND = 1000
    internal const val MOCK_PROVIDER = "com.mapbox.navigation.core.replay.ReplayLocationEngine"
    private const val EVENT_VERSION = 7

    private lateinit var applicationContext: Context
    private lateinit var metricsReporter: MetricsReporter
    private lateinit var navigationOptions: NavigationOptions
    private var lifecycleMonitor: ApplicationLifecycleMonitor? = null
    private var appInstance: Application? = null
        set(value) {
            // Don't set it multiple times to the same value, it will cause multiple registration calls.
            if (field == value) {
                return
            }
            field = value
            ifNonNull(value) { app ->
                logD("Lifecycle monitor created", LOG_CATEGORY)
                lifecycleMonitor = ApplicationLifecycleMonitor(app)
            }
        }

    private var locationEngineNameExternal: String = LocationEngine::javaClass.name
    private lateinit var locationsCollector: LocationsCollector
    private lateinit var sdkIdentifier: String
    private val feedbackEventCacheMap = LinkedHashMap<String, NavigationFeedbackEvent>()

    private var sessionState: NavigationSessionState = Idle

    private val routeData = RouteData()

    private class RouteData {
        var routeProgress: RouteProgress? = null
            set(value) {
                field = value
                onRouteDataChanged.invoke()
            }
        var originalRouteMetrics: MetricsDirectionsRoute? = null
            set(value) {
                field = value
                onRouteDataChanged.invoke()
            }

        var needHandleDeparture = false
            set(value) {
                field = value
                onRouteDataChanged.invoke()
            }

        fun hasRouteAndRouteProgress(): Boolean {
            return routeData.originalRouteMetrics != null && routeData.routeProgress != null
        }
    }

    private var telemetryState: NavTelemetryState = NavTelemetryState.Stopped
    private val isTelemetryRunning: Boolean
        get() = telemetryState is NavTelemetryState.Running
    private val isTelemetryOnPause: Boolean
        get() = telemetryState is NavTelemetryState.Paused

    private val routesObserver = RoutesObserver { result ->
        val routes = result.navigationRoutes
        val reason = result.reason

        log("onRoutesChanged. Number of routes = ${routes.size}; reason = $reason")

        when {
            reason == RoutesExtra.ROUTES_UPDATE_REASON_CLEAN_UP || routes.isEmpty() -> Unit
            reason == RoutesExtra.ROUTES_UPDATE_REASON_NEW -> {
                log("handle a new route")
                if (routeData.originalRouteMetrics != null && sessionState is ActiveGuidance) {
                    handleCancelNavigation()
                }
                resetLocalVariables()
                resetDynamicValues()
                routeData.originalRouteMetrics = MetricsDirectionsRoute(
                    routes.first().directionsRoute
                )
                routeData.needHandleDeparture = true
            }
            reason == RoutesExtra.ROUTES_UPDATE_REASON_ALTERNATIVE -> {
                log("alternative routes received")
            }
            reason == RoutesExtra.ROUTES_UPDATE_REASON_REROUTE -> {
                handleReroute(routes.first().directionsRoute)
            }
            reason == RoutesExtra.ROUTES_UPDATE_REASON_REFRESH -> {
                routeData.originalRouteMetrics = MetricsDirectionsRoute(
                    routes.first().directionsRoute
                )
            }
            else -> logW(
                "Unknown route update reason: [$reason]",
                LOG_CATEGORY
            )
        }
    }

    private val arrivalObserver = object : ArrivalObserver {
        override fun onNextRouteLegStart(routeLegProgress: RouteLegProgress) {
            log("onNextRouteLegStart")
            processArrival()
            handleCancelNavigation()
            resetDynamicValues()
            resetRouteProgress()
            routeData.needHandleDeparture = true
        }

        override fun onWaypointArrival(routeProgress: RouteProgress) {
            log("onWaypointDestinationArrival")
        }

        override fun onFinalDestinationArrival(routeProgress: RouteProgress) {
            log("onFinalDestinationArrival")
            this@MapboxNavigationTelemetry.routeData.routeProgress = routeProgress
            processArrival()
        }
    }

    private val navigationSessionStateObserver = NavigationSessionStateObserver { sessionState ->
        log("session state is $sessionState")
        val legacyState = this.sessionState
        this.sessionState = sessionState
        when (sessionState) {
            is Idle, is FreeDrive -> {
                if (legacyState is ActiveGuidance) {
                    handleCancelNavigation()
                }
                resetLocalVariables()
                resetDynamicValues()
            }
            is ActiveGuidance -> Unit // do nothing
        }

        when (val freeDriveEvent = getFreeDriveEvent(legacyState, this.sessionState)) {
            START -> {
                handleTelemetryState()
                trackFreeDrive(freeDriveEvent)
            }
            STOP -> {
                trackFreeDrive(freeDriveEvent)
                handleTelemetryState()
            }
            null -> {
                handleTelemetryState()
            }
        }
    }

    private val routeProgressObserver = RouteProgressObserver { routeProgress ->
        this.routeData.routeProgress = routeProgress
        val dynamicValues = getSessionMetadataIfTelemetryRunning()?.dynamicValues
        if (routeProgress.currentState == RouteProgressState.OFF_ROUTE) {
            dynamicValues?.accumulateDistanceTraveled(
                routeProgress.distanceTraveled.toInt()
            )
            dynamicValues?.resetCurrentDistanceTraveled()
        } else {
            dynamicValues?.currentDistanceTraveled =
                routeProgress.distanceTraveled.toInt()
        }
    }

    private val onRouteDataChanged: () -> Unit = {
        if (
            routeData.needHandleDeparture &&
            routeData.hasRouteAndRouteProgress() &&
            isTelemetryRunning
        ) {
            routeData.needHandleDeparture = false
            processDeparture()
        }
    }

    private val userFeedbackCallbacks = CopyOnWriteArraySet<UserFeedbackCallback>()

    /**
     * This method must be called before using the Telemetry object
     */
    fun initialize(
        mapboxNavigation: MapboxNavigation,
        options: NavigationOptions,
        reporter: MetricsReporter,
        locationsCollector: LocationsCollector = LocationsCollectorImpl(),
    ) {
        resetLocalVariables()
        sessionState = Idle
        this.locationsCollector = locationsCollector
        navigationOptions = options
        applicationContext = options.applicationContext
        locationEngineNameExternal = options.locationEngine.javaClass.name
        sdkIdentifier = SDK_IDENTIFIER
        metricsReporter = reporter
        feedbackEventCacheMap.clear()
        postTurnstileEvent()
        telemetryStart()
        registerListeners(mapboxNavigation)
        log("Valid initialization")
    }

    override fun destroy(mapboxNavigation: MapboxNavigation) {
        telemetryStop()

        // TODO(NAVAND-1820) MapboxMetricsReporter is destroyed here,
        // but initialized separately from MapboxNavigationTelemetry
        log("MapboxMetricsReporter disable")
        MapboxMetricsReporter.disable()

        mapboxNavigation.run {
            unregisterLocationObserver(locationsCollector)
            unregisterRouteProgressObserver(routeProgressObserver)
            unregisterRoutesObserver(routesObserver)
            unregisterNavigationSessionStateObserver(navigationSessionStateObserver)
            unregisterArrivalObserver(arrivalObserver)
        }
        userFeedbackCallbacks.clear()
    }

    private fun telemetryStart() {
        log("sessionStart")
        telemetryState = when (sessionState) {
            is Idle -> {
                NavTelemetryState.Paused(
                    SessionMetadataOnPause(
                        navigatorSessionIdentifier =
                        navObtainUniversalTelemetryNavigationSessionId(),
                    )
                )
            }
            is FreeDrive -> {
                NavTelemetryState.Running(
                    SessionMetadata(
                        navigatorSessionIdentifier =
                        navObtainUniversalTelemetryNavigationSessionId(),
                        driverModeId = navObtainUniversalTelemetryNavigationModeId(),
                        telemetryNavSessionState = TelemetryNavSessionState.FREE_DRIVE
                    )
                )
            }
            is ActiveGuidance -> {
                NavTelemetryState.Running(
                    SessionMetadata(
                        navigatorSessionIdentifier =
                        navObtainUniversalTelemetryNavigationSessionId(),
                        driverModeId = navObtainUniversalTelemetryNavigationModeId(),
                        telemetryNavSessionState = TelemetryNavSessionState.TRIP
                    )
                )
            }
        }
    }

    private fun telemetryStop() {
        log("sessionStop")
        locationsCollector.flushBuffers()
        telemetryState = NavTelemetryState.Stopped
        resetLocalVariables()
    }

    fun setApplicationInstance(app: Application) {
        appInstance = app
    }

    fun registerUserFeedbackCallback(userFeedbackCallback: UserFeedbackCallback) {
        userFeedbackCallbacks.add(userFeedbackCallback)
    }

    fun unregisterUserFeedbackCallback(userFeedbackCallback: UserFeedbackCallback) {
        userFeedbackCallbacks.remove(userFeedbackCallback)
    }

    override fun postCustomEvent(
        payload: String,
        customEventType: String,
        customEventVersion: String
    ) {
        createCustomEvent(
            payload = payload,
            customEventType = customEventType,
            customEventVersion = customEventVersion,
            phoneState = PhoneState.newInstance(applicationContext)
        ) {
            sendMetricEvent(it)
        }
    }

    @ExperimentalPreviewMapboxNavigationAPI
    override fun provideFeedbackMetadataWrapper(): FeedbackMetadataWrapper {
        (telemetryState as? NavTelemetryState.Running)?.sessionMetadata?.let { sessionMetadata ->
            return FeedbackMetadataWrapper(
                sessionMetadata.navigatorSessionIdentifier,
                sessionMetadata.driverModeId,
                sessionMetadata.telemetryNavSessionState.getModeName(),
                generateCreateDateFormatted(sessionMetadata.driverModeStartTime),
                sessionMetadata.dynamicValues.rerouteCount,
                locationsCollector.lastLocation?.toPoint(),
                locationEngineNameExternal,
                lifecycleMonitor?.obtainPortraitPercentage(),
                lifecycleMonitor?.obtainForegroundPercentage(),
                EVENT_VERSION,
                PhoneState.newInstance(applicationContext),
                routeData.originalRouteMetrics ?: MetricsDirectionsRoute(null),
                MetricsRouteProgress(routeData.routeProgress),
                createAppMetadata(),
                locationsCollector
            )
        } ?: throw IllegalStateException(
            "Feedback Metadata might be provided when trip session is started only"
        )
    }

    @OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
    override fun postUserFeedback(
        feedbackType: String,
        description: String,
        @FeedbackEvent.Source feedbackSource: String,
        screenshot: String?,
        feedbackSubType: Array<String>?,
        feedbackMetadata: FeedbackMetadata?,
        userFeedbackCallback: UserFeedbackCallback?,
    ) {
        createUserFeedback(
            feedbackType,
            description,
            feedbackSource,
            screenshot,
            feedbackSubType,
            feedbackMetadata,
            userFeedbackCallback,
        ) {
            sendMetricEvent(it)
        }
    }

    @ExperimentalPreviewMapboxNavigationAPI
    private fun createUserFeedback(
        feedbackType: String,
        description: String,
        @FeedbackEvent.Source feedbackSource: String,
        screenshot: String?,
        feedbackSubType: Array<String>?,
        feedbackMetadata: FeedbackMetadata?,
        localUserFeedbackCallback: UserFeedbackCallback?,
        onEventUpdated: ((NavigationFeedbackEvent) -> Unit)?,
    ) {
        fun notifyUserFeedbackCallbacks(feedbackEvent: NavigationFeedbackEvent) {
            val userFeedback = UserFeedback(
                feedbackEvent.feedbackId,
                feedbackType,
                feedbackSource,
                description,
                screenshot,
                feedbackSubType,
                Point.fromLngLat(feedbackEvent.lng, feedbackEvent.lat),
            )
            localUserFeedbackCallback?.onNewUserFeedback(userFeedback)
            for (callback in userFeedbackCallbacks) {
                callback.onNewUserFeedback(userFeedback)
            }
        }
        if (feedbackMetadata == null) {
            ifTelemetryRunning(
                "User Feedback event creation failed: $LOG_TELEMETRY_IS_NOT_RUNNING"
            ) {
                val feedbackEvent = NavigationFeedbackEvent(
                    PhoneState.newInstance(applicationContext),
                    NavigationStepData(MetricsRouteProgress(routeData.routeProgress))
                ).apply {
                    this.feedbackType = feedbackType
                    this.source = feedbackSource
                    this.description = description
                    this.screenshot = screenshot
                    this.feedbackSubType = feedbackSubType
                    populateWithLocalVars(it)
                }
                notifyUserFeedbackCallbacks(feedbackEvent)

                log("collect post event locations for user feedback")
                locationsCollector.collectLocations { preEventBuffer, postEventBuffer ->
                    log("locations ready")
                    feedbackEvent.apply {
                        locationsBefore = preEventBuffer.toTelemetryLocations()
                        locationsAfter = postEventBuffer.toTelemetryLocations()
                    }
                    onEventUpdated?.invoke(feedbackEvent)
                }
            }
        } else {
            log("post user feedback with feedback metadata")
            val feedbackEvent = NavigationFeedbackEvent(
                feedbackMetadata.phoneState,
                NavigationStepData(feedbackMetadata.metricsRouteProgress),
            ).apply {
                this.feedbackType = feedbackType
                this.source = feedbackSource
                this.description = description
                this.screenshot = screenshot
                this.feedbackSubType = feedbackSubType
                this.locationsBefore = feedbackMetadata.locationsBeforeEvent
                this.locationsAfter = feedbackMetadata.locationsAfterEvent
                val distanceTraveled =
                    getSessionMetadataIfTelemetryRunning()?.dynamicValues.retrieveDistanceTraveled()
                populate(
                    this@MapboxNavigationTelemetry.sdkIdentifier,
                    feedbackMetadata.metricsDirectionsRoute,
                    feedbackMetadata.metricsRouteProgress,
                    feedbackMetadata.lastLocation,
                    feedbackMetadata.locationEngineNameExternal,
                    feedbackMetadata.percentTimeInPortrait,
                    feedbackMetadata.percentTimeInForeground,
                    feedbackMetadata.sessionIdentifier,
                    feedbackMetadata.driverModeIdentifier,
                    feedbackMetadata.driverMode,
                    feedbackMetadata.driverModeStartTime,
                    feedbackMetadata.rerouteCount,
                    distanceTraveled,
                    feedbackMetadata.eventVersion,
                    feedbackMetadata.appMetadata,
                )
            }
            notifyUserFeedbackCallbacks(feedbackEvent)
            onEventUpdated?.invoke(feedbackEvent)
        }
    }

    private fun processDeparture() {
        sendMetricEvent(
            NavigationDepartEvent(PhoneState.newInstance(applicationContext)).apply {
                populateWithLocalVars(getSessionMetadataIfTelemetryRunning())
            }
        )
    }

    private fun getFreeDriveEvent(
        oldState: NavigationSessionState,
        newState: NavigationSessionState
    ): FreeDriveEventType? {
        return when {
            oldState is FreeDrive && newState !is FreeDrive -> STOP
            oldState !is FreeDrive && newState is FreeDrive -> START
            else -> null
        }
    }

    private fun handleTelemetryState() {
        val localTelemetryState = telemetryState
        when (sessionState) {
            is Idle -> {
                if (localTelemetryState is NavTelemetryState.Running) {
                    telemetryState = NavTelemetryState.Paused(
                        SessionMetadataOnPause(
                            navigatorSessionIdentifier =
                            localTelemetryState.sessionMetadata.navigatorSessionIdentifier,
                        )
                    )
                }
            }
            is FreeDrive -> {
                when (localTelemetryState) {
                    is NavTelemetryState.Paused -> {
                        telemetryState = NavTelemetryState.Running(
                            SessionMetadata(
                                navigatorSessionIdentifier = localTelemetryState
                                    .sessionMetadataOnPaused.navigatorSessionIdentifier,
                                driverModeId = navObtainUniversalTelemetryNavigationModeId(),
                                telemetryNavSessionState = TelemetryNavSessionState.FREE_DRIVE
                            )
                        )
                    }
                    is NavTelemetryState.Running -> {
                        telemetryState = NavTelemetryState.Running(
                            SessionMetadata(
                                navigatorSessionIdentifier =
                                localTelemetryState.sessionMetadata.navigatorSessionIdentifier,
                                driverModeId = navObtainUniversalTelemetryNavigationModeId(),
                                driverModeStartTime = Date(),
                                telemetryNavSessionState = TelemetryNavSessionState.FREE_DRIVE
                            )
                        )
                    }
                    NavTelemetryState.Stopped -> Unit // do nothing
                }
            }
            is ActiveGuidance -> {
                when (localTelemetryState) {
                    is NavTelemetryState.Paused -> {
                        telemetryState = NavTelemetryState.Running(
                            SessionMetadata(
                                navigatorSessionIdentifier = localTelemetryState
                                    .sessionMetadataOnPaused.navigatorSessionIdentifier,
                                driverModeId = navObtainUniversalTelemetryNavigationModeId(),
                                telemetryNavSessionState = TelemetryNavSessionState.TRIP
                            )
                        )
                    }
                    is NavTelemetryState.Running -> {
                        telemetryState = NavTelemetryState.Running(
                            SessionMetadata(
                                navigatorSessionIdentifier =
                                localTelemetryState.sessionMetadata.navigatorSessionIdentifier,
                                driverModeId = navObtainUniversalTelemetryNavigationModeId(),
                                driverModeStartTime = Date(),
                                telemetryNavSessionState = TelemetryNavSessionState.TRIP
                            )
                        )
                    }
                    NavTelemetryState.Stopped -> Unit // do nothing
                }
            }
        }
    }

    private fun trackFreeDrive(type: FreeDriveEventType) {
        log("trackFreeDrive $type")
        ifTelemetryRunning(
            "cannot handle free drive change: $LOG_TELEMETRY_IS_NOT_RUNNING"
        ) { sessionMetadata ->
            createFreeDriveEvent(
                type,
                sessionMetadata
            )
        }
    }

    private fun createFreeDriveEvent(
        type: FreeDriveEventType,
        sessionMetadata: SessionMetadata,
    ) {
        log("createFreeDriveEvent $type")
        val freeDriveEvent =
            NavigationFreeDriveEvent(PhoneState.newInstance(applicationContext)).apply {
                populate(
                    type,
                    sessionMetadata.navigatorSessionIdentifier,
                    sessionMetadata.driverModeId,
                    sessionMetadata.driverModeStartTime
                )
            }
        sendEvent(freeDriveEvent)
    }

    private fun createCustomEvent(
        payload: String,
        customEventType: String,
        customEventVersion: String,
        phoneState: PhoneState,
        onEventUpdated: ((NavigationCustomEvent) -> Unit)?
    ) {
        log("customEventType: $customEventType")
        val customEvent =
            NavigationCustomEvent().apply {
                this.payload = payload.plus(", userId = ${phoneState.userId}")
                this.type = customEventType
                this.driverMode = "freeDrive"
                this.eventVersion = EVENT_VERSION
                this.customEventVersion = customEventVersion
                lat = locationsCollector.lastLocation?.latitude ?: 0.0
                lng = locationsCollector.lastLocation?.longitude ?: 0.0
                sdkIdentifier = this@MapboxNavigationTelemetry.sdkIdentifier
                locationEngine = this@MapboxNavigationTelemetry.locationEngineNameExternal
            }
        onEventUpdated?.invoke(customEvent)
    }

    private fun sendEvent(metricEvent: MetricEvent) {
        log("${metricEvent::class.java} event sent")
        metricsReporter.addEvent(metricEvent)
    }

    private fun registerListeners(mapboxNavigation: MapboxNavigation) {
        mapboxNavigation.run {
            registerLocationObserver(locationsCollector)
            registerRouteProgressObserver(routeProgressObserver)
            registerRoutesObserver(routesObserver)
            registerNavigationSessionStateObserver(navigationSessionStateObserver)
            registerArrivalObserver(arrivalObserver)
        }
    }

    private fun sendMetricEvent(event: MetricEvent) {
        if (isTelemetryRunning || isTelemetryOnPause) {
            sendEvent(event)
        } else {
            log(
                "${event::class.java} is not sent. Caused by: " +
                    "Telemetry Session started: $isTelemetryRunning."
            )
        }
    }

    private fun handleReroute(route: DirectionsRoute) {
        if (!routeData.hasRouteAndRouteProgress()) {
            log("cannot handle reroute: $LOG_TELEMETRY_NO_ROUTE_OR_ROUTE_PROGRESS")
            return
        }
        ifTelemetryRunning(
            "cannot handle reroute: $LOG_TELEMETRY_IS_NOT_RUNNING"
        ) { sessionMetadata ->
            log("handleReroute")

            sessionMetadata.dynamicValues.run {
                val currentTime = Time.SystemImpl.millis()
                timeSinceLastReroute = (currentTime - timeOfReroute).toInt()
                timeOfReroute = currentTime
                rerouteCount++
            }

            val navigationRerouteEvent = NavigationRerouteEvent(
                PhoneState.newInstance(applicationContext),
                NavigationStepData(MetricsRouteProgress(routeData.routeProgress)),
            ).apply {
                secondsSinceLastReroute =
                    sessionMetadata
                    .dynamicValues
                    .timeSinceLastReroute / ONE_SECOND

                newDistanceRemaining = route.distance().toInt()
                newDurationRemaining = route.duration().toInt()
                newGeometry = obtainGeometry(route)
                populateWithLocalVars(sessionMetadata)
            }

            locationsCollector.collectLocations { preEventBuffer, postEventBuffer ->
                navigationRerouteEvent.apply {
                    locationsBefore = preEventBuffer.toTelemetryLocations()
                    locationsAfter = postEventBuffer.toTelemetryLocations()
                }

                sendMetricEvent(navigationRerouteEvent)
            }
        }
    }

    private fun handleCancelNavigation() {
        log("handleSessionCanceled")
        locationsCollector.flushBuffers()

        val cancelEvent =
            NavigationCancelEvent(PhoneState.newInstance(applicationContext)).apply {
                populateWithLocalVars(getSessionMetadataIfTelemetryRunning())
            }
        cancelEvent.arrivalTimestamp = generateCreateDateFormatted(Date())
        sendMetricEvent(cancelEvent)
    }

    private fun postTurnstileEvent() {
        val turnstileEvent = TurnstileEvent(
            UserSKUIdentifier.NAV2_SES_MAU,
            sdkIdentifier,
            BuildConfig.MAPBOX_NAVIGATION_VERSION_NAME,
        )
        val event = NavigationAppUserTurnstileEvent(turnstileEvent)
        log("TurnstileEvent sent")
        metricsReporter.sendTurnstileEvent(event.event)
    }

    private fun processArrival() {
        if (!routeData.hasRouteAndRouteProgress()) {
            log("cannot handle process arrival: $LOG_TELEMETRY_NO_ROUTE_OR_ROUTE_PROGRESS")
            return
        }
        ifTelemetryRunning(
            "cannot handle process arrival: $LOG_TELEMETRY_IS_NOT_RUNNING"
        ) { sessionMetadata ->
            log("you have arrived")
            sessionMetadata.dynamicValues.driverModeArrivalTime = Date()

            val arriveEvent =
                NavigationArriveEvent(PhoneState.newInstance(applicationContext)).apply {
                    populateWithLocalVars(sessionMetadata)
                }
            sendMetricEvent(arriveEvent)
        }
    }

    private fun NavigationEvent.populateWithLocalVars(sessionMetadata: SessionMetadata?) {
        val distanceTraveled = sessionMetadata?.dynamicValues.retrieveDistanceTraveled()
        this.populate(
            this@MapboxNavigationTelemetry.sdkIdentifier,
            routeData.originalRouteMetrics ?: MetricsDirectionsRoute(null),
            MetricsRouteProgress(routeData.routeProgress),
            locationsCollector.lastLocation?.toPoint(),
            locationEngineNameExternal,
            lifecycleMonitor?.obtainPortraitPercentage(),
            lifecycleMonitor?.obtainForegroundPercentage(),
            sessionMetadata?.navigatorSessionIdentifier,
            sessionMetadata?.driverModeId,
            sessionMetadata?.telemetryNavSessionState?.getModeName(),
            sessionMetadata?.driverModeStartTime?.let { generateCreateDateFormatted(it) },
            sessionMetadata?.dynamicValues?.rerouteCount,
            distanceTraveled,
            EVENT_VERSION,
            createAppMetadata()
        )
    }

    private fun DynamicSessionValues?.retrieveDistanceTraveled(): Int {
        val currentDistanceTraveled = this?.currentDistanceTraveled ?: 0
        val accumulatedDistanceTraveled = this?.accumulatedDistanceTraveled ?: 0
        return currentDistanceTraveled + accumulatedDistanceTraveled
    }

    private fun NavigationFreeDriveEvent.populate(
        type: FreeDriveEventType,
        navSessionIdentifier: String,
        modeId: String,
        modeStartTime: Date
    ) {
        log("populateFreeDriveEvent")

        this.apply {
            eventType = type.type
            location = locationsCollector.lastLocation?.toTelemetryLocation()
            eventVersion = EVENT_VERSION
            locationEngine = locationEngineNameExternal
            percentTimeInPortrait = lifecycleMonitor?.obtainPortraitPercentage() ?: 100
            percentTimeInForeground = lifecycleMonitor?.obtainForegroundPercentage() ?: 100
            simulation = locationEngineNameExternal == MOCK_PROVIDER
            navigatorSessionIdentifier = navSessionIdentifier
            sessionIdentifier = modeId
            startTimestamp = generateCreateDateFormatted(modeStartTime)
            appMetadata = createAppMetadata()
        }
    }

    private fun resetDynamicValues() {
        getSessionMetadataIfTelemetryRunning()?.dynamicValues?.reset()
    }

    private fun createAppMetadata(): AppMetadata? {
        navigationOptions.eventsAppMetadata?.let {
            return AppMetadata(it.name, it.version, it.userId, it.sessionId)
        } ?: return null
    }

    private fun resetLocalVariables() {
        resetOriginalRoute()
        resetRouteProgress()
        routeData.needHandleDeparture = false
    }

    private fun resetRouteProgress() {
        log("resetRouteProgress")
        routeData.routeProgress = null
    }

    private fun resetOriginalRoute() {
        log("resetOriginalRoute")
        routeData.originalRouteMetrics = null
    }

    private fun getSessionMetadataIfTelemetryRunning(): SessionMetadata? =
        (telemetryState as? NavTelemetryState.Running)?.sessionMetadata

    private fun ifTelemetryRunning(
        elseLog: String? = null,
        func: ((SessionMetadata) -> Unit)
    ) {
        when (val telemetryState = telemetryState) {
            is NavTelemetryState.Running -> {
                func(telemetryState.sessionMetadata)
            }
            is NavTelemetryState.Paused -> {
                elseLog?.let { log("Telemetry Paused; $it") }
            }
            NavTelemetryState.Stopped -> {
                elseLog?.let { log("Telemetry Stopped; $it") }
            }
        }
    }

    @FeedbackEvent.DriverMode
    private fun TelemetryNavSessionState.getModeName() =
        when (this) {
            TelemetryNavSessionState.TRIP -> FeedbackEvent.DRIVER_MODE_TRIP
            TelemetryNavSessionState.FREE_DRIVE -> FeedbackEvent.DRIVER_MODE_FREE_DRIVE
        }

    private fun log(message: String) {
        logD(message, LOG_CATEGORY)
    }

    private sealed class NavTelemetryState {
        /**
         * Running
         */
        class Running(val sessionMetadata: SessionMetadata) : NavTelemetryState()

        /**
         * Paused when navigator is in [NavigationSessionState.Idle]
         */
        class Paused(val sessionMetadataOnPaused: SessionMetadataOnPause) : NavTelemetryState()

        /**
         * Stopped
         */
        object Stopped : NavTelemetryState()
    }
}
