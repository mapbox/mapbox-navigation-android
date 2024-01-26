/**
 * Tampering with any file that contains billing code is a violation of Mapbox Terms of Service and will result in enforcement of the penalties stipulated in the ToS.
 */

package com.mapbox.navigation.core

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import androidx.annotation.RequiresPermission
import androidx.annotation.UiThread
import androidx.annotation.VisibleForTesting
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.annotation.module.MapboxModuleType
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.common.TilesetDescriptor
import com.mapbox.common.module.provider.MapboxModuleProvider
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.extensions.applyLanguageAndVoiceUnitOptions
import com.mapbox.navigation.base.internal.NavigationRouterV2
import com.mapbox.navigation.base.internal.trip.notification.TripNotificationInterceptorOwner
import com.mapbox.navigation.base.options.HistoryRecorderOptions
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.options.RoutingTilesOptions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.NavigationRouter
import com.mapbox.navigation.base.route.NavigationRouterCallback
import com.mapbox.navigation.base.route.RouteAlternativesOptions
import com.mapbox.navigation.base.route.Router
import com.mapbox.navigation.base.route.RouterCallback
import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.base.route.toDirectionsRoutes
import com.mapbox.navigation.base.route.toNavigationRoutes
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.model.eh.EHorizonEdge
import com.mapbox.navigation.base.trip.model.eh.EHorizonEdgeMetadata
import com.mapbox.navigation.base.trip.notification.NotificationAction
import com.mapbox.navigation.base.trip.notification.TripNotification
import com.mapbox.navigation.base.trip.notification.TripNotificationInterceptor
import com.mapbox.navigation.core.accounts.BillingController
import com.mapbox.navigation.core.arrival.ArrivalController
import com.mapbox.navigation.core.arrival.ArrivalObserver
import com.mapbox.navigation.core.arrival.ArrivalProgressObserver
import com.mapbox.navigation.core.arrival.AutoArrivalController
import com.mapbox.navigation.core.directions.LegacyNavigationRouterAdapter
import com.mapbox.navigation.core.directions.LegacyRouterAdapter
import com.mapbox.navigation.core.directions.session.DirectionsSession
import com.mapbox.navigation.core.directions.session.RoutesExtra
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.directions.session.RoutesSetStartedParams
import com.mapbox.navigation.core.directions.session.SetNavigationRoutesStartedObserver
import com.mapbox.navigation.core.directions.session.Utils
import com.mapbox.navigation.core.history.MapboxHistoryReader
import com.mapbox.navigation.core.history.MapboxHistoryRecorder
import com.mapbox.navigation.core.internal.ReachabilityService
import com.mapbox.navigation.core.internal.telemetry.CustomEvent
import com.mapbox.navigation.core.internal.telemetry.UserFeedbackCallback
import com.mapbox.navigation.core.internal.utils.InternalUtils
import com.mapbox.navigation.core.internal.utils.ModuleParams
import com.mapbox.navigation.core.internal.utils.isInternalImplementation
import com.mapbox.navigation.core.internal.utils.mapToReason
import com.mapbox.navigation.core.internal.utils.paramsProvider
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.core.navigator.CacheHandleWrapper
import com.mapbox.navigation.core.navigator.TilesetDescriptorFactory
import com.mapbox.navigation.core.preview.RoutesPreview
import com.mapbox.navigation.core.preview.RoutesPreviewObserver
import com.mapbox.navigation.core.replay.MapboxReplayer
import com.mapbox.navigation.core.reroute.InternalRerouteController
import com.mapbox.navigation.core.reroute.InternalRerouteControllerAdapter
import com.mapbox.navigation.core.reroute.LegacyRerouteControllerAdapter
import com.mapbox.navigation.core.reroute.MapboxRerouteController
import com.mapbox.navigation.core.reroute.NavigationRerouteController
import com.mapbox.navigation.core.reroute.RerouteController
import com.mapbox.navigation.core.reroute.RerouteOptionsAdapter
import com.mapbox.navigation.core.reroute.RerouteResult
import com.mapbox.navigation.core.reroute.RerouteState
import com.mapbox.navigation.core.routealternatives.AlternativeRouteMetadata
import com.mapbox.navigation.core.routealternatives.NavigationRouteAlternativesObserver
import com.mapbox.navigation.core.routealternatives.NavigationRouteAlternativesRequestCallback
import com.mapbox.navigation.core.routealternatives.RouteAlternativesController
import com.mapbox.navigation.core.routealternatives.RouteAlternativesControllerProvider
import com.mapbox.navigation.core.routealternatives.RouteAlternativesError
import com.mapbox.navigation.core.routealternatives.RouteAlternativesObserver
import com.mapbox.navigation.core.routealternatives.RouteAlternativesRequestCallback
import com.mapbox.navigation.core.routeoptions.RouteOptionsUpdater
import com.mapbox.navigation.core.routerefresh.RouteRefreshController
import com.mapbox.navigation.core.routerefresh.RouteRefreshControllerProvider
import com.mapbox.navigation.core.telemetry.MapboxNavigationTelemetry
import com.mapbox.navigation.core.telemetry.events.FeedbackEvent
import com.mapbox.navigation.core.telemetry.events.FeedbackHelper
import com.mapbox.navigation.core.telemetry.events.FeedbackMetadata
import com.mapbox.navigation.core.telemetry.events.FeedbackMetadataWrapper
import com.mapbox.navigation.core.trip.service.TripService
import com.mapbox.navigation.core.trip.session.BannerInstructionsObserver
import com.mapbox.navigation.core.trip.session.LegIndexUpdatedCallback
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.core.trip.session.NativeSetRouteError
import com.mapbox.navigation.core.trip.session.NativeSetRouteResult
import com.mapbox.navigation.core.trip.session.NativeSetRouteValue
import com.mapbox.navigation.core.trip.session.NavigationSession
import com.mapbox.navigation.core.trip.session.NavigationSessionState
import com.mapbox.navigation.core.trip.session.NavigationSessionState.ActiveGuidance
import com.mapbox.navigation.core.trip.session.NavigationSessionState.FreeDrive
import com.mapbox.navigation.core.trip.session.NavigationSessionState.Idle
import com.mapbox.navigation.core.trip.session.NavigationSessionStateObserver
import com.mapbox.navigation.core.trip.session.OffRouteObserver
import com.mapbox.navigation.core.trip.session.RoadObjectsOnRouteObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.core.trip.session.TripSession
import com.mapbox.navigation.core.trip.session.TripSessionLocationEngine
import com.mapbox.navigation.core.trip.session.TripSessionState
import com.mapbox.navigation.core.trip.session.TripSessionStateObserver
import com.mapbox.navigation.core.trip.session.VoiceInstructionsObserver
import com.mapbox.navigation.core.trip.session.eh.EHorizonObserver
import com.mapbox.navigation.core.trip.session.eh.GraphAccessor
import com.mapbox.navigation.core.trip.session.eh.RoadObjectMatcher
import com.mapbox.navigation.core.trip.session.eh.RoadObjectsStore
import com.mapbox.navigation.metrics.MapboxMetricsReporter
import com.mapbox.navigation.metrics.internal.TelemetryUtilsDelegate
import com.mapbox.navigation.navigator.internal.MapboxNativeNavigator
import com.mapbox.navigation.navigator.internal.NavigatorLoader
import com.mapbox.navigation.navigator.internal.router.RouterInterfaceAdapter
import com.mapbox.navigation.utils.internal.ConnectivityHandler
import com.mapbox.navigation.utils.internal.ThreadController
import com.mapbox.navigation.utils.internal.Time
import com.mapbox.navigation.utils.internal.ifNonNull
import com.mapbox.navigation.utils.internal.logD
import com.mapbox.navigation.utils.internal.logE
import com.mapbox.navigation.utils.internal.logI
import com.mapbox.navigation.utils.internal.monitorChannelWithException
import com.mapbox.navigator.AlertsServiceOptions
import com.mapbox.navigator.ConfigHandle
import com.mapbox.navigator.ElectronicHorizonOptions
import com.mapbox.navigator.FallbackVersionsObserver
import com.mapbox.navigator.IncidentsOptions
import com.mapbox.navigator.NavigatorConfig
import com.mapbox.navigator.PollingConfig
import com.mapbox.navigator.RouterInterface
import com.mapbox.navigator.SetRoutesReason
import com.mapbox.navigator.TileEndpointConfiguration
import com.mapbox.navigator.TilesConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.lang.reflect.Field
import java.util.Locale

private const val MAPBOX_NAVIGATION_USER_AGENT_BASE = "mapbox-navigation-android"
private const val MAPBOX_NAVIGATION_TOKEN_EXCEPTION_ROUTER =
    "You need to provide an access token in NavigationOptions in order to use the default " +
        "Router."
private const val MAPBOX_NAVIGATION_NOTIFICATION_PACKAGE_NAME =
    "com.mapbox.navigation.trip.notification.internal.MapboxTripNotification"
private const val MAPBOX_NOTIFICATION_ACTION_CHANNEL = "notificationActionButtonChannel"

