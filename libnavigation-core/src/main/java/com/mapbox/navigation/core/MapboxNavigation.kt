package com.mapbox.navigation.core

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.content.Context
import android.hardware.SensorEvent
import android.util.Log
import androidx.annotation.RequiresPermission
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineProvider
import com.mapbox.android.core.location.LocationEngineRequest
import com.mapbox.annotation.navigation.module.MapboxNavigationModuleType
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.base.accounts.SkuTokenProvider
import com.mapbox.navigation.base.extensions.ifNonNull
import com.mapbox.navigation.base.options.DEFAULT_NAVIGATOR_POLLING_DELAY
import com.mapbox.navigation.base.options.Endpoint
import com.mapbox.navigation.base.options.MapboxOnboardRouterConfig
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.route.Router
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.notification.NotificationAction
import com.mapbox.navigation.base.trip.notification.TripNotification
import com.mapbox.navigation.base.typedef.NONE_SPECIFIED
import com.mapbox.navigation.base.typedef.ROUNDING_INCREMENT_FIFTY
import com.mapbox.navigation.base.typedef.UNDEFINED
import com.mapbox.navigation.core.accounts.MapboxNavigationAccounts
import com.mapbox.navigation.core.directions.session.AdjustedRouteOptionsProvider
import com.mapbox.navigation.core.directions.session.DirectionsSession
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.directions.session.RoutesRequestCallback
import com.mapbox.navigation.core.fasterroute.FasterRouteController
import com.mapbox.navigation.core.fasterroute.FasterRouteObserver
import com.mapbox.navigation.core.module.NavigationModuleProvider
import com.mapbox.navigation.core.routerefresh.RouteRefreshController
import com.mapbox.navigation.core.telemetry.MapboxNavigationTelemetry
import com.mapbox.navigation.core.telemetry.MapboxNavigationTelemetry.TAG
import com.mapbox.navigation.core.telemetry.events.TelemetryUserFeedback
import com.mapbox.navigation.core.trip.service.TripService
import com.mapbox.navigation.core.trip.session.BannerInstructionsObserver
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.core.trip.session.OffRouteObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.core.trip.session.TripSession
import com.mapbox.navigation.core.trip.session.TripSessionStateObserver
import com.mapbox.navigation.core.trip.session.VoiceInstructionsObserver
import com.mapbox.navigation.metrics.MapboxMetricsReporter
import com.mapbox.navigation.navigator.MapboxNativeNavigator
import com.mapbox.navigation.navigator.MapboxNativeNavigatorImpl
import com.mapbox.navigation.utils.network.NetworkStatusService
import com.mapbox.navigation.utils.thread.JobControl
import com.mapbox.navigation.utils.thread.ThreadController
import com.mapbox.navigation.utils.thread.monitorChannelWithException
import java.io.File
import java.lang.reflect.Field
import java.net.URI
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.launch

private const val MAPBOX_NAVIGATION_USER_AGENT_BASE = "mapbox-navigation-android"
private const val MAPBOX_NAVIGATION_UI_USER_AGENT_BASE = "mapbox-navigation-ui-android"
private const val MAPBOX_NAVIGATION_TOKEN_EXCEPTION_OFFBOARD_ROUTER =
    "You need to provide an token access in order to use the default OffboardRouter."
private const val MAPBOX_NAVIGATION_TOKEN_EXCEPTION_ONBOARD_ROUTER =
    "You need to provide an token access in order to use the default OnboardRouter."
private const val MAPBOX_NAVIGATION_TOKEN_EXCEPTION = "A valid token is required"

