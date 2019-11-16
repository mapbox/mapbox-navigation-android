package com.mapbox.services.android.navigation.v5.internal.navigation

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.location.Location
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.telemetry.AppUserTurnstile
import com.mapbox.android.telemetry.TelemetryUtils
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.core.constants.Constants
import com.mapbox.geojson.utils.PolylineUtils
import com.mapbox.navigation.utils.exceptions.NavigationException
import com.mapbox.navigation.utils.extensions.ifNonNull
import com.mapbox.navigation.utils.time.ElapsedTime
import com.mapbox.services.android.navigation.BuildConfig
import com.mapbox.services.android.navigation.v5.internal.location.MetricsLocation
import com.mapbox.services.android.navigation.v5.internal.navigation.metrics.FeedbackEvent
import com.mapbox.services.android.navigation.v5.internal.navigation.metrics.NavigationAppUserTurnstileEvent
import com.mapbox.services.android.navigation.v5.internal.navigation.metrics.NavigationEventFactory
import com.mapbox.services.android.navigation.v5.internal.navigation.metrics.NavigationMetricListener
import com.mapbox.services.android.navigation.v5.internal.navigation.metrics.PhoneState
import com.mapbox.services.android.navigation.v5.internal.navigation.metrics.RerouteEvent
import com.mapbox.services.android.navigation.v5.internal.navigation.metrics.SessionState
import com.mapbox.services.android.navigation.v5.internal.navigation.routeprogress.MetricsRouteProgress
import com.mapbox.services.android.navigation.v5.internal.utils.RingBuffer
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigationOptions
import com.mapbox.services.android.navigation.v5.navigation.metrics.MapboxMetricsReporter
import com.mapbox.services.android.navigation.v5.navigation.metrics.MetricsReporter
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress
import java.util.ArrayList
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@SuppressLint("StaticFieldLeak")
internal object NavigationTelemetry : NavigationMetricListener {
    private const val MAPBOX_NAVIGATION_SDK_IDENTIFIER = "mapbox-navigation-android"
    private const val MAPBOX_NAVIGATION_UI_SDK_IDENTIFIER = "mapbox-navigation-ui-android"
    private const val MOCK_PROVIDER =
        "com.mapbox.services.android.navigation.v5.location.replay.ReplayRouteLocationEngine"
    private const val TWENTY_SECOND_INTERVAL = 20
    private const val LOCATION_BUFFER_MAX_SIZE = 40

    private lateinit var context: Context
    private lateinit var eventDispatcher: NavigationEventDispatcher
    private lateinit var departEventFactory: DepartEventFactory

    private var gpsEventFactory: InitialGpsEventFactory? = null
    private val queuedRerouteEvents = ArrayList<RerouteEvent>()
    private val queuedFeedbackEvents = ArrayList<FeedbackEvent>()
    private val locationBuffer: RingBuffer<Location> = RingBuffer(LOCATION_BUFFER_MAX_SIZE)
    private var metricProgress: MetricsRouteProgress = MetricsRouteProgress(null)
    private var navigationSessionState: SessionState = SessionState()
    private var metricLocation: MetricsLocation = MetricsLocation(null)
    private var isOffRoute: Boolean = false
    private var isInitialized = false

    private var lifecycleMonitor: NavigationLifecycleMonitor? = null
    private var lastRerouteDate: Date? = null
    private var routeRetrievalElapsedTime: ElapsedTime? = null
    private var routeRetrievalUuid: String = "" // empty string is treated as error

    private var sdkIdentifier: String? = null
    private var metricsReporter: MetricsReporter = MapboxMetricsReporter

    @JvmOverloads
    fun initialize(
        context: Context,
        accessToken: String,
        navigation: MapboxNavigation,
        metricsReporter: MetricsReporter = MapboxMetricsReporter
    ) {
        this.metricsReporter = metricsReporter
        if (!isInitialized) {
            validateAccessToken(accessToken)
            val options = navigation.options()
            val sdkIdentifier = obtainSdkIdentifier(options)
            this.sdkIdentifier = sdkIdentifier

            val departEventHandler = DepartEventHandler(context, sdkIdentifier, metricsReporter)
            this.departEventFactory = DepartEventFactory(departEventHandler)
            this.gpsEventFactory = InitialGpsEventFactory(metricsReporter)
            this.context = context

            val appUserTurnstileEvent = AppUserTurnstile(sdkIdentifier, BuildConfig.MAPBOX_NAVIGATION_VERSION_NAME)
            val event = NavigationAppUserTurnstileEvent(appUserTurnstileEvent)
            metricsReporter.addEvent(event)
            isInitialized = true
        }
        this.eventDispatcher = navigation.eventDispatcher
        this.eventDispatcher.addMetricEventListeners(this)
    }

