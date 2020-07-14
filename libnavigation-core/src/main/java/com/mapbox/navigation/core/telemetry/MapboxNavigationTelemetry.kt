package com.mapbox.navigation.core.telemetry

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.location.Location
import android.util.Log
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.telemetry.AppUserTurnstile
import com.mapbox.android.telemetry.TelemetryUtils
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.navigation.base.metrics.MetricEvent
import com.mapbox.navigation.base.metrics.MetricsReporter
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.trip.model.RouteProgressState
import com.mapbox.navigation.core.BuildConfig
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.NavigationSession
import com.mapbox.navigation.core.NavigationSessionStateObserver
import com.mapbox.navigation.core.internal.accounts.MapboxNavigationAccounts
import com.mapbox.navigation.core.telemetry.events.AppMetadata
import com.mapbox.navigation.core.telemetry.events.FeedbackEvent
import com.mapbox.navigation.core.telemetry.events.MetricsRouteProgress
import com.mapbox.navigation.core.telemetry.events.NavigationArriveEvent
import com.mapbox.navigation.core.telemetry.events.NavigationCancelEvent
import com.mapbox.navigation.core.telemetry.events.NavigationDepartEvent
import com.mapbox.navigation.core.telemetry.events.NavigationEvent
import com.mapbox.navigation.core.telemetry.events.NavigationFeedbackEvent
import com.mapbox.navigation.core.telemetry.events.NavigationRerouteEvent
import com.mapbox.navigation.core.telemetry.events.PhoneState
import com.mapbox.navigation.core.telemetry.events.RerouteEvent
import com.mapbox.navigation.core.telemetry.events.SessionState
import com.mapbox.navigation.metrics.MapboxMetricsReporter
import com.mapbox.navigation.metrics.internal.event.NavigationAppUserTurnstileEvent
import com.mapbox.navigation.utils.internal.JobControl
import com.mapbox.navigation.utils.internal.Time
import com.mapbox.navigation.utils.internal.ifChannelException
import com.mapbox.navigation.utils.internal.ifNonNull
import com.mapbox.navigation.utils.internal.monitorChannelWithException
import java.lang.ref.WeakReference
import java.util.ArrayDeque
import java.util.Date
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select

private data class DynamicallyUpdatedRouteValues(
    val distanceRemaining: AtomicLong = AtomicLong(0),
    val timeRemaining: AtomicInteger = AtomicInteger(0),
    val rerouteCount: AtomicInteger = AtomicInteger(0),
    val routeArrived: AtomicBoolean = AtomicBoolean(false),
    val timeOfRerouteEvent: AtomicLong = AtomicLong(0),
    val timeSinceLastReroute: AtomicInteger = AtomicInteger(0),
    var sessionId: String = TelemetryUtils.obtainUniversalUniqueIdentifier(),
    val distanceCompleted: AtomicReference<Float> = AtomicReference(0f),
    val durationRemaining: AtomicInteger = AtomicInteger(0),
    val tripIdentifier: AtomicReference<String> = AtomicReference(TelemetryUtils.obtainUniversalUniqueIdentifier()),
    var sessionStartTime: Date = Date(),
    var sessionArrivalTime: AtomicReference<Date?> = AtomicReference(null),
    var sdkId: String = "none",
    val sessionStarted: AtomicBoolean = AtomicBoolean(false),
    val originalRoute: AtomicReference<DirectionsRoute?> = AtomicReference(null)
) {
    fun reset() {
        distanceRemaining.set(0)
        timeRemaining.set(0)
        rerouteCount.set(0)
        routeArrived.set(false)
        timeOfRerouteEvent.set(0)
        sessionId = TelemetryUtils.obtainUniversalUniqueIdentifier()
        distanceCompleted.set(0f)
        durationRemaining.set(0)
        timeSinceLastReroute.set(0)
        tripIdentifier.set(TelemetryUtils.obtainUniversalUniqueIdentifier())
        sessionArrivalTime.set(null)
        sessionStarted.set(false)
    }
}