/**
 * ## Mapbox Navigation Core SDK
 * An entry point for interacting with and customizing a navigation session.
 *
 * **Only one instance of this class should be used per application process.**
 *
 * The Navigation Core SDK artifact is composed out of multiple separate artifacts or modules.
 * TODO insert modules documentation
 *
 * The [MapboxNavigation] implementation can enter into a couple of internal states:
 * - `Idle`
 * - `Free Drive`
 * - `Active Guidance`
 *
 * The SDK starts of in an `Idle` state.
 *
 * ### Location
 * Whenever the [startTripSession] is called, the SDK will enter the `Free Drive` state starting to request and propagate location updates via the [LocationObserver].
 *
 * This observer provides 2 location update values in mixed intervals - either the raw one received from the provided [LocationEngine]
 * or the enhanced one map-matched internally using SDK's native capabilities.
 *
 * In `Free Drive` mode, the enhanced location is computed using nearby to user location's routing tiles that are continuously updating in the background.
 * This can be configured using the [MapboxOnboardRouterConfig] in the [NavigationOptions].
 *
 * If the session is stopped, the SDK will stop listening for raw location updates and enter the `Idle` state.
 *
 * ### Routing
 * A route can be requested with [requestRoutes]. If the request is successful and returns a non-empty list of routes in the [RoutesObserver],
 * the first route at index 0 is going to be chosen as a primary one.
 *
 * If the SDK is in an `Idle` state, it stays in this same state even when a primary route is available.
 *
 * If the SDK is already in the `Free Drive` mode or entering it whenever a primary route is available,
 * the SDK will enter the `Active Guidance` mode instead and propagate meaningful [RouteProgress].
 * Additionally, the enhanced location's map-matching will be more precise and based on the primary route itself.
 *
 * If a new routes request is made, or the routes are manually cleared, the SDK automatically fall back to either `Idle` or `Free Drive` state.
 *
 * You can use [setRoutes] to provide new routes, clear current ones, or change the route at primary index 0.
 * todo should we expose a "primaryRouteIndex" field instead of relying on the list's order?
 *
 * @param context activity/fragment's context
 * @param accessToken [Mapbox Access Token](https://docs.mapbox.com/help/glossary/access-token/)
 * @param navigationOptions a set of [NavigationOptions] used to customize various features of the SDK
 * @param locationEngine used to listen for raw location updates
 * @param locationEngineRequest used to request raw location updates
 */