/**
 * ## Mapbox Navigation Core SDK
 * An entry point for interacting with the Mapbox Navigation SDK.
 *
 * **Only one instance of this class should be used per application process.**
 * Use [MapboxNavigationApp] to easily manage the instance across lifecycle.
 *
 * Feel free to visit our [docs pages and examples](https://docs.mapbox.com/android/beta/navigation/overview/) before diving in!
 *
 * The [MapboxNavigation] implementation can enter into the following [NavigationSessionState]s:
 * - [Idle]
 * - [FreeDrive]
 * - [ActiveGuidance]
 *
 * The SDK starts off in an [Idle] state.
 *
 * ### Location
 * Whenever the [startTripSession] is called, the SDK will enter the [FreeDrive] state starting to request and propagate location updates via the [LocationObserver].
 *
 * This observer provides 2 location update values in mixed intervals - either the raw one received from the provided [LocationEngine]
 * or the enhanced one map-matched internally using SDK's native capabilities.
 *
 * In [FreeDrive] mode, the enhanced location is computed using nearby to user location's routing tiles that are continuously updating in the background.
 * This can be configured using the [RoutingTilesOptions] in the [NavigationOptions].
 *
 * If the session is stopped, the SDK will stop listening for raw location updates and enter the [Idle] state.
 *
 * ### Routing
 * A route can be requested with:
 * - [requestRoutes], if successful, returns a route reference without acting on it. You can then pass the generated routes to [setRoutes].
 * - [setRoutes] sets new routes, clear current ones, or changes the route at primary index 0.
 * The routes are immediately available via the [RoutesObserver] and the first route (at index 0) is going to be chosen as the primary one.
 *
 * ### Route reason update
 * When routes are updated via [setRoutes] the reason that is spread via [RoutesObserver.onRoutesChanged] might be:
 * - [RoutesExtra.ROUTES_UPDATE_REASON_CLEAN_UP] if list of routes is empty;
 * - [RoutesExtra.ROUTES_UPDATE_REASON_ALTERNATIVE] if any of the routes received via [RouteAlternativesObserver.onRouteAlternatives] are set. **Order does not matter**.
 * - [RoutesExtra.ROUTES_UPDATE_REASON_NEW] otherwise.
 * If current routes are internally refreshed then the reason is [RoutesExtra.ROUTES_UPDATE_REASON_REFRESH]. In case of re-route (see [RerouteController]) the reason is [RoutesExtra.ROUTES_UPDATE_REASON_REROUTE].
 *
 * **Make sure to use the [applyDefaultNavigationOptions] for the best navigation experience** (and to set required request parameters).
 * You can also use [applyLanguageAndVoiceUnitOptions] get instructions' language and voice unit based on the device's [Locale].
 * It's also worth exploring other available options (like enabling alternative routes, specifying destination approach type, defining waypoint types, etc.).
 *
 * Example:
 * ```
 * val routeOptions = RouteOptions.builder()
 *   .applyDefaultNavigationOptions()
 *   .applyLanguageAndVoiceUnitOptions(context)
 *   .accessToken(token)
 *   .coordinatesList(listOf(origin, destination))
 *   .alternatives(true)
 *   .build()
 * mapboxNavigation.requestRoutes(
 *     routeOptions,
 *     object : NavigationRouterCallback {
 *         override fun onRoutesReady(
 *             routes: List<NavigationRoute>,
 *             routerOrigin: RouterOrigin
 *         ) {
 *             mapboxNavigation.setNavigationRoutes(routes)
 *         }
 *         override fun onFailure(reasons: List<RouterFailure>, routeOptions: RouteOptions) { }
 *         override fun onCanceled(routeOptions: RouteOptions, routerOrigin: RouterOrigin) { }
 *     }
 * )
 * ```
 *
 * If the SDK is in an [Idle] state, it stays in this same state even when a primary route is available.
 *
 * If the SDK is already in the [FreeDrive] mode or entering it whenever a primary route is available,
 * the SDK will enter the [ActiveGuidance] mode instead and propagate meaningful [RouteProgress].
 *
 * When the routes are manually cleared, the SDK automatically fall back to either [Idle] or [FreeDrive] state.
 *
 * You can use [setRoutes] to provide new routes, clear current ones, or change the route at primary index 0.
 *
 * @param navigationOptions a set of [NavigationOptions] used to customize various features of the SDK.
 */