/**
 * The one and only Telemetry class. This class handles all telemetry events.
 * Event List:
- appUserTurnstile
- navigation.depart
- navigation.feedback
- navigation.reroute
- navigation.fasterRoute
- navigation.arrive
- navigation.cancel
The class must be initialized before any telemetry events are reported. Attempting to use telemetry before initialization is called will throw an exception. Initialization may be called multiple times, the call is idempotent.
The class has two public methods, postUserFeedback() and initialize().
 */
@SuppressLint("StaticFieldLeak")
internal object MapboxNavigationTelemetry : MapboxNavigationTelemetryInterface {
    internal const val LOCATION_BUFFER_MAX_SIZE = 20
    private const val ONE_SECOND = 1000
    private const val MOCK_PROVIDER =
        "com.mapbox.navigation.core.replay.ReplayLocationEngine"

    internal const val TAG = "MAPBOX_TELEMETRY"
    private const val EVENT_VERSION = 7
    private lateinit var context: Context // Must be context.getApplicationContext
    private lateinit var telemetryThreadControl: JobControl
    private val telemetryScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var metricsReporter: MetricsReporter
    private lateinit var navigationOptions: NavigationOptions
    private lateinit var localUserAgent: String
    private var weakMapboxNavigation = WeakReference<MapboxNavigation>(null)
    private var lifecycleMonitor: ApplicationLifecycleMonitor? = null
    private var appInstance: Application? = null
        set(value) {
            // Don't set it multiple times to the same value, it will cause multiple registration calls.
            if (field == value) {
                return
            }
            field = value
            ifNonNull(value) { app ->
                Log.d(TAG, "Lifecycle monitor created")
                lifecycleMonitor = ApplicationLifecycleMonitor(app)
            }
        }

    /**
     * This class holds all mutable state of the Telemetry object
     */
    private val dynamicValues = DynamicallyUpdatedRouteValues()

    private var locationEngineNameExternal: String = LocationEngine::javaClass.name

    private lateinit var callbackDispatcher: TelemetryLocationAndProgressDispatcher
    private val navigationSessionObserver = object : NavigationSessionStateObserver {
        override fun onNavigationSessionStateChanged(navigationSession: NavigationSession.State) {
            when (navigationSession) {
                NavigationSession.State.FREE_DRIVE -> {
                    Log.d(TAG, "Navigation state is $navigationSession")
                    switchToNotActiveGuidanceBehavior()
                }
                NavigationSession.State.ACTIVE_GUIDANCE -> {
                    Log.d(TAG, "Navigation state is $navigationSession")
                    sessionStart()
                }
                NavigationSession.State.IDLE -> {
                    Log.d(TAG, "Navigation state is $navigationSession")
                    switchToNotActiveGuidanceBehavior()
                }
            }
            Log.d(TAG, "Current session state is: $navigationSession")
        }
    }

    private fun switchToNotActiveGuidanceBehavior() {
        sessionEndPredicate()
        sessionEndPredicate = {}
        postUserEventDelegate = postUserEventBeforeInit
    }

    private fun telemetryEventGate(event: MetricEvent) =
        when (isTelemetryAvailable()) {
            false -> {
                Log.i(TAG, "Route not selected. Telemetry event not sent. Caused by: Navigation " +
                    "Session started: ${dynamicValues.sessionStarted.get()} Route exists: ${dynamicValues.originalRoute.get() != null}")
                false
            }
            true -> {
                metricsReporter.addEvent(event)
                true
            }
        }

// **********  EVENT OBSERVERS ***************

    private fun populateOriginalRouteConditionally() {
        ifNonNull(weakMapboxNavigation.get()) { mapboxNavigation ->
            val routes = mapboxNavigation.getRoutes()
            if (routes.isNotEmpty()) {
                Log.d(TAG, "Getting last route from MapboxNavigation")
                callbackDispatcher.clearOriginalRoute()
                callbackDispatcher.getOriginalRouteReadWrite().set(RouteAvailable(routes[0], Date()))
            }
        }
    }