class MapboxNavigation
@JvmOverloads
constructor(
    private val context: Context,
    private val accessToken: String?,
    private val navigationOptions: NavigationOptions = defaultNavigationOptions(
        context,
        accessToken
    ),
    val locationEngine: LocationEngine = LocationEngineProvider.getBestLocationEngine(context.applicationContext),
    locationEngineRequest: LocationEngineRequest = LocationEngineRequest.Builder(1000L)
        .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
        .build()
) {

    private val mainJobController: JobControl = ThreadController.getMainScopeAndRootJob()
    private val directionsSession: DirectionsSession
    private val tripService: TripService
    private val tripSession: TripSession
    private val navigationSession = NavigationSession(context)
    private val internalRoutesObserver = createInternalRoutesObserver()
    private val internalOffRouteObserver = createInternalOffRouteObserver()
    private val fasterRouteController: FasterRouteController
    private val routeRefreshController: RouteRefreshController

    private var notificationChannelField: Field? = null
    private val MAPBOX_NAVIGATION_NOTIFICATION_PACKAGE_NAME =
        "com.mapbox.navigation.trip.notification.MapboxTripNotification"
    private val MAPBOX_NOTIFICATION_ACTION_CHANNEL = "notificationActionButtonChannel"

    init {
        ThreadController.init()
        directionsSession = NavigationComponentProvider.createDirectionsSession(
            NavigationModuleProvider.createModule(
                MapboxNavigationModuleType.HybridRouter,
                ::paramsProvider
            )
        )
        directionsSession.registerRoutesObserver(internalRoutesObserver)
        directionsSession.registerRoutesObserver(navigationSession)
        val notification: TripNotification = NavigationModuleProvider.createModule(
            MapboxNavigationModuleType.TripNotification,
            ::paramsProvider
        )
        if (notification.javaClass.name == MAPBOX_NAVIGATION_NOTIFICATION_PACKAGE_NAME) {
            notificationChannelField =
                notification.javaClass.getDeclaredField(MAPBOX_NOTIFICATION_ACTION_CHANNEL).apply {
                    isAccessible = true
                }
        }
        tripService = NavigationComponentProvider.createTripService(
            context.applicationContext,
            notification
        )
        tripSession = NavigationComponentProvider.createTripSession(
            tripService,
            locationEngine,
            locationEngineRequest,
            navigationOptions.navigatorPollingDelay
        )
        tripSession.registerOffRouteObserver(internalOffRouteObserver)
        tripSession.registerStateObserver(navigationSession)
        ifNonNull(accessToken) { token ->
            MapboxMetricsReporter.init(
                context,
                accessToken ?: throw RuntimeException(MAPBOX_NAVIGATION_TOKEN_EXCEPTION),
                obtainUserAgent(navigationOptions.isFromNavigationUi)
            )
            MapboxNavigationTelemetry.initialize(
                context.applicationContext,
                token,
                this,
                MapboxMetricsReporter,
                locationEngine.javaClass.name,
                ThreadController.getMainScopeAndRootJob(),
                navigationOptions
            )
        }

        fasterRouteController = FasterRouteController(directionsSession, tripSession)
        routeRefreshController = RouteRefreshController(directionsSession, tripSession)
        routeRefreshController.start()
    }

    /**
     * Starts listening for location updates and enters an `Active Guidance` state if there's a primary route available
     * or a `Free Drive` state otherwise.
     *
     * @see [registerTripSessionStateObserver]
     * @see [registerRouteProgressObserver]
     */
    @RequiresPermission(anyOf = [ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION])
    fun startTripSession() {
        tripSession.start()
        notificationChannelField?.let {
            monitorNotificationActionButton(it.get(null) as ReceiveChannel<NotificationAction>)
        }
    }

    /**
     * Stops listening for location updates and enters an `Idle` state.
     *
     * @see [registerTripSessionStateObserver]
     */
    fun stopTripSession() {
        tripSession.stop()
    }

    /**
     * Return the current [TripSessionState].
     */
    fun getTripSessionState() = tripSession.getState()

    /**
     * Requests a route using the provided [Router] implementation.
     * If the request succeeds and the SDK enters an `Active Guidance` state, meaningful [RouteProgress] updates will be available.
     *
     * Use [RoutesObserver] and [MapboxNavigation.registerRoutesObserver] to observe whenever the routes list reference managed by the SDK changes, regardless of a source.
     *
     * Use [MapboxNavigation.setRoutes] to supply a transformed list of routes, or a list from an external source, to be managed by the SDK.
     *
     * @param routeOptions params for the route request
     * @param routesRequestCallback listener that gets notified when request state changes
     * @see [registerRoutesObserver]
     * @see [registerRouteProgressObserver]
     */
    @JvmOverloads
    fun requestRoutes(
        routeOptions: RouteOptions,
        routesRequestCallback: RoutesRequestCallback? = null
    ) {
        directionsSession.requestRoutes(routeOptions, routesRequestCallback)
    }

    /**
     * Set a list of routes.
     *
     * If the list is empty, the SDK will exit the `Active Guidance` state.
     *
     * If the list is not empty, the route at index 0 is going to be treated as the primary route
     * and used for route progress, off route events and map-matching calculations.
     *
     * Use [RoutesObserver] and [MapboxNavigation.registerRoutesObserver] to observe whenever the routes list reference managed by the SDK changes, regardless of a source.
     *
     * @param routes a list of [DirectionsRoute]s
     */
    fun setRoutes(routes: List<DirectionsRoute>) {
        directionsSession.routes = routes
    }

    /**
     * Get a list of routes.
     *
     * If the list is not empty, the route at index 0 is the one treated as the primary route
     * and used for route progress, off route events and map-matching calculations.
     *
     * @return a list of [DirectionsRoute]s
     */
    fun getRoutes() = directionsSession.routes

    /**
     * Call this method whenever this instance of the [MapboxNavigation] is not going to be used anymore and should release all of its resources.
     */
    fun onDestroy() {
        mainJobController.scope.launch {
            Log.d(TAG, "onDestroy")
            MapboxNavigationTelemetry.unregisterListeners(this@MapboxNavigation).join()
            MapboxMetricsReporter.disable()
            ThreadController.cancelAllNonUICoroutines()
            ThreadController.cancelAllUICoroutines()
            directionsSession.shutDownSession()
            directionsSession.unregisterAllRoutesObservers()
            tripSession.unregisterAllLocationObservers()
            tripSession.unregisterAllRouteProgressObservers()
            tripSession.unregisterAllOffRouteObservers()
            tripSession.unregisterAllStateObservers()
            tripSession.unregisterAllBannerInstructionsObservers()
            tripSession.unregisterAllVoiceInstructionsObservers()
            fasterRouteController.stop()
            routeRefreshController.stop()
        }
    }

    /**
     * API used to retrieve logged location and route progress samples for debug purposes.
     */
    fun retrieveHistory(): String {
        return MapboxNativeNavigatorImpl.getHistory()
    }

    /**
     * API used to enable/disable location and route progress samples logs for debug purposes.
     */
    fun toggleHistory(isEnabled: Boolean) {
        MapboxNativeNavigatorImpl.toggleHistory(isEnabled)
    }

    /**
     * API used to artificially add debug events to logs.
     */
    fun addHistoryEvent(eventType: String, eventJsonProperties: String) {
        MapboxNativeNavigatorImpl.addHistoryEvent(eventType, eventJsonProperties)
    }

    /**
     * API used to retrieve the ssmlannouncement for voice instruction.
     */
    fun retrieveSsmlAnnouncementInstruction(index: Int): String? =
        MapboxNativeNavigatorImpl.getVoiceInstruction(index)?.ssmlAnnouncement

    /**
     * Registers [LocationObserver]. The updates are available whenever the trip session is started.
     *
     * @see [startTripSession]
     */
    fun registerLocationObserver(locationObserver: LocationObserver) {
        tripSession.registerLocationObserver(locationObserver)
    }

    /**
     * Unregisters [LocationObserver].
     */
    fun unregisterLocationObserver(locationObserver: LocationObserver) {
        tripSession.unregisterLocationObserver(locationObserver)
    }

    /**
     * Registers [RouteProgressObserver]. The updates are available whenever the trip session is started and a primary route is available.
     *
     * @see [startTripSession]
     * @see [requestRoutes] // TODO add route setter
     */
    fun registerRouteProgressObserver(routeProgressObserver: RouteProgressObserver) {
        tripSession.registerRouteProgressObserver(routeProgressObserver)
    }

    /**
     * Unregisters [RouteProgressObserver].
     */
    fun unregisterRouteProgressObserver(routeProgressObserver: RouteProgressObserver) {
        tripSession.unregisterRouteProgressObserver(routeProgressObserver)
    }

    /**
     * Registers [OffRouteObserver]. The updates are available whenever SDK is in an `Active Guidance` state and detects an off route event.
     * The SDK will automatically request redirected route.
     */
    fun registerOffRouteObserver(offRouteObserver: OffRouteObserver) {
        tripSession.registerOffRouteObserver(offRouteObserver)
    }

    /**
     * Unregisters [OffRouteObserver].
     */
    fun unregisterOffRouteObserver(offRouteObserver: OffRouteObserver) {
        tripSession.unregisterOffRouteObserver(offRouteObserver)
    }

    /**
     * Registers [RoutesObserver]. The updates are available when a new list of routes is set.
     * The route at index 0, if exist, will be treated as the primary route for 'Active Guidance' and location map-matching.
     */
    fun registerRoutesObserver(routesObserver: RoutesObserver) {
        directionsSession.registerRoutesObserver(routesObserver)
    }

    /**
     * Unregisters [RoutesObserver].
     */
    fun unregisterRoutesObserver(routesObserver: RoutesObserver) {
        directionsSession.unregisterRoutesObserver(routesObserver)
    }

    /**
     * Registers [BannerInstructionsObserver]. The updates are available whenever SDK is in an `Active Guidance` state.
     * The SDK will push this event only once per route step.
     */
    fun registerBannerInstructionsObserver(bannerInstructionsObserver: BannerInstructionsObserver) {
        tripSession.registerBannerInstructionsObserver(bannerInstructionsObserver)
    }

    /**
     * Unregisters [BannerInstructionsObserver].
     */
    fun unregisterBannerInstructionsObserver(bannerInstructionsObserver: BannerInstructionsObserver) {
        tripSession.unregisterBannerInstructionsObserver(bannerInstructionsObserver)
    }

    /**
     * Registers [VoiceInstructionsObserver]. The updates are available whenever SDK is in an `Active Guidance` state.
     * The SDK will push this event only once per route step.
     */
    fun registerVoiceInstructionsObserver(voiceInstructionsObserver: VoiceInstructionsObserver) {
        tripSession.registerVoiceInstructionsObserver(voiceInstructionsObserver)
    }

    /**
     * Unregisters [VoiceInstructionsObserver].
     */
    fun unregisterVoiceInstructionsObserver(voiceInstructionsObserver: VoiceInstructionsObserver) {
        tripSession.unregisterVoiceInstructionsObserver(voiceInstructionsObserver)
    }

    /**
     * Registers [TripSessionStateObserver]. Monitors the trip session's state.
     *
     * @see [startTripSession]
     * @see [stopTripSession]
     */
    fun registerTripSessionStateObserver(tripSessionStateObserver: TripSessionStateObserver) {
        tripSession.registerStateObserver(tripSessionStateObserver)
    }

    /**
     * Unregisters [TripSessionStateObserver].
     */
    fun unregisterTripSessionStateObserver(tripSessionStateObserver: TripSessionStateObserver) {
        tripSession.unregisterStateObserver(tripSessionStateObserver)
    }

    fun attachFasterRouteObserver(fasterRouteObserver: FasterRouteObserver) {
        fasterRouteController.attach(fasterRouteObserver)
    }

    fun detachFasterRouteObserver() {
        fasterRouteController.stop()
    }

    private fun createInternalRoutesObserver() = object : RoutesObserver {
        override fun onRoutesChanged(routes: List<DirectionsRoute>) {
            if (routes.isNotEmpty()) {
                tripSession.route = routes[0]
            } else {
                tripSession.route = null
            }
        }
    }

    private fun createInternalOffRouteObserver() = object : OffRouteObserver {
        override fun onOffRouteStateChanged(offRoute: Boolean) {
            if (offRoute) {
                reRoute()
            }
        }
    }

    private fun reRoute() {
        ifNonNull(tripSession.getEnhancedLocation()) { location ->
            val optionsRebuilt = AdjustedRouteOptionsProvider.getRouteOptions(
                directionsSession,
                tripSession,
                location
            )
                ?: return
            directionsSession.requestRoutes(
                optionsRebuilt,
                null
            )
        }
    }

    private fun obtainUserAgent(isFromNavigationUi: Boolean): String {
        return if (isFromNavigationUi) {
            "$MAPBOX_NAVIGATION_UI_USER_AGENT_BASE/${BuildConfig.MAPBOX_NAVIGATION_VERSION_NAME}"
        } else {
            "$MAPBOX_NAVIGATION_USER_AGENT_BASE/${BuildConfig.MAPBOX_NAVIGATION_VERSION_NAME}"
        }
    }

    private fun monitorNotificationActionButton(channel: ReceiveChannel<NotificationAction>) {
        mainJobController.scope.monitorChannelWithException(channel, { notificationAction ->
            when (notificationAction) {
                NotificationAction.END_NAVIGATION -> tripSession.stop()
            }
        })
    }

    /**
     * Provides parameters for Mapbox default modules, recursively if a module depends on other Mapbox modules.
     */
    private fun paramsProvider(type: MapboxNavigationModuleType): Array<Pair<Class<*>?, Any?>> {
        return when (type) {
            MapboxNavigationModuleType.HybridRouter -> arrayOf(
                Router::class.java to NavigationModuleProvider.createModule(
                    MapboxNavigationModuleType.OnboardRouter,
                    ::paramsProvider
                ),
                Router::class.java to NavigationModuleProvider.createModule(
                    MapboxNavigationModuleType.OffboardRouter,
                    ::paramsProvider
                ),
                NetworkStatusService::class.java to NetworkStatusService(context.applicationContext)
            )
            MapboxNavigationModuleType.OffboardRouter -> arrayOf(
                String::class.java to (accessToken
                    ?: throw RuntimeException(MAPBOX_NAVIGATION_TOKEN_EXCEPTION_OFFBOARD_ROUTER)),
                Context::class.java to context,
                SkuTokenProvider::class.java to MapboxNavigationAccounts.getInstance(context)
            )
            MapboxNavigationModuleType.OnboardRouter -> {
                check(accessToken != null) { MAPBOX_NAVIGATION_TOKEN_EXCEPTION_ONBOARD_ROUTER }
                arrayOf(
                    MapboxNativeNavigator::class.java to MapboxNativeNavigatorImpl,
                    MapboxOnboardRouterConfig::class.java to (navigationOptions.onboardRouterConfig
                        ?: throw RuntimeException(MAPBOX_NAVIGATION_TOKEN_EXCEPTION_ONBOARD_ROUTER))
                )
            }
            MapboxNavigationModuleType.DirectionsSession -> throw NotImplementedError() // going to be removed when next base version
            MapboxNavigationModuleType.TripNotification -> arrayOf(
                Context::class.java to context.applicationContext,
                NavigationOptions::class.java to navigationOptions
            )
            MapboxNavigationModuleType.TripService -> throw NotImplementedError() // going to be removed when next base version
            MapboxNavigationModuleType.TripSession -> throw NotImplementedError() // going to be removed when next base version
            MapboxNavigationModuleType.Logger -> arrayOf()
        }
    }

    fun updateSensorEvent(sensorEvent: SensorEvent) {
        tripSession.updateSensorEvent(sensorEvent)
    }

    /**
     * Updates the configuration to enable or disable the extended kalman filter (EKF).
     *
     * @param useEKF the new value for EKF
     */
    fun useExtendedKalmanFilter(useEKF: Boolean) {
        tripSession.useExtendedKalmanFilter(useEKF)
    }

    companion object {

        @JvmStatic
        fun postUserFeedback(
            @TelemetryUserFeedback.FeedbackType feedbackType: String,
            description: String,
            @TelemetryUserFeedback.FeedbackSource feedbackSource: String,
            screenshot: String?
        ) {
            MapboxNavigationTelemetry.postUserFeedback(
                feedbackType,
                description,
                feedbackSource,
                screenshot
            )
        }

        /**
         * Returns a pre-build set of [NavigationOptions] with smart defaults.
         *
         * Use [NavigationOptions.toBuilder] to easily customize selected options.
         */
        @JvmStatic
        fun defaultNavigationOptions(context: Context, accessToken: String?): NavigationOptions {
            val builder = NavigationOptions.Builder()
                .timeFormatType(NONE_SPECIFIED)
                .roundingIncrement(ROUNDING_INCREMENT_FIFTY)
                .navigatorPollingDelay(DEFAULT_NAVIGATOR_POLLING_DELAY)
                .distanceFormatter(
                    MapboxDistanceFormatter(
                        context.applicationContext,
                        null,
                        UNDEFINED,
                        ROUNDING_INCREMENT_FIFTY
                    )
                )

            // TODO provide a production routing tiles endpoint
            val tilesUri = URI("")
            val tilesVersion = ""
            val tilesDir = if (tilesUri.toString().isNotEmpty() && tilesVersion.isNotEmpty()) {
                File(
                    context.filesDir,
                    "Offline/${tilesUri.host}/$tilesVersion"
                ).absolutePath
            } else ""

            builder.onboardRouterConfig(
                MapboxOnboardRouterConfig(
                    tilesDir,
                    null,
                    null,
                    2,
                    Endpoint(
                        tilesUri.toString(),
                        tilesVersion,
                        accessToken ?: "",
                        "MapboxNavigationNative"
                    )
                )
            )

            return builder.build()
        }
    }
}