@UiThread
class MapboxNavigation @VisibleForTesting internal constructor(
    val navigationOptions: NavigationOptions,
    private val threadController: ThreadController,
) {

    constructor(navigationOptions: NavigationOptions) : this(navigationOptions, ThreadController())

    private val accessToken: String? = navigationOptions.accessToken
    private val mainJobController = threadController.getMainScopeAndRootJob()
    private val directionsSession: DirectionsSession
    private var navigator: MapboxNativeNavigator
    private var historyRecorderHandles: NavigatorLoader.HistoryRecorderHandles
    private val tripService: TripService
    private val tripSession: TripSession
    private val navigationSession: NavigationSession
    internal val historyRecordingStateHandler: HistoryRecordingStateHandler
    private val developerMetadataAggregator: DeveloperMetadataAggregator
    private val tripSessionLocationEngine: TripSessionLocationEngine
    private val billingController: BillingController
    private val connectivityHandler: ConnectivityHandler = ConnectivityHandler(
        Channel(Channel.CONFLATED)
    )
    private val tripNotificationInterceptorOwner = TripNotificationInterceptorOwner()
    private val internalRoutesObserver: RoutesObserver
    private val routesCacheClearer = NavigationComponentProvider.createRoutesCacheClearer()
    private val internalOffRouteObserver: OffRouteObserver
    private val internalFallbackVersionsObserver: FallbackVersionsObserver
    private val routeAlternativesController: RouteAlternativesController
    private val arrivalProgressObserver: ArrivalProgressObserver
    private val electronicHorizonOptions: ElectronicHorizonOptions = ElectronicHorizonOptions(
        navigationOptions.eHorizonOptions.length,
        navigationOptions.eHorizonOptions.expansion.toByte(),
        navigationOptions.eHorizonOptions.branchLength,
        true, // doNotRecalculateInUncertainState is not exposed and can't be changed at the moment
        navigationOptions.eHorizonOptions.minTimeDeltaBetweenUpdates,
        AlertsServiceOptions(
            navigationOptions.eHorizonOptions.alertServiceOptions.collectTunnels,
            navigationOptions.eHorizonOptions.alertServiceOptions.collectBridges,
            navigationOptions.eHorizonOptions.alertServiceOptions.collectRestrictedAreas
        ),
    )

    private val routesPreviewController = NavigationComponentProvider.createRoutesPreviewController(
        threadController.getMainScopeAndRootJob().scope
    )

    private val routeUpdateMutex = Mutex()

    // native Router Interface
    private val nativeRouter: RouterInterface

    private val routesProgressDataProvider =
        NavigationComponentProvider.createRouteRefreshRequestDataProvider()

    private val evDynamicDataHolder = NavigationComponentProvider.createEVDynamicDataHolder()

    // Router provided via @Modules, might be outer
    private val moduleRouter: NavigationRouterV2

    private val incidentsOptions: IncidentsOptions? = navigationOptions.incidentsOptions.run {
        if (graph.isNotEmpty() || apiUrl.isNotEmpty()) {
            IncidentsOptions(graph, apiUrl)
        } else {
            null
        }
    }

    private val pollingConfig = PollingConfig(
        navigationOptions.navigatorPredictionMillis / ONE_SECOND_IN_MILLIS,
        InternalUtils.UNCONDITIONAL_POLLING_PATIENCE_MILLISECONDS / ONE_SECOND_IN_MILLIS,
        InternalUtils.UNCONDITIONAL_POLLING_INTERVAL_MILLISECONDS / ONE_SECOND_IN_MILLIS,
    )

    private val navigatorConfig = NavigatorConfig(
        null,
        electronicHorizonOptions,
        pollingConfig,
        incidentsOptions,
        null,
        null,
        navigationOptions.enableSensors,
    )

    private var notificationChannelField: Field? = null

    /**
     * Reroute controller, by default uses [defaultRerouteController].
     */
    private var rerouteController: InternalRerouteController?
    private val defaultRerouteController: InternalRerouteController

    /**
     * [NavigationVersionSwitchObserver] is notified when navigation switches tiles version.
     */
    private val navigationVersionSwitchObservers = mutableSetOf<NavigationVersionSwitchObserver>()

    /**
     * [MapboxNavigation.roadObjectsStore] provides methods to get road objects metadata,
     * add and remove custom road objects.
     */
    val roadObjectsStore: RoadObjectsStore

    /**
     * [MapboxNavigation.graphAccessor] provides methods to get edge (e.g. [EHorizonEdge]) shape and
     * metadata.
     *
     * **NOTE**: The Mapbox Electronic Horizon feature of the Mapbox Navigation SDK is in public beta
     * and is subject to changes, including its pricing. Use of the feature is subject to the beta
     * product restrictions in the Mapbox Terms of Service.
     * Mapbox reserves the right to eliminate any free tier or free evaluation offers at any time and
     * require customers to place an order to purchase the Mapbox Electronic Horizon feature,
     * regardless of the level of use of the feature.
     */
    val graphAccessor: GraphAccessor

    /**
     * [MapboxNavigation.tilesetDescriptorFactory] provide methods to build navigation
     * [TilesetDescriptor]
     */
    val tilesetDescriptorFactory: TilesetDescriptorFactory

    /**
     * [MapboxNavigation.roadObjectMatcher] provides methods to match custom road objects
     * to the road graph. To make the road object discoverable by the electronic horizon module and
     * the [EHorizonObserver] in particular it must be added to the [RoadObjectsStore] with
     * [RoadObjectsStore.addCustomRoadObject]
     */
    val roadObjectMatcher: RoadObjectMatcher

    /**
     * Use the history recorder to save history files.
     *
     * @see [HistoryRecorderOptions] to enable and customize the directory
     * @see [MapboxHistoryReader] to read the files
     */
    val historyRecorder = MapboxHistoryRecorder(navigationOptions)

    /**
     * Use route refresh controller to handle route refreshes.
     * @see [RouteRefreshController] for more details.
     */
    @ExperimentalPreviewMapboxNavigationAPI
    val routeRefreshController: RouteRefreshController

    internal val copilotHistoryRecorder = MapboxHistoryRecorder(navigationOptions)

    /**
     * **THIS IS AN EXPERIMENTAL API, DO NOT USE IN A PRODUCTION ENVIRONMENT.**
     *
     * Navigation native experimental API. Do not store a reference to [Experimental] object,
     * always access the object via `mapboxNavigation.experimental` because [MapboxNavigation] is
     * responsible for it's lifecycle.
     */
    @ExperimentalPreviewMapboxNavigationAPI
    val experimental: com.mapbox.navigator.Experimental
        get() = navigator.experimental

    private var reachabilityObserverId: Long? = null

    private var latestLegIndex: Int? = null

    /**
     * Describes whether this instance of `MapboxNavigation` has been destroyed by calling
     * [onDestroy]. Once an instance is destroyed, it cannot be used anymore.
     *
     * @see [MapboxNavigationApp]
     */
    @Volatile
    var isDestroyed = false
        private set

    init {
        if (hasInstance) {
            throw IllegalStateException(
                """
                    A different MapboxNavigation instance already exists.
                    Make sure to destroy it with #onDestroy before creating a new one.
                    Also see MapboxNavigationApp for instance management assistance.
                """.trimIndent()
            )
        }
        hasInstance = true

        val config = NavigatorLoader.createConfig(
            navigationOptions.deviceProfile,
            navigatorConfig,
        )

        val tilesConfig = createTilesConfig(
            isFallback = false,
            tilesVersion = navigationOptions.routingTilesOptions.tilesVersion
        )

        historyRecorderHandles = createHistoryRecorderHandles(config)

        val cacheHandle = NavigatorLoader.createCacheHandle(
            config,
            tilesConfig,
            historyRecorderHandles.composite
        )

        nativeRouter = NavigatorLoader.createNativeRouterInterface(
            cacheHandle,
            config,
            historyRecorderHandles.composite,
        )

        val result = MapboxModuleProvider.createModule<Router>(MapboxModuleType.NavigationRouter) {
            paramsProvider(
                ModuleParams.NavigationRouter(
                    accessToken
                        ?: throw RuntimeException(MAPBOX_NAVIGATION_TOKEN_EXCEPTION_ROUTER),
                    nativeRouter,
                    threadController
                )
            )
        }
        moduleRouter = when (result) {
            is NavigationRouterV2 -> result
            is NavigationRouter -> LegacyNavigationRouterAdapter(result)
            else -> LegacyNavigationRouterAdapter(LegacyRouterAdapter(result))
        }
        navigator = NavigationComponentProvider.createNativeNavigator(
            cacheHandle,
            config,
            historyRecorderHandles.composite,
            navigationOptions.accessToken ?: "",
            if (moduleRouter.isInternalImplementation()) {
                nativeRouter
            } else {
                RouterInterfaceAdapter(moduleRouter, ::getNavigationRoutes)
            },
        )
        assignHistoryRecorders()
        navigationSession = NavigationComponentProvider.createNavigationSession()
        historyRecordingStateHandler = NavigationComponentProvider
            .createHistoryRecordingStateHandler()
        developerMetadataAggregator = NavigationComponentProvider.createDeveloperMetadataAggregator(
            historyRecordingStateHandler
        )

        val notification: TripNotification = MapboxModuleProvider
            .createModule(
                MapboxModuleType.NavigationTripNotification
            ) {
                paramsProvider(
                    ModuleParams.NavigationTripNotification(
                        navigationOptions,
                        tripNotificationInterceptorOwner,
                        navigationOptions.distanceFormatterOptions
                    )
                )
            }
        if (notification.javaClass.name == MAPBOX_NAVIGATION_NOTIFICATION_PACKAGE_NAME) {
            notificationChannelField =
                notification.javaClass.getDeclaredField(MAPBOX_NOTIFICATION_ACTION_CHANNEL).apply {
                    isAccessible = true
                }
        }
        tripService = NavigationComponentProvider.createTripService(
            navigationOptions.applicationContext,
            notification,
            threadController,
        )
        tripSessionLocationEngine = NavigationComponentProvider.createTripSessionLocationEngine(
            navigationOptions = navigationOptions
        )
        tripSession = NavigationComponentProvider.createTripSession(
            tripService = tripService,
            tripSessionLocationEngine = tripSessionLocationEngine,
            navigator = navigator,
            threadController,
        )

        tripSession.registerRouteProgressObserver(routesProgressDataProvider)
        tripSession.registerStateObserver(navigationSession)
        tripSession.registerStateObserver(historyRecordingStateHandler)

        directionsSession = NavigationComponentProvider.createDirectionsSession(moduleRouter)
        directionsSession.registerSetNavigationRoutesFinishedObserver(navigationSession)
        if (reachabilityObserverId == null) {
            reachabilityObserverId = ReachabilityService.addReachabilityObserver(
                connectivityHandler
            )
        }

        arrivalProgressObserver = NavigationComponentProvider.createArrivalProgressObserver(
            tripSession
        )
        setArrivalController()

        billingController = NavigationComponentProvider.createBillingController(
            navigationOptions.accessToken,
            navigationSession,
            tripSession,
            arrivalProgressObserver
        )

        ifNonNull(accessToken) { token ->
            runInTelemetryContext { telemetry ->
                logD(
                    "MapboxMetricsReporter.init from MapboxNavigation main",
                    telemetry.LOG_CATEGORY
                )
                MapboxMetricsReporter.init(
                    navigationOptions.applicationContext,
                    token,
                    obtainUserAgent()
                )
                MapboxMetricsReporter.toggleLogging(navigationOptions.isDebugLoggingEnabled)
                telemetry.initialize(
                    this,
                    navigationOptions,
                    MapboxMetricsReporter,
                )
            }
        }

        val routeOptionsProvider = RouteOptionsUpdater()

        routeAlternativesController = RouteAlternativesControllerProvider.create(
            navigationOptions.routeAlternativesOptions,
            navigator,
            tripSession,
            threadController,
        )
        @OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
        routeRefreshController = RouteRefreshControllerProvider.createRouteRefreshController(
            Dispatchers.Main,
            Dispatchers.Main.immediate,
            navigationOptions.routeRefreshOptions,
            directionsSession,
            routesProgressDataProvider,
            evDynamicDataHolder,
            Time.SystemImpl
        )
        @OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
        routeRefreshController.registerRouteRefreshObserver {
            internalSetNavigationRoutes(
                it.allRoutesRefreshData.map { pair -> pair.first },
                SetRoutes.RefreshRoutes(it.primaryRouteProgressData)
            )
        }

        defaultRerouteController = NavigationComponentProvider.createRerouteController(
            directionsSession,
            tripSession,
            routeOptionsProvider,
            navigationOptions.rerouteOptions,
            threadController,
            evDynamicDataHolder
        )
        rerouteController = defaultRerouteController

        internalRoutesObserver = createInternalRoutesObserver()
        internalOffRouteObserver = createInternalOffRouteObserver()
        internalFallbackVersionsObserver = createInternalFallbackVersionsObserver()
        tripSession.registerOffRouteObserver(internalOffRouteObserver)
        tripSession.registerFallbackVersionsObserver(internalFallbackVersionsObserver)
        registerRoutesObserver(internalRoutesObserver)
        setUpRouteCacheClearer()

        roadObjectsStore = RoadObjectsStore(navigator)
        graphAccessor = GraphAccessor(navigator)
        tilesetDescriptorFactory = TilesetDescriptorFactory(
            navigationOptions.routingTilesOptions,
            navigator.cache
        )
        roadObjectMatcher = RoadObjectMatcher(navigator)
    }

    /**
     * Control the location events playback during a replay trip session.
     * Start a replay trip session with [startReplayTripSession].
     */
    @ExperimentalPreviewMapboxNavigationAPI
    val mapboxReplayer: MapboxReplayer by lazy { tripSessionLocationEngine.mapboxReplayer }

    /**
     * True when [startReplayTripSession] has been called.
     * Will be false after [stopTripSession] is called.
     */
    @ExperimentalPreviewMapboxNavigationAPI
    fun isReplayEnabled(): Boolean {
        return tripSessionLocationEngine.isReplayEnabled
    }

    /**
     * Starts listening for location updates and enters an `Active Guidance` state if there's a primary route available
     * or a `Free Drive` state otherwise.
     *
     * **Starting a session can have an impact on your usage costs.** Refer to the [pricing documentation](https://docs.mapbox.com/android/beta/navigation/guides/pricing/) to learn more.
     *
     * If you set [withForegroundService] to true and your apps targets Android 12 or higher,
     * you should only invoke this method when the app is in foreground.
     * This is dictated by background restrictions introduced in Android 12.
     * See https://developer.android.com/guide/components/foreground-services#background-start-restrictions
     * for more info.
     *
     * @param withForegroundService Boolean if set to false, foreground service will not be started and
     * no notifications will be rendered, and no location updates will be available while the app is in the background.
     * Default value is set to true.
     * @see [registerTripSessionStateObserver]
     * @see [registerRouteProgressObserver]
     */
    @RequiresPermission(anyOf = [ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION])
    @JvmOverloads
    fun startTripSession(withForegroundService: Boolean = true) {
        startSession(withForegroundService, false)
    }

    /**
     * Stops listening for location updates and enters an `Idle` state.
     *
     * @see [registerTripSessionStateObserver]
     */
    fun stopTripSession() {
        runIfNotDestroyed {
            latestLegIndex = tripSession.getRouteProgress()?.currentLegProgress?.legIndex
            tripSession.stop()
        }
    }

    /**
     * Functionally the same as [startTripSession] except the locations do not come from the
     * [NavigationOptions.locationEngine]. The events are emitted by the [mapboxReplayer].
     *
     * This allows you to simulate navigation routes or replay history from the [historyRecorder].
     */
    @ExperimentalPreviewMapboxNavigationAPI
    fun startReplayTripSession(withForegroundService: Boolean = true) {
        startSession(withForegroundService, true)
    }

    /**
     * Reset the session with the same configuration. The location becomes unknown,
     * but the [NavigationOptions] stay the same.
     *
     * Call this function before significant change of location, e.g. when restarting
     * navigation simulation, or before resetting location to not real (simulated)
     * position without recreation of [MapboxNavigation].
     */
    @Deprecated(message = "use the overload with the callback instead")
    fun resetTripSession() {
        // using a blocking function to keep parity with the original implementation so that
        // Nav Native is fully done with the reset when this function returns
        runBlocking {
            logD(LOG_CATEGORY) {
                "Resetting trip session"
            }
            navigator.resetRideSession()
            logI(LOG_CATEGORY) {
                "Trip session reset"
            }
        }
    }

    /**
     * Reset the session with the same configuration. The location becomes unknown,
     * but the [NavigationOptions] stay the same.
     *
     * Call this function before significant change of location, e.g. when restarting
     * navigation simulation, or before resetting location to not real (simulated)
     * position without recreation of [MapboxNavigation].
     */
    fun resetTripSession(callback: TripSessionResetCallback) {
        logD(LOG_CATEGORY) {
            "Resetting trip session"
        }
        mainJobController.scope.launch(Dispatchers.Main.immediate) {
            navigator.resetRideSession()
            logI(LOG_CATEGORY) {
                "Trip session reset"
            }
            callback.onTripSessionReset()
        }
    }

    /**
     * Query if the [MapboxNavigation] is running a foreground service
     * @return true if the [MapboxNavigation] is running a foreground service else false
     */
    fun isRunningForegroundService(): Boolean {
        return tripSession.isRunningWithForegroundService()
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
     * Provides the current navigation session state.
     *
     * @return current [NavigationSessionState]
     * @see NavigationSessionStateObserver
     * @see [registerNavigationSessionStateObserver]
     */
    fun getNavigationSessionState(): NavigationSessionState = navigationSession.state

    /**
     * Provides the current Z-Level. Can be used to build a route from a proper level of a road.
     *
     * @return current Z-Level.
     */
    fun getZLevel(): Int? = tripSession.zLevel

    /**
     * Requests a route using the available [Router] implementation.
     *
     * Use [MapboxNavigation.setRoutes] to supply the returned list of routes, transformed list, or a list from an external source, to be managed by the SDK.
     *
     * Example:
     * ```
     * mapboxNavigation.requestRoutes(routeOptions, object : RouterCallback {
     *     override fun onRoutesReady(routes: List<DirectionsRoute>) {
     *         ...
     *         mapboxNavigation.setRoutes(routes)
     *     }
     *     ...
     * })
     * ```
     *
     * @param routeOptions params for the route request.
     * **Make sure to use the [applyDefaultNavigationOptions] for the best navigation experience** (and to set required request parameters).
     * You can also use [applyLanguageAndVoiceUnitOptions] get instructions' language and voice unit based on the device's [Locale].
     * It's also worth exploring other available options (like enabling alternative routes, specifying destination approach type, defining waypoint types, etc.)
     * @param routesRequestCallback listener that gets notified when request state changes
     * @return requestId, see [cancelRouteRequest]
     * @see [registerRoutesObserver]
     * @see [registerRouteProgressObserver]
     * @see [applyDefaultNavigationOptions]
     * @see [applyLanguageAndVoiceUnitOptions]
     */
    @Deprecated("use #requestRoutes(RouteOptions, NavigationRouterCallback) instead")
    fun requestRoutes(
        routeOptions: RouteOptions,
        routesRequestCallback: RouterCallback
    ): Long {
        return requestRoutes(
            routeOptions,
            object : NavigationRouterCallback {
                override fun onRoutesReady(
                    routes: List<NavigationRoute>,
                    routerOrigin: RouterOrigin
                ) {
                    routesRequestCallback.onRoutesReady(
                        routes.toDirectionsRoutes(),
                        routerOrigin
                    )
                }

                override fun onFailure(reasons: List<RouterFailure>, routeOptions: RouteOptions) {
                    routesRequestCallback.onFailure(reasons, routeOptions)
                }

                override fun onCanceled(routeOptions: RouteOptions, routerOrigin: RouterOrigin) {
                    routesRequestCallback.onCanceled(routeOptions, routerOrigin)
                }
            }
        )
    }

    /**
     * Requests a route using the available [NavigationRouter] implementation.
     *
     * Use [MapboxNavigation.setNavigationRoutes] to supply the returned list of routes, transformed list, or a list from an external source, to be managed by the SDK.
     *
     * Example:
     * ```
     * mapboxNavigation.requestRoutes(routeOptions, object : NavigationRouterCallback {
     *     override fun onRoutesReady(routes: List<NavigationRoute>) {
     *         ...
     *         mapboxNavigation.setNavigationRoutes(routes)
     *     }
     *     ...
     * })
     * ```
     *
     * @param routeOptions params for the route request.
     * **Make sure to use the [applyDefaultNavigationOptions] for the best navigation experience** (and to set required request parameters).
     * You can also use [applyLanguageAndVoiceUnitOptions] get instructions' language and voice unit based on the device's [Locale].
     * It's also worth exploring other available options (like enabling alternative routes, specifying destination approach type, defining waypoint types, etc.)
     * @param callback listener that gets notified when request state changes
     * @return requestId, see [cancelRouteRequest]
     * @see [registerRoutesObserver]
     * @see [registerRouteProgressObserver]
     * @see [applyDefaultNavigationOptions]
     * @see [applyLanguageAndVoiceUnitOptions]
     */
    fun requestRoutes(
        routeOptions: RouteOptions,
        callback: NavigationRouterCallback
    ): Long {
        return directionsSession.requestRoutes(routeOptions, callback)
    }

    /**
     * Cancels a specific route request using the ID returned by [requestRoutes].
     */
    fun cancelRouteRequest(requestId: Long) {
        directionsSession.cancelRouteRequest(requestId)
    }

    /**
     * Set a list of routes.
     *
     * If the list is not empty, the route at index 0 is valid, and the trip session is started,
     * then the SDK enters an `Active Guidance` state and [RouteProgress] updates will be available.
     *
     * If the list is empty, the SDK will exit the `Active Guidance` state.
     *
     * Use [RoutesObserver] and [MapboxNavigation.registerRoutesObserver] to observe whenever the
     * routes list reference managed by the SDK changes, regardless of a source.
     *
     * This call is asynchronous, only once processing finishes the effects are available through [MapboxNavigation.getNavigationRoutes] and [RoutesObserver].
     * If this call fails, no changes are made to the routes managed by the [MapboxNavigation], they are not cleared. Observe the processing with [MapboxNavigation.setNavigationRoutes]'s callback.
     *
     * **Deprecated**: use #setNavigationRoutes(List<NavigationRoute>) instead. To fetch [NavigationRoute]
     * list use #requestRoutes(RouteOptions, NavigationRouterCallback) or create an instance of
     * [NavigationRoute] via [NavigationRoute.create], which requires an original [DirectionsResponse]
     * object or a raw JSON response.
     *
     * @param routes a list of [DirectionsRoute]s
     * @param initialLegIndex starting leg to follow. By default the first leg is used.
     * @see [requestRoutes]
     */
    @JvmOverloads
    @Deprecated(
        "use #setNavigationRoutes(List<NavigationRoute>) instead",
        ReplaceWith(
            "setNavigationRoutes(routes.toNavigationRoutes(), initialLegIndex)",
            "com.mapbox.navigation.base.route.toNavigationRoutes"
        )
    )
    fun setRoutes(routes: List<DirectionsRoute>, initialLegIndex: Int = 0) {
        setNavigationRoutes(routes.toNavigationRoutes(), initialLegIndex)
    }

    /**
     * Set a list of routes.
     *
     * If the list is not empty, the route at index 0 is valid, and the trip session is started,
     * then the SDK enters an `Active Guidance` state and [RouteProgress] updates will be available.
     *
     * If the list is empty, the SDK will exit the `Active Guidance` state.
     *
     * Use [RoutesObserver] and [MapboxNavigation.registerRoutesObserver] to observe whenever the routes list reference managed by the SDK changes, regardless of a source.
     *
     * This call is asynchronous, only once [callback] returns the effects are available through [MapboxNavigation.getNavigationRoutes] and [RoutesObserver].
     * If this call fails, no changes are made to the routes managed by the [MapboxNavigation], they are not cleared.
     *
     * @param routes a list of [NavigationRoute]s
     * @param initialLegIndex starting leg to follow. By default the first leg is used.
     * @param callback callback to be called when routes are set or ignored due to an error. See [RoutesSetCallback].
     * @see [requestRoutes]
     */
    @JvmOverloads
    fun setNavigationRoutes(
        routes: List<NavigationRoute>,
        initialLegIndex: Int = 0,
        callback: RoutesSetCallback? = null
    ) {
        if (routes.isNotEmpty()) {
            billingController.onExternalRouteSet(routes.first(), initialLegIndex)
        }

        // Telemetry uses this field to determine what type of event should be triggered.
        val setRoutesInfo = when {
            routes.isEmpty() -> SetRoutes.CleanUp
            routes.first() == directionsSession.routes.firstOrNull() ->
                SetRoutes.Alternatives(initialLegIndex)
            else -> SetRoutes.NewRoutes(initialLegIndex)
        }
        internalSetNavigationRoutes(
            routes,
            setRoutesInfo,
            callback,
        )
    }

    /***
     * Sets routes to preview.
     * Triggers an update in [RoutesPreviewObserver] and changes [MapboxNavigation.getRoutesPreview].
     * Preview state is updated asynchronously as it requires the SDK to process routes and compute alternative metadata. Subscribe for updates using [MapboxNavigation.registerRoutesPreviewObserver] to receive new routes preview state when the processing will be completed.
     *
     * If [routes] isn't empty, the route with [primaryRouteIndex] is considered as primary, the others as alternatives.
     * To cleanup routes preview state pass an empty list as [routes].
     *
     * Use [RoutesPreview.routesList] to start Active Guidance after route's preview:
     * ```
     * mapboxNavigation.getRoutesPreview()?.routesList?.let{ routesList ->
     *     mapboxNavigation.setNavigationRoutes(routesList)
     * }
     * ```
     * Routes preview state is controlled by the SDK's user. If you want to stop routes preview when you start active guidance, do it manually:
     * ```
     * mapboxNavigation.setRoutesPreview(emptyList())
     * ```
     *
     * @param routes to preview
     * @param primaryRouteIndex index of primary route from [routes]
     */
    @ExperimentalPreviewMapboxNavigationAPI
    @JvmOverloads
    fun setRoutesPreview(routes: List<NavigationRoute>, primaryRouteIndex: Int = 0) {
        routesPreviewController.previewNavigationRoutes(routes, primaryRouteIndex)
    }

    /***
     * Changes primary route for current preview state without changing order of [RoutesPreview.originalRoutesList].
     * Order is important for a case when routes are displayed as a list on UI, the list shouldn't change order when a user choose different primary route.
     *
     * In case [changeRoutesPreviewPrimaryRoute] is called while the the other set of routes are being processed, the current state with a new routes will be reapplied after the current processing.
     *
     * @param newPrimaryRoute is a new primary route
     * @throws [IllegalArgumentException] if [newPrimaryRoute] isn't found in the previewed routes list
     */
    @ExperimentalPreviewMapboxNavigationAPI
    @Throws(IllegalArgumentException::class)
    fun changeRoutesPreviewPrimaryRoute(newPrimaryRoute: NavigationRoute) {
        routesPreviewController.changeRoutesPreviewPrimaryRoute(newPrimaryRoute)
    }

    /**
     * Convenience method that takes previewed routes and sets them to Navigator,
     * clearing the previewed routes afterwards.
     * Equivalent to:
     * ```
     * mapboxNavigation.setNavigationRoutes(mapboxNavigation.getRoutesPreview()!!.routesList)
     * mapboxNavigation.setRoutesPreview(emptyList())
     * ```
     * @throws IllegalArgumentException when previewed routes are empty.
     */
    @Throws(IllegalArgumentException::class)
    @ExperimentalPreviewMapboxNavigationAPI
    fun moveRoutesFromPreviewToNavigator() {
        val preview = getRoutesPreview()
        requireNotNull(preview) {
            "Can't move routes from preview to navigator as no previewed routes are available. " +
                "Make sure you have set routes to preview and received previewed routes " +
                "in RoutesPreviewObserver before moving them to navigator."
        }
        setNavigationRoutes(preview.routesList)
        setRoutesPreview(emptyList())
    }

    /***
     * Registers [RoutesPreviewObserver] to be notified when routes preview state changes.
     * [observer] is immediately called with current preview state
     *
     * @param observer to be called on routes preview state changes
     */
    @ExperimentalPreviewMapboxNavigationAPI
    fun registerRoutesPreviewObserver(observer: RoutesPreviewObserver) {
        routesPreviewController.registerRoutesPreviewObserver(observer)
    }

    /***
     * Unregisters observer which were registered using [registerRoutesPreviewObserver]
     * @param observer which stops receiving updates when routes preview changes
     */
    @ExperimentalPreviewMapboxNavigationAPI
    fun unregisterRoutesPreviewObserver(observer: RoutesPreviewObserver) {
        routesPreviewController.unregisterRoutesPreviewObserver(observer)
    }

    /***
     * Returns current state of routes preview
     */
    @ExperimentalPreviewMapboxNavigationAPI
    fun getRoutesPreview(): RoutesPreview? = routesPreviewController.getRoutesPreview()

    /**
     * Requests road graph data update and invokes the callback on result.
     * Use this method if the frequency of application relaunch is too low
     * to always get the latest road graph data.
     * Recreate [MapboxNavigation] instance in the callback when updates are available:
     * ```
     *   mapboxNavigation.requestRoadGraphDataUpdate(object : RoadGraphDataUpdateCallback {
     *       override fun onRoadGraphDataUpdateInfoAvailable(
     *           isUpdateAvailable: Boolean,
     *           versionInfo: RoadGraphVersionInfo?
     *       ) {
     *           if (isUpdateAvailable) {
     *               val currentOptions = mapboxNavigation.navigationOptions
     *               MapboxNavigationApp.disable()
     *               MapboxNavigationApp.setup(currentOptions)
     *           }
     *       }
     *   })
     * ```
     *
     * @param callback callback to be invoked when the information about available
     *   updates is received. See [RoadGraphDataUpdateCallback].
     */
    fun requestRoadGraphDataUpdate(callback: RoadGraphDataUpdateCallback) {
        CacheHandleWrapper.requestRoadGraphDataUpdate(navigator.cache, callback)
    }

    /**
     * Returns leg index the user is currently on. If no RouteProgress updates are available,
     * returns the value passed as `initialLegIndex` parameter to `setNavigationRoutes`.
     * If no routes are set, returns 0.
     */
    fun currentLegIndex(): Int {
        return tripSession.getRouteProgress()?.currentLegProgress?.legIndex
            ?: directionsSession.initialLegIndex
    }

    private fun internalSetNavigationRoutes(
        routes: List<NavigationRoute>,
        setRoutesInfo: SetRoutes,
        callback: RoutesSetCallback? = null,
    ) {
        logD(LOG_CATEGORY) {
            "setting routes; reason: ${setRoutesInfo.mapToReason()}; IDs: ${routes.map { it.id }}"
        }
        directionsSession.setNavigationRoutesStarted(RoutesSetStartedParams(routes))
        when (setRoutesInfo) {
            SetRoutes.CleanUp,
            is SetRoutes.NewRoutes,
            is SetRoutes.Reroute -> {
                rerouteController?.interrupt()
            }
            is SetRoutes.RefreshRoutes,
            is SetRoutes.Alternatives -> {
                // do not interrupt reroute when primary route has not changed
            }
        }
        threadController.getMainScopeAndRootJob().scope.launch(Dispatchers.Main.immediate) {
            routeUpdateMutex.withLock {
                val routesSetResult: Expected<RoutesSetError, RoutesSetSuccess>
                if (
                    setRoutesInfo is SetRoutes.Alternatives &&
                    routes.first().id != directionsSession.routes.firstOrNull()?.id
                ) {
                    routesSetResult = ExpectedFactory.createError(
                        RoutesSetError(
                            "Alternatives ${routes.drop(1).map { it.id }} " +
                                "are outdated. Primary route has changed " +
                                "from ${routes.first().id} " +
                                "to ${directionsSession.routes.firstOrNull()?.id}"
                        )
                    )
                } else {
                    historyRecordingStateHandler.setRoutes(routes)
                    when (val processedRoutes = setRoutesToTripSession(routes, setRoutesInfo)) {
                        is NativeSetRouteValue -> {
                            val directionsSessionRoutes = Utils.createDirectionsSessionRoutes(
                                routes,
                                processedRoutes,
                                setRoutesInfo
                            )
                            directionsSession.setNavigationRoutesFinished(directionsSessionRoutes)
                            routesSetResult = ExpectedFactory.createValue(
                                RoutesSetSuccess(
                                    directionsSessionRoutes.ignoredRoutes.associate {
                                        it.navigationRoute.id to
                                            RoutesSetError("invalid alternative")
                                    }
                                )
                            )
                        }
                        is NativeSetRouteError -> {
                            logE(
                                "Routes with IDs ${routes.map { it.id }} " +
                                    "will be ignored as they are not valid"
                            )
                            routesSetResult = ExpectedFactory.createError(
                                RoutesSetError(processedRoutes.error)
                            )
                            historyRecordingStateHandler.lastSetRoutesFailed()
                        }
                    }
                }
                callback?.onRoutesSet(routesSetResult)
            }
        }
    }

    private fun resetTripSessionRoutes() {
        threadController.getMainScopeAndRootJob().scope.launch(Dispatchers.Main.immediate) {
            routeUpdateMutex.withLock {
                val routes = directionsSession.routes
                val legIndex = latestLegIndex ?: directionsSession.initialLegIndex
                setRoutesToTripSession(routes, SetRoutes.NewRoutes(legIndex))
            }
        }
    }

    /**
     * Call to set routes to trip session.
     * This call should be synchronized and only one should be executing at a time.
     */
    private suspend fun setRoutesToTripSession(
        routes: List<NavigationRoute>,
        setRoutesInfo: SetRoutes,
    ): NativeSetRouteResult {
        return tripSession.setRoutes(routes, setRoutesInfo).apply {
            if (this is NativeSetRouteValue) {
                routeAlternativesController.processAlternativesMetadata(
                    this.routes,
                    nativeAlternatives
                )
            }
        }
    }

    /**
     * Get a list of routes.
     *
     * If the list is not empty, the route at index 0 is the one treated as the primary route
     * and used for route progress, off route events and map-matching calculations.
     *
     * @return a list of [DirectionsRoute]s
     */
    @Deprecated(
        "use #getNavigationRoutes() instead",
        ReplaceWith(
            "getNavigationRoutes().toDirectionsRoutes()",
            "com.mapbox.navigation.base.route.toDirectionsRoutes"
        )
    )
    fun getRoutes(): List<DirectionsRoute> = directionsSession.routes.toDirectionsRoutes()

    /**
     * Get a list of routes.
     *
     * If the list is not empty, the route at index 0 is the one treated as the primary route
     * and used for route progress, off route events and map-matching calculations.
     *
     * @return a list of [NavigationRoute]s
     */
    fun getNavigationRoutes(): List<NavigationRoute> = directionsSession.routes

    /**
     * Requests an alternative route using the original [RouteOptions] associated with
     * [MapboxNavigation.setRoutes()] call and [Router] implementation.
     * @see [registerRouteAlternativesObserver]
     */
    fun requestAlternativeRoutes() {
        routeAlternativesController.triggerAlternativeRequest(null)
    }

    /**
     * Requests an alternative route using the original [RouteOptions] associated with
     * [MapboxNavigation.setRoutes()] call and [Router] implementation.
     * @see [registerRouteAlternativesObserver]
     */
    fun requestAlternativeRoutes(callback: NavigationRouteAlternativesRequestCallback? = null) {
        routeAlternativesController.triggerAlternativeRequest(callback)
    }

    /**
     * Requests an alternative route using the original [RouteOptions] associated with
     * [MapboxNavigation.setRoutes()] call and [Router] implementation.
     * @see [registerRouteAlternativesObserver]
     */
    fun requestAlternativeRoutes(callback: RouteAlternativesRequestCallback) {
        routeAlternativesController.triggerAlternativeRequest(
            object : NavigationRouteAlternativesRequestCallback {
                override fun onRouteAlternativeRequestFinished(
                    routeProgress: RouteProgress,
                    alternatives: List<NavigationRoute>,
                    routerOrigin: RouterOrigin
                ) {
                    callback.onRouteAlternativeRequestFinished(
                        routeProgress,
                        alternatives.toDirectionsRoutes(),
                        routerOrigin
                    )
                }

                override fun onRouteAlternativesRequestError(error: RouteAlternativesError) {
                    callback.onRouteAlternativesAborted(error.message)
                }
            }
        )
    }

    /**
     * Call this method whenever this instance of the [MapboxNavigation] is not going to be used anymore and should release all of its resources.
     */
    fun onDestroy() {
        if (isDestroyed) return
        logD("onDestroy", LOG_CATEGORY)
        billingController.onDestroy()
        directionsSession.shutdown()
        directionsSession.unregisterAllSetNavigationRoutesFinishedObserver()
        tripSession.stop()
        tripSession.unregisterAllLocationObservers()
        tripSession.unregisterAllRouteProgressObservers()
        tripSession.unregisterAllOffRouteObservers()
        tripSession.unregisterAllStateObservers()
        tripSession.unregisterAllBannerInstructionsObservers()
        tripSession.unregisterAllVoiceInstructionsObservers()
        tripSession.unregisterAllRoadObjectsOnRouteObservers()
        tripSession.unregisterAllEHorizonObservers()
        tripSession.unregisterAllFallbackVersionsObservers()
        routeAlternativesController.unregisterAll()
        internalSetNavigationRoutes(emptyList(), SetRoutes.CleanUp)
        resetTripSession()
        navigator.unregisterAllObservers()
        navigationVersionSwitchObservers.clear()
        arrivalProgressObserver.unregisterAllObservers()

        navigationSession.unregisterAllNavigationSessionStateObservers()
        historyRecordingStateHandler.unregisterAllStateChangeObservers()
        historyRecordingStateHandler.unregisterAllCopilotSessionObservers()
        developerMetadataAggregator.unregisterAllObservers()
        @OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
        routeRefreshController.destroy()
        routesPreviewController.unregisterAllRoutesPreviewObservers()
        runInTelemetryContext { telemetry ->
            telemetry.destroy(this@MapboxNavigation)
        }
        threadController.cancelAllNonUICoroutines()
        threadController.cancelAllUICoroutines()
        ifNonNull(reachabilityObserverId) {
            ReachabilityService.removeReachabilityObserver(it)
            reachabilityObserverId = null
        }

        isDestroyed = true
        hasInstance = false
    }

    /**
     * Registers [LocationObserver]. The updates are available whenever the trip session is started.
     *
     * @see [LocationMatcherResult]
     * @see [startTripSession]
     */
    fun registerLocationObserver(locationObserver: LocationObserver) {
        tripSession.registerLocationObserver(locationObserver)
    }

    /**
     * Unregisters [LocationObserver].
     *
     * @see [LocationMatcherResult]
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
     *
     * If the observer is registered when new routes are being set with [MapboxNavigation.setNavigationRoutes],
     * the observer waits for the processing to finish before delivering the latest result.
     */
    fun registerRoutesObserver(routesObserver: RoutesObserver) {
        threadController.getMainScopeAndRootJob().scope.launch(Dispatchers.Main.immediate) {
            routeUpdateMutex.withLock {
                directionsSession.registerSetNavigationRoutesFinishedObserver(routesObserver)
            }
        }
    }

    /**
     * Unregisters [RoutesObserver].
     */
    fun unregisterRoutesObserver(routesObserver: RoutesObserver) {
        directionsSession.unregisterSetNavigationRoutesFinishedObserver(routesObserver)
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
     * Allows you to intercept the notification builder before it is passed to the android sdk. Use
     * this interceptor to extend the notification, or apply your own modifications. Use `null`
     * to clear the interceptor you have set previously.
     *
     * @param interceptor [TripNotificationInterceptor]
     */
    fun setTripNotificationInterceptor(interceptor: TripNotificationInterceptor?) {
        tripNotificationInterceptorOwner.interceptor = interceptor
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
     * By default [MapboxRerouteController] is used.
     * Pass `null` to disable automatic reroute.
     * A user will stay in `OFF_ROUTE` state until a new route is set or the user gets back to the route.
     */
    @Deprecated(
        "Custom rerouting logic is deprecated. Use setRerouteEnabled to enable/disable reroutes"
    )
    fun setRerouteController(rerouteController: RerouteController?) {
        setRerouteController(
            rerouteController?.let { LegacyRerouteControllerAdapter(it) }
        )
    }

    /**
     * Set [NavigationRerouteController] that's automatically invoked when user is off-route.
     *
     * By default [MapboxRerouteController] is used.
     * Pass `null` to disable automatic reroute.
     * A user will stay in `OFF_ROUTE` state until a new route is set or the user gets back to the route.
     */
    @JvmOverloads
    @Deprecated(
        "Custom rerouting logic is deprecated. Use setRerouteEnabled to enable/disable reroutes."
    )
    fun setRerouteController(
        rerouteController: NavigationRerouteController? = defaultRerouteController
    ) {
        val oldController = this.rerouteController
        this.rerouteController = if (rerouteController is InternalRerouteController) {
            rerouteController
        } else {
            rerouteController?.let { InternalRerouteControllerAdapter(it) }
        }
        if (oldController?.state == RerouteState.FetchingRoute) {
            oldController.interrupt()
            reroute()
        }
    }

    /**
     * Set [RerouteOptionsAdapter]. It allows modify [RouteOptions] of default implementation of
     * [NavigationRerouteController] ([RerouteController]).
     */
    fun setRerouteOptionsAdapter(
        rerouteOptionsAdapter: RerouteOptionsAdapter?
    ) {
        (rerouteController as? MapboxRerouteController)
            ?.setRerouteOptionsAdapter(rerouteOptionsAdapter)
    }

    /**
     * Get currently set [RerouteController].
     *
     * @see setRerouteController
     */
    fun getRerouteController(): NavigationRerouteController? = rerouteController

    /**
     * Enables/disables rerouting.
     *
     * @param enabled true if rerouting should be enabled, false otherwise
     */
    fun setRerouteEnabled(enabled: Boolean) {
        if (enabled) {
            if (rerouteController == null) {
                setRerouteController(defaultRerouteController)
            }
        } else {
            setRerouteController(null)
        }
    }

    /**
     * Whether rerouting is enabled. True by default.
     * It can be disabled by invoking [setRerouteEnabled] with false as a parameter,
     * or via a deprecated [setRerouteController] with null controller.
     */
    fun isRerouteEnabled(): Boolean = rerouteController != null

    /**
     * Registers [ArrivalObserver]. Monitor arrival at stops and destinations. For more control
     * of arrival at stops, see [setArrivalController]. Use [RouteProgressObserver] to create
     * custom experiences for arrival based on time or distance.
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
     * @param callback [LegIndexUpdatedCallback] callback to handle leg index updates.
     */
    fun navigateNextRouteLeg(callback: LegIndexUpdatedCallback) {
        arrivalProgressObserver.navigateNextRouteLeg(callback)
    }

    /**
     * Registers an observer that gets notified whenever the route changes and provides the list
     * of road objects on this new route, if there are any. The objects returned here are equal to
     * the ones available in [RouteProgress.upcomingRoadObjects], but they capture the whole route
     * (not only what's ahead of us).
     *
     * @see unregisterRoadObjectsOnRouteObserver
     */
    fun registerRoadObjectsOnRouteObserver(
        roadObjectsOnRouteObserver: RoadObjectsOnRouteObserver
    ) {
        tripSession.registerRoadObjectsOnRouteObserver(roadObjectsOnRouteObserver)
    }

    /**
     * Unregisters the route objects observer.
     *
     * @see registerRoadObjectsOnRouteObserver
     */
    fun unregisterRoadObjectsOnRouteObserver(
        roadObjectsOnRouteObserver: RoadObjectsOnRouteObserver
    ) {
        tripSession.unregisterRoadObjectsOnRouteObserver(roadObjectsOnRouteObserver)
    }

    /**
     * To start listening EHorizon updates [EHorizonObserver] should be registered.
     * Observer will be called when the EHorizon changes.
     * To save resources and be more efficient callbacks return minimum data.
     * To get [EHorizonEdge] shape or [EHorizonEdgeMetadata] use [MapboxNavigation.graphAccessor]
     * To get more data about EHorizon road object use [MapboxNavigation.roadObjectsStore]
     *
     * Registering an EHorizonObserver activates the Electronic Horizon module.
     *
     * **NOTE**: The Mapbox Electronic Horizon feature of the Mapbox Navigation SDK is in public beta
     * and is subject to changes, including its pricing. Use of the feature is subject to the beta
     * product restrictions in the Mapbox Terms of Service.
     * Mapbox reserves the right to eliminate any free tier or free evaluation offers at any time and
     * require customers to place an order to purchase the Mapbox Electronic Horizon feature,
     * regardless of the level of use of the feature.
     *
     * @see unregisterEHorizonObserver
     */
    @ExperimentalPreviewMapboxNavigationAPI
    fun registerEHorizonObserver(eHorizonObserver: EHorizonObserver) {
        tripSession.registerEHorizonObserver(eHorizonObserver)
    }

    /**
     * Unregisters a EHorizon observer.
     *
     * Unregistering all observers deactivates the module.
     *
     * **NOTE**: The Mapbox Electronic Horizon feature of the Mapbox Navigation SDK is in public beta
     * and is subject to changes, including its pricing. Use of the feature is subject to the beta
     * product restrictions in the Mapbox Terms of Service.
     * Mapbox reserves the right to eliminate any free tier or free evaluation offers at any time and
     * require customers to place an order to purchase the Mapbox Electronic Horizon feature,
     * regardless of the level of use of the feature.
     *
     * @see registerEHorizonObserver
     */
    @ExperimentalPreviewMapboxNavigationAPI
    fun unregisterEHorizonObserver(eHorizonObserver: EHorizonObserver) {
        tripSession.unregisterEHorizonObserver(eHorizonObserver)
    }

    /**
     * Send user feedback about an issue or problem with the Navigation SDK.
     *
     * @param feedbackType one of [FeedbackEvent.Type] or a custom one
     * @param description description message
     * @param feedbackSource one of [FeedbackEvent.Source]
     * @param screenshot encoded screenshot
     * @param feedbackSubType optional array of [FeedbackEvent.SubType] and/or custom subtypes
     *
     * @see [FeedbackHelper.getFeedbackSubTypes]
     * to retrieve possible feedback subtypes for a given [feedbackType]
     * @see [ViewUtils.capture] to capture screenshots
     * @see [FeedbackHelper.encodeScreenshot] to encode screenshots
     */
    @OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
    @JvmOverloads
    fun postUserFeedback(
        feedbackType: String,
        description: String,
        @FeedbackEvent.Source feedbackSource: String,
        screenshot: String,
        feedbackSubType: Array<String>? = emptyArray(),
    ) {
        postUserFeedback(
            feedbackType,
            description,
            feedbackSource,
            screenshot,
            feedbackSubType,
            feedbackMetadata = null,
            userFeedbackCallback = null,
        )
    }

    /**
     * Send user feedback about an issue or problem with the Navigation SDK.
     *
     * Method can be invoked out of the trip session (whenever until [onDestroy] is called), because
     * a feedback is attached to passed location and time in the past when [FeedbackMetadata] was
     * generated (see [provideFeedbackMetadataWrapper]).
     *
     * @param feedbackType one of [FeedbackEvent.Type] or a custom one
     * @param description description message
     * @param feedbackSource one of [FeedbackEvent.Source]
     * @param screenshot encoded screenshot
     * @param feedbackSubType optional array of [FeedbackEvent.SubType] and/or custom subtypes
     * @param feedbackMetadata use it to attach feedback to a specific passed location.
     * See [FeedbackMetadata] and [FeedbackMetadataWrapper]
     *
     * @see [FeedbackHelper.getFeedbackSubTypes]
     * to retrieve possible feedback subtypes for a given [feedbackType]
     * @see [ViewUtils.capture] to capture screenshots
     * @see [FeedbackHelper.encodeScreenshot] to encode screenshots
     */
    @ExperimentalPreviewMapboxNavigationAPI
    @JvmOverloads
    fun postUserFeedback(
        feedbackType: String,
        description: String,
        @FeedbackEvent.Source feedbackSource: String,
        screenshot: String,
        feedbackSubType: Array<String>? = emptyArray(),
        feedbackMetadata: FeedbackMetadata,
    ) {
        postUserFeedback(
            feedbackType,
            description,
            feedbackSource,
            screenshot,
            feedbackSubType,
            feedbackMetadata,
            userFeedbackCallback = null,
        )
    }

    @ExperimentalPreviewMapboxNavigationAPI
    internal fun postUserFeedback(
        feedbackType: String,
        description: String,
        @FeedbackEvent.Source feedbackSource: String,
        screenshot: String,
        feedbackSubType: Array<String>?,
        feedbackMetadata: FeedbackMetadata?,
        userFeedbackCallback: UserFeedbackCallback?,
    ) {
        runInTelemetryContext { telemetry ->
            telemetry.postUserFeedback(
                feedbackType,
                description,
                feedbackSource,
                screenshot,
                feedbackSubType,
                feedbackMetadata,
                userFeedbackCallback,
            )
        }
    }

    @ExperimentalPreviewMapboxNavigationAPI
    internal fun postCustomEvent(
        payload: String,
        @CustomEvent.Type customEventType: String,
        customEventVersion: String,
    ) {
        runInTelemetryContext { telemetry ->
            telemetry.postCustomEvent(
                payload = payload,
                customEventType = customEventType,
                customEventVersion = customEventVersion
            )
        }
    }

    /**
     * Provides wrapper of [FeedbackMetadata]. [FeedbackMetadata] is used to send deferred
     * feedback attached to passed location. It contains data (like location, time) when method is
     * invoked.
     *
     * Note: method throws [IllegalStateException] if trips session is not
     * started ([startTripSession]) or telemetry is disabled.
     */
    @ExperimentalPreviewMapboxNavigationAPI
    fun provideFeedbackMetadataWrapper(): FeedbackMetadataWrapper =
        runInTelemetryContext { telemetry ->
            telemetry.provideFeedbackMetadataWrapper()
        } ?: throw java.lang.IllegalStateException(
            "To get FeedbackMetadataWrapper Telemetry must be enabled"
        )

    /**
     * Start observing alternatives routes for a trip session via [RouteAlternativesObserver].
     * Route alternatives are requested periodically based on [RouteAlternativesOptions].
     *
     * @param routeAlternativesObserver RouteAlternativesObserver
     */
    fun registerRouteAlternativesObserver(routeAlternativesObserver: RouteAlternativesObserver) {
        routeAlternativesController.register(routeAlternativesObserver)
    }

    /**
     * Stop observing the possibility of route alternatives.
     *
     * @param routeAlternativesObserver RouteAlternativesObserver
     */
    fun unregisterRouteAlternativesObserver(routeAlternativesObserver: RouteAlternativesObserver) {
        routeAlternativesController.unregister(routeAlternativesObserver)
    }

    /**
     * Start observing alternatives routes for a trip session via [NavigationRouteAlternativesObserver].
     * Route alternatives are requested periodically based on [RouteAlternativesOptions].
     */
    fun registerRouteAlternativesObserver(
        routeAlternativesObserver: NavigationRouteAlternativesObserver
    ) {
        routeAlternativesController.register(routeAlternativesObserver)
    }

    /**
     * Stop observing the possibility of route alternatives.
     */
    fun unregisterRouteAlternativesObserver(
        routeAlternativesObserver: NavigationRouteAlternativesObserver
    ) {
        routeAlternativesController.unregister(routeAlternativesObserver)
    }

    /**
     * If the provided [navigationRoute] is an alternative route in the current session,
     * this function will return the associated route metadata.
     *
     * This function is guaranteed to return a valid result (if available) only after the [navigationRoute]
     * has been processed by [MapboxNavigation] and returned via [RoutesObserver] or [NavigationRouteAlternativesObserver].
     * To process the routes, call [MapboxNavigation.setNavigationRoutes].
     *
     * Whenever [RoutesObserver] or [NavigationRouteAlternativesObserver] fires, the previously obtained metadata becomes invalid.
     *
     * This function returns `null` for primary route in the current session.
     */
    fun getAlternativeMetadataFor(navigationRoute: NavigationRoute): AlternativeRouteMetadata? {
        return routeAlternativesController.getMetadataFor(navigationRoute)
    }

    /**
     * If the provided [navigationRoutes] are alternative routes in the current session,
     * this function will return the associated with those route metadata.
     *
     * This function is guaranteed to return a valid result (if available) only after the [navigationRoutes]
     * have been processed by [MapboxNavigation] and returned via [RoutesObserver] or [NavigationRouteAlternativesObserver].
     * To process the routes, call [MapboxNavigation.setNavigationRoutes].
     *
     * Whenever [RoutesObserver] or [NavigationRouteAlternativesObserver] fires, the previously obtained metadata becomes invalid.
     *
     * This function doesn't return anything for primary route in the current session.
     */
    fun getAlternativeMetadataFor(
        navigationRoutes: List<NavigationRoute>
    ): List<AlternativeRouteMetadata> {
        return navigationRoutes.mapNotNull { getAlternativeMetadataFor(it) }
    }

    /**
     * Start observing navigation tiles version switch via [NavigationVersionSwitchObserver].
     * Navigation might switch to a fallback tiles version when target tiles are not available
     * and return back to the target version when tiles are loaded.
     *
     * @param observer NavigationVersionSwitchObserver
     *
     * @see [NavigationVersionSwitchObserver]
     */
    fun registerNavigationVersionSwitchObserver(observer: NavigationVersionSwitchObserver) {
        navigationVersionSwitchObservers.add(observer)
    }

    /**
     * Stop observing tiles version switch via [NavigationVersionSwitchObserver].
     * Navigation might switch to a fallback tiles version when target tiles are not available
     * and return back to the target version when tiles are loaded.
     *
     * @param observer NavigationVersionSwitchObserver
     *
     * @see [NavigationVersionSwitchObserver]
     */
    fun unregisterNavigationVersionSwitchObserver(observer: NavigationVersionSwitchObserver) {
        navigationVersionSwitchObservers.remove(observer)
    }

    /**
     * Register a [NavigationSessionStateObserver] to be notified of the various Session states.
     *
     * @param navigationSessionStateObserver NavigationSessionStateObserver
     */
    fun registerNavigationSessionStateObserver(
        navigationSessionStateObserver: NavigationSessionStateObserver
    ) {
        navigationSession.registerNavigationSessionStateObserver(navigationSessionStateObserver)
    }

    /**
     * Unregisters a [NavigationSessionStateObserver].
     *
     * @param navigationSessionStateObserver NavigationSessionStateObserver
     */
    fun unregisterNavigationSessionStateObserver(
        navigationSessionStateObserver: NavigationSessionStateObserver
    ) {
        navigationSession.unregisterNavigationSessionStateObserver(navigationSessionStateObserver)
    }

    /**
     * Registers a [DeveloperMetadataObserver] to be notified of [DeveloperMetadata] changes.
     *
     * @param developerMetadataObserver [DeveloperMetadataObserver]
     */
    @ExperimentalPreviewMapboxNavigationAPI
    fun registerDeveloperMetadataObserver(
        developerMetadataObserver: DeveloperMetadataObserver
    ) {
        developerMetadataAggregator.registerObserver(developerMetadataObserver)
    }

    /**
     * Unregisters a [DeveloperMetadataObserver].
     *
     * @param developerMetadataObserver [DeveloperMetadataObserver]
     */
    @ExperimentalPreviewMapboxNavigationAPI
    fun unregisterDeveloperMetadataObserver(
        developerMetadataObserver: DeveloperMetadataObserver
    ) {
        developerMetadataAggregator.unregisterObserver(developerMetadataObserver)
    }

    /**
     * Invoke when any component of EV data is changed so that it can be used in refresh requests.
     * You can pass only changed components of EV data via [data], all the previous values
     * that have not changed will be cached on the SDK side.
     * **NOTE: Only provide parameters that are compatible with an EV route refresh. If you pass any other parameters via this method, the route refresh request will fail.**
     *
     * Example:
     * ```
     *     mapOf(
     *         "ev_initial_charge" to "90",
     *         "energy_consumption_curve" to "0,300;20,120;40,150",
     *         "auxiliary_consumption" to "300"
     *     )
     * ```
     * If you previously invoked this function, and then the charge changes to 80,
     * you can also invoke it again with only one parameter:
     * ```
     *     mapOf("ev_initial_charge" to "80")
     * ```
     * as an argument. This way "ev_initial_charge" will be updated and the following parameters
     * will be used from the previous invocation.
     * It would be equivalent to passing the following map:
     * ```
     *     mapOf(
     *         "ev_initial_charge" to "80",
     *         "energy_consumption_curve" to "0,300;20,120;40,150",
     *         "auxiliary_consumption" to "300"
     *     )
     * ```
     *
     * @param data Map describing the changed EV data
     */
    @ExperimentalPreviewMapboxNavigationAPI
    fun onEVDataUpdated(data: Map<String, String>) {
        evDynamicDataHolder.updateData(data)
    }

    internal fun registerOnRoutesSetStartedObserver(observer: SetNavigationRoutesStartedObserver) {
        directionsSession.registerSetNavigationRoutesStartedObserver(observer)
    }

    internal fun unregisterOnRoutesSetStartedObserver(
        observer: SetNavigationRoutesStartedObserver
    ) {
        directionsSession.unregisterSetNavigationRoutesStartedObserver(observer)
    }

    private fun createHistoryRecorderHandles(config: ConfigHandle) =
        NavigatorLoader.createHistoryRecorderHandles(
            config,
            historyRecorder.fileDirectory(),
            copilotHistoryRecorder.copilotFileDirectory(),
        )

    private fun assignHistoryRecorders() {
        historyRecorder.historyRecorderHandle = historyRecorderHandles.general
        copilotHistoryRecorder.historyRecorderHandle = historyRecorderHandles.copilot
    }

    private fun startSession(withTripService: Boolean, withReplayEnabled: Boolean) {
        runIfNotDestroyed {
            tripSession.start(
                withTripService = withTripService,
                withReplayEnabled = withReplayEnabled
            )
            resetTripSessionRoutes()
            notificationChannelField?.let {
                monitorNotificationActionButton(it.get(null) as ReceiveChannel<NotificationAction>)
            }
        }
    }

    private fun createInternalRoutesObserver() = RoutesObserver { result ->
        latestLegIndex = null
        routesProgressDataProvider.onNewRoutes()
        @OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
        routeRefreshController.requestPlannedRouteRefresh(result.navigationRoutes)
    }

    private fun createInternalOffRouteObserver() = OffRouteObserver { offRoute ->
        if (offRoute) {
            reroute()
        } else {
            rerouteController?.interrupt()
        }
    }

    private fun createInternalFallbackVersionsObserver() = object : FallbackVersionsObserver {
        override fun onFallbackVersionsFound(versions: List<String>) {
            if (versions.isNotEmpty()) {
                // the last version in the list is the latest one
                val tilesVersion = versions.last()
                recreateNavigatorInstance(isFallback = true, tilesVersion = tilesVersion)
                navigationVersionSwitchObservers.forEach {
                    it.onSwitchToFallbackVersion(tilesVersion)
                }
            } else {
                logD(
                    "FallbackVersionsObserver.onFallbackVersionsFound called with an empty " +
                        "versions list, navigator can't be recreated.",
                    LOG_CATEGORY
                )
            }
        }

        override fun onCanReturnToLatest(version: String) {
            recreateNavigatorInstance(
                isFallback = false,
                tilesVersion = navigationOptions.routingTilesOptions.tilesVersion
            )
            navigationVersionSwitchObservers.forEach { observer ->
                observer.onSwitchToTargetVersion(
                    navigationOptions.routingTilesOptions.tilesVersion.takeIf { it.isNotEmpty() }
                )
            }
        }
    }

    private fun recreateNavigatorInstance(isFallback: Boolean, tilesVersion: String) {
        logD(
            "recreateNavigatorInstance(). " +
                "isFallback = $isFallback, tilesVersion = $tilesVersion",
            LOG_CATEGORY
        )

        val config = NavigatorLoader.createConfig(
            navigationOptions.deviceProfile,
            navigatorConfig,
        )
        historyRecorderHandles = createHistoryRecorderHandles(config)

        mainJobController.scope.launch {
            val cacheHandle = NavigatorLoader.createCacheHandle(
                config,
                createTilesConfig(isFallback, tilesVersion),
                historyRecorderHandles.composite
            )

            navigator.recreate(
                cacheHandle,
                config,
                historyRecorderHandles.composite,
                navigationOptions.accessToken ?: "",
                if (moduleRouter.isInternalImplementation()) {
                    nativeRouter
                } else {
                    RouterInterfaceAdapter(moduleRouter, ::getNavigationRoutes)
                },
            )
            assignHistoryRecorders()

            val routes = directionsSession.routes
            if (routes.isNotEmpty()) {
                navigator.setRoutes(
                    primaryRoute = routes[0],
                    startingLeg = tripSession.getRouteProgress()?.currentLegProgress?.legIndex ?: 0,
                    alternatives = routes.drop(1),
                    when (isFallback) {
                        true -> SetRoutesReason.FALLBACK_TO_OFFLINE
                        false -> SetRoutesReason.RESTORE_TO_ONLINE
                    }
                )
            }
        }
    }

    private fun reroute() {
        rerouteController?.reroute { result: RerouteResult ->
            internalSetNavigationRoutes(
                result.routes,
                SetRoutes.Reroute(result.initialLegIndex)
            )
        }
    }

    private inline fun <T> runInTelemetryContext(func: (MapboxNavigationTelemetry) -> T): T? {
        return if (TelemetryUtilsDelegate.getEventsCollectionState()) {
            func(MapboxNavigationTelemetry)
        } else {
            null
        }
    }

    private fun obtainUserAgent(): String {
        return "$MAPBOX_NAVIGATION_USER_AGENT_BASE/${BuildConfig.MAPBOX_NAVIGATION_VERSION_NAME}"
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

    private fun createTilesConfig(
        isFallback: Boolean,
        tilesVersion: String
    ): TilesConfig {
        // TODO StrictMode may report a violation as we're creating a File from the Main
        val offlineFilesPath = RoutingTilesFiles(navigationOptions.applicationContext)
            .absolutePath(navigationOptions.routingTilesOptions)
        val dataset = StringBuilder().apply {
            append(navigationOptions.routingTilesOptions.tilesDataset)
            append("/")
            append(navigationOptions.routingTilesOptions.tilesProfile)
        }.toString()

        return TilesConfig(
            offlineFilesPath,
            navigationOptions.routingTilesOptions.tileStore,
            null,
            null,
            null,
            THREADS_COUNT,
            TileEndpointConfiguration(
                navigationOptions.routingTilesOptions.tilesBaseUri.toString(),
                dataset,
                tilesVersion,
                navigationOptions.accessToken ?: "",
                isFallback,
                navigationOptions.routingTilesOptions.tilesVersion,
                navigationOptions.routingTilesOptions.minDaysBetweenServerAndLocalTilesVersion
            )
        )
    }

    private fun runIfNotDestroyed(block: () -> Any?) {
        when (isDestroyed) {
            false -> {
                block()
            }
            true -> {
                throw IllegalStateException(
                    """
                        This instance of MapboxNavigation is destroyed.
                    """.trimIndent()
                )
            }
        }
    }

    @OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
    private fun setUpRouteCacheClearer() {
        registerRoutesObserver(routesCacheClearer)
        registerRoutesPreviewObserver(routesCacheClearer)
    }

    private companion object {

        @Volatile
        private var hasInstance = false

        private const val LOG_CATEGORY = "MapboxNavigation"
        private const val USER_AGENT: String = "MapboxNavigationNative"
        private const val THREADS_COUNT = 2
        private const val ONE_SECOND_IN_MILLIS = 1000.0
    }
}