    private fun sessionStart() {
        var isActiveGuidance = true
        ifNonNull(weakMapboxNavigation.get()?.getRoutes()) { routes ->
            isActiveGuidance = routes.isNotEmpty()
        }
        when (isActiveGuidance) {
            true -> {
                telemetryThreadControl.scope.launch {
                    callbackDispatcher.resetRouteProgressProcessor()
                    postUserEventDelegate =
                        postUserFeedbackEventAfterInit // Telemetry is initialized and the user selected a route. Allow user feedback events to be posted
                    handleSessionStart()
                    sessionEndPredicate = {
                        telemetryScope.launch {
                            sessionStop()
                        }
                    }
                }
            }
            false -> {
                // Do nothing
                Log.d(TAG, "Only Active Guidance supported")
            }
        }
    }

    private suspend fun sessionStop() {
        Log.d(TAG, "sessionStop")
        postUserEventDelegate =
            postUserEventBeforeInit // The navigation session is over, disallow posting user feedback events
        when (dynamicValues.routeArrived.get()) { // Cancellation events will be sent after an arrival event.
            true, false -> {
                telemetryScope.launch {
                    Log.d(TAG, "calling processCancellationAfterArrival()")
                    processCancellation()
                }.join()
            }
        }
    }

    /**
     * The Navigation session is considered to be guided if it has been started and at least one route is active,
     * it is a free guided / idle session otherwise
     */
    private fun isTelemetryAvailable(): Boolean {
        return dynamicValues.originalRoute.get() != null && dynamicValues.sessionStarted.get()
    }

    /**
     * This method generates an off-route telemetry event. Part of the code is suspendable
     * because it waits for a new route to be offered by the SDK in response to a reroute
     */
    private fun handleOffRouteEvent() {
        telemetryThreadControl.scope.launch {
            dynamicValues.timeOfRerouteEvent.set(Time.SystemImpl.millis())
            dynamicValues.rerouteCount.addAndGet(1) // increment reroute count
            val prevRoute = callbackDispatcher.getRouteProgress()
            val newRoute = callbackDispatcher.getDirectionsRouteChannel().receive() // Suspend until we get a value

            dynamicValues.distanceRemaining.set(newRoute.route.distance()?.toLong() ?: -1)
            dynamicValues.timeSinceLastReroute.set((Time.SystemImpl.millis() - dynamicValues.timeOfRerouteEvent.get()).toInt())
            callbackDispatcher.addLocationEventDescriptor(ItemAccumulationEventDescriptor(
                ArrayDeque(callbackDispatcher.getCopyOfCurrentLocationBuffer()),
                ArrayDeque()
            ) { preEventBuffer, postEventBuffer ->
                telemetryThreadControl.scope.launch {

                    // Populate the RerouteEvent
                    val rerouteEvent = RerouteEvent(populateSessionState()).apply {
                        newDistanceRemaining = newRoute.route.distance()?.toInt() ?: -1
                        newDurationRemaining = newRoute.route.duration()?.toInt() ?: -1
                        newRouteGeometry = obtainGeometry(newRoute.route)
                    }

                    // Populate and then send a NavigationRerouteEvent
                    val metricsRouteProgress = MetricsRouteProgress(prevRoute.routeProgress)
                    val navigationRerouteEvent = NavigationRerouteEvent(PhoneState(context), rerouteEvent, metricsRouteProgress).apply {
                        locationsBefore = preEventBuffer.toTypedArray()
                        locationsAfter = postEventBuffer.toTypedArray()
                        secondsSinceLastReroute = dynamicValues.timeSinceLastReroute.get() / ONE_SECOND
                        distanceRemaining = dynamicValues.distanceRemaining.get().toInt()
                        distanceCompleted = dynamicValues.distanceCompleted.get().toInt()
                        durationRemaining = dynamicValues.durationRemaining.get()
                    }
                    populateNavigationEvent(navigationRerouteEvent)
                    val result = telemetryEventGate(
                        navigationRerouteEvent
                    )
                    Log.d(TAG, "REROUTE event sent $result")
                }
            })
        }
    }

