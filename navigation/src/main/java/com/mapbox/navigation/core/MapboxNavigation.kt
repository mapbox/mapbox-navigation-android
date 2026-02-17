/**
 * Tampering with any file that contains billing code is a violation of Mapbox Terms of Service and will result in enforcement of the penalties stipulated in the ToS.
 */

package com.mapbox.navigation.core

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.app.Application
import androidx.annotation.RequiresPermission
import androidx.annotation.RestrictTo
import androidx.annotation.UiThread
import androidx.annotation.VisibleForTesting
import com.mapbox.annotation.MapboxExperimental
import com.mapbox.annotation.module.MapboxModuleType
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.common.BaseMapboxInitializer
import com.mapbox.common.TilesetDescriptor
import com.mapbox.common.module.provider.MapboxModuleProvider
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.extensions.applyLanguageAndVoiceUnitOptions
import com.mapbox.navigation.base.internal.NotSupportedForNativeRouteObject
import com.mapbox.navigation.base.internal.accounts.SkuIdProvider
import com.mapbox.navigation.base.internal.accounts.SkuIdProviderImpl
import com.mapbox.navigation.base.internal.clearCache
import com.mapbox.navigation.base.internal.extensions.internalAlternativeRouteIndices
import com.mapbox.navigation.base.internal.performance.PerformanceTracker
import com.mapbox.navigation.base.internal.reroute.getRepeatRerouteAfterOffRouteDelaySeconds
import com.mapbox.navigation.base.internal.route.parsing.setupParsing
import com.mapbox.navigation.base.internal.tilestore.NavigationTileStoreOwner
import com.mapbox.navigation.base.internal.trip.notification.TripNotificationInterceptorOwner
import com.mapbox.navigation.base.options.HistoryRecorderOptions
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.options.RoutingTilesOptions
import com.mapbox.navigation.base.route.MapMatchingMatch
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.NavigationRouterCallback
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.model.eh.EHorizonEdge
import com.mapbox.navigation.base.trip.model.eh.EHorizonEdgeMetadata
import com.mapbox.navigation.base.trip.notification.NotificationAction
import com.mapbox.navigation.base.trip.notification.TripNotification
import com.mapbox.navigation.base.trip.notification.TripNotificationInterceptor
import com.mapbox.navigation.base.utils.DecodeUtils
import com.mapbox.navigation.core.accounts.BillingController
import com.mapbox.navigation.core.arrival.ArrivalController
import com.mapbox.navigation.core.arrival.ArrivalObserver
import com.mapbox.navigation.core.arrival.ArrivalProgressObserver
import com.mapbox.navigation.core.arrival.AutoArrivalController
import com.mapbox.navigation.core.directions.session.DirectionsSession
import com.mapbox.navigation.core.directions.session.DirectionsSessionRoutes
import com.mapbox.navigation.core.directions.session.RoutesExtra
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.directions.session.RoutesSetStartedParams
import com.mapbox.navigation.core.directions.session.RoutesUpdatedResult
import com.mapbox.navigation.core.directions.session.SetNavigationRoutesStartedObserver
import com.mapbox.navigation.core.directions.session.Utils
import com.mapbox.navigation.core.directions.session.findRoute
import com.mapbox.navigation.core.directions.session.routesPlusIgnored
import com.mapbox.navigation.core.history.MapboxHistoryReader
import com.mapbox.navigation.core.history.MapboxHistoryRecorder
import com.mapbox.navigation.core.history.TestingContext
import com.mapbox.navigation.core.history.TestingContext.Companion.toNativeObject
import com.mapbox.navigation.core.internal.LowMemoryManager
import com.mapbox.navigation.core.internal.MapboxNavigationSDKInitializerImpl
import com.mapbox.navigation.core.internal.ReachabilityService
import com.mapbox.navigation.core.internal.SdkInfoProvider
import com.mapbox.navigation.core.internal.congestions.TrafficOverrideHandler
import com.mapbox.navigation.core.internal.nativeNavigator
import com.mapbox.navigation.core.internal.performance.RouteParsingHistoryTracker
import com.mapbox.navigation.core.internal.router.GetRouteSignature
import com.mapbox.navigation.core.internal.router.RouterWrapper
import com.mapbox.navigation.core.internal.telemetry.ExtendedUserFeedback
import com.mapbox.navigation.core.internal.utils.InternalUtils
import com.mapbox.navigation.core.internal.utils.ModuleParams
import com.mapbox.navigation.core.internal.utils.mapToReason
import com.mapbox.navigation.core.internal.utils.paramsProvider
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.core.mapmatching.MapMatchingAPICallback
import com.mapbox.navigation.core.mapmatching.MapMatchingAPIProvider
import com.mapbox.navigation.core.mapmatching.MapMatchingOptions
import com.mapbox.navigation.core.mapmatching.MapMatchingSuccessfulResult
import com.mapbox.navigation.core.navigator.CacheHandleWrapper
import com.mapbox.navigation.core.navigator.TilesetDescriptorFactory
import com.mapbox.navigation.core.navigator.offline.TilesetVersionManager
import com.mapbox.navigation.core.preview.RoutesPreview
import com.mapbox.navigation.core.preview.RoutesPreviewObserver
import com.mapbox.navigation.core.replay.MapboxReplayer
import com.mapbox.navigation.core.reroute.InternalRerouteController
import com.mapbox.navigation.core.reroute.NativeMapboxRerouteController
import com.mapbox.navigation.core.reroute.RerouteController
import com.mapbox.navigation.core.reroute.RerouteController.RerouteStateObserver
import com.mapbox.navigation.core.reroute.RerouteOptionsAdapter
import com.mapbox.navigation.core.reroute.RerouteResult
import com.mapbox.navigation.core.reroute.RerouteState
import com.mapbox.navigation.core.reroute.RerouteState.FetchingRoute
import com.mapbox.navigation.core.reroute.RerouteStateV2
import com.mapbox.navigation.core.routealternatives.AlternativeRouteMetadata
import com.mapbox.navigation.core.routealternatives.RouteAlternativesController
import com.mapbox.navigation.core.routealternatives.RouteAlternativesControllerProvider
import com.mapbox.navigation.core.routealternatives.UpdateRouteSuggestion
import com.mapbox.navigation.core.routeoptions.RouteOptionsUpdater
import com.mapbox.navigation.core.routerefresh.RouteRefreshController
import com.mapbox.navigation.core.routerefresh.RouteRefreshControllerProvider
import com.mapbox.navigation.core.telemetry.ApplicationLifecycleMonitor
import com.mapbox.navigation.core.telemetry.NavigationTelemetry
import com.mapbox.navigation.core.telemetry.UserFeedback
import com.mapbox.navigation.core.telemetry.events.FeedbackEvent
import com.mapbox.navigation.core.telemetry.events.FeedbackHelper
import com.mapbox.navigation.core.telemetry.events.FeedbackMetadata
import com.mapbox.navigation.core.telemetry.events.FeedbackMetadataWrapper
import com.mapbox.navigation.core.trip.RelevantVoiceInstructionsCallback
import com.mapbox.navigation.core.trip.VoiceInstructionsAvailableObserver
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
import com.mapbox.navigation.core.utils.PermissionsChecker
import com.mapbox.navigation.core.utils.SystemLocaleWatcher
import com.mapbox.navigation.navigator.internal.MapboxNativeNavigator
import com.mapbox.navigation.navigator.internal.NavigatorLoader
import com.mapbox.navigation.navigator.internal.NavigatorLoader.createRoadObjectMatcherOptions
import com.mapbox.navigation.utils.internal.ConnectivityHandler
import com.mapbox.navigation.utils.internal.ThreadController
import com.mapbox.navigation.utils.internal.Time
import com.mapbox.navigation.utils.internal.ifNonNull
import com.mapbox.navigation.utils.internal.logD
import com.mapbox.navigation.utils.internal.logE
import com.mapbox.navigation.utils.internal.logI
import com.mapbox.navigation.utils.internal.logW
import com.mapbox.navigation.utils.internal.monitorChannelWithException
import com.mapbox.navigator.AlertsServiceOptions
import com.mapbox.navigator.CacheHandle
import com.mapbox.navigator.ConfigHandle
import com.mapbox.navigator.ElectronicHorizonOptions
import com.mapbox.navigator.FallbackVersionsObserver
import com.mapbox.navigator.IncidentsOptions
import com.mapbox.navigator.NavigatorConfig
import com.mapbox.navigator.PollingConfig
import com.mapbox.navigator.RerouteStrategyForMatchRoute
import com.mapbox.navigator.SetRoutesReason
import com.mapbox.navigator.TileEndpointConfiguration
import com.mapbox.navigator.TilesConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.lang.reflect.Field
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

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
 * - [requestRoutes], if successful, returns a route reference without acting on it. You can then pass the generated routes to [setNavigationRoutes].
 * - [setNavigationRoutes] sets new routes, clear current ones, or changes the route at primary index 0.
 * The routes are immediately available via the [RoutesObserver] and the first route (at index 0) is going to be chosen as the primary one.
 *
 * ### Route reason update
 * When routes are updated via [setNavigationRoutes] the reason that is spread via [RoutesObserver.onRoutesChanged] might be:
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
 *             @RouterOrigin routerOrigin: String
 *         ) {
 *             mapboxNavigation.setNavigationRoutes(routes)
 *         }
 *         override fun onFailure(reasons: List<RouterFailure>, routeOptions: RouteOptions) { }
 *         override fun onCanceled(routeOptions: RouteOptions, @RouterOrigin routerOrigin: String) { }
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
 * You can use [setNavigationRoutes] to provide new routes, clear current ones, or change the route at primary index 0.
 *
 * @param navigationOptions a set of [NavigationOptions] used to customize various features of the SDK.
 * @param historyRecorder Use the history recorder to save history files.
 *   @see [HistoryRecorderOptions] to enable and customize the directory
 *   @see [MapboxHistoryReader] to read the files
 */
