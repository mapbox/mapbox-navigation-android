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
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private data class DynamicallyUpdatedRouteValues(
    var distanceRemaining: AtomicLong,
    var timeRemaining: AtomicInteger,
    var rerouteCount: AtomicInteger,
    var routeCanceled: AtomicBoolean,
    var routeArrived: AtomicBoolean,
    var offRouteCount: AtomicInteger,
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
The class has two public methods, postUserFeedbackEvent() and initialize().
 */
@SuppressLint("StaticFieldLeak")
internal object MapboxNavigationTelemetry : MapboxNavigationTelemetryInterface {
    internal const val LOCATION_BUFFER_MAX_SIZE = 20
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
            AtomicInteger(0),
            AtomicLong(0)
    )

    private var locationEngineName: String = LocationEngine::javaClass.name

    private val CURRENT_SESSION_CONTROL: AtomicReference<CurrentSessionState> =
            AtomicReference(CurrentSessionState.SESSION_END) // A switch that maintains session state (start/end)

    private enum class CurrentSessionState {
        SESSION_START,
        SESSION_END
    }

    private val callbackDispatcher =
            TelemetryLocationAndProgressDispatcher() // The class responds to most notification events

    private fun telemetryEventGate(event: MetricEvent) {
        when (callbackDispatcher.isRouteAvailable().get()) {
            null -> {
                Log.i(TAG, "Route not selected. Telemetry event not sent")
            }
            else -> {
                metricsReporter.addEvent(event)
            }
        }
    }
    // **********  EVENT OBSERVERS ***************
    /**
     * Callback that monitors session start/stop. Session stop is interpreted as both cancel and stop of the session
     */
    private val sessionStateObserver = object : TripSessionStateObserver {
        override fun onSessionStateChanged(tripSessionState: TripSessionState) {
            when (tripSessionState) {
                TripSessionState.STARTED -> {
                    postUserEventDelegate =
                            postUserFeedbackEventAfterInit // Telemetry is initialized and the user selected a route. Allow user feedback events to be posted
                    handleSessionStart()
                }
                TripSessionState.STOPPED -> {
                    postUserEventDelegate =
                            postUserEventBeforeInit // The navigation session is over, disallow posting user feedback events
                    handleSessionCanceled()
                    handleSessionStop()
                }
            }
        }
    }

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
            telemetryEventGate(TelemetryFasterRoute(
                    metadata = populateEventMetadataAndUpdateState(Date(), locationEngineName = locationEngineName),
                    newDistanceRemaining = fasterRoute.distance()?.toInt() ?: -1,
                    newDurationRemaining = fasterRoute.duration()?.toInt() ?: -1,
                    newGeometry = fasterRoute.geometry(),
                    step = telemetryStep
            )
            )
        }
    }

    /**
     * Callback to observe off route events
     */
    private val rerouteObserver = object : OffRouteObserver {
        override fun onOffRouteStateChanged(offRoute: Boolean) {
            when (offRoute) {
                true -> {
                    telemetryThreadControl.scope.launch {
                        dynamicValues.timeOfRerouteEvent.set(Time.SystemImpl.millis())
                        dynamicValues.rerouteCount.addAndGet(1) // increment reroute count
                        dynamicValues.distanceRemaining.set(callbackDispatcher.getRouteProgress().routeProgress.distanceRemaining().toLong())
                        val offRouteBuffers = callbackDispatcher.getLocationBuffersAsync().await()
                        var timeSinceLastEvent =
                                (Time.SystemImpl.millis() - dynamicValues.timeOfRerouteEvent.get()).toInt()
                        if (timeSinceLastEvent < 1000) {
                            timeSinceLastEvent = 0
                        }
                        telemetryEventGate(TelemetryReroute(
                                newDistanceRemaining = callbackDispatcher.getRouteProgress().routeProgress.durationRemaining().toInt(),
                                locationsBefore = offRouteBuffers.first.toTypedArray(),
                                locationsAfter = offRouteBuffers.second.toTypedArray(),
                                metadata = populateEventMetadataAndUpdateState(Date(), locationEngineName = locationEngineName),
                                feedbackId = TelemetryUtils.obtainUniversalUniqueIdentifier(),
                                secondsSinceLastReroute = timeSinceLastEvent / 1000
                        )
                        )
                    }
                }
                false -> {
                    Log.i(TAG, "On route")
                }
            }
        }
    }
    /**
     * The lambda that is called if the SDK client did not initialize telemetry. If telemetry is not initialized,
     * calls to post a user feedback event will fail with this exception
     */
    private val postUserEventBeforeInit: (String, String, String, String?) -> Unit = { _, _, _, _ ->
        Log.d(TAG, "Not in a navigation session, Cannot send user feedback events")
    }

    /**
     * The lambda that is called once telemetry is initialized.
     */
    private val postUserFeedbackEventAfterInit: (String, String, String, String?) -> Unit =
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
    private val primaryInitializer: (Context, String, MapboxNavigation, MetricsReporter, String, JobControl, NavigationOptions) -> Boolean =
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
                true
            }

    private var initializer =
            primaryInitializer // The initialize dispatcher that points to either pre or post initialization lambda

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

    /**
     * This method sends a user feedback event to the back-end servers. The method will suspend because the helper method it calls is itself suspendable
     * The method may suspend until it collects 40 location events. The worst case scenario is a 40 second suspension, 20 is best case
     */
    override fun postUserFeedbackEvent(
        @TelemetryUserFeedback.FeedbackType feedbackType: String,
        description: String,
        @TelemetryUserFeedback.FeedbackSource feedbackSource: String,
        screenshot: String?
    ) {
        postUserEventDelegate(feedbackType, description, feedbackSource, screenshot)
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
        val lastProgress = callbackDispatcher.getRouteProgress()
        telemetryThreadControl.scope.launch {
            val twoBuffers = callbackDispatcher.getLocationBuffersAsync().await()
            val feedbackEvent = TelemetryUserFeedback(
                    feedbackSource,
                    feedbackType,
                    description,
                    TelemetryUtils.retrieveVendorId(),
                    locationsBefore = twoBuffers.first.toTypedArray(),
                    locationsAfter = twoBuffers.second.toTypedArray(),
                    feedbackId = TelemetryUtils.obtainUniversalUniqueIdentifier(),
                    screenshot = screenshot,
                    step = lastProgress.routeProgress.currentLegProgress()?.let { routeLegProgress ->
                        populateTelemetryStep(routeLegProgress)
                    },
                    metadata = populateEventMetadataAndUpdateState(Date(), locationEngineName = locationEngineName)
            )
            metricsReporter.addEvent(feedbackEvent)
        }
    }

    /**
     * This method posts a cancel event in response to onSessionEnd
     */
    private fun handleSessionCanceled() {
        dynamicValues.routeCanceled.set(true) // Set cancel state unconditionally
        when (dynamicValues.routeArrived.get()) {
            true -> {
                val cancelEvent = TelemetryCancel(
                        arrivalTimestamp = Date().toString(),
                        metadata = populateEventMetadataAndUpdateState(Date(), locationEngineName = locationEngineName)
                )
                telemetryEventGate(cancelEvent)
            }
            false -> {
                val cancelEvent = TelemetryCancel(metadata = populateEventMetadataAndUpdateState(Date(), locationEngineName = locationEngineName))
                telemetryEventGate(cancelEvent)
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
        callbackDispatcher.isRouteAvailable().set(null)
    }

    /**
     * This method starts a session. If a session is active it will terminate it, causing an stop/cancel event to be sent to the servers.
     * Every session start is guaranteed to have a session end.
     */
    private fun handleSessionStart() {
        callbackDispatcher.isRouteAvailable().get()?.route?.let { directionsRoute ->
            // Expected session == SESSION_END
            CURRENT_SESSION_CONTROL.compareAndSet(
                    CurrentSessionState.SESSION_END,
                    CurrentSessionState.SESSION_START
            ).let { previousSessionState ->
                when (previousSessionState) {
                    true -> {
                        sessionStartHelper(directionsRoute, callbackDispatcher.getLastLocation())
                    }
                    false -> {
                        handleSessionStop()
                        sessionStartHelper(directionsRoute, callbackDispatcher.getLastLocation())
                        Log.e(TAG, "sessionEnd() not called. Calling it by default")
                    }
                }
            }
        } ?: Log.e(TAG, "Telemetry received a null DirectionsRoute. Session not started")
    }

    /**
     * This method is used by a lambda. Since the Telemetry class is a singleton, U.I. elements may call postTurnstileEvent() before the singleton is initialized.
     * A lambda guards against this possibility
     */
    private fun postTurnstileEvent() {
        // AppUserTurnstile is implemented in mapbox-telemetry-sdk
        val sdkType = generateSdkIdentifier()
        val appUserTurnstileEvent =
                AppUserTurnstile(sdkType, BuildConfig.MAPBOX_NAVIGATION_VERSION_NAME)
        val event = NavigationAppUserTurnstileEvent(appUserTurnstileEvent)
        metricsReporter.addEvent(event)
    }

    /**
     * This method starts a session. The start of a session does not result in a telemetry event being sent to the servers.
     * It is only the initialization of the [TelemetryMetadata] object with two UUIDs
     */
    private fun sessionStartHelper(
        directionsRoute: DirectionsRoute,
        location: Location
    ) {
        telemetryThreadControl.scope.launch {
            // Initialize identifiers unique to this session
            populateEventMetadataAndUpdateState(
                    Date(),
                    directionsRoute,
                    locationEngineName,
                    location
            ).apply {
                sessionIdentifier = TelemetryUtils.obtainUniversalUniqueIdentifier()
                startTimestamp = Date().toString()
            }
            telemetryEventGate(telemetryDeparture(directionsRoute))
            monitorRoutProgress()
        }
    }

    /**
     * This method waits for an [RouteProgressState.ROUTE_ARRIVED] event. Once received, it terminates the wait-loop and
     * sends the telemetry data to the servers.
     */
    private suspend fun monitorRoutProgress() {
        var continueRunning = true
        while (coroutineContext.isActive && continueRunning) {
            try {
                val routeData = callbackDispatcher.getRouteProgressChannel().receive()
                when (routeData.routeProgress.currentState()) {
                    RouteProgressState.ROUTE_ARRIVED -> {
                        dynamicValues.routeCanceled.set(false)
                        telemetryEventGate(TelemetryArrival(
                                arrivalTimestamp = Date().toString(),
                                metadata = populateEventMetadataAndUpdateState(Date(), locationEngineName = locationEngineName).apply {
                                    lat = callbackDispatcher.getLastLocation().latitude.toFloat()
                                    lng = callbackDispatcher.getLastLocation().longitude.toFloat()
                                    distanceCompleted = routeData.routeProgress.distanceTraveled().toInt()
                                    dynamicValues.routeArrived.set(true)
                                }
                        )
                        )
                        continueRunning = false
                    }
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

    private fun telemetryDeparture(directionsRoute: DirectionsRoute): MetricEvent {
        return TelemetryDepartureEvent(populateEventMetadataAndUpdateState(Date(), locationEngineName = locationEngineName).apply {
            lat = callbackDispatcher.getLastLocation().latitude.toFloat()
            lng = callbackDispatcher.getLastLocation().longitude.toFloat()
            originalRequestIdentifier = directionsRoute.routeOptions()?.requestUuid()
            requestIdentifier = directionsRoute.routeOptions()?.requestUuid()
            originalGeometry = directionsRoute.geometry()
        })
    }

    private fun registerForNotification(mapboxNavigation: MapboxNavigation) {
        mapboxNavigation.registerOffRouteObserver(rerouteObserver)
        mapboxNavigation.registerRouteProgressObserver(callbackDispatcher)
        mapboxNavigation.registerTripSessionStateObserver(sessionStateObserver)
        mapboxNavigation.registerFasterRouteObserver(fasterRouteObserver)
        mapboxNavigation.registerLocationObserver(callbackDispatcher)
        mapboxNavigation.registerRoutesObserver(callbackDispatcher)
    }

    override fun unregisterListeners(mapboxNavigation: MapboxNavigation) {
        mapboxNavigation.unregisterOffRouteObserver(rerouteObserver)
        mapboxNavigation.unregisterRouteProgressObserver(callbackDispatcher)
        mapboxNavigation.unregisterTripSessionStateObserver(sessionStateObserver)
        mapboxNavigation.unregisterFasterRouteObserver(fasterRouteObserver)
        mapboxNavigation.unregisterLocationObserver(callbackDispatcher)
        mapboxNavigation.unregisterRoutesObserver(callbackDispatcher)
    }

    private fun validateAccessToken(accessToken: String?) {
        if (accessToken.isNullOrEmpty() ||
                (!accessToken.toLowerCase(Locale.US).startsWith("pk.") &&
                        !accessToken.toLowerCase(Locale.US).startsWith("sk."))
        ) {
            throw NavigationException("A valid access token must be passed in when first initializing MapboxNavigation")
        }
    }

    private fun populateEventMetadataAndUpdateState(
        creationDate: Date,
        directionsRoute: DirectionsRoute? = null,
        locationEngineName: String,
        currentLocation: Location? = null
    ): TelemetryMetadata {
        val sdkType = generateSdkIdentifier()
        dynamicValues.distanceRemaining.set(callbackDispatcher.getRouteProgress().routeProgress.distanceRemaining().toLong())
        dynamicValues.timeRemaining.set(callbackDispatcher.getRouteProgress().routeProgress.durationRemaining().toInt())
        return TelemetryMetadata(
                created = creationDate.toString(),
                startTimestamp = Date().toString(),
                device = Build.DEVICE,
                sdkIdentifier = sdkType,
                sdkVersion = BuildConfig.MAPBOX_NAVIGATION_VERSION_NAME,
                simulation = MOCK_PROVIDER == locationEngineName,
                locationEngine = locationEngineName,
                sessionIdentifier = TelemetryUtils.obtainUniversalUniqueIdentifier(),
                originalRequestIdentifier = directionsRoute?.routeOptions()?.requestUuid(),
                requestIdentifier = directionsRoute?.routeOptions()?.requestUuid(),
                lat = currentLocation?.latitude?.toFloat() ?: 0f,
                lng = currentLocation?.longitude?.toFloat() ?: 0f,
                originalGeometry = obtainGeometry(directionsRoute),
                originalEstimatedDistance = directionsRoute?.distance()?.toInt() ?: 0,
                originalEstimatedDuration = directionsRoute?.duration()?.toInt() ?: 0,
                originalStepCount = obtainStepCount(directionsRoute),
                geometry = obtainGeometry(directionsRoute),
                estimatedDistance = directionsRoute?.distance()?.toInt() ?: 0,
                estimatedDuration = directionsRoute?.duration()?.toInt() ?: 0,
                stepCount = obtainStepCount(directionsRoute),
                distanceCompleted = 0,
                distanceRemaining = dynamicValues.distanceRemaining.get().toInt(),
                absoluteDistanceToDestination = obtainAbsoluteDistance(
                        callbackDispatcher.getLastLocation(),
                        obtainRouteDestination(directionsRoute)
                ),
                durationRemaining = callbackDispatcher.getRouteProgress().routeProgress.currentLegProgress()?.currentStepProgress()?.durationRemaining()?.toInt()
                        ?: 0,
                rerouteCount = dynamicValues.rerouteCount.get(),
                applicationState = TelemetryUtils.obtainApplicationState(context),
                batteryPluggedIn = TelemetryUtils.isPluggedIn(context),
                batteryLevel = TelemetryUtils.obtainBatteryLevel(context),
                connectivity = TelemetryUtils.obtainCellularNetworkType(context),
                screenBrightness = obtainScreenBrightness(context),
                volumeLevel = obtainVolumeLevel(context),
                audioType = obtainAudioType(context)
        )
    }

    private fun generateSdkIdentifier() =
            if (navigationOptions.isFromNavigationUi) "mapbox-navigation-ui-android" else "mapbox-navigation-android"
}
