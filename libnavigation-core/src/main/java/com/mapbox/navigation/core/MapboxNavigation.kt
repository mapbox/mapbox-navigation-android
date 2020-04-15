package com.mapbox.navigation.core

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.content.Context
import android.hardware.SensorEvent
import androidx.annotation.RequiresPermission
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineProvider
import com.mapbox.android.core.location.LocationEngineRequest
import com.mapbox.annotation.module.MapboxModuleType
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.base.common.logger.Logger
import com.mapbox.base.common.logger.model.Message
import com.mapbox.base.common.logger.model.Tag
import com.mapbox.common.module.provider.MapboxModuleProvider
import com.mapbox.navigation.base.TimeFormat
import com.mapbox.navigation.base.internal.VoiceUnit
import com.mapbox.navigation.base.internal.accounts.SkuTokenProvider
import com.mapbox.navigation.base.internal.accounts.UrlSkuTokenProvider
import com.mapbox.navigation.base.options.DEFAULT_NAVIGATOR_PREDICTION_MILLIS
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.options.OnboardRouterOptions
import com.mapbox.navigation.base.route.Router
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.notification.NotificationAction
import com.mapbox.navigation.base.trip.notification.TripNotification
import com.mapbox.navigation.core.accounts.NavigationAccountsSession
import com.mapbox.navigation.core.arrival.ArrivalController
import com.mapbox.navigation.core.arrival.ArrivalObserver
import com.mapbox.navigation.core.arrival.ArrivalProgressObserver
import com.mapbox.navigation.core.arrival.AutoArrivalController
import com.mapbox.navigation.core.directions.session.AdjustedRouteOptionsProvider
import com.mapbox.navigation.core.directions.session.DirectionsSession
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.directions.session.RoutesRequestCallback
import com.mapbox.navigation.core.fasterroute.FasterRouteController
import com.mapbox.navigation.core.fasterroute.FasterRouteObserver
import com.mapbox.navigation.core.internal.MapboxDistanceFormatter
import com.mapbox.navigation.core.internal.accounts.MapboxNavigationAccounts
import com.mapbox.navigation.core.internal.trip.service.TripService
import com.mapbox.navigation.core.routerefresh.RouteRefreshController
import com.mapbox.navigation.core.telemetry.MapboxNavigationTelemetry
import com.mapbox.navigation.core.telemetry.events.FeedbackEvent
import com.mapbox.navigation.core.trip.session.BannerInstructionsObserver
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.core.trip.session.OffRouteObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.core.trip.session.TripSession
import com.mapbox.navigation.core.trip.session.TripSessionStateObserver
import com.mapbox.navigation.core.trip.session.VoiceInstructionsObserver
import com.mapbox.navigation.metrics.MapboxMetricsReporter
import com.mapbox.navigation.navigator.internal.MapboxNativeNavigator
import com.mapbox.navigation.navigator.internal.MapboxNativeNavigatorImpl
import com.mapbox.navigation.utils.internal.JobControl
import com.mapbox.navigation.utils.internal.NetworkStatusService
import com.mapbox.navigation.utils.internal.ThreadController
import com.mapbox.navigation.utils.internal.ifNonNull
import com.mapbox.navigation.utils.internal.monitorChannelWithException
import java.lang.reflect.Field
import kotlinx.coroutines.channels.ReceiveChannel

private const val MAPBOX_NAVIGATION_USER_AGENT_BASE = "mapbox-navigation-android"
private const val MAPBOX_NAVIGATION_UI_USER_AGENT_BASE = "mapbox-navigation-ui-android"
private const val MAPBOX_NAVIGATION_TOKEN_EXCEPTION_OFFBOARD_ROUTER =
    "You need to provide an access token in NavigationOptions in order to use the default OffboardRouter. " +
        "Also see MapboxNavigation#defaultNavigationOptions"
private const val MAPBOX_NAVIGATION_TOKEN_EXCEPTION_ONBOARD_ROUTER =
    "You need to provide an access token in NavigationOptions in order to use the default OnboardRouter. " +
        "Also see MapboxNavigation#defaultNavigationOptions"
private const val MAPBOX_NAVIGATION_OPTIONS_EXCEPTION_ONBOARD_ROUTER =
    "You need to provide OnboardRouterOptions in NavigationOptions in order to use the default OnboardRouter. " +
        "Also see MapboxNavigation#defaultNavigationOptions"
