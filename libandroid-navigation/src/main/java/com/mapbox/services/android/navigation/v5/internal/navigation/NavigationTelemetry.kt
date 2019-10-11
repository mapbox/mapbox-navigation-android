package com.mapbox.services.android.navigation.v5.internal.navigation

import android.app.Application
import android.content.Context
import android.location.Location

import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.telemetry.TelemetryUtils
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.core.constants.Constants
import com.mapbox.core.utils.TextUtils
import com.mapbox.geojson.utils.PolylineUtils
import com.mapbox.services.android.navigation.BuildConfig
import com.mapbox.services.android.navigation.v5.internal.exception.NavigationException
import com.mapbox.services.android.navigation.v5.internal.location.MetricsLocation
import com.mapbox.services.android.navigation.v5.internal.navigation.metrics.FeedbackEvent
import com.mapbox.services.android.navigation.v5.internal.navigation.metrics.NavigationMetricListener
import com.mapbox.services.android.navigation.v5.internal.navigation.metrics.RerouteEvent
import com.mapbox.services.android.navigation.v5.internal.navigation.metrics.SessionState
import com.mapbox.services.android.navigation.v5.internal.navigation.metrics.TelemetryEvent
import com.mapbox.services.android.navigation.v5.internal.navigation.routeprogress.MetricsRouteProgress
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigationOptions
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress
import com.mapbox.services.android.navigation.v5.internal.utils.RingBuffer

import java.util.ArrayList
import java.util.Arrays
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

internal object NavigationTelemetry : NavigationMetricListener {
    private const val MAPBOX_NAVIGATION_SDK_IDENTIFIER = "mapbox-navigation-android"
    private const val MAPBOX_NAVIGATION_UI_SDK_IDENTIFIER = "mapbox-navigation-ui-android"
    private const val MOCK_PROVIDER =
        "com.mapbox.services.android.navigation.v5.location.replay" + ".ReplayRouteLocationEngine"
    private const val TWENTY_SECOND_INTERVAL = 20
    private const val LOCATION_BUFFER_MAX_SIZE = 40

    private lateinit var context: Context
    private lateinit var eventDispatcher: NavigationEventDispatcher
    private lateinit var departEventFactory: DepartEventFactory

    private val queuedRerouteEvents = ArrayList<RerouteEvent>()
    private val queuedFeedbackEvents = ArrayList<FeedbackEvent>()
    private val locationBuffer: RingBuffer<Location> = RingBuffer(LOCATION_BUFFER_MAX_SIZE)
    private var metricProgress: MetricsRouteProgress = MetricsRouteProgress(null)
    private var navigationSessionState: SessionState = SessionState.builder().build()
    private var metricLocation: MetricsLocation = MetricsLocation(null)
    private val gpsEventFactory = InitialGpsEventFactory()
    private var isOffRoute: Boolean = false
    private var isInitialized = false

    private var lifecycleMonitor: NavigationLifecycleMonitor? = null
    private var lastRerouteDate: Date? = null
    private var routeRetrievalElapsedTime: ElapsedTime? = null
    private var routeRetrievalUuid: String = "" // empty string is treated as error

    fun initialize(
        context: Context,
        accessToken: String,
        navigation: MapboxNavigation
    ) {
        if (!isInitialized) {
            validateAccessToken(accessToken)
            val departEventHandler = DepartEventHandler(context)
            this.departEventFactory = DepartEventFactory(departEventHandler)
            this.context = context
            val options = navigation.options()
            NavigationMetricsWrapper.init(
                context, accessToken, BuildConfig.MAPBOX_NAVIGATION_EVENTS_USER_AGENT,
                obtainSdkIdentifier(options)
            )
            NavigationMetricsWrapper.toggleLogging(options.isDebugLoggingEnabled)
            val navTurnstileEvent = NavigationMetricsWrapper.turnstileEvent()
            // TODO Check if we are sending two turnstile events (Maps and Nav) and if so, do we want to track them
            // separately?
            NavigationMetricsWrapper.push(navTurnstileEvent)
            isInitialized = true
        }
        this.eventDispatcher = navigation.eventDispatcher
        this.eventDispatcher.addMetricEventListeners(this)
    }

