package com.mapbox.navigation.core.telemetry

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Build
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
import com.mapbox.navigation.core.accounts.MapboxNavigationAccounts
import com.mapbox.navigation.core.fasterroute.FasterRouteObserver
import com.mapbox.navigation.core.telemetry.events.MOCK_PROVIDER
import com.mapbox.navigation.core.telemetry.events.TelemetryArrival
import com.mapbox.navigation.core.telemetry.events.TelemetryCancel
import com.mapbox.navigation.core.telemetry.events.TelemetryDepartureEvent
import com.mapbox.navigation.core.telemetry.events.TelemetryFasterRoute
import com.mapbox.navigation.core.telemetry.events.TelemetryMetadata
import com.mapbox.navigation.core.telemetry.events.TelemetryReroute
import com.mapbox.navigation.core.telemetry.events.TelemetryStep
import com.mapbox.navigation.core.telemetry.events.TelemetryUserFeedback
import com.mapbox.navigation.core.trip.session.OffRouteObserver
import com.mapbox.navigation.core.trip.session.TripSessionState
import com.mapbox.navigation.core.trip.session.TripSessionStateObserver
import com.mapbox.navigation.metrics.internal.NavigationAppUserTurnstileEvent
import com.mapbox.navigation.utils.exceptions.NavigationException
import com.mapbox.navigation.utils.thread.JobControl
import com.mapbox.navigation.utils.thread.ifChannelException
import com.mapbox.navigation.utils.time.Time
import java.util.ArrayDeque
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select