    override fun onRouteProgressUpdate(routeProgress: RouteProgress) {
        this.metricProgress = MetricsRouteProgress(routeProgress)
        updateLifecyclePercentages()
        navigationSessionState = departEventFactory.send(
            navigationSessionState,
            metricProgress,
            metricLocation
        )
    }

    override fun onOffRouteEvent(offRouteLocation: Location) {
        if (!isOffRoute) {
            updateDistanceCompleted()
            queueRerouteEvent()
            isOffRoute = true
        }
    }

    override fun onArrival(routeProgress: RouteProgress) {
        // Update arrival time stamp
        navigationSessionState.apply {
            arrivalTimestamp = Date()
            tripIdentifier = TelemetryUtils.obtainUniversalUniqueIdentifier()
        }
        updateLifecyclePercentages()
        // Send arrival event
        val event = NavigationEventFactory.buildNavigationArriveEvent(
            PhoneState(context),
            navigationSessionState,
            MetricsRouteProgress(routeProgress),
            metricLocation.location,
            sdkIdentifier ?: ""
        )
        metricsReporter.addEvent(event)
    }

    /**
     * Called when navigation is starting for the first time.
     * Initializes the [SessionState].
     *
     * @param directionsRoute first route passed to navigation
     */
    fun startSession(
        directionsRoute: DirectionsRoute,
        locationEngineName: LocationEngine
    ) {
        updateLocationEngineNameAndSimulation(locationEngineName)
        navigationSessionState.apply {
            sessionIdentifier = TelemetryUtils.obtainUniversalUniqueIdentifier()
            tripIdentifier = TelemetryUtils.obtainUniversalUniqueIdentifier()
            originalDirectionRoute = directionsRoute
            originalRequestIdentifier = directionsRoute.routeOptions()?.requestUuid()
            requestIdentifier = directionsRoute.routeOptions()?.requestUuid()
            currentDirectionRoute = directionsRoute
            eventRouteDistanceCompleted = 0.0
            rerouteCount = 0
        }
        sendRouteRetrievalEventIfExists()
        gpsEventFactory?.navigationStarted(navigationSessionState.sessionIdentifier)
    }

    fun stopSession() {
        sendCancelEvent()
        gpsEventFactory?.reset()
        resetDepartFactory()
    }

    /**
     * Called when a new [DirectionsRoute] is given in
     * [MapboxNavigation.startNavigation].
     *
     * At this point, navigation has already begun and the [SessionState]
     * needs to be updated.
     *
     * @param directionsRoute new route passed to [MapboxNavigation]
     */
    fun updateSessionRoute(directionsRoute: DirectionsRoute) {
        navigationSessionState.tripIdentifier = TelemetryUtils.obtainUniversalUniqueIdentifier()
        navigationSessionState.currentDirectionRoute = directionsRoute
        eventDispatcher.addMetricEventListeners(this)
        if (isOffRoute) {
            // If we are off-route, update the reroute count
            navigationSessionState.rerouteCount = navigationSessionState.rerouteCount + 1
            navigationSessionState.requestIdentifier =
                if (directionsRoute.routeOptions() != null) {
                    directionsRoute.routeOptions()?.requestUuid()
                } else {
                    null
                }
            updateLastRerouteEvent(directionsRoute)
            lastRerouteDate = Date()
            isOffRoute = false
        }
    }

    /**
     * Called during [NavigationTelemetry.initialize]
     * and any time [MapboxNavigation] gets an updated location engine.
     */
    fun updateLocationEngineNameAndSimulation(locationEngine: LocationEngine?) {
        if (locationEngine == null) {
            return
        }
        val engineName = locationEngine.javaClass.name
        navigationSessionState.apply {
            locationEngineName = engineName
            mockLocation = engineName == MOCK_PROVIDER
        }
    }