    /**
     * The lambda that is called if the SDK client did not initialize telemetry. If telemetry is not initialized,
     * calls to post a user feedback event will fail silently
     */
    private val postUserEventBeforeInit: suspend (String, String, String, String?, Array<String>?, AppMetadata?) -> Unit =
        { _, _, _, _, _, _ ->
            Log.d(TAG, "Not in a navigation session, cannot send user feedback events")
        }

    /**
     * The lambda that is called once telemetry is initialized.
     */
    private val postUserFeedbackEventAfterInit: suspend (String, String, String, String?, Array<String>?, AppMetadata?) -> Unit =
        { feedbackType, description, feedbackSource, screenshot, feedbackSubType, appMetadata ->
            postUserFeedbackHelper(
                feedbackType,
                description,
                feedbackSource,
                screenshot,
                feedbackSubType,
                appMetadata
            )
        }

    /**
     * The delegate lambda that dispatches either a pre or post initialization userFeedbackEvent
     */
    private var postUserEventDelegate = postUserEventBeforeInit

    /**
     * One-time initializer. Called in response to initialize() and then replaced with a no-op lambda to prevent multiple initialize() calls
     */
    private val preInitializePredicate: (Context, MapboxNavigation, MetricsReporter, String, JobControl, NavigationOptions, String) -> Boolean =
        { context, mapboxNavigation, metricsReporter, name, jobControl, options, userAgent ->
            telemetryThreadControl = jobControl
            weakMapboxNavigation = WeakReference(mapboxNavigation)
            weakMapboxNavigation.get()?.registerNavigationSessionObserver(navigationSessionObserver)
            registerForNotification(mapboxNavigation)
            monitorOffRouteEvents()
            populateOriginalRouteConditionally()
            this.context = context
            localUserAgent = userAgent
            locationEngineNameExternal = name
            navigationOptions = options
            this.metricsReporter = metricsReporter
            initializer =
                postInitializePredicate // prevent primaryInitializer() from being called more than once.
            postTurnstileEvent()
            monitorJobCancelation()
            Log.i(TAG, "Valid initialization")
            true
        }

    // Calling initialize multiple times does no harm. This call is a no-op.
    private var postInitializePredicate: (Context, MapboxNavigation, MetricsReporter, String, JobControl, NavigationOptions, String) -> Boolean =
        { _, _, _, _, _, _, _ ->
            Log.i(TAG, "Already initialized")
            false
        }

    private var initializer =
        preInitializePredicate // The initialize dispatcher that points to either pre or post initialization lambda

    private var sessionEndPredicate = { }

    fun setApplicationInstance(app: Application) {
        appInstance = app
    }

    /**
     * This method must be called before using the Telemetry object
     */
    fun initialize(
        context: Context,
        mapboxNavigation: MapboxNavigation,
        metricsReporter: MetricsReporter,
        locationEngineName: String,
        jobControl: JobControl,
        options: NavigationOptions,
        userAgent: String
    ) = initializer(
        context,
        mapboxNavigation,
        metricsReporter,
        locationEngineName,
        jobControl,
        options,
        userAgent
    )

    private fun monitorOffRouteEvents() {
        telemetryThreadControl.scope.monitorChannelWithException(callbackDispatcher.getOffRouteEventChannel(), { offRoute ->
            when (offRoute) {
                true -> {
                    handleOffRouteEvent()
                }
                false -> {
                }
            }
        })
    }