@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
@UiThread
class MapboxNavigation @VisibleForTesting internal constructor(
    val navigationOptions: NavigationOptions,
    private val threadController: ThreadController,
    val historyRecorder: MapboxHistoryRecorder,
    internal val copilotHistoryRecorder: MapboxHistoryRecorder,
    internal val compositeRecorder: MapboxHistoryRecorder,
    private val permissionsChecker: PermissionsChecker,
    private val lowMemoryManager: LowMemoryManager,
) {

    internal constructor(navigationOptions: NavigationOptions) : this(
        navigationOptions,
        ThreadController(),
        MapboxHistoryRecorder(navigationOptions),
        MapboxHistoryRecorder(navigationOptions),
        MapboxHistoryRecorder(navigationOptions),
        PermissionsChecker(navigationOptions.applicationContext),
        LowMemoryManager.create(),
    )

    private val mainJobController = threadController.getMainScopeAndRootJob()
    private val directionsSession: DirectionsSession

    private val historyRecorderHandles: NavigatorLoader.HistoryRecorderHandles
    private val tripService: TripService
    private val tripSession: TripSession
    private val navigationSession: NavigationSession
    internal val historyRecordingStateHandler: HistoryRecordingStateHandler
    private val developerMetadataAggregator: DeveloperMetadataAggregator
    private val tripSessionLocationEngine: TripSessionLocationEngine
    private val billingController: BillingController
    private val connectivityHandler: ConnectivityHandler = ConnectivityHandler(
        Channel(Channel.CONFLATED),
    )
    private val tripNotificationInterceptorOwner = TripNotificationInterceptorOwner()
    private val trafficOverrideHandler = TrafficOverrideHandler(
        navigationOptions.trafficOverrideOptions,
    )
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
            navigationOptions.eHorizonOptions.alertServiceOptions.collectRestrictedAreas,
            navigationOptions.eHorizonOptions.alertServiceOptions.collectMergingAreas,
            navigationOptions.eHorizonOptions.alertServiceOptions.collectServiceAreas,
        ),
        navigationOptions.eHorizonOptions.enableEnhancedDataAlongEH,
    )

    private val routesPreviewController = NavigationComponentProvider.createRoutesPreviewController(
        threadController.getMainScopeAndRootJob().scope,
    )

    private val routeUpdateMutex = Mutex()

    private val mapMatchingAPI = MapMatchingAPIProvider.provideMapMatchingAPI()

    private val routesProgressDataProvider =
        NavigationComponentProvider.createRouteRefreshRequestDataProvider()

    private val evDynamicDataHolder = NavigationComponentProvider.createEVDynamicDataHolder()

    private val routerWrapper: RouterWrapper

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
        navigationOptions.enableSensors,
        RerouteStrategyForMatchRoute.REROUTE_DISABLED,
        createRoadObjectMatcherOptions(navigationOptions.roadObjectMatcherOptions),
    )

    private val lowMemoryObserver = LowMemoryManager.Observer {
        clearCaches()
    }

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
     * Use route refresh controller to handle route refreshes.
     * @see [RouteRefreshController] for more details.
     */
    @ExperimentalPreviewMapboxNavigationAPI
    val routeRefreshController: RouteRefreshController

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

    /**
     * Use this property to provide ETC gate update info.
     *
     * Deprecated, should be replaced by `DataInputsManager` which is available as a separate SDK artifact.
     * [Contact us](https://www.mapbox.com/support) for more information.
     */
    @ExperimentalPreviewMapboxNavigationAPI
    @Suppress("ReplaceWith")
    @Deprecated("EtcGateApi is deprecated")
    val etcGateAPI: EtcGateApi

    /**
     * Manager for checking tileset version availability and update requirements.
     * This manager provides functionality to:
     * - Check available tileset versions from the server
     * - Determine if downloaded tilesets need updates based on age or version
     * - Identify blocked versions that should not be used
     * @see TilesetVersionManager for detailed API documentation
     */
    @ExperimentalPreviewMapboxNavigationAPI
    val tilesetVersionManager: TilesetVersionManager

    private var reachabilityObserverId: Long? = null

    private var latestLegIndex: Int? = null

    private val historyRecorders: List<MapboxHistoryRecorder> = listOf(
        historyRecorder,
        copilotHistoryRecorder,
    )

    private val systemLocaleWatcher: SystemLocaleWatcher

    private val appLifecycleMonitor = ApplicationLifecycleMonitor(
        navigationOptions.applicationContext as Application,
    )

    internal val navigationTelemetry: NavigationTelemetry

    /**
     * Describes whether this instance of `MapboxNavigation` has been destroyed by calling
     * [onDestroy]. Once an instance is destroyed, it cannot be used anymore.
     *
     * @see [MapboxNavigationApp]
     */
    @Volatile
    var isDestroyed = false
        private set

    private var _navigator: MapboxNativeNavigator?

    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    val navigator: MapboxNativeNavigator
        get() = _navigator ?: error("MapboxNavigation is destroyed")

    internal val skuIdProvider: SkuIdProvider

    init {
        if (
            navigationOptions.nativeRouteObject &&
            navigationOptions.trafficOverrideOptions.isEnabled
        ) {
            // TODO: remove validation once native route object supports traffic override
            // https://mapbox.atlassian.net/browse/NAVAND-6480
            NotSupportedForNativeRouteObject(
                "trafficOverrideOptions.isEnabled = true",
            )
        }

        val initSectionName = "MapboxNavigation#init-"
        PerformanceTracker.trackPerformanceSync(
            "${initSectionName}MapboxNavigationSDKInitializerImpl",
        ) {
            BaseMapboxInitializer.init(MapboxNavigationSDKInitializerImpl::class.java)
        }
        if (hasInstance) {
            throw IllegalStateException(
                """
                    A different MapboxNavigation instance already exists.
                    Make sure to destroy it with #onDestroy before creating a new one.
                    Also see MapboxNavigationApp for instance management assistance.
                """.trimIndent(),
            )
        }
        hasInstance = true

        val config = NavigatorLoader.createConfig(
            deviceProfile = navigationOptions.deviceProfile,
            navigatorConfig = navigatorConfig,
        )

        NavigationTileStoreOwner.init(
            RoutingTilesTileStoreProvider(navigationOptions.routingTilesOptions),
        )

        val tilesConfig = createTilesConfig(
            isFallback = false,
            tilesVersion = navigationOptions.routingTilesOptions.tilesVersion,
        )

        historyRecorderHandles = createHistoryRecorderHandles(config)

        _navigator = NavigationComponentProvider.createNativeNavigator(
            tilesConfig,
            config,
            historyRecorderHandles.composite,
            PerformanceTracker.trackPerformanceSync("${initSectionName}createOfflineCacheHandle") {
                createOfflineCacheHandle(config)
            },
            NavigationComponentProvider.createEventsMetadataInterface(
                navigationOptions.applicationContext,
                appLifecycleMonitor,
                navigationOptions.eventsAppMetadata,
            ),
            NavigatorLoader.createRoadObjectMatcherConfig(
                navigationOptions.roadObjectMatcherOptions,
            ),
        )

        // TODO: move router out of directions session to avoid cycle dependency.
        var routeLookup: (String) -> NavigationRoute? = { null }
        val parsing = setupParsing(
            nativeRoute = navigationOptions.nativeRouteObject,
            time = Time.SystemClockImpl,
            routeParsingTracking = RouteParsingHistoryTracker(compositeRecorder),
            parsingDispatcher = ThreadController.DefaultDispatcher,
            existingParsedRoutesLookup = { routeLookup.invoke(it) },
            prepareForParsingAction = { prepareNavigationForRoutesParsing() },
        )
        routerWrapper = PerformanceTracker.trackPerformanceSync(
            "${initSectionName}RouterWrapper",
        ) {
            RouterWrapper(
                navigator.getRouter(),
                threadController,
                parsing,
            )
        }

        etcGateAPI = PerformanceTracker.trackPerformanceSync("${initSectionName}etcGateAPI") {
            EtcGateApi(navigator.experimental)
        }

        historyRecorder.historyRecorderHandle = historyRecorderHandles.general
        copilotHistoryRecorder.historyRecorderHandle = historyRecorderHandles.copilot
        compositeRecorder.historyRecorderHandle = historyRecorderHandles.composite

        navigationSession = NavigationComponentProvider.createNavigationSession()
        historyRecordingStateHandler = NavigationComponentProvider
            .createHistoryRecordingStateHandler()
        developerMetadataAggregator = NavigationComponentProvider.createDeveloperMetadataAggregator(
            historyRecordingStateHandler,
        )

        val notification: TripNotification = PerformanceTracker.trackPerformanceSync(
            "${initSectionName}notification",
        ) {
            MapboxModuleProvider
                .createModule(
                    MapboxModuleType.NavigationTripNotification,
                ) {
                    paramsProvider(
                        ModuleParams.NavigationTripNotification(
                            navigationOptions,
                            tripNotificationInterceptorOwner,
                            navigationOptions.distanceFormatterOptions,
                        ),
                    )
                }
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
            locationOptions = navigationOptions.locationOptions,
        )

        directionsSession = NavigationComponentProvider.createDirectionsSession(routerWrapper)
        directionsSession.registerSetNavigationRoutesFinishedObserver(navigationSession)

        tripSession = NavigationComponentProvider.createTripSession(
            tripService = tripService,
            directionsSession = directionsSession,
            tripSessionLocationEngine = tripSessionLocationEngine,
            navigator = navigator,
            threadController,
            navigationOptions.rerouteOptions.getRepeatRerouteAfterOffRouteDelaySeconds(),
        )

        tripSession.registerRouteProgressObserver(routesProgressDataProvider)
        tripSession.registerStateObserver(navigationSession)
        tripSession.registerStateObserver(historyRecordingStateHandler)

        if (reachabilityObserverId == null) {
            reachabilityObserverId = ReachabilityService.addReachabilityObserver(
                connectivityHandler,
            )
        }
        routeLookup = directionsSession::findRoute

        if (navigationOptions.trafficOverrideOptions.isEnabled) {
            tripSession.registerRouteProgressObserver(trafficOverrideHandler)
            tripSession.registerLocationObserver(trafficOverrideHandler)
            registerRoutesObserver(trafficOverrideHandler)
            trafficOverrideHandler.registerRouteTrafficRefreshObserver { navigationRoutes ->
                setExternallyRefreshedRoutes(navigationRoutes, isManualRefresh = true)
            }
        }

        arrivalProgressObserver = NavigationComponentProvider.createArrivalProgressObserver(
            tripSession,
        )
        setArrivalController()

        skuIdProvider = SkuIdProviderImpl()

        val sdkInformation = SdkInfoProvider.sdkInformation()

        billingController = NavigationComponentProvider.createBillingController(
            navigationSession,
            tripSession,
            arrivalProgressObserver,
            skuIdProvider,
            sdkInformation,
        )

        navigationTelemetry = NavigationTelemetry.create(tripSession, navigator)

        val routeOptionsProvider = RouteOptionsUpdater()

        routeAlternativesController = RouteAlternativesControllerProvider.create(
            navigationOptions.routeAlternativesOptions,
            navigator,
            tripSession,
            threadController,
            parsing,
        )
        routeAlternativesController.setRouteUpdateSuggestionListener(::updateRoutes)
        @OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
        routeRefreshController = RouteRefreshControllerProvider.createRouteRefreshController(
            Dispatchers.Main,
            Dispatchers.Main.immediate,
            navigationOptions.routeRefreshOptions,
            directionsSession,
            routesProgressDataProvider,
            evDynamicDataHolder,
            Time.SystemClockImpl,
        )
        @OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
        routeRefreshController.registerRouteRefreshObserver {
            internalSetNavigationRoutes(
                listOf(it.primaryRouteRefresherResult.route) +
                    it.alternativesRouteRefresherResults.map { it.route },
                SetRoutes.RefreshRoutes.RefreshControllerRefresh(it),
            )
        }

        val nativeRerouteController = nativeNavigator.getRerouteController()
        val nativeRerouteDetector = nativeNavigator.getRerouteDetector()
        // NN initializes detector and controller in case reroute is enabled in custom config
        // {"features": {"useInternalReroute": true }}
        // until https://mapbox.atlassian.net/browse/NAVAND-3575 is completed
        // useInternalReroute is supposed to be used only for internal testing
        defaultRerouteController = if (
            nativeRerouteController != null && nativeRerouteDetector != null
        ) {
            NativeMapboxRerouteController(
                rerouteEventsProvider = nativeNavigator,
                rerouteController = nativeRerouteController,
                rerouteDetector = nativeRerouteDetector,
                getCurrentRoutes = directionsSession::routesPlusIgnored,
                updateRoutes = { routes, legIndex ->
                    internalSetNavigationRoutes(routes, SetRoutes.Reroute(legIndex))
                    true
                },
                scope = mainJobController.scope,
                routeParser = parsing,
            )
        } else {
            NavigationComponentProvider.createRerouteController(
                directionsSession,
                tripSession,
                routeOptionsProvider,
                navigationOptions.rerouteOptions,
                threadController,
                evDynamicDataHolder,
            )
        }
        rerouteController = defaultRerouteController

        internalRoutesObserver = createInternalRoutesObserver()
        internalOffRouteObserver = createInternalOffRouteObserver()
        internalFallbackVersionsObserver = createInternalFallbackVersionsObserver()
        tripSession.registerFallbackVersionsObserver(internalFallbackVersionsObserver)
        rerouteController?.let {
            tripSession.setOffRouteObserverForReroute(internalOffRouteObserver, it)
        }
        registerRoutesObserver(internalRoutesObserver)
        registerRoutesObserver(routeRefreshController::onRoutesChanged)
        setUpRouteCacheClearer()

        roadObjectsStore = RoadObjectsStore(navigator)
        graphAccessor = GraphAccessor(navigator)
        tilesetDescriptorFactory = PerformanceTracker.trackPerformanceSync(
            "${initSectionName}tilesetDescriptorFactory",
        ) {
            TilesetDescriptorFactory(
                navigationOptions.routingTilesOptions,
                navigator.cache,
            )
        }
        roadObjectMatcher = RoadObjectMatcher(navigator)

        systemLocaleWatcher = SystemLocaleWatcher.create(
            navigationOptions.applicationContext,
            navigator,
        )

        tilesetVersionManager = NavigationComponentProvider.createTilesetVersionManager(
            routingTilesOptions = navigationOptions.routingTilesOptions,
            tileStore = NavigationTileStoreOwner.invoke(),
        )

        registerRouteProgressObserver(
            NavigationComponentProvider.createForkPointPassedObserver(
                directionsSession,
                ::currentLegIndex,
            ),
        )

        lowMemoryManager.addObserver(lowMemoryObserver)
    }

    @OptIn(ExperimentalMapboxNavigationAPI::class)
    private fun createOfflineCacheHandle(config: ConfigHandle): CacheHandle? {
        val version = navigationOptions.routingTilesOptions.fallbackOfflineTilesVersion
            ?: return null
        return NavigatorLoader.createCacheHandle(
            config,
            createTilesConfig(false, version),
            historyRecorderHandles.composite,
        )
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
     * Default value is set to true. To start foreground service your application needs to have required permissions.
     * See [FOREGROUND_SERVICE_TYPE_LOCATION documentation page](https://developer.android.com/reference/android/content/pm/ServiceInfo#FOREGROUND_SERVICE_TYPE_LOCATION)
     * for more information.
     *
     * @see [registerTripSessionStateObserver]
     * @see [registerRouteProgressObserver]
     */
    @RequiresPermission(anyOf = [ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION])
    @JvmOverloads
    fun startTripSession(withForegroundService: Boolean = true) {
        if (withForegroundService) {
            checkForegroundServiceLocationPermissions(justLogs = true)
        }
        startSession(withForegroundService, false)
    }

    /**
     * Functionally the same as [startTripSession] but throws [IllegalStateException] if
     * the application doesn't have permissions to start foreground service with location type.
     *
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
     * Default value is set to true. To start foreground service your application needs to have required permissions.
     * See [FOREGROUND_SERVICE_TYPE_LOCATION documentation page](https://developer.android.com/reference/android/content/pm/ServiceInfo#FOREGROUND_SERVICE_TYPE_LOCATION)
     * for more information.
     *
     * @throws IllegalStateException if [withForegroundService] is true and the application
     * doesn't have permissions to start foreground services with location type. See
     * [FOREGROUND_SERVICE_TYPE_LOCATION documentation page](https://developer.android.com/reference/android/content/pm/ServiceInfo#FOREGROUND_SERVICE_TYPE_LOCATION)
     * for more information.
     *
     * @see startTripSession
     * @see [registerTripSessionStateObserver]
     * @see [registerRouteProgressObserver]
     */
    @ExperimentalPreviewMapboxNavigationAPI
    @RequiresPermission(anyOf = [ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION])
    @JvmOverloads
    fun startTripSessionWithPermissionCheck(withForegroundService: Boolean = true) {
        if (withForegroundService) {
            checkForegroundServiceLocationPermissions(justLogs = false)
        }
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
            routeAlternativesController.pause()
            routeRefreshController.pauseRouteRefreshes()
            tripSession.stop()
            navigator.pause()
        }
    }

    /**
     * Functionally the same as [startTripSession] except the locations do not come from the
     * [NavigationOptions.mockLocationProvider]. The events are emitted by the [mapboxReplayer].
     *
     * This allows you to simulate navigation routes or replay history from the [historyRecorder].
     *
     * @param withForegroundService Boolean if set to false, foreground service will not be started and
     * no notifications will be rendered, and no location updates will be available while the app is in the background.
     * Default value is set to true. To start foreground service your application needs to have required permissions.
     * See [FOREGROUND_SERVICE_TYPE_LOCATION documentation page](https://developer.android.com/reference/android/content/pm/ServiceInfo#FOREGROUND_SERVICE_TYPE_LOCATION)
     * for more information.
     */
    @ExperimentalPreviewMapboxNavigationAPI
    fun startReplayTripSession(withForegroundService: Boolean = true) {
        if (withForegroundService) {
            checkForegroundServiceLocationPermissions(justLogs = true)
        }

        startSession(withForegroundService, true)
    }

    /**
     * Functionally the same as [startReplayTripSession] but throws [IllegalStateException] if
     * the application doesn't have permissions to start foreground service with location type.
     *
     * Functionally the same as [startTripSession] except the locations do not come from the
     * [NavigationOptions.mockLocationProvider]. The events are emitted by the [mapboxReplayer].
     *
     * This allows you to simulate navigation routes or replay history from the [historyRecorder].
     *
     * @param withForegroundService Boolean if set to false, foreground service will not be started and
     * no notifications will be rendered, and no location updates will be available while the app is in the background.
     * Default value is set to true. To start foreground service your application needs to have required permissions.
     * See [FOREGROUND_SERVICE_TYPE_LOCATION documentation page](https://developer.android.com/reference/android/content/pm/ServiceInfo#FOREGROUND_SERVICE_TYPE_LOCATION)
     * for more information.
     *
     * @throws IllegalStateException if [withForegroundService] is true and the application
     * doesn't have permissions to start foreground services with location type. See
     * [FOREGROUND_SERVICE_TYPE_LOCATION documentation page](https://developer.android.com/reference/android/content/pm/ServiceInfo#FOREGROUND_SERVICE_TYPE_LOCATION)
     * for more information.
     *
     * @see startReplayTripSession
     */
    @ExperimentalPreviewMapboxNavigationAPI
    @RequiresPermission(anyOf = [ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION])
    fun startReplayTripSessionWithPermissionCheck(withForegroundService: Boolean = true) {
        if (withForegroundService) {
            checkForegroundServiceLocationPermissions(justLogs = false)
        }

        startSession(withForegroundService, true)
    }

    private fun checkForegroundServiceLocationPermissions(justLogs: Boolean) {
        val result = permissionsChecker.hasForegroundServiceLocationPermissions()
        if (result.isValue) {
            return
        }

        val msg = "Starting foreground service on Android 14 or later require " +
            "Manifest.permission.FOREGROUND_SERVICE_LOCATION and one of the following " +
            "permissions: Manifest.permission.ACCESS_COARSE_LOCATION, " +
            "Manifest.permission.ACCESS_FINE_LOCATION. " +
            "${result.error}"

        if (justLogs) {
            logE(msg)
        } else {
            error(msg)
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
     * Requests a route.
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
        callback: NavigationRouterCallback,
    ): Long {
        return directionsSession.requestRoutes(
            routeOptions,
            GetRouteSignature(GetRouteSignature.Reason.NEW_ROUTE, GetRouteSignature.Origin.APP),
            callback,
        )
    }

    /**
     * Cancels a specific route request using the ID returned by [requestRoutes].
     */
    fun cancelRouteRequest(requestId: Long) {
        directionsSession.cancelRouteRequest(requestId)
    }

    /**
     * Requests routes from
     * [Mapbox Map Matching API](https://docs.mapbox.com/api/navigation/map-matching/) and
     * transforms result to [NavigationRoute]s that could be used for [setNavigationRoutes].
     * @param mapMatchingOptions map matching options
     * @param callback listener that gets notified when request state changes
     * @return requestId, see [cancelMapMatchingRequest]
     * @see [MapMatchingMatch], [MapMatchingSuccessfulResult], [MapMatchingAPICallback]
     */
    @ExperimentalPreviewMapboxNavigationAPI
    fun requestMapMatching(
        mapMatchingOptions: MapMatchingOptions,
        callback: MapMatchingAPICallback,
    ): Long {
        return mapMatchingAPI.requestMapMatching(mapMatchingOptions, callback)
    }

    /**
     * Cancels a specific map matching request using the ID returned by [requestMapMatching].
     */
    @ExperimentalPreviewMapboxNavigationAPI
    fun cancelMapMatchingRequest(requestId: Long) {
        mapMatchingAPI.cancel(requestId)
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
        callback: RoutesSetCallback? = null,
    ) {
        if (routes.isNotEmpty()) {
            billingController.onExternalRouteSet(routes.first(), initialLegIndex)
        }

        // Telemetry uses this field to determine what type of event should be triggered.
        val setRoutesInfo = when {
            routes.isEmpty() -> SetRoutes.CleanUp
            routes.first() == directionsSession.routes.firstOrNull() -> {
                SetRoutes.Alternatives(initialLegIndex)
            }

            directionsSession.routes.map { it.id }.contains(routes.first().id) -> {
                SetRoutes.Reorder(initialLegIndex)
            }

            else -> SetRoutes.NewRoutes(initialLegIndex)
        }
        internalSetNavigationRoutes(
            routes,
            setRoutesInfo,
            callback,
        )
    }

    /**
     * Switches [MapboxNavigation] to alternative route, i.e. the selected alternative become primary
     * route, while primary route becomes alternative.
     * Limitation: switch to alternative route could be performed only when [getTripSessionState]
     * is [TripSessionState.STARTED] and after at lest one [RouteProgress] was emitted after
     * the latest routes update.
     * @param alternativeRoute is an alternative route the navigation should switch to. It should be
     * present among [getNavigationRoutes] and should not be the same as primary route.
     * @param callback notifies about result.
     */
    @ExperimentalPreviewMapboxNavigationAPI
    fun switchToAlternativeRoute(
        alternativeRoute: NavigationRoute,
        callback: RoutesSetCallback? = null,
    ) {
        val routeProgress = tripSession.getRouteProgress()
        if (routeProgress == null) {
            val errorMessage = "No route progress available"
            logE(errorMessage, LOG_CATEGORY)
            callback?.onRoutesSet(ExpectedFactory.createError(RoutesSetError(errorMessage)))
            return
        }
        val alternativeIndices = routeProgress
            .internalAlternativeRouteIndices()[alternativeRoute.id]
        val allRoutes = getNavigationRoutes()
        val alternativeSwitchTo = allRoutes.firstOrNull { it.id == alternativeRoute.id }
        if (alternativeIndices == null || alternativeSwitchTo == null) {
            val errorMessage = "Can't switch to alternative ${alternativeRoute.id} " +
                "as it isn't present among currently tracked alternatives: " +
                "${allRoutes.drop(1).map { it.id }}"
            logE(errorMessage, LOG_CATEGORY)
            callback?.onRoutesSet(
                ExpectedFactory.createError(RoutesSetError(errorMessage)),
            )
            return
        }
        logI(LOG_CATEGORY) {
            "Switching to ${alternativeSwitchTo.id} leg ${alternativeIndices.legIndex}"
        }
        val newRoutes = allRoutes.toMutableList().apply {
            remove(alternativeSwitchTo)
            add(0, alternativeSwitchTo)
        }
        setNavigationRoutes(newRoutes, alternativeIndices.legIndex, callback)
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

    internal fun setExternallyRefreshedRoutes(
        routes: List<NavigationRoute>,
        isManualRefresh: Boolean,
    ) {
        internalSetNavigationRoutes(
            routes,
            SetRoutes.RefreshRoutes.ExternalRefresh(
                legIndex = currentLegIndex(),
                isManual = isManualRefresh,
            ),
        )
    }

    private fun internalSetNavigationRoutes(
        routes: List<NavigationRoute>,
        setRoutesInfo: SetRoutes,
        callback: RoutesSetCallback? = null,
    ) {
        logI(LOG_CATEGORY) {
            "setting routes; reason: ${setRoutesInfo.mapToReason()}; IDs: ${routes.map { it.id }}"
        }
        directionsSession.setNavigationRoutesStarted(RoutesSetStartedParams(routes))
        when (setRoutesInfo) {
            SetRoutes.CleanUp,
            is SetRoutes.NewRoutes,
            is SetRoutes.Reroute,
            is SetRoutes.Reorder,
            -> {
                rerouteController?.interrupt()
            }

            is SetRoutes.RefreshRoutes,
            is SetRoutes.Alternatives,
            -> {
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
                                "to ${directionsSession.routes.firstOrNull()?.id}",
                        ),
                    )

                    // Even though we are not setting new routes here,
                    // we need to inform that the operation (setNavigationRoutesStarted) is finished
                    directionsSession.setNavigationRoutesFinished(
                        DirectionsSessionRoutes(
                            acceptedRoutes = directionsSession.routes,
                            ignoredRoutes = directionsSession.ignoredRoutes,
                            setRoutesInfo = setRoutesInfo,
                        ),
                    )
                } else {
                    historyRecordingStateHandler.setRoutes(routes)
                    when (val processedRoutes = setRoutesToTripSession(routes, setRoutesInfo)) {
                        is NativeSetRouteValue -> {
                            val directionsSessionRoutes = Utils.createDirectionsSessionRoutes(
                                routes,
                                processedRoutes,
                                setRoutesInfo,
                            )
                            directionsSession.setNavigationRoutesFinished(directionsSessionRoutes)
                            if (
                                setRoutesInfo is SetRoutes.RefreshRoutes.ExternalRefresh &&
                                setRoutesInfo.isManual
                            ) {
                                routeRefreshController.onRoutesRefreshedManually(
                                    routes,
                                )
                            }
                            routesSetResult = ExpectedFactory.createValue(
                                RoutesSetSuccess(
                                    directionsSessionRoutes.ignoredRoutes.associate {
                                        it.navigationRoute.id to
                                            RoutesSetError("invalid alternative")
                                    },
                                ),
                            )
                        }

                        is NativeSetRouteError -> {
                            logE(
                                "Routes with IDs ${routes.map { it.id }} " +
                                    "will be ignored as they are not valid",
                            )
                            routesSetResult = ExpectedFactory.createError(
                                RoutesSetError(processedRoutes.error),
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
                    nativeAlternatives,
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
     * @return a list of [NavigationRoute]s
     */
    fun getNavigationRoutes(): List<NavigationRoute> = directionsSession.routes

    private fun clearCaches() {
        DecodeUtils.clearCache()
    }

    /**
     * Call this method whenever this instance of the [MapboxNavigation] is not going to be used anymore and should release all of its resources.
     */
    internal fun onDestroy() {
        if (isDestroyed) return

        logD("onDestroy", LOG_CATEGORY)

        lowMemoryManager.removeObserver(lowMemoryObserver)
        systemLocaleWatcher.destroy()
        billingController.onDestroy()
        directionsSession.shutdown()
        directionsSession.unregisterAllSetNavigationRoutesFinishedObserver()
        mapMatchingAPI.cancelAll()
        tripSession.stop()
        tripSession.unregisterAllLocationObservers()
        tripSession.unregisterAllRouteProgressObservers()
        tripSession.unregisterAllOffRouteObservers()
        tripSession.unregisterAllStateObservers()
        tripSession.unregisterAllBannerInstructionsObservers()
        tripSession.unregisterAllVoiceInstructionsObservers()
        tripSession.unregisterAllEHorizonObservers()
        tripSession.unregisterAllFallbackVersionsObservers()
        tripSession.resetOffRouteObserverForReroute()
        tripSessionLocationEngine.destroy()
        routeAlternativesController.unregisterAll()
        navigationTelemetry.clearObservers()
        internalSetNavigationRoutes(emptyList(), SetRoutes.CleanUp)

        // using reset with callback = NULL to make sure it is run synchronously in NN
        navigator.reset(null)

        navigator.unregisterAllObservers()
        navigator.shutdown()

        navigationVersionSwitchObservers.clear()
        arrivalProgressObserver.unregisterAllObservers()

        navigationSession.unregisterAllNavigationSessionStateObservers()
        historyRecordingStateHandler.unregisterAllStateChangeObservers()
        historyRecordingStateHandler.unregisterAllCopilotSessionObservers()
        developerMetadataAggregator.unregisterAllObservers()
        @OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
        routeRefreshController.destroy()
        routesPreviewController.unregisterAllRoutesPreviewObservers()
        threadController.cancelAllNonUICoroutines()
        threadController.cancelAllUICoroutines()
        ifNonNull(reachabilityObserverId) {
            ReachabilityService.removeReachabilityObserver(it)
            reachabilityObserverId = null
        }
        navigator.resetAdasisMessageCallback()
        historyRecorders.forEach { it.unregisterAllHistoryRecordingEnabledObservers() }

        clearCaches()

        _navigator = null
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
        bannerInstructionsObserver: BannerInstructionsObserver,
    ) {
        tripSession.registerBannerInstructionsObserver(bannerInstructionsObserver)
    }

    /**
     * Unregisters [BannerInstructionsObserver].
     */
    fun unregisterBannerInstructionsObserver(
        bannerInstructionsObserver: BannerInstructionsObserver,
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
     * Register [VoiceInstructionsAvailableObserver] which informs you when there are an available
     * voice instructions.
     *
     * Note: to retrieve the actual voice instructions when they are available, use
     * [registerRelevantVoiceInstructionsCallback].
     *
     * @param observer the observer to register
     *
     * @see [unregisterVoiceInstructionsAvailableObserver]
     */
    @MapboxExperimental
    fun registerVoiceInstructionsAvailableObserver(observer: VoiceInstructionsAvailableObserver) {
        tripSession.registerVoiceInstructionsAvailabilityObserver(observer)
    }

    /**
     * Unregisters [VoiceInstructionsAvailableObserver].
     *
     * @param observer the observer to unregister
     *
     * @see [registerVoiceInstructionsAvailableObserver]
     */
    @MapboxExperimental
    fun unregisterVoiceInstructionsAvailableObserver(observer: VoiceInstructionsAvailableObserver) {
        tripSession.unregisterVoiceInstructionsAvailabilityObserver(observer)
    }

    /**
     * Registers a [RelevantVoiceInstructionsCallback] to receive the relevant voice instructions.
     *
     * Note: the observer will be **automatically unregistered** after receiving the callback, as
     * this is a one-time fetch operation. And if you need to get the relevant voice instructions
     * again, simply call this method again.
     *
     * @param callback the observer to register (will be auto-unregistered after callback)
     */
    @MapboxExperimental
    fun registerRelevantVoiceInstructionsCallback(callback: RelevantVoiceInstructionsCallback) {
        tripSession.registerRelevantVoiceInstructionsCallback(callback)
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
     * Registers [RoutesInvalidatedObserver].
     * Use this method to keep track of routes that become invalidated,
     * which means they won't be refreshed anymore.
     * It is recommended to rebuild the route
     * when you receive a notification of it being invalidated.
     */
    fun registerRoutesInvalidatedObserver(observer: RoutesInvalidatedObserver) {
        routeRefreshController.registerRoutesInvalidatedObserver(observer)
    }

    /**
     * Unregisters [RoutesInvalidatedObserver].
     */
    fun unregisterRoutesInvalidatedObserver(observer: RoutesInvalidatedObserver) {
        routeRefreshController.unregisterRoutesInvalidatedObserver(observer)
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
     * Set [RerouteOptionsAdapter]. It allows modify [RouteOptions] of default implementation of
     * [RerouteController].
     */
    fun setRerouteOptionsAdapter(
        rerouteOptionsAdapter: RerouteOptionsAdapter?,
    ) {
        rerouteController?.setRerouteOptionsAdapter(rerouteOptionsAdapter)
    }

    /**
     * Get currently used [RerouteController]. Null if [isRerouteEnabled] is false.
     */
    fun getRerouteController(): RerouteController? = rerouteController

    /**
     * Enables/disables rerouting.
     *
     * @param enabled true if rerouting should be enabled, false otherwise
     */
    fun setRerouteEnabled(enabled: Boolean) {
        logI("Set reroute enabled = $enabled")
        if (enabled) {
            if (rerouteController == null) {
                defaultRerouteController.setEnabled(true)
                rerouteController = defaultRerouteController
            }
        } else {
            val oldController = rerouteController
            if (oldController != null) {
                val oldState = oldController.state
                if (oldState == RerouteState.FetchingRoute) {
                    oldController.interrupt()
                }
                oldController.setEnabled(false)
                rerouteController = null
            }
        }
    }

    /**
     * Whether rerouting is enabled. True by default.
     * It can be disabled by invoking [setRerouteEnabled] with false as a parameter.
     */
    fun isRerouteEnabled(): Boolean = rerouteController != null

    /**
     * Triggers route recalculation for the case when user wants to
     * change route options during active guidance.
     *
     * Route recalculation is an asynchronous operation. You can track reroute progress via
     * [RerouteStateObserver], use [RerouteController.registerRerouteStateObserver] to set one.
     * In case of successful route replan, you will receive new routes in [RoutesObserver] with
     * [RoutesUpdatedResult.reason] equals to [RoutesExtra.ROUTES_UPDATE_REASON_REROUTE].
     *
     * Alternatively, you can pass a [ReplanRoutesCallback] parameter to receive the result of the
     * rerouting for once. Keep in mind that the callback will be called on the main thread.
     *
     * Route options are supposed to be updated by [RerouteOptionsAdapter]. Implement
     * your [RerouteOptionsAdapter] so that it updates all route options
     * which users can modify during active guidance. Set your [RerouteOptionsAdapter]
     * to [MapboxNavigation] using [setRerouteOptionsAdapter].
     *
     * Be aware that route replan could be interrupted by a new reroute, for example the one
     * caused by deviation from the primary route. Because of that, it's important to update all
     * changeable route options on every reroute in [RerouteOptionsAdapter] and be ready to receive
     * [RerouteState.Interrupted] in [RerouteStateObserver].
     *
     * This method does nothing if rerouting is disabled. See [isRerouteEnabled] to check
     * if rerouting is enabled and [setRerouteEnabled] to turn off/on rerouting.
     *
     * @param resultCallback optional [ReplanRoutesCallback] to receive the result of the rerouting.
     */
    @OptIn(ExperimentalMapboxNavigationAPI::class)
    @ExperimentalPreviewMapboxNavigationAPI
    @JvmOverloads
    fun replanRoute(resultCallback: ReplanRoutesCallback? = null) {
        rerouteController?.let { controller ->
            // Interrupt any already existing ongoing rerouting requests to prevent unexpected states.
            if (rerouteController?.state == FetchingRoute) controller.interrupt()

            val rerouteStateObserver = createSingleUseRerouteObserver(resultCallback)
            rerouteStateObserver?.let {
                rerouteController?.registerRerouteStateV2Observer(rerouteStateObserver)
            }

            controller.rerouteOnParametersChange { result ->
                // Unregister the temporary observer if it's available.
                rerouteStateObserver?.let { observer ->
                    rerouteController?.unregisterRerouteStateV2Observer(observer)
                }

                // When the controller gets a new route, handle the result.
                handleReplanResult(result, resultCallback)
            }
        }
    }

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
     * This function is deprecated, use postUserFeedback(UserFeedback) instead.
     *
     * Send user feedback about an issue or problem with the Navigation SDK.
     *
     * @param feedbackType one of [FeedbackEvent.Type] or a custom one
     * @param description description message
     * @param feedbackSource source of feedback. Deprecated, will be ignored
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
    @Deprecated("This function is deprecated", ReplaceWith("postUserFeedback(UserFeedback)"))
    fun postUserFeedback(
        feedbackType: String,
        description: String,
        @FeedbackEvent.Source feedbackSource: String,
        screenshot: String,
        feedbackSubType: Array<String>? = emptyArray(),
    ) {
        postUserFeedbackInternal(
            feedbackType,
            description,
            screenshot,
            feedbackSubType,
            feedbackMetadata = null,
        )
    }

    /**
     * This function is deprecated, use postUserFeedback(UserFeedback) instead.
     *
     * Send user feedback about an issue or problem with the Navigation SDK.
     *
     * Method can be invoked out of the trip session (whenever until [onDestroy] is called), because
     * a feedback is attached to passed location and time in the past when [FeedbackMetadata] was
     * generated (see [provideFeedbackMetadataWrapper]).
     *
     * @param feedbackType one of [FeedbackEvent.Type] or a custom one
     * @param description description message
     * @param feedbackSource source of feedback. Deprecated, will be ignored
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
    @Deprecated("This function is deprecated", ReplaceWith("postUserFeedback(UserFeedback)"))
    fun postUserFeedback(
        feedbackType: String,
        description: String,
        @FeedbackEvent.Source feedbackSource: String,
        screenshot: String,
        feedbackSubType: Array<String>? = emptyArray(),
        feedbackMetadata: FeedbackMetadata,
    ) {
        postUserFeedbackInternal(
            feedbackType,
            description,
            screenshot,
            feedbackSubType,
            feedbackMetadata,
        )
    }

    /**
     * Send user feedback about an issue or problem with the Navigation SDK.
     *
     * Method can be invoked out of the trip session (whenever until [onDestroy] is called), because
     * a feedback is attached to passed location and time in the past when [FeedbackMetadata] was
     * generated (see [provideFeedbackMetadataWrapper]).
     *
     * @param userFeedback feedback data
     */
    @ExperimentalPreviewMapboxNavigationAPI
    fun postUserFeedback(userFeedback: UserFeedback) {
        navigationTelemetry.postUserFeedback(userFeedback, null)
    }

    @ExperimentalPreviewMapboxNavigationAPI
    @JvmSynthetic
    internal fun postUserFeedbackInternal(
        userFeedback: UserFeedback,
        callback: (ExtendedUserFeedback) -> Unit,
    ) {
        navigationTelemetry.postUserFeedback(userFeedback, callback)
    }

    @ExperimentalPreviewMapboxNavigationAPI
    @JvmSynthetic
    internal fun postUserFeedbackInternal(
        feedbackType: String,
        description: String,
        screenshot: String,
        feedbackSubType: Array<String>?,
        feedbackMetadata: FeedbackMetadata?,
    ) {
        val userFeedback = UserFeedback.Builder(
            feedbackType = feedbackType,
            description = description,
        )
            .feedbackSubTypes(feedbackSubType?.toList() ?: emptyList())
            .feedbackMetadata(feedbackMetadata)
            .screenshot(screenshot)
            .build()

        postUserFeedback(userFeedback)
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
    fun provideFeedbackMetadataWrapper(): FeedbackMetadataWrapper {
        return navigationTelemetry.provideFeedbackMetadataWrapper()
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
        navigationRoutes: List<NavigationRoute>,
    ): List<AlternativeRouteMetadata> {
        return navigationRoutes.mapNotNull { getAlternativeMetadataFor(it) }
    }

    /**
     * Enables/disabled continuous alternatives automatic update. Enabled by default.
     * When enabled, [MapboxNavigation] automatically:
     * 1. Removes passed alternatives
     * 2. Updates available alternatives
     * 3. Switches to an online alternative when internet connection is restored
     * @param enabled defines if continuous alternatives should be automatically updated.
     */
    fun setContinuousAlternativesEnabled(enabled: Boolean) {
        val listener = if (enabled) {
            ::updateRoutes
        } else {
            null
        }
        routeAlternativesController.setRouteUpdateSuggestionListener(listener)
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
        navigationSessionStateObserver: NavigationSessionStateObserver,
    ) {
        navigationSession.registerNavigationSessionStateObserver(navigationSessionStateObserver)
    }

    /**
     * Unregisters a [NavigationSessionStateObserver].
     *
     * @param navigationSessionStateObserver NavigationSessionStateObserver
     */
    fun unregisterNavigationSessionStateObserver(
        navigationSessionStateObserver: NavigationSessionStateObserver,
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
        developerMetadataObserver: DeveloperMetadataObserver,
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
        developerMetadataObserver: DeveloperMetadataObserver,
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
        navigator.onEVDataUpdated(evDynamicDataHolder.currentData(emptyMap()))
    }

    /**
     * Retrieves current road graph version information. The retrieval process waits for the
     * tiles config to resolve.
     *
     * @param timeoutSeconds Request timeout in seconds, pass null for the infinite timeout.
     * @param callback The callback to call when result request finished,
     * either with success or error, or when timeout passed.
     */
    @ExperimentalPreviewMapboxNavigationAPI
    @JvmOverloads
    fun getRoadGraphVersionInfo(
        // TODO: remove parameter: https://mapbox.atlassian.net/browse/NAVAND-6910
        timeoutSeconds: Int? = null,
        callback: RoadGraphVersionInfoCallback,
    ) {
        navigator.cache.getCurrentRoadGraphVersionInfo(
            { isVersionResolved, currentVersionInfo ->
                if (!isVersionResolved || currentVersionInfo == null) {
                    callback.onError(isTimeoutError = !isVersionResolved)
                } else {
                    callback.onVersionInfo(
                        RoadGraphVersionInfoCallback.VersionInfo.createFromNative(
                            currentVersionInfo,
                        ),
                    )
                }
            },
        )
    }

    /**
     * Updates the testing context and writes "SetTestingContext" event to history if it is changed.
     *
     * The [TestingContext] structure is designed to provide runtime information
     * about the test vehicle and project for testing purposes.
     * This structure is intended strictly for testing and debugging purposes.
     *
     * @param testingContext Setting this value to empty optional clears the current testing context
     * and records an "empty" "SetTestingContext" event in the history.
     */
    @ExperimentalPreviewMapboxNavigationAPI
    fun setTestingContext(testingContext: TestingContext) {
        val nativeTestingContext = toNativeObject(testingContext)
        navigator.setTestingContext(nativeTestingContext)
    }

    /**
     * Allows other Nav SDK modules observe the latest EV state which is
     * accumulated based on data provided in [onEVDataUpdated].
     * Route options of the current route isn't taken into account in
     * [internalEvUpdatedData], only data provided in [onEVDataUpdated].
     */
    internal fun internalEvUpdatedData(): StateFlow<Map<String, String>> {
        return evDynamicDataHolder.updatedRawData()
    }

    internal fun registerOnRoutesSetStartedObserver(observer: SetNavigationRoutesStartedObserver) {
        directionsSession.registerSetNavigationRoutesStartedObserver(observer)
    }

    internal fun unregisterOnRoutesSetStartedObserver(
        observer: SetNavigationRoutesStartedObserver,
    ) {
        directionsSession.unregisterSetNavigationRoutesStartedObserver(observer)
    }

    private fun createHistoryRecorderHandles(
        config: ConfigHandle,
    ) = PerformanceTracker.trackPerformanceSync("createHistoryRecorderHandles") {
        NavigatorLoader.createHistoryRecorderHandles(
            config,
            historyRecorder.fileDirectory(),
            copilotHistoryRecorder.copilotFileDirectory(),
            sdkInformation = SdkInfoProvider.sdkInformation(),
        )
    }

    private fun startSession(withTripService: Boolean, withReplayEnabled: Boolean) {
        runIfNotDestroyed {
            val previousState = tripSession.getState()
            navigator.resume()
            tripSession.start(
                withTripService = withTripService,
                withReplayEnabled = withReplayEnabled,
            )
            routeAlternativesController.resume()
            routeRefreshController.resumeRouteRefreshes()
            // It's possible that we are in a state when routes are set,
            // but session is not started. In this case, as soon as routes are set,
            // NN starts generating status updates (which results in our route progress updates,
            // voice and banner instructions events, etc.).
            // If we first set the routes and then start the session, we might lose all the events
            // between these two actions (because native status observer is only registered
            // when the session is started). To avoid that,
            // we set the routes again on session start. Then NN will generate all the
            // relevant events again, because it will treat the routes as completely new.
            if (previousState == TripSessionState.STOPPED) {
                resetTripSessionRoutes()
            }
            notificationChannelField?.let {
                monitorNotificationActionButton(it.get(null) as ReceiveChannel<NotificationAction>)
            }
        }
    }

    private fun createInternalRoutesObserver() = RoutesObserver { _ ->
        latestLegIndex = null
        routesProgressDataProvider.onNewRoutes()
    }

    private fun createInternalOffRouteObserver() = OffRouteObserver { offRoute ->
        if (offRoute) {
            rerouteOnDeviation()
        }
    }

    private fun createInternalFallbackVersionsObserver() = object : FallbackVersionsObserver {
        override fun onFallbackVersionsFound(versions: List<String>) {
            logI(
                "FallbackVersionsObserver.onFallbackVersionsFound called with versions = $versions",
                LOG_CATEGORY,
            )
            if (versions.isNotEmpty()) {
                // the last version in the list is the latest one
                val tilesVersion = versions.last()
                recreateNavigatorInstance(isFallback = true, tilesVersion = tilesVersion)
                navigationVersionSwitchObservers.forEach {
                    it.onSwitchToFallbackVersion(tilesVersion)
                }
            }
        }

        override fun onCanReturnToLatest(version: String) {
            logI(
                "FallbackVersionsObserver.onCanReturnToLatest called with version = $version",
                LOG_CATEGORY,
            )
            recreateNavigatorInstance(
                isFallback = false,
                tilesVersion = navigationOptions.routingTilesOptions.tilesVersion,
            )
            navigationVersionSwitchObservers.forEach { observer ->
                observer.onSwitchToTargetVersion(
                    navigationOptions.routingTilesOptions.tilesVersion.takeIf { it.isNotEmpty() },
                )
            }
        }
    }

    private fun recreateNavigatorInstance(isFallback: Boolean, tilesVersion: String) {
        logD(
            "recreateNavigatorInstance(). " +
                "isFallback = $isFallback, tilesVersion = $tilesVersion",
            LOG_CATEGORY,
        )

        mainJobController.scope.launch {
            navigator.recreate(createTilesConfig(isFallback, tilesVersion))

            routerWrapper.resetRouter(navigator.getRouter())

            etcGateAPI.experimental = navigator.experimental

            val routes = directionsSession.routes
            if (routes.isNotEmpty()) {
                navigator.setRoutes(
                    primaryRoute = routes[0],
                    startingLeg = tripSession.getRouteProgress()?.currentLegProgress?.legIndex ?: 0,
                    alternatives = routes.drop(1),
                    when (isFallback) {
                        true -> SetRoutesReason.FALLBACK_TO_OFFLINE
                        false -> SetRoutesReason.RESTORE_TO_ONLINE
                    },
                )
            }
        }
    }

    private fun rerouteOnDeviation() {
        rerouteController?.rerouteOnDeviation { result: RerouteResult ->
            logI(LOG_CATEGORY) {
                "Reroute on deviation: $result, " +
                    "tripSession.isOffRoute = ${tripSession.isOffRoute}"
            }
            if (tripSession.isOffRoute) {
                internalSetNavigationRoutes(
                    result.routes,
                    SetRoutes.Reroute(result.initialLegIndex),
                )
                true
            } else {
                false
            }
        }
    }

    private fun monitorNotificationActionButton(channel: ReceiveChannel<NotificationAction>) {
        mainJobController.scope.monitorChannelWithException(
            channel,
            { notificationAction ->
                when (notificationAction) {
                    NotificationAction.END_NAVIGATION -> tripSession.stop()
                }
            },
        )
    }

    @OptIn(ExperimentalMapboxNavigationAPI::class)
    private fun createTilesConfig(
        isFallback: Boolean,
        tilesVersion: String,
    ): TilesConfig {
        val tilesPath = RoutingTilesFiles(navigationOptions.applicationContext)
            .absolutePath(navigationOptions.routingTilesOptions)

        val sdEndpointConfiguration = navigationOptions.routingTilesOptions.let {
            TileEndpointConfiguration(
                it.tilesBaseUri.toString(),
                createDataset(it.tilesDataset, it.tilesProfile),
                tilesVersion,
                isFallback,
                it.tilesVersion,
                it.minDaysBetweenServerAndLocalTilesVersion,
            )
        }

        val hdEndpointConfiguration = navigationOptions.routingTilesOptions.hdTilesOptions?.let {
            TileEndpointConfiguration(
                it.tilesBaseUri.toString(),
                createDataset(it.tilesDataset, it.tilesProfile),
                it.tilesVersion,
                false,
                it.tilesVersion,
                it.minDaysBetweenServerAndLocalTilesVersion,
            )
        }

        return TilesConfig(
            /* tilesPath = */ tilesPath,
            /* tileStore = */ NavigationTileStoreOwner(),
            /* inMemoryTileCache = */ null,
            /* onDiskTileCache = */ null,
            /* endpointConfig = */ sdEndpointConfiguration,
            /* hdEndpointConfig = */ hdEndpointConfiguration,
        )
    }

    private fun createDataset(dataset: String, profile: String): String {
        val prefixedProfile = if (profile.isEmpty()) "" else "/$profile"
        return "$dataset$prefixedProfile"
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
                    """.trimIndent(),
                )
            }
        }
    }

    @OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
    private fun setUpRouteCacheClearer() {
        registerRoutesObserver(routesCacheClearer)
        registerRoutesPreviewObserver(routesCacheClearer)
    }

    private suspend fun prepareNavigationForRoutesParsing() {
        withContext(Dispatchers.Main.immediate) {
            if (directionsSession.routesPlusIgnored.size > 1) {
                suspendCoroutine<Unit> { continuation ->
                    setNavigationRoutes(directionsSession.routes.take(1), currentLegIndex()) {
                        continuation.resume(Unit)
                    }
                }
            }
            val preview = getRoutesPreview()
            if (preview != null) {
                if (preview.routesList.size > 1) {
                    suspendCoroutine { continuation ->
                        routesPreviewController.previewNavigationRoutes(
                            listOf(preview.originalRoutesList[preview.primaryRouteIndex]),
                        ) {
                            continuation.resume(Unit)
                        }
                    }
                }
            }
        }
    }

    private fun updateRoutes(suggestion: UpdateRouteSuggestion) {
        setNavigationRoutes(suggestion.newRoutes)
    }

    private fun handleReplanResult(result: RerouteResult, callback: ReplanRoutesCallback?) {
        internalSetNavigationRoutes(
            routes = result.routes,
            setRoutesInfo = SetRoutes.Reroute(result.initialLegIndex),
        ) { routesSetResult ->
            if (callback == null) return@internalSetNavigationRoutes

            routesSetResult.onValue {
                logI(LOG_CATEGORY) {
                    "Updating the caller of the new routes using the passed callback instance."
                }
                callback.onNewRoutes(result.routes, result.origin)
            }.onError { error ->
                logE(LOG_CATEGORY) {
                    "Updating the caller of the error using the passed callback instance."
                }
                callback.onFailure(
                    ReplanRouteError(
                        ReplanRouteError.REPLAN_ROUTE_ERROR,
                        error.message,
                    ),
                )
            }
        }
    }

    @OptIn(ExperimentalMapboxNavigationAPI::class)
    private fun createSingleUseRerouteObserver(
        callback: ReplanRoutesCallback?,
    ): RerouteController.RerouteStateV2Observer? {
        if (isRerouteEnabled().not() || callback == null) return null

        return object : RerouteController.RerouteStateV2Observer {
            override fun onRerouteStateChanged(rerouteState: RerouteStateV2) {
                when (rerouteState) {
                    is RerouteStateV2.Interrupted -> {
                        // If the reroute is interrupted, notify the callback and unregister.
                        rerouteController?.unregisterRerouteStateV2Observer(this)
                        logI(LOG_CATEGORY) {
                            "Updating the caller of the interruption using the passed callback" +
                                " instance."
                        }
                        callback.onFailure(
                            ReplanRouteError(
                                ReplanRouteError.REPLAN_ROUTE_INTERRUPTED,
                                "Reroute was interrupted.",
                            ),
                        )
                    }

                    is RerouteStateV2.Failed -> {
                        // If the reroute fails, notify the callback and unregister.
                        rerouteController?.unregisterRerouteStateV2Observer(this)
                        logE(LOG_CATEGORY) {
                            "Updating the caller of the error using the passed callback instance."
                        }
                        callback.onFailure(
                            ReplanRouteError(
                                ReplanRouteError.REPLAN_ROUTE_ERROR,
                                rerouteState.message,
                            ),
                        )
                    }

                    is RerouteStateV2.FetchingRoute,
                    is RerouteStateV2.Idle,
                    is RerouteStateV2.RouteFetched,
                    is RerouteStateV2.Deviation.RouteIgnored,
                    is RerouteStateV2.Deviation.ApplyingRoute,
                    -> {
                        // No-op as these states are handled elsewhere.
                    }
                    else -> {
                        logW(LOG_CATEGORY) { "Unexpected state: $rerouteState" }
                    }
                }
            }
        }
    }

    private companion object {

        @Volatile
        private var hasInstance = false

        private const val LOG_CATEGORY = "MapboxNavigation"
        private const val ONE_SECOND_IN_MILLIS = 1000.0
    }
}