    override fun onRouteProgressUpdate(routeProgress: RouteProgress) {
        this.metricProgress = MetricsRouteProgress(routeProgress)
        updateLifecyclePercentages()
        navigationSessionState =
            departEventFactory.send(navigationSessionState, metricProgress, metricLocation)
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
        navigationSessionState = navigationSessionState.toBuilder()
            .arrivalTimestamp(Date())
            .tripIdentifier(TelemetryUtils.obtainUniversalUniqueIdentifier())
            .build()
        updateLifecyclePercentages()
        // Send arrival event
        NavigationMetricsWrapper.arriveEvent(
            navigationSessionState,
            routeProgress, metricLocation.location, context
        )
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
        navigationSessionState = navigationSessionState.toBuilder()
            .sessionIdentifier(TelemetryUtils.obtainUniversalUniqueIdentifier())
            .tripIdentifier(TelemetryUtils.obtainUniversalUniqueIdentifier())
            .originalDirectionRoute(directionsRoute)
            .originalRequestIdentifier(directionsRoute.routeOptions()?.requestUuid())
            .requestIdentifier(directionsRoute.routeOptions()?.requestUuid())
            .currentDirectionRoute(directionsRoute)
            .eventRouteDistanceCompleted(0.0)
            .rerouteCount(0)
            .build()
        sendRouteRetrievalEventIfExists()
        gpsEventFactory.navigationStarted(navigationSessionState.sessionIdentifier())
    }

    fun stopSession() {
        sendCancelEvent()
        gpsEventFactory.reset()
        resetDepartFactory()
    }