    fun updateLocation(context: Context, location: Location) {
        gpsEventFactory?.gpsReceived(MetadataBuilder.getMetadata(context))
        metricLocation = MetricsLocation(location)
        locationBuffer.addLast(location)
        checkRerouteQueue()
        checkFeedbackQueue()
    }

    /**
     * Creates a new [FeedbackEvent] and adds it to the queue
     * of events to be sent.
     *
     * @param feedbackType defined in FeedbackEvent
     * @param description optional String describing event
     * @param feedbackSource from either reroute or UI
     * @return String feedbackId to identify the event created if needed
     */
    fun recordFeedbackEvent(
        @FeedbackEvent.FeedbackType feedbackType: String,
        description: String,
        @FeedbackEvent.FeedbackSource feedbackSource: String
    ): String {
        val feedbackEvent = queueFeedbackEvent(
            feedbackType,
            description,
            feedbackSource
        )
        return feedbackEvent.eventId
    }

    /**
     * Updates an existing feedback event generated by [MapboxNavigation.recordFeedback].
     *
     * Uses a feedback ID to find the correct event and then adjusts the feedbackType and description.
     *
     * @param feedbackId generated from [MapboxNavigation.recordFeedback]
     * @param feedbackType from list of set feedback types
     * @param description an optional description to provide more detail about the feedback
     * @param screenshot an optional encoded screenshot to provide more detail about the feedback
     */
    fun updateFeedbackEvent(
        feedbackId: String,
        @FeedbackEvent.FeedbackType feedbackType: String,
        description: String,
        screenshot: String?
    ) {
        // Find the event and send
        findQueuedFeedbackEvent(feedbackId)?.let {
            it.feedbackType = feedbackType
            it.description = description
            it.screenshot = screenshot
        }
    }

    /**
     * Cancels an existing feedback event generated by [MapboxNavigation.recordFeedback].
     *
     * Uses a feedback ID to find the correct event and then cancels it (will no longer be recorded).
     *
     * @param feedbackId generated from [MapboxNavigation.recordFeedback]
     */
    fun cancelFeedback(feedbackId: String) {
        // Find the event and remove it from the queue
        queuedFeedbackEvents.remove(findQueuedFeedbackEvent(feedbackId))
    }

    /**
     * Added once created in the [NavigationService], this class
     * provides data regarding the [android.app.Activity] lifecycle.
     *
     * @param application to register the callbacks
     */
    fun initializeLifecycleMonitor(application: Application) {
        if (lifecycleMonitor == null) {
            lifecycleMonitor = NavigationLifecycleMonitor(application)
        }
    }

    fun endSession() {
        flushEventQueues()
        lifecycleMonitor = null
        isInitialized = false
    }

    fun routeRetrievalEvent(
        elapsedTime: ElapsedTime,
        routeUuid: String
    ) {
        if (navigationSessionState.sessionIdentifier.isNotEmpty()) {
            val event = RouteRetrievalEvent(
                elapsedTime.elapsedTime,
                routeUuid,
                navigationSessionState.sessionIdentifier,
                MetadataBuilder.getMetadata(context)
            )
            metricsReporter.addEvent(event)
        } else {
            routeRetrievalElapsedTime = elapsedTime
            routeRetrievalUuid = routeUuid
        }
    }

    private fun validateAccessToken(accessToken: String) {
        if (accessToken.isEmpty() || !accessToken.toLowerCase(Locale.US).startsWith("pk.") && !accessToken.toLowerCase(
                Locale.US
            ).startsWith("sk.")
        ) {
            throw NavigationException("A valid access token must be passed in when first initializing MapboxNavigation")
        }
    }

    private fun obtainSdkIdentifier(options: MapboxNavigationOptions): String =
        if (options.isFromNavigationUi) {
            MAPBOX_NAVIGATION_UI_SDK_IDENTIFIER
        } else {
            MAPBOX_NAVIGATION_SDK_IDENTIFIER
        }

