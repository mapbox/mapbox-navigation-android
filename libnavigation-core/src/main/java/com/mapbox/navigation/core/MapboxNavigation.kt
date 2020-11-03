package com.mapbox.navigation.core

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.content.Context
import android.hardware.SensorEvent
import androidx.annotation.RequiresPermission
import androidx.annotation.UiThread
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.annotation.module.MapboxModuleType
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.base.common.logger.Logger
import com.mapbox.base.common.logger.model.Message
import com.mapbox.common.module.provider.MapboxModuleProvider
import com.mapbox.common.module.provider.ModuleProviderArgument
import com.mapbox.navigation.base.internal.VoiceUnit
import com.mapbox.navigation.base.internal.accounts.UrlSkuTokenProvider
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.options.OnboardRouterOptions
import com.mapbox.navigation.base.route.Router
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.model.alert.UpcomingRouteAlert
import com.mapbox.navigation.base.trip.notification.NotificationAction
import com.mapbox.navigation.base.trip.notification.TripNotification
import com.mapbox.navigation.core.MapboxNavigation.Companion.defaultNavigationOptionsBuilder
import com.mapbox.navigation.core.accounts.NavigationAccountsSession
import com.mapbox.navigation.core.arrival.ArrivalController
import com.mapbox.navigation.core.arrival.ArrivalObserver
import com.mapbox.navigation.core.arrival.ArrivalProgressObserver
import com.mapbox.navigation.core.arrival.AutoArrivalController
import com.mapbox.navigation.core.directions.session.DirectionsSession
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.directions.session.RoutesRequestCallback
import com.mapbox.navigation.core.fasterroute.FasterRouteController
import com.mapbox.navigation.core.fasterroute.FasterRouteDetector
import com.mapbox.navigation.core.fasterroute.FasterRouteObserver
import com.mapbox.navigation.core.fasterroute.RouteComparator
import com.mapbox.navigation.core.internal.accounts.MapboxNavigationAccounts
import com.mapbox.navigation.core.internal.formatter.MapboxDistanceFormatter
import com.mapbox.navigation.core.reroute.MapboxRerouteController
import com.mapbox.navigation.core.reroute.RerouteController
import com.mapbox.navigation.core.reroute.RerouteState
import com.mapbox.navigation.core.routeoptions.MapboxRouteOptionsUpdater
import com.mapbox.navigation.core.routerefresh.RouteRefreshController
import com.mapbox.navigation.core.telemetry.MapboxNavigationTelemetry
import com.mapbox.navigation.core.telemetry.events.AppMetadata
import com.mapbox.navigation.core.telemetry.events.FeedbackEvent
import com.mapbox.navigation.core.trip.service.TripService
import com.mapbox.navigation.core.trip.session.BannerInstructionsObserver
import com.mapbox.navigation.core.trip.session.EHorizonObserver
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.core.trip.session.OffRouteObserver
import com.mapbox.navigation.core.trip.session.RouteAlertsObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.core.trip.session.TripSession
import com.mapbox.navigation.core.trip.session.TripSessionState
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
import com.mapbox.navigator.NavigatorConfig
import com.mapbox.navigator.TileEndpointConfiguration
import com.mapbox.navigator.TilesConfig
import kotlinx.coroutines.channels.ReceiveChannel
import java.lang.reflect.Field

private const val MAPBOX_NAVIGATION_USER_AGENT_BASE = "mapbox-navigation-android"
private const val MAPBOX_NAVIGATION_UI_USER_AGENT_BASE = "mapbox-navigation-ui-android"
private const val MAPBOX_NAVIGATION_TOKEN_EXCEPTION_ROUTER =
    "You need to provide an access token in NavigationOptions in order to use the default " +
        "Router. Also see MapboxNavigation#defaultNavigationOptionsBuilder"
private const val MAPBOX_NAVIGATION_NOTIFICATION_PACKAGE_NAME =
    "com.mapbox.navigation.trip.notification.internal.MapboxTripNotification"
private const val MAPBOX_NOTIFICATION_ACTION_CHANNEL = "notificationActionButtonChannel"