    /**
     * Called when a new [DirectionsRoute] is given in
     * [MapboxNavigation.startNavigation].
     *
     *
     * At this point, navigation has already begun and the [SessionState]
     * needs to be updated.
     *
     * @param directionsRoute new route passed to [MapboxNavigation]
     */
    fun updateSessionRoute(directionsRoute: DirectionsRoute) {
        val navigationBuilder = navigationSessionState.toBuilder()
            .tripIdentifier(TelemetryUtils.obtainUniversalUniqueIdentifier())
        navigationBuilder.currentDirectionRoute(directionsRoute)
        eventDispatcher.addMetricEventListeners(this)

        if (isOffRoute) {
            // If we are off-route, update the reroute count
            navigationBuilder.rerouteCount(navigationSessionState.rerouteCount() + 1)
            val hasRouteOptions = directionsRoute.routeOptions() != null
            navigationBuilder.requestIdentifier(if (hasRouteOptions) directionsRoute.routeOptions()?.requestUuid() else null)
            navigationSessionState = navigationBuilder.build()
            updateLastRerouteEvent(directionsRoute)
            lastRerouteDate = Date()
            isOffRoute = false
        } else {
            // Not current off-route - update the session
            navigationSessionState = navigationBuilder.build()
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

        val locationEngineName = locationEngine.javaClass.name
        val isSimulationEnabled = locationEngineName == MOCK_PROVIDER
        navigationSessionState = navigationSessionState.toBuilder()
            .locationEngineName(locationEngineName)
            .mockLocation(isSimulationEnabled)
            .build()
    }

    fun updateLocation(context: Context, location: Location) {
        gpsEventFactory.gpsReceived(MetadataBuilder.getMetadata(context))
        metricLocation = MetricsLocation(location)
        locationBuffer.addLast(location)
        checkRerouteQueue()
        checkFeedbackQueue()
    }

    /**
     * Creates a new [FeedbackEvent] and adds it to the queue
     * of events to be sent.
     *
     * @param feedbackType   defined in FeedbackEvent
     * @param description    optional String describing event
     * @param feedbackSource from either reroute or UI
     * @return String feedbackId to identify the event created if needed
     */
    fun recordFeedbackEvent(
        @FeedbackEvent.FeedbackType
        feedbackType: String,
        description: String,
        @FeedbackEvent.FeedbackSource feedbackSource: String
    ): String {
        val feedbackEvent = queueFeedbackEvent(feedbackType, description, feedbackSource)
        return feedbackEvent.eventId
    }

    /**
     * Updates an existing feedback event generated by [MapboxNavigation.recordFeedback].
     *
     *
     * Uses a feedback ID to find the correct event and then adjusts the feedbackType and description.
     *
     * @param feedbackId   generated from [MapboxNavigation.recordFeedback]
     * @param feedbackType from list of set feedback types
     * @param description  an optional description to provide more detail about the feedback
     * @param screenshot   an optional encoded screenshot to provide more detail about the feedback
     */
    fun updateFeedbackEvent(
        feedbackId: String,
        @FeedbackEvent.FeedbackType feedbackType: String,
        description: String,
        screenshot: String
    ) {
        // Find the event and send
        val feedbackEvent = findQueuedTelemetryEvent(feedbackId) as FeedbackEvent?
        if (feedbackEvent != null) {
            feedbackEvent.feedbackType = feedbackType
            feedbackEvent.description = description
            feedbackEvent.screenshot = screenshot
        }
    }

    /**
     * Cancels an existing feedback event generated by [MapboxNavigation.recordFeedback].
     *
     *
     * Uses a feedback ID to find the correct event and then cancels it (will no longer be recorded).
     *
     * @param feedbackId generated from [MapboxNavigation.recordFeedback]
     */
    fun cancelFeedback(feedbackId: String) {
        // Find the event and remove it from the queue
        val feedbackEvent = findQueuedTelemetryEvent(feedbackId) as FeedbackEvent?
        queuedFeedbackEvents.remove(feedbackEvent)
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
        NavigationMetricsWrapper.disable()
        isInitialized = false
    }

    internal fun routeRetrievalEvent(
        elapsedTime: ElapsedTime?,
        routeUuid: String
    ) {
        if (navigationSessionState.sessionIdentifier().isNotEmpty()) {
            val time = elapsedTime?.elapsedTime ?: return
            NavigationMetricsWrapper.routeRetrievalEvent(
                time, routeUuid,
                navigationSessionState.sessionIdentifier(), MetadataBuilder.getMetadata(context)
            )
        } else {
            routeRetrievalElapsedTime = elapsedTime
            routeRetrievalUuid = routeUuid
        }
    }

    private fun validateAccessToken(accessToken: String) {
        if (TextUtils.isEmpty(accessToken) || !accessToken.toLowerCase(Locale.US).startsWith("pk.") && !accessToken.toLowerCase(
                Locale.US
            ).startsWith("sk.")
        ) {
            throw NavigationException("A valid access token must be passed in when first initializing" + " MapboxNavigation")
        }
    }

    private fun obtainSdkIdentifier(options: MapboxNavigationOptions): String {
        var sdkIdentifier = MAPBOX_NAVIGATION_SDK_IDENTIFIER
        if (options.isFromNavigationUi) {
            sdkIdentifier = MAPBOX_NAVIGATION_UI_SDK_IDENTIFIER
        }
        return sdkIdentifier
    }

    private fun sendRouteRetrievalEventIfExists() {
        if (routeRetrievalElapsedTime != null) {
            routeRetrievalEvent(routeRetrievalElapsedTime, routeRetrievalUuid)
            routeRetrievalElapsedTime = null
            routeRetrievalUuid = ""
        }
    }

    private fun sendCancelEvent() {
        if (navigationSessionState.startTimestamp() != null) {
            NavigationMetricsWrapper.cancelEvent(
                navigationSessionState, metricProgress, metricLocation.location, context
            )
        }
    }

    private fun flushEventQueues() {
        for (feedbackEvent in queuedFeedbackEvents) {
            sendFeedbackEvent(feedbackEvent)
        }
        for (rerouteEvent in queuedRerouteEvents) {
            sendRerouteEvent(rerouteEvent)
        }
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

    private fun shouldSendEvent(sessionState: SessionState): Boolean {
        return dateDiff(
            sessionState.eventDate(),
            Date(),
            TimeUnit.SECONDS
        ) > TWENTY_SECOND_INTERVAL
    }

    private fun createLocationListBeforeEvent(eventDate: Date?): List<Location> {
        if (eventDate == null) {
            emptyList<Location>()
        }

        val locations = locationBuffer.toTypedArray()
        // Create current list of dates
        val currentLocationList = Arrays.asList(*locations)
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

    private fun createLocationListAfterEvent(eventDate: Date?): List<Location> {
        if (eventDate == null) {
            emptyList<Location>()
        }

        val locations = locationBuffer.toTypedArray()
        // Create current list of dates
        val currentLocationList = Arrays.asList(*locations)
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
        val currentDistanceCompleted =
            navigationSessionState.eventRouteDistanceCompleted() + metricProgress.distanceTraveled
        navigationSessionState = navigationSessionState.toBuilder()
            .eventRouteDistanceCompleted(currentDistanceCompleted)
            .build()
    }

    private fun queueRerouteEvent() {
        updateLifecyclePercentages()
        // Create a new session state given the current navigation session
        val eventDate = Date()
        val rerouteEventSessionState = navigationSessionState.toBuilder()
            .eventDate(eventDate)
            .eventRouteProgress(metricProgress)
            .eventLocation(metricLocation.location)
            .secondsSinceLastReroute(getSecondsSinceLastReroute(eventDate))
            .build()

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
            navigationSessionState.eventRouteDistanceCompleted() + metricProgress.distanceTraveled

        // Create a new session state given the current navigation session
        val feedbackEventSessionState = navigationSessionState.toBuilder()
            .eventDate(Date())
            .eventRouteProgress(metricProgress)
            .eventRouteDistanceCompleted(distanceCompleted)
            .eventLocation(metricLocation.location)
            .build()

        val feedbackEvent = FeedbackEvent(feedbackEventSessionState, feedbackSource)
        feedbackEvent.description = description
        feedbackEvent.feedbackType = feedbackType
        queuedFeedbackEvents.add(feedbackEvent)
        return feedbackEvent
    }

    private fun sendRerouteEvent(rerouteEvent: RerouteEvent) {
        // If there isn't an updated geometry, don't send
        if (rerouteEvent.sessionState.startTimestamp() == null) {
            return
        }
        // Create arrays with locations from before / after the reroute occurred
        val beforeLocations = createLocationListBeforeEvent(rerouteEvent.sessionState.eventDate())
        val afterLocations = createLocationListAfterEvent(rerouteEvent.sessionState.eventDate())
        // Update session state with locations after feedback
        val rerouteSessionState = rerouteEvent.sessionState.toBuilder()
            .beforeEventLocations(beforeLocations)
            .afterEventLocations(afterLocations)
            .build()
        // Set the updated session state
        rerouteEvent.sessionState = rerouteSessionState

        NavigationMetricsWrapper.rerouteEvent(
            rerouteEvent, metricProgress,
            rerouteEvent.sessionState.eventLocation(), context
        )
    }

    private fun sendFeedbackEvent(feedbackEvent: FeedbackEvent) {
        if (feedbackEvent.sessionState.startTimestamp() == null) {
            return
        }
        // Create arrays with locations from before / after the reroute occurred
        val beforeLocations =
            createLocationListBeforeEvent(feedbackEvent.sessionState.eventDate())
        val afterLocations = createLocationListAfterEvent(feedbackEvent.sessionState.eventDate())
        // Update session state with locations after feedback
        val feedbackSessionState = feedbackEvent.sessionState.toBuilder()
            .beforeEventLocations(beforeLocations)
            .afterEventLocations(afterLocations)
            .build()

        NavigationMetricsWrapper.feedbackEvent(
            feedbackSessionState,
            metricProgress,
            feedbackEvent.sessionState.eventLocation(),
            feedbackEvent.description ?: "",
            feedbackEvent.feedbackType,
            feedbackEvent.screenshot,
            feedbackEvent.feedbackSource,
            context
        )
    }

    private fun dateDiff(
        firstDate: Date?,
        secondDate: Date,
        timeUnit: TimeUnit
    ): Long {
        if (firstDate == null) {
            return 0L
        }

        val diffInMillis = secondDate.time - firstDate.time
        return timeUnit.convert(diffInMillis, TimeUnit.MILLISECONDS)
    }

    private fun findQueuedTelemetryEvent(eventId: String): TelemetryEvent? {
        for (feedbackEvent in queuedFeedbackEvents) {
            if (feedbackEvent.eventId == eventId) {
                return feedbackEvent
            }
        }
        for (rerouteEvent in queuedRerouteEvents) {
            if (rerouteEvent.eventId == eventId) {
                return rerouteEvent
            }
        }
        return null
    }

    private fun updateLifecyclePercentages() {
        lifecycleMonitor?.let {
            navigationSessionState = navigationSessionState.toBuilder()
                .percentInForeground(it.obtainForegroundPercentage())
                .percentInPortrait(it.obtainPortraitPercentage())
                .build()
        }
    }

    private fun updateLastRerouteEvent(newDirectionsRoute: DirectionsRoute) {
        if (queuedRerouteEvents.isEmpty()) {
            return
        }

        val rerouteEvent = queuedRerouteEvents[queuedRerouteEvents.size - 1]
        val geometryPositions =
            PolylineUtils.decode(newDirectionsRoute.geometry() ?: "", Constants.PRECISION_6)
        PolylineUtils.encode(geometryPositions, Constants.PRECISION_5)
        rerouteEvent.newRouteGeometry =
            PolylineUtils.encode(geometryPositions, Constants.PRECISION_5)
        val newDistanceRemaining = newDirectionsRoute.distance()?.toInt() ?: 0
        rerouteEvent.newDistanceRemaining = newDistanceRemaining
        val newDurationRemaining = newDirectionsRoute.duration()?.toInt() ?: 0
        rerouteEvent.newDurationRemaining = newDurationRemaining
    }

    private fun getSecondsSinceLastReroute(eventDate: Date): Int {
        val seconds = -1
        val rerouteDate = lastRerouteDate
        if (rerouteDate == null) {
            return seconds
        } else {
            val millisSinceLastReroute = eventDate.time - rerouteDate.time
            return TimeUnit.MILLISECONDS.toSeconds(millisSinceLastReroute).toInt()
        }
    }

    private fun resetDepartFactory() {
        departEventFactory.reset()
    }
}