    private fun monitorJobCancelation() {
        telemetryScope.launch {
            select {
                telemetryThreadControl.job.onJoin {
                    Log.d(TAG, "master job canceled")
                    callbackDispatcher.flushBuffers()
                    MapboxMetricsReporter.disable() // Disable telemetry unconditionally
                }
            }
        }
    }

    /**
     * This method sends a user feedback event to the back-end servers. The method will suspend because the helper method it calls is itself suspendable
     * The method may suspend until it collects 40 location events. The worst case scenario is a 40 location suspension, 20 is best case
     */
    override fun postUserFeedback(
        @FeedbackEvent.Type feedbackType: String,
        description: String,
        @FeedbackEvent.Source feedbackSource: String,
        screenshot: String?,
        feedbackSubType: Array<String>?,
        appMetadata: AppMetadata?
    ) {
        telemetryThreadControl.scope.launch {
            postUserEventDelegate(feedbackType, description, feedbackSource, screenshot, feedbackSubType, appMetadata)
        }
    }

    /**
     * Helper class that posts user feedback. The call is available only after initialization
     */
    private fun postUserFeedbackHelper(
        @FeedbackEvent.Type feedbackType: String,
        description: String,
        @FeedbackEvent.Source feedbackSource: String,
        screenshot: String?,
        feedbackSubType: Array<String>?,
        appMetadata: AppMetadata?
    ) {
        Log.d(TAG, "trying to post a user feedback event")
        val lastProgress = callbackDispatcher.getRouteProgress()
        callbackDispatcher.addLocationEventDescriptor(ItemAccumulationEventDescriptor(
            ArrayDeque(callbackDispatcher.getCopyOfCurrentLocationBuffer()),
            ArrayDeque()
        ) { preEventBuffer, postEventBuffer ->
            val feedbackEvent = NavigationFeedbackEvent(PhoneState(context), MetricsRouteProgress(lastProgress.routeProgress)).apply {
                this.feedbackType = feedbackType
                this.source = feedbackSource
                this.description = description
                this.screenshot = screenshot
                this.locationsBefore = preEventBuffer.toTypedArray()
                this.locationsAfter = postEventBuffer.toTypedArray()
                this.feedbackSubType = feedbackSubType
                this.appMetadata = appMetadata
            }
            populateNavigationEvent(feedbackEvent)
            val eventPosted = telemetryEventGate(feedbackEvent)
            Log.i(TAG, "Posting a user feedback event $eventPosted")
        })
    }

    /**
     * This method posts a cancel event in response to onSessionEnd
     */
    private suspend fun handleSessionCanceled(): CompletableDeferred<Boolean> {
        val retVal = CompletableDeferred<Boolean>()
        val cancelEvent = NavigationCancelEvent(PhoneState(context))
        ifNonNull(dynamicValues.sessionArrivalTime.get()) {
            cancelEvent.arrivalTimestamp = TelemetryUtils.generateCreateDateFormatted(it)
        }
        populateNavigationEvent(cancelEvent)
        val result = telemetryEventGate(cancelEvent)
        Log.d(TAG, "CANCEL event sent $result")
        callbackDispatcher.cancelCollectionAndPostFinalEvents().join()
        retVal.complete(true)
        return retVal
    }

    /**
     * This method clears the state data for the Telemetry object in response to onSessionEnd
     */
    private fun handleSessionStop() {
        dynamicValues.reset()
        callbackDispatcher.clearOriginalRoute()
    }

    /**
     * This method starts a session. If a session is active it will terminate it, causing an stop/cancel event to be sent to the servers.
     * Every session start is guaranteed to have a session end.
     */
    private fun handleSessionStart() {
        telemetryThreadControl.scope.launch {
            Log.d(TAG, "Waiting in handleSessionStart")
            dynamicValues.originalRoute.set(callbackDispatcher.getOriginalRouteAsync().await())
            dynamicValues.originalRoute.get()?.let { directionsRoute ->
                Log.d(TAG, "The wait is over")
                sessionStartHelper(directionsRoute)
            }
        }
    }

