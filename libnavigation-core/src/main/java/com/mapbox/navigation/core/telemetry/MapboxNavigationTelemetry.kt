package com.mapbox.navigation.core.telemetry

import android.app.Application
import android.content.Context
import android.location.Location
import android.os.Build
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.telemetry.AppUserTurnstile
import com.mapbox.android.telemetry.TelemetryUtils.generateCreateDateFormatted
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.base.common.logger.Logger
import com.mapbox.base.common.logger.model.Message
import com.mapbox.base.common.logger.model.Tag
import com.mapbox.navigation.base.metrics.MetricEvent
import com.mapbox.navigation.base.metrics.MetricsReporter
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.trip.model.RouteLegProgress
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.BuildConfig
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.arrival.ArrivalObserver
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.internal.accounts.MapboxNavigationAccounts
import com.mapbox.navigation.core.internal.telemetry.CachedNavigationFeedbackEvent
import com.mapbox.navigation.core.telemetry.events.AppMetadata
import com.mapbox.navigation.core.telemetry.events.FeedbackEvent
import com.mapbox.navigation.core.telemetry.events.FreeDriveEventType
import com.mapbox.navigation.core.telemetry.events.FreeDriveEventType.START
import com.mapbox.navigation.core.telemetry.events.FreeDriveEventType.STOP
import com.mapbox.navigation.core.telemetry.events.MetricsRouteProgress
import com.mapbox.navigation.core.telemetry.events.NavigationArriveEvent
import com.mapbox.navigation.core.telemetry.events.NavigationCancelEvent
import com.mapbox.navigation.core.telemetry.events.NavigationDepartEvent
import com.mapbox.navigation.core.telemetry.events.NavigationEvent
import com.mapbox.navigation.core.telemetry.events.NavigationFeedbackEvent
import com.mapbox.navigation.core.telemetry.events.NavigationFreeDriveEvent
import com.mapbox.navigation.core.telemetry.events.NavigationRerouteEvent
import com.mapbox.navigation.core.telemetry.events.PhoneState
import com.mapbox.navigation.core.telemetry.events.TelemetryLocation
import com.mapbox.navigation.core.trip.session.NavigationSessionState
import com.mapbox.navigation.core.trip.session.NavigationSessionState.ActiveGuidance
import com.mapbox.navigation.core.trip.session.NavigationSessionState.FreeDrive
import com.mapbox.navigation.core.trip.session.NavigationSessionState.Idle
import com.mapbox.navigation.core.trip.session.NavigationSessionStateObserver
import com.mapbox.navigation.core.trip.session.OffRouteObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.metrics.MapboxMetricsReporter
import com.mapbox.navigation.metrics.internal.event.NavigationAppUserTurnstileEvent
import com.mapbox.navigation.utils.internal.Time
import com.mapbox.navigation.utils.internal.ifNonNull
import java.util.Date
import kotlin.collections.set

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
 */