private data class DynamicallyUpdatedRouteValues(
    var distanceRemaining: AtomicLong,
    var timeRemaining: AtomicInteger,
    var rerouteCount: AtomicInteger,
    var routeCanceled: AtomicBoolean,
    var routeArrived: AtomicBoolean,
    val timeOfRerouteEvent: AtomicLong
)

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
    internal const val TAG = "MAPBOX_TELEMETRY"

    private lateinit var context: Context // Must be context.getApplicationContext
    private lateinit var mapboxToken: String
    private lateinit var telemetryThreadControl: JobControl
    private lateinit var metricsReporter: MetricsReporter
    private lateinit var navigationOptions: NavigationOptions
    /**
     * This class holds all mutable state of the Telemetry object
     */
    private val dynamicValues = DynamicallyUpdatedRouteValues(
        AtomicLong(0),
        AtomicInteger(0),
        AtomicInteger(0),
        AtomicBoolean(false),
        AtomicBoolean(false),
        AtomicLong(0)
    )

    private var locationEngineName: String = LocationEngine::javaClass.name
    private val currentLocation: AtomicReference<Location?> = AtomicReference(null)

    private val CURRENT_SESSION_CONTROL: AtomicReference<CurrentSessionState> =
        AtomicReference(CurrentSessionState.SESSION_END) // A switch that maintains session state (start/end)

    private enum class CurrentSessionState {
        SESSION_START,
        SESSION_END
    }

    private lateinit var callbackDispatcher: TelemetryLocationAndProgressDispatcher

    private fun telemetryEventGate(event: MetricEvent): CompletableDeferred<Boolean> {
        val completion = CompletableDeferred<Boolean>()
        when (callbackDispatcher.isRouteAvailable()) {
            null -> {
                Log.i(TAG, "Route not selected. Telemetry event not sent")
                completion.complete(false)
            }
            else -> {
                metricsReporter.addEvent(event)
                completion.complete(true)
            }
        }
        return completion
    }
    // **********  EVENT OBSERVERS ***************
    /**
     * Callback that monitors session start/stop. Session stop is interpreted as both cancel and stop of the session
     */

    private val sessionStateObserver = object : TripSessionStateObserver {
        override fun onSessionStateChanged(tripSessionState: TripSessionState) {
            when (tripSessionState) {
                TripSessionState.STARTED -> {
                    telemetryThreadControl.scope.launch {
                        postUserEventDelegate =
                            postUserFeedbackEventAfterInit // Telemetry is initialized and the user selected a route. Allow user feedback events to be posted
                        handleSessionStart(callbackDispatcher.getFirstLocationAsync().await())
                    }
                }
                TripSessionState.STOPPED -> {
                    postUserEventDelegate =
                        postUserEventBeforeInit // The navigation session is over, disallow posting user feedback events
                    when (dynamicValues.routeArrived.get()) {
                        true -> {
                            handleSessionStop()
                            Log.d(TAG, "you have arrived")
                        }
                        false -> {
                            telemetryThreadControl.scope.launch {
                                Log.d(TAG, "Session was canceled")
                                handleSessionCanceled()
                                handleSessionStop()
                            }
                        }
                    }
                }
            }
        }
    }

    // TODO Removing Faster Route temporarily as legacy isn't sending these events at the moment
    /**
     * Callback to observe faster route events
     */
    private val fasterRouteObserver = object : FasterRouteObserver {
        override fun onFasterRouteAvailable(fasterRoute: DirectionsRoute) {
            var telemetryStep: TelemetryStep? = null
            callbackDispatcher.getRouteProgress().routeProgress.currentLegProgress()
                ?.let { routeProgress ->
                    telemetryStep = populateTelemetryStep(routeProgress)
                }
            telemetryEventGate(
                TelemetryFasterRoute(
                    metadata = populateMetadataWithInitialValues(populateEventMetadataAndUpdateState(
                        Date(),
                        locationEngineName = locationEngineName,
                        lastLocation = currentLocation.get()
                    )),
                    newDistanceRemaining = fasterRoute.distance()?.toInt() ?: -1,
                    newDurationRemaining = fasterRoute.duration()?.toInt() ?: -1,
                    newGeometry = fasterRoute.geometry(),
                    step = telemetryStep
                )
            )
        }
    }

    /**
     * This method generates an off-route telemetry event. Part of the code is suspendable
     * because it waits for a new route to be offered by the SDK in response to a reroute
     */
    private fun handleOffRouteEvent() {
        dynamicValues.timeOfRerouteEvent.set(Time.SystemImpl.millis())
        val rerouteCount = dynamicValues.rerouteCount.addAndGet(1) // increment reroute count
        val prevRoute = callbackDispatcher.getRouteProgress()
        telemetryThreadControl.scope.launch {
            val newRoute = callbackDispatcher.getDirectionsRouteChannel()
                .receive() // Suspend until we get a value
            dynamicValues.distanceRemaining.set(newRoute.route.distance()?.toLong() ?: -1)
            var timeSinceLastEvent =
                (Time.SystemImpl.millis() - dynamicValues.timeOfRerouteEvent.get()).toInt()
            if (timeSinceLastEvent < ONE_SECOND) {
                timeSinceLastEvent = 0
            }
            val telemetryStep: TelemetryStep? = null
            if (newRoute.route.legs()?.isNotEmpty() == true) {
                newRoute.route.legs()?.let { nullableLegList ->
                    if (nullableLegList[0].steps()?.isNotEmpty() == true) {
                        nullableLegList[0].steps()?.let { legsList ->
                            populateTelemetryStep(
                                legsList[0],
                                prevRoute.routeProgress.currentLegProgress()
                            )
                        }
                    }
                }
            }
            callbackDispatcher.addLocationEventDescriptor(ItemAccumulationEventDescriptor(
                ArrayDeque(callbackDispatcher.getCopyOfCurrentLocationBuffer()),
                ArrayDeque()
            ) { preEventBuffer, postEventBuffer ->
                telemetryThreadControl.scope.launch {
                    val result = telemetryEventGate(
                            TelemetryReroute(
                                    newDistanceRemaining = newRoute.route.distance()?.toInt() ?: -1,
                                    newDurationRemaining = newRoute.route.duration()?.toInt() ?: -1,
                                    newGeometry = obtainGeometry(newRoute.route),
                                    step = telemetryStep,
                                    locationsBefore = preEventBuffer.toTypedArray(),
                                    locationsAfter = postEventBuffer.toTypedArray(),
                                    metadata = populateMetadataWithInitialValues(populateEventMetadataAndUpdateState(
                                            Date(),
                                            rerouteCount = rerouteCount,
                                            locationEngineName = locationEngineName,
                                            route = newRoute.route
                                    )),
                                    feedbackId = TelemetryUtils.obtainUniversalUniqueIdentifier(),
                                    secondsSinceLastReroute = timeSinceLastEvent / ONE_SECOND
                            )
                    ).await()
                    Log.d(TAG, "REROUTE event sent $result" +
                            "")
                }
            })
        }
    }

    /**
     * Callback to observe off route events
     */
    private val rerouteObserver = object : OffRouteObserver {
        override fun onOffRouteStateChanged(offRoute: Boolean) {
            when (offRoute) {
                true -> {
                    handleOffRouteEvent()
                }
                false -> {
                    Log.i(TAG, "On route")
                }
            }
        }
    }
    /**
     * The lambda that is called if the SDK client did not initialize telemetry. If telemetry is not initialized,
     * calls to post a user feedback event will fail silently
     */
    private val postUserEventBeforeInit: suspend (String, String, String, String?) -> Unit =
        { _, _, _, _ ->
            Log.d(TAG, "Not in a navigation session, Cannot send user feedback events")
        }

    /**
     * The lambda that is called once telemetry is initialized.
     */
    private val postUserFeedbackEventAfterInit: suspend (String, String, String, String?) -> Unit =
        { feedbackType, description, feedbackSource, screenshot ->
            postUserFeedbackHelper(
                feedbackType,
                description,
                feedbackSource,
                screenshot
            )
        }

    /**
     * The delegate lambda that dispatches either a pre or post initialization userFeedbackEvent
     */
    private var postUserEventDelegate = postUserEventBeforeInit

    /**
     * One-time initializer. Called in response to initialize() and then replaced with a no-op lambda to prevent multiple initialize() calls
     */
    private val initializerDelegate: (Context, String, MapboxNavigation, MetricsReporter, String, JobControl, NavigationOptions) -> Boolean =
        { context, token, mapboxNavigation, metricsReporter, name, jobControl, options ->
            this.context = context
            locationEngineName = name
            navigationOptions = options
            telemetryThreadControl = jobControl
            mapboxToken = token
            validateAccessToken(mapboxToken)
            this.metricsReporter = metricsReporter
            initializer =
                postInitialize // prevent primaryInitializer() from being called more than once.
            registerForNotification(mapboxNavigation)
            postTurnstileEvent()
            monitorJobCancelation()
            true
        }

    private var initializer =
        initializerDelegate // The initialize dispatcher that points to either pre or post initialization lambda

    // Calling initialize multiple times does no harm. This call is a no-op.
    private var postInitialize: (Context, String, MapboxNavigation, MetricsReporter, String, JobControl, NavigationOptions) -> Boolean =
        { _, _, _, _, _, _, _ -> false }

    /**
     * This method must be called before using the Telemetry object
     */
    fun initialize(
        context: Context,
        mapboxToken: String,
        mapboxNavigation: MapboxNavigation,
        metricsReporter: MetricsReporter,
        locationEngineName: String,
        jobControl: JobControl,
        options: NavigationOptions
    ) = initializer(
        context,
        mapboxToken,
        mapboxNavigation,
        metricsReporter,
        locationEngineName,
        jobControl,
        options
    )

    private fun monitorJobCancelation() {
        CoroutineScope(Job() + Dispatchers.IO).launch {
            select {
                telemetryThreadControl.job.onJoin {
                    Log.d(TAG, "master job canceled")
                    callbackDispatcher.flushBuffers()
                }
            }
        }
    }
    /**
     * This method sends a user feedback event to the back-end servers. The method will suspend because the helper method it calls is itself suspendable
     * The method may suspend until it collects 40 location events. The worst case scenario is a 40 location suspension, 20 is best case
     */
    override fun postUserFeedback(
        @TelemetryUserFeedback.FeedbackType feedbackType: String,
        description: String,
        @TelemetryUserFeedback.FeedbackSource feedbackSource: String,
        screenshot: String?
    ) {
        telemetryThreadControl.scope.launch {
            postUserEventDelegate(feedbackType, description, feedbackSource, screenshot)
        }
    }

    /**
     * Helper class that posts user feedback. The call is available only after initialization
     */
    private fun postUserFeedbackHelper(
        @TelemetryUserFeedback.FeedbackType feedbackType: String,
        description: String,
        @TelemetryUserFeedback.FeedbackSource feedbackSource: String,
        screenshot: String?
    ) {
        Log.d(TAG, "trying to post a user feedback event")
        val lastProgress = callbackDispatcher.getRouteProgress()
        callbackDispatcher.addLocationEventDescriptor(ItemAccumulationEventDescriptor(
            ArrayDeque(callbackDispatcher.getCopyOfCurrentLocationBuffer()),
            ArrayDeque()
        ) { preEventBuffer, postEventBuffer ->
            val feedbackEvent = TelemetryUserFeedback(
                feedbackSource = feedbackSource,
                feedbackType = feedbackType,
                description = description,
                userId = TelemetryUtils.retrieveVendorId(),
                locationsBefore = preEventBuffer.toTypedArray(),
                locationsAfter = postEventBuffer.toTypedArray(),
                feedbackId = TelemetryUtils.obtainUniversalUniqueIdentifier(),
                screenshot = screenshot,
                step = lastProgress.routeProgress.currentLegProgress()?.let { routeLegProgress ->
                    populateTelemetryStep(routeLegProgress)
                },
                metadata = populateMetadataWithInitialValues(populateEventMetadataAndUpdateState(
                    Date(),
                    locationEngineName = locationEngineName
                ))
            )
            Log.i(TAG, "Posting a user feedback event")
            metricsReporter.addEvent(feedbackEvent)
        })
    }

    /**
     * This method posts a cancel event in response to onSessionEnd
     */
    private suspend fun handleSessionCanceled() {
        dynamicValues.routeCanceled.set(true) // Set cancel state unconditionally
        if (CURRENT_SESSION_CONTROL.compareAndSet(
                CurrentSessionState.SESSION_START,
                CurrentSessionState.SESSION_END
            )
        ) {
            when (dynamicValues.routeArrived.get()) {
                true -> {
                    val cancelEvent = TelemetryCancel(
                        arrivalTimestamp = Date().toString(),
                        metadata = populateMetadataWithInitialValues(populateEventMetadataAndUpdateState(
                            Date(),
                            locationEngineName = locationEngineName
                        ))
                    )
                    telemetryThreadControl.scope.launch {
                        val result = telemetryEventGate(cancelEvent).await()
                        Log.d(TAG, "ARRIVAL event sent $result")
                        callbackDispatcher.cancelCollectionAndPostFinalEvents()
                    }
                }
                false -> {
                    val cancelEvent = TelemetryCancel(
                        metadata = populateMetadataWithInitialValues(populateEventMetadataAndUpdateState(
                            Date(),
                            locationEngineName = locationEngineName
                        ))
                    )
                    val result = telemetryEventGate(cancelEvent).await()
                    Log.d(TAG, "CANCEL event sent $result")
                    callbackDispatcher.cancelCollectionAndPostFinalEvents()
                }
            }
        }
    }

    /**
     * This method clears the state data for the Telemetry object in response to onSessionEnd
     */
    private fun handleSessionStop() {
        dynamicValues.routeArrived.set(false)
        dynamicValues.routeCanceled.set(false)
        dynamicValues.distanceRemaining.set(0)
        dynamicValues.rerouteCount.set(0)
        dynamicValues.timeRemaining.set(0)
        callbackDispatcher.clearOriginalRoute()
    }

    /**
     * This method starts a session. If a session is active it will terminate it, causing an stop/cancel event to be sent to the servers.
     * Every session start is guaranteed to have a session end.
     */
    private fun handleSessionStart(startingLocation: Location) {
        telemetryThreadControl.scope.launch {
            Log.d(TAG, "Wating in handdleSessionStart")
            val directionsRoute = callbackDispatcher.getOriginalRouteAsync().await()
            Log.d(TAG, "The wait is over")
            // Expected session == SESSION_END
            CURRENT_SESSION_CONTROL.compareAndSet(
                    CurrentSessionState.SESSION_END,
                    CurrentSessionState.SESSION_START
            ).let { previousSessionState ->
                when (previousSessionState) {
                    true -> {
                        Log.d(TAG, "Handling true")
                        sessionStartHelper(directionsRoute, startingLocation)
                    }
                    false -> {
                        Log.d(TAG, "Handling false")
                        handleSessionStop()
                        sessionStartHelper(directionsRoute, callbackDispatcher.getLastLocation())
                        Log.e(TAG, "sessionEnd() not called. Calling it by default")
                    }
                }
            }
        }
    }

    /**
     * This method is used by a lambda. Since the Telemetry class is a singleton, U.I. elements may call postTurnstileEvent() before the singleton is initialized.
     * A lambda guards against this possibility
     */
    private fun postTurnstileEvent() {
        // AppUserTurnstile is implemented in mapbox-telemetry-sdk
        val sdkType = generateSdkIdentifier()
        val appUserTurnstileEvent =
            AppUserTurnstile(sdkType, BuildConfig.MAPBOX_NAVIGATION_VERSION_NAME).also {
                it.setSkuId(
                    MapboxNavigationAccounts.getInstance(
                        context
                    ).obtainSkuToken()
                )
            }
        val event = NavigationAppUserTurnstileEvent(appUserTurnstileEvent)
        metricsReporter.addEvent(event)
    }

    /**
     * This method starts a session. The start of a session does not result in a telemetry event being sent to the servers.
     * It is only the initialization of the [TelemetryMetadata] object with two UUIDs
     */
    private fun sessionStartHelper(
        directionsRoute: DirectionsRoute,
        location: Location?
    ) {
        telemetryThreadControl.scope.launch {
            // Initialize identifiers unique to this session
            populateMetadataWithInitialValues(populateEventMetadataAndUpdateState(
                Date(),
                directionsRoute,
                locationEngineName,
                location
            )).apply {
                sessionIdentifier = TelemetryUtils.obtainUniversalUniqueIdentifier()
                startTimestamp = Date().toString()
            }
            val result = telemetryEventGate(telemetryDeparture(directionsRoute, callbackDispatcher.getFirstLocationAsync().await())).await()
            Log.d(TAG, "DEPARTURE event sent $result")
            monitorSession()
        }
    }

    /**
     * This method waits for an [RouteProgressState.ROUTE_ARRIVED] event. Once received, it terminates the wait-loop and
     * sends the telemetry data to the servers.
     */
    private suspend fun monitorSession() {
        var continueRunning = true
        while (coroutineContext.isActive && continueRunning) {
            try {
                val routeData = callbackDispatcher.getRouteProgressChannel().receive()
                when (routeData.routeProgress.currentState()) {
                    RouteProgressState.ROUTE_ARRIVED -> {
                        dynamicValues.routeCanceled.set(false)
                        val result = telemetryEventGate(
                            TelemetryArrival(
                                arrivalTimestamp = Date().toString(),
                                metadata = populateMetadataWithInitialValues(populateEventMetadataAndUpdateState(
                                    Date(),
                                    locationEngineName = locationEngineName
                                )).apply {
                                    lat = callbackDispatcher.getLastLocation()?.latitude?.toFloat() ?: currentLocation.get()?.latitude?.toFloat() ?: 0f
                                    lng = callbackDispatcher.getLastLocation()?.longitude?.toFloat() ?: currentLocation.get()?.longitude?.toFloat() ?: 0f
                                    distanceCompleted =
                                        routeData.routeProgress.distanceTraveled().toInt() // TODO: Log this data to see what is returned from the SDK
                                    dynamicValues.routeArrived.set(true)
                                }
                            )
                        ).await()
                        Log.d(TAG, "ARRIVAL event sent $result")
                        callbackDispatcher.cancelCollectionAndPostFinalEvents()
                        continueRunning = false
                    } // END
                    RouteProgressState.LOCATION_TRACKING -> {
                        dynamicValues.timeRemaining.set(callbackDispatcher.getRouteProgress().routeProgress.durationRemaining().toInt())
                        dynamicValues.distanceRemaining.set(callbackDispatcher.getRouteProgress().routeProgress.distanceRemaining().toLong())
                    }
                    else -> {
                        // Do nothing
                    }
                }
            } catch (e: Exception) {
                e.ifChannelException {
                    continueRunning = false
                }
            }
        }
    }

    private fun telemetryDeparture(directionsRoute: DirectionsRoute, startingLocation: Location): MetricEvent {
        return TelemetryDepartureEvent(
            populateMetadataWithInitialValues(populateEventMetadataAndUpdateState(
                Date(),
                locationEngineName = locationEngineName
            )).apply {
                lat = startingLocation.latitude.toFloat()
                lng = startingLocation.longitude.toFloat()
                originalRequestIdentifier = directionsRoute.routeOptions()?.requestUuid()
                requestIdentifier = directionsRoute.routeOptions()?.requestUuid()
                originalGeometry = directionsRoute.geometry()
            })
    }

    private fun registerForNotification(mapboxNavigation: MapboxNavigation) {
        callbackDispatcher = TelemetryLocationAndProgressDispatcher(telemetryThreadControl.scope) // The class responds to most notification events
        mapboxNavigation.registerOffRouteObserver(rerouteObserver)
        mapboxNavigation.registerRouteProgressObserver(callbackDispatcher)
        mapboxNavigation.registerTripSessionStateObserver(sessionStateObserver)
        // TODO Removing Faster Route temporarily as legacy isn't sending these events at the moment
        // mapboxNavigation.registerFasterRouteObserver(fasterRouteObserver)
        mapboxNavigation.registerLocationObserver(callbackDispatcher)
        mapboxNavigation.registerRoutesObserver(callbackDispatcher)
    }

    override fun unregisterListeners(mapboxNavigation: MapboxNavigation) {
        callbackDispatcher.cancelCollectionAndPostFinalEvents()
        mapboxNavigation.unregisterOffRouteObserver(rerouteObserver)
        mapboxNavigation.unregisterRouteProgressObserver(callbackDispatcher)
        mapboxNavigation.unregisterTripSessionStateObserver(sessionStateObserver)
        // TODO Removing Faster Route temporarily as legacy isn't sending these events at the moment
        // mapboxNavigation.unregisterFasterRouteObserver(fasterRouteObserver)
        mapboxNavigation.unregisterLocationObserver(callbackDispatcher)
        mapboxNavigation.unregisterRoutesObserver(callbackDispatcher)
        initializer = initializerDelegate
    }

    private fun validateAccessToken(accessToken: String?) {
        if (accessToken.isNullOrEmpty() ||
            (!accessToken.toLowerCase(Locale.US).startsWith("pk.") &&
                !accessToken.toLowerCase(Locale.US).startsWith("sk."))
        ) {
            throw NavigationException("A valid access token must be passed in when first initializing MapboxNavigation")
        }
    }

    private fun populateMetadataWithInitialValues(metaData: TelemetryMetadata): TelemetryMetadata {
        val directionsRoute = callbackDispatcher.getOriginalRoute()
        metaData.apply {
            originalRequestIdentifier = directionsRoute?.route?.routeOptions()?.requestUuid()
            originalGeometry = obtainGeometry(directionsRoute?.route)
            originalEstimatedDistance = directionsRoute?.route?.distance()?.toInt() ?: 0
            originalEstimatedDuration = directionsRoute?.route?.duration()?.toInt() ?: 0
            originalStepCount = obtainStepCount(directionsRoute?.route)
        }
        return metaData
    }

    private fun populateEventMetadataAndUpdateState(
        creationDate: Date,
        route: DirectionsRoute? = null,
        locationEngineName: String,
        lastLocation: Location? = null,
        rerouteCount: Int? = null
    ): TelemetryMetadata {
        val sdkType = generateSdkIdentifier()
        dynamicValues.distanceRemaining.set(callbackDispatcher.getRouteProgress().routeProgress.distanceRemaining().toLong())
        dynamicValues.timeRemaining.set(callbackDispatcher.getRouteProgress().routeProgress.durationRemaining().toInt())
        val directionsRoute = route ?: callbackDispatcher.getRouteProgress().routeProgress.route()
        val location: Location? = callbackDispatcher.getLastLocation() ?: currentLocation.get()
        val originalDistance = callbackDispatcher.getOriginalRoute()?.route?.distance()
        val completedDistance = originalDistance?.toLong() ?: 0 - dynamicValues.distanceRemaining.get()
        val metadata =
            TelemetryMetadata(
                created = creationDate.toString(),
                startTimestamp = Date().toString(),
                device = Build.DEVICE,
                sdkIdentifier = sdkType,
                sdkVersion = BuildConfig.MAPBOX_NAVIGATION_VERSION_NAME,
                simulation = MOCK_PROVIDER == locationEngineName,
                locationEngine = locationEngineName,
                sessionIdentifier = TelemetryUtils.obtainUniversalUniqueIdentifier(),
                requestIdentifier = directionsRoute?.routeOptions()?.requestUuid(),
                lat = lastLocation?.latitude?.toFloat() ?: location?.latitude?.toFloat() ?: 0f,
                lng = lastLocation?.longitude?.toFloat() ?: location?.longitude?.toFloat() ?: 0f,
                geometry = obtainGeometry(directionsRoute),
                estimatedDistance = directionsRoute?.distance()?.toInt() ?: 0, // TODO:OZ verify
                estimatedDuration = directionsRoute?.duration()?.toInt() ?: 0, // TODO:OZ verify
                stepCount = obtainStepCount(directionsRoute),
                distanceCompleted = completedDistance.toInt(),
                distanceRemaining = dynamicValues.distanceRemaining.get().toInt(),
                absoluteDistanceToDestination = obtainAbsoluteDistance(
                    callbackDispatcher.getLastLocation(),
                    obtainRouteDestination(directionsRoute)
                ),
                durationRemaining = callbackDispatcher.getRouteProgress().routeProgress.currentLegProgress()?.currentStepProgress()?.durationRemaining()?.toInt() ?: 0,
                rerouteCount = rerouteCount ?: 0,
                applicationState = TelemetryUtils.obtainApplicationState(context),
                batteryPluggedIn = TelemetryUtils.isPluggedIn(context),
                batteryLevel = TelemetryUtils.obtainBatteryLevel(context),
                connectivity = TelemetryUtils.obtainCellularNetworkType(context),
                screenBrightness = obtainScreenBrightness(context),
                volumeLevel = obtainVolumeLevel(context),
                audioType = obtainAudioType(context)
            )
        return metadata
    }

    private fun generateSdkIdentifier() =
        if (navigationOptions.isFromNavigationUi) "mapbox-navigation-ui-android" else "mapbox-navigation-android"
}