    /**
     * This method is used by a lambda. Since the Telemetry class is a singleton, U.I. elements may call postTurnstileEvent() before the singleton is initialized.
     * A lambda guards against this possibility
     */
    private fun postTurnstileEvent() {
        // AppUserTurnstile is implemented in mapbox-telemetry-sdk
        val sdkId = generateSdkIdentifier()
        dynamicValues.sdkId = generateSdkIdentifier()
        val appUserTurnstileEvent =
            AppUserTurnstile(sdkId, BuildConfig.MAPBOX_NAVIGATION_VERSION_NAME).also {
                it.setSkuId(
                    MapboxNavigationAccounts.getInstance(
                        context
                    ).obtainSkuId()
                )
            }
        val event = NavigationAppUserTurnstileEvent(appUserTurnstileEvent)
        metricsReporter.addEvent(event)
    }

    /**
     * This method starts a session. The start of a session does not result in a telemetry event being sent to the servers.
     */
    private fun sessionStartHelper(directionsRoute: DirectionsRoute) {
        dynamicValues.sessionId = TelemetryUtils.obtainUniversalUniqueIdentifier()
        dynamicValues.sessionStartTime = Date()
        dynamicValues.sessionStarted.set(true)
        telemetryThreadControl.scope.launch {
            val result = telemetryEventGate(telemetryDeparture(directionsRoute, callbackDispatcher.getFirstLocationAsync().await()))
            Log.d(TAG, "DEPARTURE event sent $result")
            monitorSession()
        }
    }

    private suspend fun processCancellation() {
        Log.d(TAG, "Session was canceled")
        handleSessionCanceled().await()
        handleSessionStop()
    }

    private suspend fun processArrival() {
        Log.d(TAG, "you have arrived")
        dynamicValues.tripIdentifier.set(TelemetryUtils.obtainUniversalUniqueIdentifier())
        dynamicValues.sessionArrivalTime.set(Date())
        val arriveEvent = NavigationArriveEvent(PhoneState(context))
        dynamicValues.routeArrived.set(true)
        populateNavigationEvent(arriveEvent)
        val result = telemetryEventGate(arriveEvent)
        Log.d(TAG, "ARRIVAL event sent $result")
        callbackDispatcher.cancelCollectionAndPostFinalEvents().join()
        populateOriginalRouteConditionally()
    }

    /**
     * This method waits for an [RouteProgressState.ROUTE_COMPLETE] event. Once received, it terminates the wait-loop and
     * sends the telemetry data to the servers.
     */
    private suspend fun monitorSession() {
        var continueRunning = true
        var trackingEvent = 0
        while (coroutineContext.isActive && continueRunning) {
            try {
                val routeData = callbackDispatcher.getRouteProgressChannel().receive()
                dynamicValues.distanceCompleted.set(dynamicValues.distanceCompleted.get() + routeData.routeProgress.distanceTraveled)
                dynamicValues.distanceRemaining.set(routeData.routeProgress.distanceRemaining.toLong())
                dynamicValues.durationRemaining.set(routeData.routeProgress.durationRemaining.toInt())
                when (routeData.routeProgress.currentState) {
                    RouteProgressState.ROUTE_COMPLETE -> {
                        when (dynamicValues.sessionStarted.get()) {
                            true -> {
                                processArrival()
                                continueRunning = false
                            }
                            false -> {
                                // Do nothing.
                                Log.d(TAG, "route arrival received before a session start")
                            }
                        }
                    } // END
                    RouteProgressState.LOCATION_TRACKING -> {
                        dynamicValues.timeRemaining.set(callbackDispatcher.getRouteProgress().routeProgress.durationRemaining.toInt())
                        dynamicValues.distanceRemaining.set(callbackDispatcher.getRouteProgress().routeProgress.distanceRemaining.toLong())
                        when (trackingEvent > 20) {
                            true -> {
                                Log.i(TAG, "LOCATION_TRACKING received $trackingEvent")
                                trackingEvent = 0
                            }
                            false -> {
                                trackingEvent++
                            }
                        }
                    }
                    else -> {
                        // Do nothing
                    }
                }
            } catch (e: Exception) {
                Log.i(TAG, "monitorSession ${e.localizedMessage}")
                e.ifChannelException {
                    continueRunning = false
                }
            }
        }
    }