private data class DynamicSessionValues(
    var rerouteCount: Int = 0,
    var timeOfReroute: Long = 0L,
    var timeSinceLastReroute: Int = 0,
    var driverModeArrivalTime: Date? = null,
) {
    fun reset() {
        rerouteCount = 0
        timeOfReroute = 0
        timeSinceLastReroute = 0
        driverModeArrivalTime = null
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
 */
internal object MapboxNavigationTelemetry {
    internal val TAG = Tag("MbxTelemetry")

    private const val ONE_SECOND = 1000
    private const val MOCK_PROVIDER = "com.mapbox.navigation.core.replay.ReplayLocationEngine"
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
                logger?.d(TAG, Message("Lifecycle monitor created"))
                lifecycleMonitor = ApplicationLifecycleMonitor(app)
            }
        }

    private var locationEngineNameExternal: String = LocationEngine::javaClass.name
    private lateinit var locationsCollector: LocationsCollector
    private lateinit var sdkIdentifier: String
    private var logger: Logger? = null
    private val feedbackEventCacheMap = LinkedHashMap<String, NavigationFeedbackEvent>()

    private var sessionState: NavigationSessionState = Idle

    private var appMetadataSessionId: String = ""

    private val routeData = RouteData()

    private class RouteData {
        var routeProgress: RouteProgress? = null
            set(value) {
                field = value
                onRouteDataChanged.invoke()
            }
        var originalRoute: DirectionsRoute? = null
            set(value) {
                field = value
                onRouteDataChanged.invoke()
            }

        var needHandleDeparture = false
            set(value) {
                field = value
                onRouteDataChanged.invoke()
            }
        var needHandleReroute = false

        fun hasRouteAndRouteProgress(): Boolean {
            return routeData.originalRoute != null && routeData.routeProgress != null
        }
    }

    private var telemetryState: NavTelemetryState = NavTelemetryState.Stopped
    private val isTelemetryRunning: Boolean
        get() = telemetryState is NavTelemetryState.Running
    private val isTelemetryOnPause: Boolean
        get() = telemetryState is NavTelemetryState.Paused

    private val routesObserver = RoutesObserver { routes ->
        log("onRoutesChanged. size = ${routes.size}")
        routes.getOrNull(0)?.let {
            if (routeData.originalRoute != null) {
                if (routeData.needHandleReroute) {
                    routeData.needHandleReroute = false
                    handleReroute(it)
                } else {
                    log("handle ExternalRoute")
                    handleCancelNavigation()
                    resetLocalVariables()
                    resetDynamicValues()
                    routeData.originalRoute = it
                    routeData.needHandleDeparture = true
                }
            } else {
                log("handle a new route")
                resetLocalVariables()
                resetDynamicValues()
                routeData.originalRoute = it
                routeData.needHandleDeparture = true
            }
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
                appMetadataSessionId = sessionState.sessionId
                handleTelemetryState()
                trackFreeDrive(freeDriveEvent)
            }
            STOP -> {
                trackFreeDrive(freeDriveEvent)
                appMetadataSessionId = sessionState.sessionId
                handleTelemetryState()
            }
            null -> {
                appMetadataSessionId = sessionState.sessionId
                handleTelemetryState()
            }
        }
    }

    private val offRouteObserver = OffRouteObserver { offRoute ->
        log("onOffRouteStateChanged $offRoute")
        if (offRoute) {
            routeData.needHandleReroute = true
        }
    }

    private val routeProgressObserver = RouteProgressObserver { routeProgress ->
        this.routeData.routeProgress = routeProgress
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

    /**
     * This method must be called before using the Telemetry object
     */
    fun initialize(
        mapboxNavigation: MapboxNavigation,
        options: NavigationOptions,
        reporter: MetricsReporter,
        logger: Logger?,
        locationsCollector: LocationsCollector = LocationsCollectorImpl(logger),
    ) {
        resetLocalVariables()
        sessionState = Idle
        this.logger = logger
        this.locationsCollector = locationsCollector
        navigationOptions = options
        applicationContext = options.applicationContext
        locationEngineNameExternal = options.locationEngine.javaClass.name
        sdkIdentifier = if (options.isFromNavigationUi) {
            "mapbox-navigation-ui-android"
        } else {
            "mapbox-navigation-android"
        }
        metricsReporter = reporter
        feedbackEventCacheMap.clear()
        postTurnstileEvent()
        telemetryStart()
        registerListeners(mapboxNavigation)
        log("Valid initialization")
    }

    fun destroy(mapboxNavigation: MapboxNavigation) {
        telemetryStop()
        log("MapboxMetricsReporter disable")
        MapboxMetricsReporter.disable()
        mapboxNavigation.run {
            unregisterLocationObserver(locationsCollector)
            unregisterRouteProgressObserver(routeProgressObserver)
            unregisterRoutesObserver(routesObserver)
            unregisterOffRouteObserver(offRouteObserver)
            unregisterNavigationSessionStateObserver(navigationSessionStateObserver)
            unregisterArrivalObserver(arrivalObserver)
        }
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

    fun postUserFeedback(
        @FeedbackEvent.Type feedbackType: String,
        description: String,
        @FeedbackEvent.Source feedbackSource: String,
        screenshot: String?,
        feedbackSubType: Array<@FeedbackEvent.SubType String>?,
    ) {
        createUserFeedback(
            feedbackType,
            description,
            feedbackSource,
            screenshot,
            feedbackSubType,
            null
        ) {
            sendMetricEvent(it)
        }
    }

    fun cacheUserFeedback(
        @FeedbackEvent.Type feedbackType: String,
        description: String,
        @FeedbackEvent.Source feedbackSource: String,
        screenshot: String?,
        feedbackSubType: Array<String>?,
    ) {
        createUserFeedback(
            feedbackType,
            description,
            feedbackSource,
            screenshot,
            feedbackSubType,
            {
                feedbackEventCacheMap[it.feedbackId] = it
            },
            {
                feedbackEventCacheMap[it.feedbackId]?.let { cachedEvent ->
                    cachedEvent.locationsAfter = it.locationsAfter
                    cachedEvent.locationsBefore = it.locationsBefore
                } ?: feedbackEventCacheMap.put(it.feedbackId, it)
            }
        )
    }

    fun getCachedUserFeedback(): List<CachedNavigationFeedbackEvent> {
        locationsCollector.flushBuffers()
        return feedbackEventCacheMap.map {
            it.value.getCachedNavigationFeedbackEvent()
        }
    }

    fun postCachedUserFeedback(cachedFeedbackEventList: List<CachedNavigationFeedbackEvent>) {
        log("post cached user feedback events")
        val feedbackEventCache = LinkedHashMap(feedbackEventCacheMap)
        feedbackEventCacheMap.clear()

        cachedFeedbackEventList.forEach { cachedFeedback ->
            sendEvent(
                feedbackEventCache[cachedFeedback.feedbackId]?.apply {
                    update(cachedFeedback)
                } ?: return@forEach
            )
        }
    }

    private fun createUserFeedback(
        @FeedbackEvent.Type feedbackType: String,
        description: String,
        @FeedbackEvent.Source feedbackSource: String,
        screenshot: String?,
        feedbackSubType: Array<@FeedbackEvent.SubType String>?,
        onEventCreated: ((NavigationFeedbackEvent) -> Unit)? = null,
        onEventUpdated: ((NavigationFeedbackEvent) -> Unit)? = null
    ) {
        ifTelemetryRunning(
            "User Feedback event creation failed: $LOG_TELEMETRY_IS_NOT_RUNNING"
        ) {
            log("collect post event locations for user feedback")
            val feedbackEvent = NavigationFeedbackEvent(
                PhoneState(applicationContext),
                MetricsRouteProgress(routeData.routeProgress)
            ).apply {
                this.feedbackType = feedbackType
                this.source = feedbackSource
                this.description = description
                this.screenshot = screenshot
                this.feedbackSubType = feedbackSubType
                populate()
            }

            onEventCreated?.let { it(feedbackEvent) }

            locationsCollector.collectLocations { preEventBuffer, postEventBuffer ->
                log("locations ready")
                feedbackEvent.apply {
                    locationsBefore = preEventBuffer.toTelemetryLocations()
                    locationsAfter = postEventBuffer.toTelemetryLocations()
                }
                onEventUpdated?.let { it(feedbackEvent) }
            }
        }
    }

    private fun processDeparture() {
        sendMetricEvent(
            NavigationDepartEvent(PhoneState(applicationContext)).apply { populate() }
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
        val freeDriveEvent = NavigationFreeDriveEvent(PhoneState(applicationContext)).apply {
            populate(
                type,
                sessionMetadata.navigatorSessionIdentifier,
                sessionMetadata.driverModeId,
                sessionMetadata.driverModeStartTime
            )
        }
        sendEvent(freeDriveEvent)
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
            registerOffRouteObserver(offRouteObserver)
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
                PhoneState(applicationContext),
                MetricsRouteProgress(routeData.routeProgress)
            ).apply {
                secondsSinceLastReroute =
                    sessionMetadata
                    .dynamicValues
                    .timeSinceLastReroute / ONE_SECOND

                newDistanceRemaining = route.distance().toInt()
                newDurationRemaining = route.duration().toInt()
                newGeometry = obtainGeometry(route)
                populate()
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
            NavigationCancelEvent(PhoneState(applicationContext)).apply { populate() }
        cancelEvent.arrivalTimestamp = generateCreateDateFormatted(Date())
        sendMetricEvent(cancelEvent)
    }

    private fun postTurnstileEvent() {
        val turnstileEvent =
            AppUserTurnstile(sdkIdentifier, BuildConfig.MAPBOX_NAVIGATION_VERSION_NAME).also {
                it.setSkuId(MapboxNavigationAccounts.obtainSkuId())
            }
        val event = NavigationAppUserTurnstileEvent(turnstileEvent)
        sendEvent(event)
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
                NavigationArriveEvent(PhoneState(applicationContext)).apply { populate() }
            sendMetricEvent(arriveEvent)
        }
    }

    private fun NavigationEvent.populate() {
        log("populateNavigationEvent")

        this.apply {
            sdkIdentifier = this@MapboxNavigationTelemetry.sdkIdentifier

            routeData.routeProgress?.let { routeProgress ->
                stepIndex = routeProgress.currentLegProgress?.currentStepProgress?.stepIndex ?: 0

                distanceRemaining = routeProgress.distanceRemaining.toInt()
                durationRemaining = routeProgress.durationRemaining.toInt()
                distanceCompleted = routeProgress.distanceTraveled.toInt()

                routeProgress.route.let {
                    geometry = it.geometry()
                    profile = it.routeOptions()?.profile()
                    requestIdentifier = it.requestUuid()
                    stepCount = obtainStepCount(it)
                    legIndex = it.routeIndex()?.toInt() ?: 0
                    legCount = it.legs()?.size ?: 0

                    absoluteDistanceToDestination = obtainAbsoluteDistance(
                        locationsCollector.lastLocation,
                        obtainRouteDestination(it)
                    )
                    estimatedDistance = it.distance().toInt()
                    estimatedDuration = it.duration().toInt()
                    totalStepCount = obtainStepCount(it)
                }
            }

            routeData.originalRoute?.let {
                originalStepCount = obtainStepCount(it)
                originalEstimatedDistance = it.distance().toInt()
                originalEstimatedDuration = it.duration().toInt()
                originalRequestIdentifier = it.requestUuid()
                originalGeometry = it.geometry()
            }

            locationEngine = locationEngineNameExternal
            tripIdentifier = navObtainUniversalTelemetryTripId()
            lat = locationsCollector.lastLocation?.latitude ?: 0.0
            lng = locationsCollector.lastLocation?.longitude ?: 0.0
            simulation = locationEngineNameExternal == MOCK_PROVIDER
            percentTimeInPortrait = lifecycleMonitor?.obtainPortraitPercentage() ?: 100
            percentTimeInForeground = lifecycleMonitor?.obtainForegroundPercentage() ?: 100

            getSessionMetadataIfTelemetryRunning()?.let { sessionMetadata ->
                navigatorSessionIdentifier = sessionMetadata.navigatorSessionIdentifier

                sessionIdentifier = sessionMetadata.driverModeId
                startTimestamp = generateCreateDateFormatted(sessionMetadata.driverModeStartTime)
                driverMode = sessionMetadata.telemetryNavSessionState.getModeName()

                rerouteCount = sessionMetadata.dynamicValues.rerouteCount
            }

            eventVersion = EVENT_VERSION
            appMetadata = createAppMetadata()
        }
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
            return AppMetadata(it.name, it.version, it.userId, appMetadataSessionId)
        } ?: return null
    }

    private fun resetLocalVariables() {
        resetOriginalRoute()
        resetRouteProgress()
        routeData.needHandleReroute = false
        routeData.needHandleDeparture = false
    }

    private fun resetRouteProgress() {
        log("resetRouteProgress")
        routeData.routeProgress = null
    }

    private fun resetOriginalRoute() {
        log("resetOriginalRoute")
        routeData.originalRoute = null
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

    private fun List<Location>.toTelemetryLocations(): Array<TelemetryLocation> {
        val feedbackLocations = mutableListOf<TelemetryLocation>()
        forEach {
            feedbackLocations.add(it.toTelemetryLocation())
        }

        return feedbackLocations.toTypedArray()
    }

    internal fun Location.toTelemetryLocation(): TelemetryLocation {
        return TelemetryLocation(
            latitude,
            longitude,
            speed,
            bearing,
            altitude,
            time.toString(),
            accuracy,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                verticalAccuracyMeters
            } else {
                0f
            }
        )
    }

    private fun log(message: String) {
        logger?.d(TAG, Message(message))
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