    private fun sendRouteRetrievalEventIfExists() {
        routeRetrievalElapsedTime?.let {
            routeRetrievalEvent(it, routeRetrievalUuid)
            routeRetrievalElapsedTime = null
            routeRetrievalUuid = ""
        }
    }

    private fun sendCancelEvent() {
        ifNonNull(navigationSessionState.startTimestamp) {
            val event = NavigationEventFactory.buildNavigationCancelEvent(
                PhoneState(context),
                navigationSessionState,
                metricProgress,
                metricLocation.location,
                sdkIdentifier ?: ""
            )
            metricsReporter.addEvent(event)
        }
    }

    private fun flushEventQueues() {
        queuedFeedbackEvents.forEach { sendFeedbackEvent(it) }
        queuedRerouteEvents.forEach { sendRerouteEvent(it) }
    }

    private fun checkRerouteQueue() {
        val iterator = queuedRerouteEvents.listIterator()
        while (iterator.hasNext()) {
            val rerouteEvent = iterator.next()
            if (shouldSendEvent(rerouteEvent.sessionState)) {
                sendRerouteEvent(rerouteEvent)
                iterator.remove()
            }
        }
    }

    private fun checkFeedbackQueue() {
        val iterator = queuedFeedbackEvents.listIterator()
        while (iterator.hasNext()) {
            val feedbackEvent = iterator.next()
            if (shouldSendEvent(feedbackEvent.sessionState)) {
                sendFeedbackEvent(feedbackEvent)
                iterator.remove()
            }
        }
    }

    private fun shouldSendEvent(sessionState: SessionState): Boolean =
        dateDiff(sessionState.eventDate, Date()) > TWENTY_SECOND_INTERVAL

    private fun createLocationListBeforeEvent(eventDate: Date?): List<Location>? {
        if (eventDate == null) {
            return null
        }
        val locations = locationBuffer.toTypedArray()
        // Create current list of dates
        val currentLocationList = listOf(*locations)
        // Setup list for dates before the event
        val locationsBeforeEvent = ArrayList<Location>()
        // Add any events before the event date
        for (location in currentLocationList) {
            val locationDate = Date(location.time)
            if (locationDate.before(eventDate)) {
                locationsBeforeEvent.add(location)
            }
        }
        return locationsBeforeEvent
    }

    private fun createLocationListAfterEvent(eventDate: Date?): List<Location>? {
        if (eventDate == null) {
            return null
        }
        val locations = locationBuffer.toTypedArray()
        // Create current list of dates
        val currentLocationList = listOf(*locations)
        // Setup list for dates after the event
        val locationsAfterEvent = ArrayList<Location>()
        // Add any events after the event date
        for (location in currentLocationList) {
            val locationDate = Date(location.time)
            if (locationDate.after(eventDate)) {
                locationsAfterEvent.add(location)
            }
        }
        return locationsAfterEvent
    }

    private fun updateDistanceCompleted() {
        navigationSessionState.eventRouteDistanceCompleted =
            navigationSessionState.eventRouteDistanceCompleted + metricProgress.distanceTraveled
    }

    private fun queueRerouteEvent() {
        updateLifecyclePercentages()
        // Create a new session state given the current navigation session
        val currentDate = Date()
        val rerouteEventSessionState = navigationSessionState.copy()
        rerouteEventSessionState.apply {
            eventDate = currentDate
            eventRouteProgress = metricProgress
            eventLocation = metricLocation.location
            secondsSinceLastReroute = getSecondsSinceLastReroute(currentDate)
        }
        val rerouteEvent = RerouteEvent(rerouteEventSessionState)
        queuedRerouteEvents.add(rerouteEvent)
    }

    private fun queueFeedbackEvent(
        @FeedbackEvent.FeedbackType feedbackType: String,
        description: String,
        @FeedbackEvent.FeedbackSource feedbackSource: String
    ): FeedbackEvent {
        updateLifecyclePercentages()
        // Distance completed = previous distance completed + current RouteProgress distance traveled
        val distanceCompleted =
            navigationSessionState.eventRouteDistanceCompleted + metricProgress.distanceTraveled

        // Create a new session state given the current navigation session
        val feedbackEventSessionState = navigationSessionState.copy()
        feedbackEventSessionState.apply {
            eventDate = Date()
            eventRouteProgress = metricProgress
            eventRouteDistanceCompleted = distanceCompleted
            eventLocation = metricLocation.location
        }
        val feedbackEvent = FeedbackEvent(feedbackEventSessionState, feedbackSource)
        feedbackEvent.description = description
        feedbackEvent.feedbackType = feedbackType
        queuedFeedbackEvents.add(feedbackEvent)
        return feedbackEvent
    }