    private fun telemetryDeparture(directionsRoute: DirectionsRoute, startingLocation: Location): MetricEvent {
        val departEvent = NavigationDepartEvent(PhoneState(context))
        populateNavigationEvent(departEvent, directionsRoute, startingLocation)
        return departEvent
    }

    private fun registerForNotification(mapboxNavigation: MapboxNavigation) {
        callbackDispatcher = TelemetryLocationAndProgressDispatcher(telemetryThreadControl.scope) // The class responds to most notification events
        mapboxNavigation.registerRouteProgressObserver(callbackDispatcher)
        mapboxNavigation.registerLocationObserver(callbackDispatcher)
        mapboxNavigation.registerRoutesObserver(callbackDispatcher)
        mapboxNavigation.registerOffRouteObserver(callbackDispatcher)
    }

    override fun unregisterListeners(mapboxNavigation: MapboxNavigation) {
        mapboxNavigation.unregisterRouteProgressObserver(callbackDispatcher)
        mapboxNavigation.unregisterLocationObserver(callbackDispatcher)
        mapboxNavigation.unregisterRoutesObserver(callbackDispatcher)
        mapboxNavigation.unregisterOffRouteObserver(callbackDispatcher)
        mapboxNavigation.unregisterNavigationSessionObserver(navigationSessionObserver)
        Log.d(TAG, "resetting Telemetry initialization")
        initializer = preInitializePredicate
    }

    private fun populateSessionState(newLocation: Location? = null): SessionState {
        val timeSinceReroute: Int = if (dynamicValues.timeSinceLastReroute.get() == 0) {
            -1
        } else
            dynamicValues.timeSinceLastReroute.get()

        return SessionState().apply {
            secondsSinceLastReroute = timeSinceReroute
            eventLocation = newLocation ?: callbackDispatcher.getLastLocation() ?: Location("unknown")

            eventDate = Date()
            eventRouteDistanceCompleted = callbackDispatcher.getRouteProgress().routeProgress.distanceTraveled.toDouble()
            originalDirectionRoute = callbackDispatcher.getOriginalRouteReadOnly()?.route
            currentDirectionRoute = callbackDispatcher.getRouteProgress().routeProgress.route
            sessionIdentifier = dynamicValues.sessionId
            tripIdentifier = dynamicValues.tripIdentifier.get()
            originalRequestIdentifier = callbackDispatcher.getOriginalRouteReadOnly()?.route?.routeOptions()?.requestUuid()
            requestIdentifier = callbackDispatcher.getRouteProgress().routeProgress.route.routeOptions()?.requestUuid()
            mockLocation = locationEngineName == MOCK_PROVIDER
            rerouteCount = dynamicValues.rerouteCount.get()
            startTimestamp = dynamicValues.sessionStartTime
            arrivalTimestamp = dynamicValues.sessionArrivalTime.get()
            locationEngineName = locationEngineNameExternal
            percentInForeground = lifecycleMonitor?.obtainForegroundPercentage() ?: 100
            percentInPortrait = lifecycleMonitor?.obtainPortraitPercentage() ?: 100
        }
    }

