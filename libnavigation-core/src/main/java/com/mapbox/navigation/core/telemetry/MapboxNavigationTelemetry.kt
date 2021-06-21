package com.mapbox.navigation.core.telemetry

import android.app.Application
import android.content.Context
import android.location.Location
import android.os.Build
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.telemetry.AppUserTurnstile
import com.mapbox.android.telemetry.TelemetryUtils.generateCreateDateFormatted
import com.mapbox.android.telemetry.TelemetryUtils.obtainUniversalUniqueIdentifier
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
import com.mapbox.navigation.core.NavigationSession
import com.mapbox.navigation.core.NavigationSession.State.ACTIVE_GUIDANCE
import com.mapbox.navigation.core.NavigationSession.State.FREE_DRIVE
import com.mapbox.navigation.core.NavigationSession.State.IDLE
import com.mapbox.navigation.core.NavigationSessionStateObserver
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
import com.mapbox.navigation.core.trip.session.OffRouteObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.metrics.MapboxMetricsReporter
import com.mapbox.navigation.metrics.internal.event.NavigationAppUserTurnstileEvent
import com.mapbox.navigation.utils.internal.Time
import com.mapbox.navigation.utils.internal.ifNonNull
import java.util.Date

private data class DynamicSessionValues(
    var rerouteCount: Int = 0,
    var timeOfReroute: Long = 0L,
    var timeSinceLastReroute: Int = 0,
    var sessionId: String? = null,
    var sessionStartTime: Date? = null,
    var sessionArrivalTime: Date? = null,
    var sessionStarted: Boolean = false,
) {
    fun reset() {
        rerouteCount = 0
        timeOfReroute = 0
        timeSinceLastReroute = 0
        sessionId = null
        sessionStartTime = null
        sessionArrivalTime = null
        sessionStarted = false
    }
}

private data class DynamicFreeDriveValues(
    var sessionId: String? = null,
    var sessionStartTime: Date? = null
) {
    fun reset() {
        sessionId = null
        sessionStartTime = null
    }
}

/**
 * The one and only Telemetry class. This class handles all telemetry events.
 * Event List:
- appUserTurnstile
- navigation.depart
- navigation.feedback
- navigation.reroute
- navigation.arrive
- navigation.cancel
The class must be initialized before any telemetry events are reported. Attempting to use telemetry before initialization is called will throw an exception. Initialization may be called multiple times, the call is idempotent.
The class has two public methods, postUserFeedback() and initialize().
 */