private const val MAPBOX_NAVIGATION_TOKEN_EXCEPTION = "You need to provide an access token in NavigationOptions " +
    "Also see MapboxNavigation#defaultNavigationOptions"

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
 * This can be configured using the [OnboardRouterOptions] in the [NavigationOptions].
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
 * @param navigationOptions a set of [NavigationOptions] used to customize various features of the SDK.
 * Use [defaultNavigationOptions] to set default options
 * @param locationEngine used to listen for raw location updates
 * @param locationEngineRequest used to request raw location updates
 */
class MapboxNavigation
@JvmOverloads
constructor(
    private val context: Context,
    private val navigationOptions: NavigationOptions,
    val locationEngine: LocationEngine = LocationEngineProvider.getBestLocationEngine(context.applicationContext),
    locationEngineRequest: LocationEngineRequest = LocationEngineRequest.Builder(1000L)
        .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
        .build()
) {

    private val accessToken: String? = navigationOptions.accessToken
    private val mainJobController: JobControl = ThreadController.getMainScopeAndRootJob()
    private val directionsSession: DirectionsSession
    private val navigator: MapboxNativeNavigator
    private val tripService: TripService
    private val tripSession: TripSession
    private val navigationSession: NavigationSession
    private val navigationAccountsSession = NavigationAccountsSession(context)
    private val logger: Logger
    private val internalRoutesObserver = createInternalRoutesObserver()
    private val internalOffRouteObserver = createInternalOffRouteObserver()
    private val fasterRouteController: FasterRouteController
    private val routeRefreshController: RouteRefreshController
    private val arrivalProgressObserver: ArrivalProgressObserver

    private var notificationChannelField: Field? = null
    private val MAPBOX_NAVIGATION_NOTIFICATION_PACKAGE_NAME =
        "com.mapbox.navigation.trip.notification.MapboxTripNotification"
    private val MAPBOX_NOTIFICATION_ACTION_CHANNEL = "notificationActionButtonChannel"

    init {
        ThreadController.init()
        logger = MapboxModuleProvider.createModule(MapboxModuleType.CommonLogger, ::paramsProvider)
        navigator = NavigationComponentProvider.createNativeNavigator(navigationOptions.deviceProfile, logger)
        navigationSession = NavigationComponentProvider.createNavigationSession()
        directionsSession = NavigationComponentProvider.createDirectionsSession(
            MapboxModuleProvider.createModule(MapboxModuleType.NavigationRouter, ::paramsProvider)
        )
        directionsSession.registerRoutesObserver(internalRoutesObserver)
        directionsSession.registerRoutesObserver(navigationSession)
        val notification: TripNotification = MapboxModuleProvider.createModule(
            MapboxModuleType.NavigationTripNotification,
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
            notification,
            logger
        )
        tripSession = NavigationComponentProvider.createTripSession(
            tripService,
            locationEngine,
            locationEngineRequest,
            navigationOptions.navigatorPredictionMillis,
            navigator = navigator,
            logger = logger
        )
        tripSession.registerOffRouteObserver(internalOffRouteObserver)
        tripSession.registerStateObserver(navigationSession)
        navigationSession.registerNavigationSessionStateObserver(navigationAccountsSession)
        ifNonNull(accessToken) { token ->
            logger.d(
                Tag(MapboxNavigationTelemetry.TAG),
                Message("MapboxMetricsReporter.init from MapboxNavigation main")
            )
            MapboxMetricsReporter.init(
                context,
                accessToken ?: throw RuntimeException(MAPBOX_NAVIGATION_TOKEN_EXCEPTION),
                obtainUserAgent(navigationOptions.isFromNavigationUi)
            )
            MapboxMetricsReporter.toggleLogging(navigationOptions.isDebugLoggingEnabled)
            MapboxNavigationTelemetry.initialize(
                context.applicationContext,
                this,
                MapboxMetricsReporter,
                locationEngine.javaClass.name,
                ThreadController.getMainScopeAndRootJob(),
                navigationOptions,
                obtainUserAgent(navigationOptions.isFromNavigationUi)
            )
        }

        fasterRouteController = FasterRouteController(directionsSession, tripSession, logger)
        routeRefreshController = RouteRefreshController(directionsSession, tripSession, logger)
        routeRefreshController.start()

        arrivalProgressObserver = ArrivalProgressObserver(tripSession)
        attachArrivalController()
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
     * Return the current [TripSession]'s state.
     * The state is [STARTED] when the session is active, running a foreground service and
     * requesting and returning location updates.
     * The state is [STOPPED] when the session is inactive.
     *
     * @return current [TripSessionState]
     * @see [registerTripSessionStateObserver]
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
        logger.d(
            Tag(MapboxNavigationTelemetry.TAG),
            Message("onDestroy")
        )
        MapboxNavigationTelemetry.unregisterListeners(this@MapboxNavigation)
        directionsSession.shutdown()
        directionsSession.unregisterAllRoutesObservers()
        tripSession.stop()
        tripSession.unregisterAllLocationObservers()
        tripSession.unregisterAllRouteProgressObservers()
        tripSession.unregisterAllOffRouteObservers()
        tripSession.unregisterAllStateObservers()
        tripSession.unregisterAllBannerInstructionsObservers()
        tripSession.unregisterAllVoiceInstructionsObservers()
        tripSession.route = null

        // TODO replace this with a destroy when nav-native has a destructor
        navigator.create(navigationOptions.deviceProfile, logger)

        navigationSession.unregisterAllNavigationSessionStateObservers()
        fasterRouteController.stop()
        routeRefreshController.stop()
        ThreadController.cancelAllNonUICoroutines()
        ThreadController.cancelAllUICoroutines()
    }

    /**
     * API used to retrieve logged location and route progress samples for debug purposes.
     *
     * @return history trace string
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
     * API used to retrieve the SSML announcement for voice instructions.
     *
     * @return SSML voice instruction announcement string
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

    /**
     * Attach your own controller to determine when drivers arrived at stops via [ArrivalController]
     * Use [navigateNextRouteLeg] to manually move navigator to the next stop. To reset to the
     * automatic arrival controller, call [attachArrivalController].
     *
     * @param arrivalController [ArrivalController]
     */
    @JvmOverloads
    fun attachArrivalController(arrivalController: ArrivalController = AutoArrivalController()) {
        arrivalProgressObserver.attach(arrivalController)
        tripSession.registerRouteProgressObserver(arrivalProgressObserver)
    }

    /**
     * Disable arrival at stops completely. Use this if you want to write your
     * own mechanism for handling arrival at stops.
     */
    fun removeArrivalController() {
        tripSession.unregisterRouteProgressObserver(arrivalProgressObserver)
    }

    /**
     * Registers [ArrivalObserver]. Monitor arrival at stops and destinations. For more control
     * of arrival at stops, see [attachArrivalController].
     *
     * @see [unregisterArrivalObserver]
     */
    fun registerArrivalObserver(arrivalObserver: ArrivalObserver) {
        arrivalProgressObserver.registerObserver(arrivalObserver)
    }

    /**
     * Unregisters [ArrivalObserver].
     *
     * @see [registerArrivalObserver]
     */
    fun unregisterArrivalObserver(arrivalObserver: ArrivalObserver) {
        arrivalProgressObserver.unregisterObserver(arrivalObserver)
    }

    /**
     * After arriving at a stop, this can be used to manually decide when to start
     * navigating to the next stop. Use the [ArrivalController] to control when to
     * call [navigateNextRouteLeg].
     *
     * @return true if navigation to next stop could be started, false otherwise
     */
    fun navigateNextRouteLeg(): Boolean {
        return arrivalProgressObserver.navigateNextRouteLeg()
    }

    /**
     * Start observing faster routes for a trip session via [FasterRouteObserver]
     *
     * @param fasterRouteObserver FasterRouteObserver
     */
    fun attachFasterRouteObserver(fasterRouteObserver: FasterRouteObserver) {
        fasterRouteController.attach(fasterRouteObserver)
    }

    /**
     * Stop observing the possibility of faster routes.
     */
    fun detachFasterRouteObserver() {
        fasterRouteController.stop()
    }

    /**
     * Register a [NavigationSessionStateObserver] to be notified of the various Session states. Not publicly available
     */
    internal fun registerNavigationSessionObserver(navigationSessionStateObserver: NavigationSessionStateObserver) {
        navigationSession.registerNavigationSessionStateObserver(navigationSessionStateObserver)
    }

    /**
     * Unregisters a [NavigationSessionStateObserver]. Not publicly available
     */
    internal fun unregisterNavigationSessionObserver(navigationSessionStateObserver: NavigationSessionStateObserver) {
        navigationSession.unregisterNavigationSessionStateObserver(navigationSessionStateObserver)
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
                reroute()
            }
        }
    }

    private fun reroute() {
        ifNonNull(tripSession.getEnhancedLocation()) { location ->
            val optionsRebuilt = AdjustedRouteOptionsProvider.getRouteOptions(
                directionsSession,
                tripSession,
                location
            ) ?: return
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
    private fun paramsProvider(type: MapboxModuleType): Array<Pair<Class<*>?, Any?>> {
        return when (type) {
            MapboxModuleType.NavigationRouter -> arrayOf(
                Router::class.java to MapboxModuleProvider.createModule(
                    MapboxModuleType.NavigationOnboardRouter,
                    ::paramsProvider
                ),
                Router::class.java to MapboxModuleProvider.createModule(
                    MapboxModuleType.NavigationOffboardRouter,
                    ::paramsProvider
                ),
                NetworkStatusService::class.java to NetworkStatusService(context.applicationContext)
            )
            MapboxModuleType.NavigationOffboardRouter -> arrayOf(
                String::class.java to (accessToken
                    ?: throw RuntimeException(MAPBOX_NAVIGATION_TOKEN_EXCEPTION_OFFBOARD_ROUTER)),
                Context::class.java to context,
                UrlSkuTokenProvider::class.java to MapboxNavigationAccounts.getInstance(context)
            )
            MapboxModuleType.NavigationOnboardRouter -> {
                check(accessToken != null) { MAPBOX_NAVIGATION_TOKEN_EXCEPTION_ONBOARD_ROUTER }
                arrayOf(
                    String::class.java to accessToken,
                    MapboxNativeNavigator::class.java to MapboxNativeNavigatorImpl,
                    OnboardRouterOptions::class.java to (navigationOptions.onboardRouterOptions
                        ?: throw RuntimeException(MAPBOX_NAVIGATION_OPTIONS_EXCEPTION_ONBOARD_ROUTER)),
                    Logger::class.java to logger,
                    SkuTokenProvider::class.java to MapboxNavigationAccounts.getInstance(context)
                )
            }
            MapboxModuleType.NavigationTripNotification -> arrayOf(
                Context::class.java to context.applicationContext,
                NavigationOptions::class.java to navigationOptions
            )
            MapboxModuleType.CommonLogger -> arrayOf()
            MapboxModuleType.CommonLibraryLoader -> throw IllegalArgumentException("not supported: $type")
            MapboxModuleType.CommonHttpClient -> throw IllegalArgumentException("not supported: $type")
        }
    }

    /**
     * Sends an event to improve navigation positioning. See SensorEventEmitter to register
     *
     * @param sensorEvent the Android sensor event, it will be ignored if it is not recognized
     */
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

    /**
     * Toggles Electronic Horizon on or off.
     *
     * Electronic Horizon is still **experimental**, which means that the design of the
     * APIs has open issues which may (or may not) lead to their changes in the future.
     * Roughly speaking, there is a chance that those declarations will be deprecated in the near
     * future or the semantics of their behavior may change in some way that may break some code.
     *
     * For now, Electronic Horizon only works in Free Drive.
     *
     * @param isEnabled set this to true to turn on Electronic Horizon and false to turn it off
     */
    fun toggleElectronicHorizon(isEnabled: Boolean) {
        MapboxNativeNavigatorImpl.toggleElectronicHorizon(isEnabled)
    }

    companion object {

        /**
         * Send user feedback about an issue or problem with the Navigation SDK
         *
         * @param feedbackType one of [FeedbackEvent.Type]
         * @param description description message
         * @param feedbackSource one of [FeedbackEvent.Source]
         * @param screenshot encoded screenshot (optional)
         * @param feedbackSubType array of [FeedbackEvent.Description] (optional)
         */
        @JvmStatic
        fun postUserFeedback(
            @FeedbackEvent.Type feedbackType: String,
            description: String,
            @FeedbackEvent.Source feedbackSource: String,
            screenshot: String?,
            feedbackSubType: Array<String>? = emptyArray()
        ) {
            MapboxNavigationTelemetry.postUserFeedback(
                feedbackType,
                description,
                feedbackSource,
                screenshot,
                feedbackSubType
            )
        }

        /**
         * Returns a pre-build set of [NavigationOptions] with smart defaults.
         *
         * Use [NavigationOptions.toBuilder] to easily customize selected options.
         *
         * @param context [Context]
         * @param accessToken Mapbox access token
         * @return default [NavigationOptions]
         */
        @JvmStatic
        fun defaultNavigationOptions(context: Context, accessToken: String?): NavigationOptions {
            val distanceFormatter = MapboxDistanceFormatter.builder()
                .withUnitType(VoiceUnit.UNDEFINED)
                .withRoundingIncrement(Rounding.INCREMENT_FIFTY)
                .build(context)
            val builder = NavigationOptions.Builder()
                .accessToken(accessToken)
                .timeFormatType(TimeFormat.NONE_SPECIFIED)
                .navigatorPredictionMillis(DEFAULT_NAVIGATOR_PREDICTION_MILLIS)
                .distanceFormatter(distanceFormatter)

            val onboardRouterOptions = OnboardRouterOptions.Builder()
                .internalFilePath(context)
                .build()
            builder.onboardRouterOptions(onboardRouterOptions)

            return builder.build()
        }
    }
}