    private fun populateNavigationEvent(navigationEvent: NavigationEvent, route: DirectionsRoute? = null, newLocation: Location? = null) {
        val directionsRoute = route ?: callbackDispatcher.getRouteProgress().routeProgress.route
        val location = newLocation ?: callbackDispatcher.getLastLocation()
        navigationEvent.startTimestamp = TelemetryUtils.generateCreateDateFormatted(dynamicValues.sessionStartTime)
        navigationEvent.sdkIdentifier = generateSdkIdentifier()
        navigationEvent.sessionIdentifier = dynamicValues.sessionId
        navigationEvent.geometry = callbackDispatcher.getRouteProgress().routeProgress.route.geometry()
        navigationEvent.profile = callbackDispatcher.getRouteProgress().routeProgress.route.routeOptions()?.profile()
        navigationEvent.originalRequestIdentifier = callbackDispatcher.getOriginalRouteReadOnly()?.route?.routeOptions()?.requestUuid()
        navigationEvent.requestIdentifier = callbackDispatcher.getRouteProgress().routeProgress.route.routeOptions()?.requestUuid()
        navigationEvent.originalGeometry = callbackDispatcher.getOriginalRouteReadOnly()?.route?.geometry()
        navigationEvent.locationEngine = locationEngineNameExternal
        navigationEvent.tripIdentifier = TelemetryUtils.obtainUniversalUniqueIdentifier()
        navigationEvent.lat = location?.latitude ?: 0.0
        navigationEvent.lng = location?.longitude ?: 0.0
        navigationEvent.simulation = locationEngineNameExternal == MOCK_PROVIDER
        navigationEvent.absoluteDistanceToDestination = obtainAbsoluteDistance(callbackDispatcher.getLastLocation(), obtainRouteDestination(directionsRoute))
        navigationEvent.percentTimeInPortrait = lifecycleMonitor?.obtainPortraitPercentage() ?: 100
        navigationEvent.percentTimeInForeground = lifecycleMonitor?.obtainForegroundPercentage() ?: 100
        navigationEvent.distanceCompleted = dynamicValues.distanceCompleted.get().toInt()
        navigationEvent.distanceRemaining = dynamicValues.distanceRemaining.get().toInt()
        navigationEvent.durationRemaining = dynamicValues.durationRemaining.get().toInt()
        navigationEvent.eventVersion = EVENT_VERSION
        navigationEvent.estimatedDistance = directionsRoute?.distance()?.toInt() ?: 0
        navigationEvent.estimatedDuration = directionsRoute?.duration()?.toInt() ?: 0
        navigationEvent.rerouteCount = dynamicValues.rerouteCount.get()
        navigationEvent.originalEstimatedDistance = callbackDispatcher.getOriginalRouteReadOnly()?.route?.distance()?.toInt() ?: 0
        navigationEvent.originalEstimatedDuration = callbackDispatcher.getOriginalRouteReadOnly()?.route?.duration()?.toInt() ?: 0
        navigationEvent.stepCount = obtainStepCount(callbackDispatcher.getRouteProgress().routeProgress.route)
        navigationEvent.originalStepCount = obtainStepCount(callbackDispatcher.getOriginalRouteReadOnly()?.route)
        navigationEvent.legIndex = callbackDispatcher.getRouteProgress().routeProgress.route.routeIndex()?.toInt() ?: 0
        navigationEvent.legCount = callbackDispatcher.getRouteProgress().routeProgress.route.legs()?.size ?: 0
        navigationEvent.stepIndex = callbackDispatcher.getRouteProgress().routeProgress.currentLegProgress?.currentStepProgress?.stepIndex ?: 0

        // TODO:OZ voiceIndex is not available in SDK 1.0 and was not set in the legacy telemetry        navigationEvent.voiceIndex
        // TODO:OZ bannerIndex is not available in SDK 1.0 and was not set in the legacy telemetry        navigationEvent.bannerIndex
        navigationEvent.totalStepCount = obtainStepCount(directionsRoute)
    }

    private fun generateSdkIdentifier() =
        if (navigationOptions.isFromNavigationUi) "mapbox-navigation-ui-android" else "mapbox-navigation-android"
}