    private fun sendRerouteEvent(rerouteEvent: RerouteEvent) {
        val rerouteSessionState = rerouteEvent.sessionState
        // If there isn't an updated geometry, don't send
        if (rerouteSessionState.startTimestamp == null) {
            return
        }
        // Update session state with locations from before / after the reroute occurred
        rerouteSessionState.beforeEventLocations = createLocationListBeforeEvent(rerouteSessionState.eventDate)
        rerouteSessionState.afterEventLocations = createLocationListAfterEvent(rerouteSessionState.eventDate)
        val event = NavigationEventFactory.buildNavigationRerouteEvent(
            PhoneState(context),
            rerouteEvent.sessionState,
            metricProgress,
            rerouteSessionState.eventLocation,
            sdkIdentifier ?: "",
            rerouteEvent
        )
        metricsReporter.addEvent(event)
    }

    private fun sendFeedbackEvent(feedbackEvent: FeedbackEvent) {
        val feedbackSessionState = feedbackEvent.sessionState
        if (feedbackEvent.sessionState.startTimestamp == null) {
            return
        }
        // Update sessions state with locations from before / after feedback
        feedbackSessionState.beforeEventLocations = createLocationListBeforeEvent(feedbackSessionState.eventDate)
        feedbackSessionState.afterEventLocations = createLocationListAfterEvent(feedbackSessionState.eventDate)
        val event = NavigationEventFactory.buildNavigationFeedbackEvent(
            PhoneState(context),
            feedbackSessionState,
            metricProgress,
            feedbackSessionState.eventLocation,
            sdkIdentifier ?: "",
            feedbackEvent.description ?: "",
            feedbackEvent.feedbackType,
            feedbackEvent.screenshot ?: "",
            feedbackEvent.feedbackSource
        )
        metricsReporter.addEvent(event)
    }

    private fun dateDiff(
        firstDate: Date?,
        secondDate: Date
    ): Long {
        if (firstDate == null) {
            return 0L
        }
        val diffInMillis = secondDate.time - firstDate.time
        return TimeUnit.SECONDS.convert(diffInMillis, TimeUnit.MILLISECONDS)
    }

    private fun findQueuedFeedbackEvent(eventId: String): FeedbackEvent? {
        for (feedbackEvent in queuedFeedbackEvents) {
            if (feedbackEvent.eventId == eventId) {
                return feedbackEvent
            }
        }
        return null
    }

    private fun updateLifecyclePercentages() {
        lifecycleMonitor?.let {
            navigationSessionState.percentInForeground = it.obtainForegroundPercentage()
            navigationSessionState.percentInPortrait = it.obtainPortraitPercentage()
        }
    }

    private fun updateLastRerouteEvent(newDirectionsRoute: DirectionsRoute) {
        if (queuedRerouteEvents.isEmpty()) {
            return
        }
        val geometryPositions = PolylineUtils.decode(
            newDirectionsRoute.geometry() ?: "",
            Constants.PRECISION_6
        )
        PolylineUtils.encode(geometryPositions, Constants.PRECISION_5)
        queuedRerouteEvents.last().apply {
            newRouteGeometry = PolylineUtils.encode(
                geometryPositions,
                Constants.PRECISION_5
            )
            newDistanceRemaining = newDirectionsRoute.distance()?.toInt() ?: 0
            newDurationRemaining = newDirectionsRoute.duration()?.toInt() ?: 0
        }
    }

    private fun getSecondsSinceLastReroute(eventDate: Date): Int =
        lastRerouteDate?.let {
            val millisSinceLastReroute = eventDate.time - it.time
            TimeUnit.MILLISECONDS.toSeconds(millisSinceLastReroute).toInt()
        } ?: -1

    private fun resetDepartFactory() {
        departEventFactory.reset()
    }
}