internal object MapboxNavigationTelemetry :
    RouteProgressObserver,
    RoutesObserver,
    OffRouteObserver,
    NavigationSessionStateObserver,
    ArrivalObserver {
    internal val TAG = Tag("MbxTelemetry")

    private const val ONE_SECOND = 1000
    private const val MOCK_PROVIDER = "com.mapbox.navigation.core.replay.ReplayLocationEngine"
    private const val EVENT_VERSION = 7

    private lateinit var context: Context // Must be context.getApplicationContext
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
    private val dynamicValues = DynamicSessionValues()
    private val dynamicFreeDriveValues = DynamicFreeDriveValues()
    private var locationEngineNameExternal: String = LocationEngine::javaClass.name
    private lateinit var locationsCollector: LocationsCollector
    private lateinit var sdkIdentifier: String
    private var logger: Logger? = null
    private val feedbackEventCacheMap = LinkedHashMap<String, NavigationFeedbackEvent>()

    private var needHandleReroute = false
    private var sessionState: NavigationSession.State = IDLE
    private var routeProgress: RouteProgress? = null
    private var originalRoute: DirectionsRoute? = null
    private var needStartSession = false

    /**
     * This method must be called before using the Telemetry object
     */
    fun initialize(
        mapboxNavigation: MapboxNavigation,
        options: NavigationOptions,
        reporter: MetricsReporter,
        logger: Logger?,
        locationsCollector: LocationsCollector = LocationsCollectorImpl(logger)
    ) {
        reset()
        dynamicFreeDriveValues.reset()
        sessionState = IDLE
        this.logger = logger
        this.locationsCollector = locationsCollector
        navigationOptions = options
        context = options.applicationContext
        locationEngineNameExternal = options.locationEngine.javaClass.name
        sdkIdentifier = if (options.isFromNavigationUi) {
            "mapbox-navigation-ui-android"
        } else {
            "mapbox-navigation-android"
        }
        metricsReporter = reporter
        feedbackEventCacheMap.clear()

        registerListeners(mapboxNavigation)
        postTurnstileEvent()
        log("Valid initialization")
    }

    fun setApplicationInstance(app: Application) {
        appInstance = app
    }

    fun postUserFeedback(
        @FeedbackEvent.Type feedbackType: String,
        description: String,
        @FeedbackEvent.Source feedbackSource: String,
        screenshot: String?,
        feedbackSubType: Array<String>?,
        appMetadata: AppMetadata?
    ) {
        createUserFeedback(
            feedbackType,
            description,
            feedbackSource,
            screenshot,
            feedbackSubType,
            appMetadata,
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
        appMetadata: AppMetadata?
    ) {
        createUserFeedback(
            feedbackType,
            description,
            feedbackSource,
            screenshot,
            feedbackSubType,
            appMetadata,
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
        feedbackSubType: Array<String>?,
        appMetadata: AppMetadata?,
        onEventCreated: ((NavigationFeedbackEvent) -> Unit)? = null,
        onEventUpdated: ((NavigationFeedbackEvent) -> Unit)? = null
    ) {
        if (dynamicValues.sessionStarted && dataInitialized()) {
            log("collect post event locations for user feedback")
            val feedbackEvent = NavigationFeedbackEvent(
                PhoneState(context),
                MetricsRouteProgress(routeProgress)
            ).apply {
                this.feedbackType = feedbackType
                this.source = feedbackSource
                this.description = description
                this.screenshot = screenshot
                this.feedbackSubType = feedbackSubType
                this.appMetadata = appMetadata
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
        } else {
            logger?.e(
                TAG,
                Message(
                    "User Feedback event creation failed. The event can only be created in " +
                        "active guidance (trips session started and route is available)."
                )
            )
        }
    }

    fun unregisterListeners(mapboxNavigation: MapboxNavigation) {
        mapboxNavigation.run {
            unregisterLocationObserver(locationsCollector)
            unregisterRouteProgressObserver(this@MapboxNavigationTelemetry)
            unregisterRoutesObserver(this@MapboxNavigationTelemetry)
            unregisterOffRouteObserver(this@MapboxNavigationTelemetry)
            unregisterNavigationSessionObserver(this@MapboxNavigationTelemetry)
            unregisterArrivalObserver(this@MapboxNavigationTelemetry)
        }
        MapboxMetricsReporter.disable()
    }

    override fun onRouteProgressChanged(routeProgress: RouteProgress) {
        this.routeProgress = routeProgress
        startSessionIfNeedAndCan()
    }

    override fun onRoutesChanged(routes: List<DirectionsRoute>) {
        log("onRoutesChanged. size = ${routes.size}")
        routes.getOrNull(0)?.let {
            if (sessionState == ACTIVE_GUIDANCE) {
                if (originalRoute != null) {
                    if (needHandleReroute) {
                        needHandleReroute = false
                        handleReroute(it)
                    } else {
                        log("handle ExternalRoute")
                        sessionStop()
                        originalRoute = it
                        needStartSession = true
                        startSessionIfNeedAndCan()
                    }
                } else {
                    originalRoute = it
                    needStartSession = true
                    startSessionIfNeedAndCan()
                }
            } else {
                originalRoute = it
            }
        }
    }

    override fun onNavigationSessionStateChanged(navigationSession: NavigationSession.State) {
        log("session state is $navigationSession")
        when (navigationSession) {
            IDLE, FREE_DRIVE -> {
                sessionStop()
                handleStateChanged(sessionState, navigationSession)
            }
            ACTIVE_GUIDANCE -> {
                locationsCollector.flushBuffers()
                handleStateChanged(sessionState, navigationSession)
                needStartSession = true
                startSessionIfNeedAndCan()
            }
        }
        sessionState = navigationSession
    }

    private fun handleStateChanged(
        oldState: NavigationSession.State,
        newState: NavigationSession.State
    ) {
        when {
            oldState == FREE_DRIVE && newState == IDLE -> trackFreeDrive(STOP)
            oldState == FREE_DRIVE && newState == ACTIVE_GUIDANCE -> trackFreeDrive(STOP)
            oldState != FREE_DRIVE && newState == FREE_DRIVE -> trackFreeDrive(START)
        }
    }

    private fun trackFreeDrive(type: FreeDriveEventType) {
        log("trackFreeDrive $type")
        dynamicFreeDriveValues.run {
            if (type == START) {
                sessionId = obtainUniversalUniqueIdentifier()
                sessionStartTime = Date()
            }

            createFreeDriveEvent(type, sessionId, sessionStartTime)

            if (type == STOP) {
                reset()
            }
        }
    }

    private fun createFreeDriveEvent(
        type: FreeDriveEventType,
        sessionId: String?,
        sessionStartTime: Date?
    ) {
        log("createFreeDriveEvent $type")
        if (sessionId != null && sessionStartTime != null) {
            val freeDriveEvent = NavigationFreeDriveEvent(PhoneState(context)).apply {
                populate(type, sessionId, sessionStartTime)
            }
            sendEvent(freeDriveEvent)
        } else {
            log(
                "FreeDriveEvent can't be sent. " +
                    "sessionId = $sessionId, sessionStartTime = $sessionStartTime"
            )
        }
    }

    private fun sendEvent(metricEvent: MetricEvent) {
        log("${metricEvent::class.java} event sent")
        metricsReporter.addEvent(metricEvent)
    }

    override fun onOffRouteStateChanged(offRoute: Boolean) {
        log("onOffRouteStateChanged $offRoute")
        if (offRoute) {
            needHandleReroute = true
        }
    }

    override fun onNextRouteLegStart(routeLegProgress: RouteLegProgress) {
        log("onNextRouteLegStart")
        processArrival()
        handleSessionCanceled()
        sessionStart()
    }

    override fun onWaypointArrival(routeProgress: RouteProgress) {
        log("onWaypointDestinationArrival")
    }

    override fun onFinalDestinationArrival(routeProgress: RouteProgress) {
        log("onFinalDestinationArrival")
        this.routeProgress = routeProgress
        processArrival()
    }

    private fun registerListeners(mapboxNavigation: MapboxNavigation) {
        mapboxNavigation.run {
            registerLocationObserver(locationsCollector)
            registerRouteProgressObserver(this@MapboxNavigationTelemetry)
            registerRoutesObserver(this@MapboxNavigationTelemetry)
            registerOffRouteObserver(this@MapboxNavigationTelemetry)
            registerNavigationSessionObserver(this@MapboxNavigationTelemetry)
            registerArrivalObserver(this@MapboxNavigationTelemetry)
        }
    }

    private fun sessionStart() {
        if (dataInitialized()) {
            log("sessionStart")
            dynamicValues.run {
                sessionId = obtainUniversalUniqueIdentifier()
                sessionStartTime = Date()
                sessionStarted = true
            }

            val departEvent = NavigationDepartEvent(PhoneState(context)).apply { populate() }
            sendMetricEvent(departEvent)
        }
    }

    private fun sessionStop() {
        log("sessionStop")
        handleSessionCanceled()
        reset()
    }

    private fun sendMetricEvent(event: MetricEvent) {
        if (isTelemetryAvailable()) {
            sendEvent(event)
        } else {
            log(
                "${event::class.java} not sent. Caused by: " +
                    "Navigation Session started: ${dynamicValues.sessionStarted}. " +
                    "Route exists: ${originalRoute != null}"
            )
        }
    }

    /**
     * The Navigation session is considered to be guided if it has been started and at least one route is active,
     * it is a free drive / idle session otherwise
     */
    private fun isTelemetryAvailable(): Boolean {
        return originalRoute != null && dynamicValues.sessionStarted
    }

    private fun handleReroute(route: DirectionsRoute) {
        if (dynamicValues.sessionStarted && dataInitialized()) {
            log("handleReroute")

            dynamicValues.run {
                val currentTime = Time.SystemImpl.millis()
                timeSinceLastReroute = (currentTime - timeOfReroute).toInt()
                timeOfReroute = currentTime
                rerouteCount++
            }

            val navigationRerouteEvent = NavigationRerouteEvent(
                PhoneState(context),
                MetricsRouteProgress(routeProgress)
            ).apply {
                secondsSinceLastReroute = dynamicValues.timeSinceLastReroute / ONE_SECOND
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

    private fun handleSessionCanceled() {
        if (dynamicValues.sessionStarted && dataInitialized()) {
            log("handleSessionCanceled")
            locationsCollector.flushBuffers()

            val cancelEvent = NavigationCancelEvent(PhoneState(context)).apply { populate() }
            ifNonNull(dynamicValues.sessionArrivalTime) {
                cancelEvent.arrivalTimestamp = generateCreateDateFormatted(it)
            }
            sendMetricEvent(cancelEvent)
        }
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
        if (dynamicValues.sessionStarted && dataInitialized()) {
            log("you have arrived")
            dynamicValues.sessionArrivalTime = Date()

            val arriveEvent = NavigationArriveEvent(PhoneState(context)).apply { populate() }
            sendMetricEvent(arriveEvent)
        }
    }

    private fun NavigationEvent.populate() {
        log("populateNavigationEvent")

        this.apply {
            sdkIdentifier = this@MapboxNavigationTelemetry.sdkIdentifier

            routeProgress!!.let { routeProgress ->
                stepIndex = routeProgress.currentLegProgress?.currentStepProgress?.stepIndex ?: 0

                distanceRemaining = routeProgress.distanceRemaining.toInt()
                durationRemaining = routeProgress.durationRemaining.toInt()
                distanceCompleted = routeProgress.distanceTraveled.toInt()

                routeProgress.route.let {
                    geometry = it.geometry()
                    profile = it.routeOptions()?.profile()
                    requestIdentifier = it?.requestUuid()
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

            originalRoute!!.let {
                originalStepCount = obtainStepCount(it)
                originalEstimatedDistance = it.distance().toInt()
                originalEstimatedDuration = it.duration().toInt()
                originalRequestIdentifier = it?.requestUuid()
                originalGeometry = it.geometry()
            }

            locationEngine = locationEngineNameExternal
            tripIdentifier = obtainUniversalUniqueIdentifier()
            lat = locationsCollector.lastLocation?.latitude ?: 0.0
            lng = locationsCollector.lastLocation?.longitude ?: 0.0
            simulation = locationEngineNameExternal == MOCK_PROVIDER
            percentTimeInPortrait = lifecycleMonitor?.obtainPortraitPercentage() ?: 100
            percentTimeInForeground = lifecycleMonitor?.obtainForegroundPercentage() ?: 100

            dynamicValues.let {
                startTimestamp = generateCreateDateFormatted(it.sessionStartTime)
                rerouteCount = it.rerouteCount
                sessionIdentifier = it.sessionId
            }

            eventVersion = EVENT_VERSION
        }
    }

    private fun NavigationFreeDriveEvent.populate(
        type: FreeDriveEventType,
        sessionId: String,
        sessionStartTime: Date
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
            sessionIdentifier = sessionId
            startTimestamp = generateCreateDateFormatted(sessionStartTime)
        }
    }

    private fun reset() {
        dynamicValues.reset()
        resetOriginalRoute()
        resetRouteProgress()
        needHandleReroute = false
        needStartSession = false
    }

    private fun resetRouteProgress() {
        log("resetRouteProgress")
        routeProgress = null
    }

    private fun resetOriginalRoute() {
        log("resetOriginalRoute")
        originalRoute = null
    }

    private fun startSessionIfNeedAndCan() {
        if (needStartSession && dataInitialized()) {
            needStartSession = false
            sessionStart()
        }
    }

    private fun dataInitialized(): Boolean {
        return originalRoute != null && routeProgress != null
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
}