/**
 * ## Mapbox Navigation Core SDK
 * An entry point for interacting with the Mapbox Navigation SDK.
 *
 * **Only one instance of this class should be used per application process.**
 * Use [MapboxNavigationProvider] to easily manage the instance across lifecycle.
 *
 * Feel free to visit our [docs pages and examples](https://docs.mapbox.com/android/beta/navigation/overview/) before diving in!
 *
 * The [MapboxNavigation] implementation can enter into a couple of internal states:
 * - `Idle`
 * - `Free Drive`
 * - `Active Guidance`
 *
 * The SDK starts off in an `Idle` state.
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
 *
 * If a new routes request is made, or the routes are manually cleared, the SDK automatically fall back to either `Idle` or `Free Drive` state.
 *
 * You can use [setRoutes] to provide new routes, clear current ones, or change the route at primary index 0.
 *
 * @param navigationOptions a set of [NavigationOptions] used to customize various features of the SDK.
 * Use [defaultNavigationOptionsBuilder] to set default options
 */
@UiThread
class MapboxNavigation(
    val navigationOptions: NavigationOptions
) {

    private val accessToken: String? = navigationOptions.accessToken
    private val mainJobController: JobControl = ThreadController.getMainScopeAndRootJob()
    private val directionsSession: DirectionsSession
    private val navigator: MapboxNativeNavigator
    private val tripService: TripService
    private val tripSession: TripSession
    private val navigationSession: NavigationSession
    private val navigationAccountsSession =
        NavigationAccountsSession(navigationOptions.applicationContext)
    private val logger: Logger
    private val internalRoutesObserver: RoutesObserver
    private val internalOffRouteObserver: OffRouteObserver
    private val fasterRouteController: FasterRouteController
    private val routeRefreshController: RouteRefreshController
    private val arrivalProgressObserver: ArrivalProgressObserver
    private val navigatorConfig = NavigatorConfig(null, null, null)

    private var notificationChannelField: Field? = null

    /**
     * Reroute controller, by default uses [defaultRerouteController].
     */
    private var rerouteController: RerouteController?
    private val defaultRerouteController: RerouteController

    init {
        ThreadController.init()
        logger = MapboxModuleProvider.createModule(MapboxModuleType.CommonLogger, ::paramsProvider)
        navigator = NavigationComponentProvider.createNativeNavigator(
            navigationOptions.deviceProfile,
            navigatorConfig,
            createTilesConfig()
        )
        navigationSession = NavigationComponentProvider.createNavigationSession()
        directionsSession = NavigationComponentProvider.createDirectionsSession(
            MapboxModuleProvider.createModule(MapboxModuleType.NavigationRouter, ::paramsProvider)
        )
        directionsSession.registerRoutesObserver(navigationSession)
        val notification: TripNotification = MapboxModuleProvider
            .createModule(MapboxModuleType.NavigationTripNotification, ::paramsProvider)
        if (notification.javaClass.name == MAPBOX_NAVIGATION_NOTIFICATION_PACKAGE_NAME) {
            notificationChannelField =
                notification.javaClass.getDeclaredField(MAPBOX_NOTIFICATION_ACTION_CHANNEL).apply {
                    isAccessible = true
                }
        }
        tripService = NavigationComponentProvider.createTripService(
            navigationOptions.applicationContext,
            notification,
            logger
        )
        tripSession = NavigationComponentProvider.createTripSession(
            tripService = tripService,
            locationEngine = navigationOptions.locationEngine,
            navigatorPredictionMillis = navigationOptions.navigatorPredictionMillis,
            navigator = navigator,
            logger = logger,
            accessToken = accessToken
        )
        tripSession.registerStateObserver(navigationSession)
        navigationSession.registerNavigationSessionStateObserver(navigationAccountsSession)

        arrivalProgressObserver = ArrivalProgressObserver(tripSession)
        setArrivalController()

        ifNonNull(accessToken) { token ->
            logger.d(
                MapboxNavigationTelemetry.TAG,
                Message("MapboxMetricsReporter.init from MapboxNavigation main")
            )
            MapboxMetricsReporter.init(
                navigationOptions.applicationContext,
                token,
                obtainUserAgent(navigationOptions.isFromNavigationUi)
            )
            MapboxMetricsReporter.toggleLogging(navigationOptions.isDebugLoggingEnabled)
            MapboxNavigationTelemetry.initialize(
                this,
                navigationOptions,
                MapboxMetricsReporter,
                logger
            )
        }

        val routeOptionsProvider = MapboxRouteOptionsUpdater(logger)

        fasterRouteController = FasterRouteController(
            directionsSession,
            tripSession,
            routeOptionsProvider,
            FasterRouteDetector(RouteComparator()),
            logger
        )
        routeRefreshController = RouteRefreshController(directionsSession, tripSession, logger)
        routeRefreshController.start()

        defaultRerouteController = MapboxRerouteController(
            directionsSession,
            tripSession,
            routeOptionsProvider,
            ThreadController,
            logger
        )
        rerouteController = defaultRerouteController

        internalRoutesObserver = createInternalRoutesObserver()
        internalOffRouteObserver = createInternalOffRouteObserver()
        tripSession.registerOffRouteObserver(internalOffRouteObserver)
        directionsSession.registerRoutesObserver(internalRoutesObserver)
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
     * Reset the session with the same configuration. The location becomes unknown,
     * but the [NavigationOptions] stay the same. This can be used to transport the
     * navigator to a new location.
     */
    fun resetTripSession() {
        navigator.resetRideSession()
    }

    /**
     * Return the current [TripSession]'s state.
     * The state is [TripSessionState.STARTED] when the session is active, running a foreground service and
     * requesting and returning location updates.
     * The state is [TripSessionState.STOPPED] when the session is inactive.
     *
     * @return current [TripSessionState]
     * @see [registerTripSessionStateObserver]
     */
    fun getTripSessionState(): TripSessionState = tripSession.getState()

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
        rerouteController?.interrupt()
        directionsSession.requestRoutes(routeOptions, routesRequestCallback)
    }

    /**
     * Set a list of routes.
     *
     * If the list is empty, the SDK will exit the `Active Guidance` state.
     *
     * If the list is not empty, the route at index 0 is going to be treated as the primary route
     * and used for route progress calculations and off route events.
     *
     * Use [RoutesObserver] and [MapboxNavigation.registerRoutesObserver] to observe whenever the routes list reference managed by the SDK changes, regardless of a source.
     *
     * @param routes a list of [DirectionsRoute]s
     */
    fun setRoutes(routes: List<DirectionsRoute>) {
        rerouteController?.interrupt()
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
    fun getRoutes(): List<DirectionsRoute> = directionsSession.routes

    /**
     * Call this method whenever this instance of the [MapboxNavigation] is not going to be used anymore and should release all of its resources.
     */
    fun onDestroy() {
        logger.d(
            MapboxNavigationTelemetry.TAG,
            Message("MapboxNavigation onDestroy")
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
        tripSession.unregisterAllRouteAlertsObservers()
        tripSession.unregisterAllEHorizonObservers()
        directionsSession.routes = emptyList()
        resetTripSession()

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
     * @see [requestRoutes]
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
     *
     * The SDK will automatically request redirected route. You can control or observe this request with
     * [RerouteController] and [RerouteState].
     * @see getRerouteController
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
     * The route at index 0, if exist, will be treated as the primary route for 'Active Guidance'.
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
    fun registerBannerInstructionsObserver(
        bannerInstructionsObserver: BannerInstructionsObserver
    ) {
        tripSession.registerBannerInstructionsObserver(bannerInstructionsObserver)
    }

    /**
     * Unregisters [BannerInstructionsObserver].
     */
    fun unregisterBannerInstructionsObserver(
        bannerInstructionsObserver: BannerInstructionsObserver
    ) {
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
     * Set your own controller to determine when drivers arrived at stops via [ArrivalController].
     * Use [navigateNextRouteLeg] to manually move navigator to the next stop. To reset to the
     * automatic arrival controller, call [setArrivalController].
     *
     * By default uses [AutoArrivalController].
     *
     * Set *null* disables arrival at stops completely. Use this if you want to write your
     * own mechanism for handling arrival at stops.
     *
     * @param arrivalController [ArrivalController]
     */
    @JvmOverloads
    fun setArrivalController(arrivalController: ArrivalController? = AutoArrivalController()) {
        if (arrivalController == null) {
            tripSession.unregisterRouteProgressObserver(arrivalProgressObserver)
        } else {
            arrivalProgressObserver.attach(arrivalController)
            tripSession.registerRouteProgressObserver(arrivalProgressObserver)
        }
    }

    /**
     * Set [RerouteController] that's automatically invoked when user is off-route.
     *
     * By default uses [MapboxRerouteController]. Setting *null* disables auto-reroute.
     */
    @JvmOverloads
    fun setRerouteController(rerouteController: RerouteController? = defaultRerouteController) {
        val legacyRerouteController = this.rerouteController
        this.rerouteController = rerouteController

        if (legacyRerouteController?.state == RerouteState.FetchingRoute) {
            legacyRerouteController.interrupt()
            reroute()
        }
    }

    /**
     * Get currently set [RerouteController].
     *
     * @see setRerouteController
     */
    fun getRerouteController(): RerouteController? = rerouteController

    /**
     * Registers [ArrivalObserver]. Monitor arrival at stops and destinations. For more control
     * of arrival at stops, see [setArrivalController].
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
     * Registers an observer that gets notified whenever the route changes and provides the list
     * of alerts on this new route, if there are any. The alerts returned here are equal to the ones
     * available in [RouteProgress.upcomingRouteAlerts], but they capture the whole route
     * (not only what's ahead of us) and don't have the [UpcomingRouteAlert.distanceToStart] data.
     *
     * @see unregisterRouteAlertsObserver
     */
    fun registerRouteAlertsObserver(routeAlertsObserver: RouteAlertsObserver) {
        tripSession.registerRouteAlertsObserver(routeAlertsObserver)
    }

    /**
     * Unregisters the route alerts observer.
     *
     * @see registerRouteAlertsObserver
     */
    fun unregisterRouteAlertsObserver(routeAlertsObserver: RouteAlertsObserver) {
        tripSession.unregisterRouteAlertsObserver(routeAlertsObserver)
    }

    /**
     * Observer will be called when the EHorizon changes.
     *
     * Registering an EHorizonObserver activates the Electronic Horizon module.
     *
     * Electronic Horizon is still **experimental**, which means that the design of the
     * APIs has open issues which may (or may not) lead to their changes in the future.
     * Roughly speaking, there is a chance that those declarations will be deprecated in the near
     * future or the semantics of their behavior may change in some way that may break some code.
     *
     * @see unregisterEHorizonObserver
     */
    fun registerEHorizonObserver(eHorizonObserver: EHorizonObserver) {
        tripSession.registerEHorizonObserver(eHorizonObserver)
    }

    /**
     * Unregisters a EHorizon observer.
     *
     * Unregistering all observers deactivates the module.
     *
     * Electronic Horizon is still **experimental**, which means that the design of the
     * APIs has open issues which may (or may not) lead to their changes in the future.
     * Roughly speaking, there is a chance that those declarations will be deprecated in the near
     * future or the semantics of their behavior may change in some way that may break some code.
     *
     * @see registerEHorizonObserver
     */
    fun unregisterEHorizonObserver(eHorizonObserver: EHorizonObserver) {
        tripSession.unregisterEHorizonObserver(eHorizonObserver)
    }

    /**
     * Send user feedback about an issue or problem with the Navigation SDK.
     *
     * @param feedbackType one of [FeedbackEvent.Type]
     * @param description description message
     * @param feedbackSource one of [FeedbackEvent.Source]
     * @param screenshot encoded screenshot (optional)
     * @param feedbackSubType array of [FeedbackEvent.Description] (optional)
     * @param appMetadata [AppMetadata] information (optional)
     */
    fun postUserFeedback(
        @FeedbackEvent.Type feedbackType: String,
        description: String,
        @FeedbackEvent.Source feedbackSource: String,
        screenshot: String?,
        feedbackSubType: Array<String>? = emptyArray(),
        appMetadata: AppMetadata? = null
    ) {
        MapboxNavigationTelemetry.postUserFeedback(
            feedbackType,
            description,
            feedbackSource,
            screenshot,
            feedbackSubType,
            appMetadata
        )
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
    internal fun registerNavigationSessionObserver(
        navigationSessionStateObserver: NavigationSessionStateObserver
    ) {
        navigationSession.registerNavigationSessionStateObserver(navigationSessionStateObserver)
    }

    /**
     * Unregisters a [NavigationSessionStateObserver]. Not publicly available
     */
    internal fun unregisterNavigationSessionObserver(
        navigationSessionStateObserver: NavigationSessionStateObserver
    ) {
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
        rerouteController?.reroute(
            object : RerouteController.RoutesCallback {
                override fun onNewRoutes(routes: List<DirectionsRoute>) {
                    setRoutes(routes)
                }
            }
        )
    }

    private fun obtainUserAgent(isFromNavigationUi: Boolean): String {
        return if (isFromNavigationUi) {
            "$MAPBOX_NAVIGATION_UI_USER_AGENT_BASE/${BuildConfig.MAPBOX_NAVIGATION_VERSION_NAME}"
        } else {
            "$MAPBOX_NAVIGATION_USER_AGENT_BASE/${BuildConfig.MAPBOX_NAVIGATION_VERSION_NAME}"
        }
    }

    private fun monitorNotificationActionButton(channel: ReceiveChannel<NotificationAction>) {
        mainJobController.scope.monitorChannelWithException(
            channel,
            { notificationAction ->
                when (notificationAction) {
                    NotificationAction.END_NAVIGATION -> tripSession.stop()
                }
            }
        )
    }

    /**
     * Provides parameters for Mapbox default modules, recursively if a module depends on other Mapbox modules.
     */
    private fun paramsProvider(type: MapboxModuleType): Array<ModuleProviderArgument> {
        return when (type) {
            MapboxModuleType.NavigationRouter -> arrayOf(
                ModuleProviderArgument(
                    String::class.java,
                    accessToken ?: throw RuntimeException(MAPBOX_NAVIGATION_TOKEN_EXCEPTION_ROUTER)
                ),
                ModuleProviderArgument(Context::class.java, navigationOptions.applicationContext),
                ModuleProviderArgument(
                    UrlSkuTokenProvider::class.java,
                    MapboxNavigationAccounts.getInstance(navigationOptions.applicationContext)
                ),
                ModuleProviderArgument(
                    MapboxNativeNavigator::class.java,
                    MapboxNativeNavigatorImpl
                ),
                ModuleProviderArgument(Logger::class.java, logger),
                ModuleProviderArgument(
                    NetworkStatusService::class.java,
                    NetworkStatusService(navigationOptions.applicationContext)
                )
            )
            MapboxModuleType.NavigationTripNotification -> arrayOf(
                ModuleProviderArgument(NavigationOptions::class.java, navigationOptions)
            )
            MapboxModuleType.CommonLogger -> arrayOf()
            MapboxModuleType.CommonLibraryLoader ->
                throw IllegalArgumentException("not supported: $type")
            MapboxModuleType.CommonHttpClient ->
                throw IllegalArgumentException("not supported: $type")
            MapboxModuleType.MapTelemetry -> throw IllegalArgumentException("not supported: $type")
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

    private fun createTilesConfig(): TilesConfig {
        // TODO StrictMode may report a violation as we're creating a File from the Main
        val offlineFilesPath = OnboardRouterFiles(navigationOptions.applicationContext, logger)
            .absolutePath(navigationOptions.onboardRouterOptions)
        return TilesConfig(
            offlineFilesPath,
            null,
            null,
            THREADS_COUNT,
            TileEndpointConfiguration(
                navigationOptions.onboardRouterOptions.tilesUri.toString(),
                navigationOptions.onboardRouterOptions.tilesVersion,
                navigationOptions.accessToken ?: "",
                USER_AGENT,
                "",
                NativeSkuTokenProvider(
                    MapboxNavigationAccounts.getInstance(navigationOptions.applicationContext)
                )
            )
        )
    }

    companion object {

        private const val USER_AGENT: String = "MapboxNavigationNative"
        private const val THREADS_COUNT = 2

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
        fun defaultNavigationOptionsBuilder(
            context: Context,
            accessToken: String?
        ): NavigationOptions.Builder {
            val distanceFormatter = MapboxDistanceFormatter.Builder(context)
                .unitType(VoiceUnit.UNDEFINED)
                .roundingIncrement(Rounding.INCREMENT_FIFTY)
                .build()
            return NavigationOptions.Builder(context)
                .accessToken(accessToken)
                .distanceFormatter(distanceFormatter)
        }
    }
}
