package com.mapbox.navigation.navigator.internal

import androidx.annotation.RestrictTo
import com.google.gson.JsonArray
import com.google.gson.JsonParser
import com.mapbox.api.directionsrefresh.v1.models.DirectionsRefreshResponse
import com.mapbox.api.directionsrefresh.v1.models.DirectionsRouteRefresh
import com.mapbox.api.directionsrefresh.v1.models.RouteLegRefresh
import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.common.TileStore
import com.mapbox.common.TilesetDescriptor
import com.mapbox.navigation.base.internal.performance.PerformanceTracker
import com.mapbox.navigation.base.internal.route.nativeRoute
import com.mapbox.navigation.base.internal.utils.Constants
import com.mapbox.navigation.base.internal.utils.Constants.RouteResponse.KEY_NOTIFICATIONS
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.options.PredictiveCacheLocationOptions
import com.mapbox.navigation.base.options.RoutingTilesOptions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.navigator.internal.utils.toEvStateData
import com.mapbox.navigation.utils.internal.ThreadController
import com.mapbox.navigation.utils.internal.logD
import com.mapbox.navigation.utils.internal.logE
import com.mapbox.navigator.ADASISv2MessageCallback
import com.mapbox.navigator.AdasisConfig
import com.mapbox.navigator.AdasisFacadeBuilder
import com.mapbox.navigator.AdasisFacadeHandle
import com.mapbox.navigator.CacheHandle
import com.mapbox.navigator.ConfigHandle
import com.mapbox.navigator.ElectronicHorizonObserver
import com.mapbox.navigator.EventsMetadataInterface
import com.mapbox.navigator.Experimental
import com.mapbox.navigator.FallbackVersionsObserver
import com.mapbox.navigator.FixLocation
import com.mapbox.navigator.GraphAccessor
import com.mapbox.navigator.HistoryRecorderHandle
import com.mapbox.navigator.InputsServiceHandle
import com.mapbox.navigator.NavigationStatus
import com.mapbox.navigator.Navigator
import com.mapbox.navigator.NavigatorObserver
import com.mapbox.navigator.PredictiveCacheController
import com.mapbox.navigator.PredictiveLocationTrackerOptions
import com.mapbox.navigator.RefreshRouteResult
import com.mapbox.navigator.RerouteControllerInterface
import com.mapbox.navigator.RerouteDetectorInterface
import com.mapbox.navigator.RerouteObserver
import com.mapbox.navigator.ResetCallback
import com.mapbox.navigator.RoadObjectMatcher
import com.mapbox.navigator.RoadObjectsStore
import com.mapbox.navigator.RoadObjectsStoreObserver
import com.mapbox.navigator.RouteAlternative
import com.mapbox.navigator.RouteAlternativesControllerInterface
import com.mapbox.navigator.RouterInterface
import com.mapbox.navigator.SensorData
import com.mapbox.navigator.SetRoutesParams
import com.mapbox.navigator.SetRoutesReason
import com.mapbox.navigator.SetRoutesResult
import com.mapbox.navigator.Telemetry
import com.mapbox.navigator.TilesConfig
import com.mapbox.navigator.UpdateExternalSensorDataCallback
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.concurrent.CopyOnWriteArraySet
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

/**
 * Default implementation of [MapboxNativeNavigator] interface.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
class MapboxNativeNavigatorImpl(
    tilesConfig: TilesConfig,
    private val historyRecorderComposite: HistoryRecorderHandle?,
    private val offlineCacheHandle: CacheHandle?,
    override val config: ConfigHandle,
    override val eventsMetadataProvider: EventsMetadataInterface,
) : MapboxNativeNavigator {

    override lateinit var navigator: Navigator
    override lateinit var graphAccessor: GraphAccessor
    override lateinit var roadObjectMatcher: RoadObjectMatcher
    override lateinit var roadObjectsStore: RoadObjectsStore
    override lateinit var experimental: Experimental
    override lateinit var cache: CacheHandle
    override lateinit var routeAlternativesController: RouteAlternativesControllerInterface
    override lateinit var telemetry: Telemetry

    private lateinit var adasisFacade: AdasisFacadeHandle

    override val inputsService: InputsServiceHandle =
        NavigatorLoader.createInputService(config, historyRecorderComposite)

    private val nativeNavigatorRecreationObservers =
        CopyOnWriteArraySet<NativeNavigatorRecreationObserver>()

    init {
        init(tilesConfig)
    }

    private fun init(tilesConfig: TilesConfig) {
        val sectionName = "MapboxNativeNavigatorImpl#init-"
        val cacheHandle = PerformanceTracker.trackPerformance("${sectionName}cacheHandle") {
            NavigatorLoader.createCacheHandle(
                config,
                tilesConfig,
                historyRecorderComposite,
            )
        }

        adasisFacade = PerformanceTracker.trackPerformance("${sectionName}adasisFacade") {
            AdasisFacadeBuilder.build(config, cacheHandle, historyRecorderComposite)
        }

        navigator = PerformanceTracker.trackPerformance("${sectionName}navigator") {
            NavigatorLoader.createNavigator(
                cacheHandle,
                config,
                historyRecorderComposite,
                offlineCacheHandle,
                inputsService,
                adasisFacade,
            )
        }

        cache = cacheHandle
        graphAccessor = PerformanceTracker.trackPerformance("${sectionName}graphAccessor") {
            GraphAccessor(cacheHandle)
        }
        roadObjectMatcher = PerformanceTracker.trackPerformance("${sectionName}roadObjectMatcher") {
            RoadObjectMatcher(cacheHandle)
        }
        roadObjectsStore = PerformanceTracker.trackPerformance("${sectionName}roadObjectsStore") {
            navigator.roadObjectStore()
        }
        experimental = PerformanceTracker.trackPerformance("${sectionName}experimental") {
            navigator.experimental
        }
        routeAlternativesController = PerformanceTracker.trackPerformance(
            "${sectionName}routeAlternativesController",
        ) {
            navigator.routeAlternativesController
        }
        telemetry = PerformanceTracker.trackPerformance("${sectionName}telemetry") {
            navigator.getTelemetry(eventsMetadataProvider)
        }
    }

    /**
     * Recreate native objects and notify listeners.
     */
    override fun recreate(tilesConfig: TilesConfig) {
        val storeNavSessionState = navigator.storeNavigationSession()

        unregisterAllNativeNavigatorObservers()
        navigator.shutdown()

        init(tilesConfig)

        navigator.restoreNavigationSession(storeNavSessionState)
        nativeNavigatorRecreationObservers.forEach {
            it.onNativeNavigatorRecreated()
        }
    }

    /**
     * Get router
     */
    override fun getRouter(): RouterInterface {
        return navigator.router
    }

    override fun getRerouteDetector(): RerouteDetectorInterface? {
        return navigator.rerouteDetector
    }

    override fun getRerouteController(): RerouteControllerInterface? {
        return navigator.rerouteController
    }

    override suspend fun resetRideSession() = suspendCancellableCoroutine<Unit> {
        navigator.reset {
            it.resume(Unit)
        }
    }

    override fun startNavigationSession() {
        navigator.startNavigationSession()
    }

    override fun stopNavigationSession() {
        navigator.stopNavigationSession()
    }

    /**
     * Passes in the current raw location of the user.
     *
     * @param rawLocation The current raw [FixLocation] of user.
     *
     * @return true if the raw location was usable, false if not.
     */
    override suspend fun updateLocation(rawLocation: FixLocation): Boolean =
        suspendCancellableCoroutine { continuation ->
            navigator.updateLocation(rawLocation) {
                continuation.resume(it)
            }
        }

    private val currentNavigatorObservers = mutableListOf<NavigatorObserver>()
    override fun addNavigatorObserver(navigatorObserver: NavigatorObserver) {
        currentNavigatorObservers.add(navigatorObserver)
        navigator.addObserver(navigatorObserver)
    }

    override fun removeNavigatorObserver(navigatorObserver: NavigatorObserver) {
        navigator.removeObserver(navigatorObserver)
        currentNavigatorObservers.remove(navigatorObserver)
    }

    // Routing

    override suspend fun setRoutes(
        primaryRoute: NavigationRoute?,
        startingLeg: Int,
        alternatives: List<NavigationRoute>,
        reason: SetRoutesReason,
    ): Expected<String, SetRoutesResult> = suspendCancellableCoroutine { continuation ->
        navigator.setRoutes(
            primaryRoute?.let { route ->
                SetRoutesParams(
                    route.nativeRoute(),
                    startingLeg,
                    alternatives.map { it.nativeRoute() },
                )
            },
            reason,
        ) { result ->
            result.onError {
                logE(
                    "Failed to set the primary route with alternatives, " +
                        "active guidance session will not function correctly. Reason: $it",
                    LOG_CATEGORY,
                )
            }
            continuation.resume(result)
        }
    }

    override suspend fun setAlternativeRoutes(
        routes: List<NavigationRoute>,
    ): List<RouteAlternative> = suspendCancellableCoroutine { continuation ->
        navigator.setAlternativeRoutes(
            routes.map { it.nativeRoute() },
        ) { result ->
            result.onError {
                logE(
                    "Failed to set alternative routes, " +
                        "alternatives will be ignored. Reason: $it",
                    LOG_CATEGORY,
                )
            }
            continuation.resume(result.value ?: emptyList())
        }
    }

    /**
     * Updates annotations so that subsequent calls to getStatus will
     * reflect the most current annotations for the route.
     *
     * This methods manufactures a [DirectionsRefreshResponse] to adhere to requirements from
     * https://github.com/mapbox/mapbox-navigation-native/pull/5420 where the full response has to be provided
     * to [Navigator.refreshRoute], not only the annotations/incidents collections.
     */
    override suspend fun refreshRoute(
        route: NavigationRoute,
    ): Expected<String, List<RouteAlternative>> {
        val refreshedLegs = route.directionsRoute.legs()?.map { routeLeg ->
            val notifications = routeLeg.unrecognizedJsonProperties?.get(KEY_NOTIFICATIONS)
            RouteLegRefresh.builder()
                .annotation(routeLeg.annotation())
                .incidents(routeLeg.incidents())
                .unrecognizedJsonProperties(notifications?.let { mapOf(KEY_NOTIFICATIONS to it) })
                .build()
        }
        val refreshedWaypoints = route.waypoints
        val refreshRoute = DirectionsRouteRefresh.builder()
            .legs(refreshedLegs)
            .unrecognizedJsonProperties(
                refreshedWaypoints?.let { waypoints ->
                    mapOf(
                        Constants.RouteResponse.KEY_WAYPOINTS to JsonArray().apply {
                            waypoints.forEach { waypoint ->
                                add(JsonParser.parseString(waypoint.toJson()))
                            }
                        },
                    )
                },
            )
            .build()
        val refreshResponse = DirectionsRefreshResponse.builder()
            .code("200")
            .route(refreshRoute)
            .build()

        val refreshResponseJson = withContext(ThreadController.DefaultDispatcher) {
            refreshResponse.toJson()
        }

        val callback = {
                continuation: Continuation<Expected<String, List<RouteAlternative>>>,
                expected: Expected<String, RefreshRouteResult>, ->
            expected.fold(
                { error ->
                    logE(
                        "Annotations update failed for route with ID '${route.id}'. " +
                            "Reason: $error",
                        LOG_CATEGORY,
                    )
                    continuation.resume(ExpectedFactory.createError(error))
                },
                { refreshRouteResult ->
                    logD(
                        "Annotations updated successfully " +
                            "for route with ID: '${refreshRouteResult.route.routeId}'. " +
                            "Alternatives IDs: " +
                            refreshRouteResult.alternatives
                                .joinToString { it.id.toString() }
                                .ifBlank { "[no alternatives]" },
                        LOG_CATEGORY,
                    )
                    continuation.resume(
                        ExpectedFactory.createValue(refreshRouteResult.alternatives),
                    )
                },
            )
        }
        return suspendCancellableCoroutine { continuation ->
            logD(
                "Refreshing native route ${route.nativeRoute().routeId} " +
                    "with generated refresh response: $refreshResponseJson",
                LOG_CATEGORY,
            )
            navigator.refreshRoute(
                refreshResponseJson,
                route.nativeRoute().routeId,
            ) { callback(continuation, it) }
        }
    }

    /**
     * Follows a new leg of the already loaded directions.
     * Returns an initialized navigation status if no errors occurred
     * otherwise, it returns an invalid navigation status state.
     *
     * @param legIndex new leg index
     *
     * @return an initialized [NavigationStatus] if no errors, invalid otherwise
     */
    override suspend fun updateLegIndex(legIndex: Int): Boolean =
        suspendCancellableCoroutine { continuation ->
            navigator.changeLeg(legIndex) {
                continuation.resume(it)
            }
        }

    // EV

    override fun onEVDataUpdated(data: Map<String, String>) {
        navigator.onEvDataUpdated(data.toEvStateData())
    }

    // EH

    /**
     * Sets the Electronic Horizon observer
     *
     * @param eHorizonObserver
     */
    override fun setElectronicHorizonObserver(eHorizonObserver: ElectronicHorizonObserver?) {
        navigator.setElectronicHorizonObserver(eHorizonObserver)
    }

    override fun addRoadObjectsStoreObserver(roadObjectsStoreObserver: RoadObjectsStoreObserver) {
        roadObjectsStore.addObserver(roadObjectsStoreObserver)
    }

    override fun removeRoadObjectsStoreObserver(
        roadObjectsStoreObserver: RoadObjectsStoreObserver,
    ) {
        roadObjectsStore.removeObserver(roadObjectsStoreObserver)
    }

    override fun setFallbackVersionsObserver(fallbackVersionsObserver: FallbackVersionsObserver?) {
        navigator.setFallbackVersionsObserver(fallbackVersionsObserver)
    }

    override fun setNativeNavigatorRecreationObserver(
        nativeNavigatorRecreationObserver: NativeNavigatorRecreationObserver,
    ) {
        nativeNavigatorRecreationObservers.add(nativeNavigatorRecreationObserver)
    }

    private fun unregisterAllNativeNavigatorObservers() {
        navigator.setElectronicHorizonObserver(null)
        navigator.setFallbackVersionsObserver(null)
        currentNavigatorObservers.forEach {
            navigator.removeObserver(it)
        }
        currentNavigatorObservers.clear()
    }

    override fun unregisterAllObservers() {
        unregisterAllNativeNavigatorObservers()
        roadObjectsStore.removeAllCustomRoadObjects()
        nativeNavigatorRecreationObservers.clear()
    }

    override fun pause() {
        navigator.pause()
    }

    override fun resume() {
        navigator.resume()
    }

    override fun shutdown() {
        navigator.shutdown()
    }

    /**
     * Creates a Maps [PredictiveCacheController].
     *
     * @param tileStore Maps [TileStore]
     * @param tilesetDescriptor Maps tilesetDescriptor [TilesetDescriptor]
     * @param predictiveCacheLocationOptions [PredictiveCacheLocationOptions]
     *
     * @return [PredictiveCacheController]
     */
    override fun createMapsPredictiveCacheController(
        tileStore: TileStore,
        tilesetDescriptor: TilesetDescriptor,
        predictiveCacheLocationOptions: PredictiveCacheLocationOptions,
    ): PredictiveCacheController =
        navigator.createPredictiveCacheController(
            tileStore,
            listOf(tilesetDescriptor),
            predictiveCacheLocationOptions.toPredictiveLocationTrackerOptions(),
        )

    /**
     * Creates a search [PredictiveCacheController].
     *
     * @param tileStore Maps [TileStore]
     * @param searchTilesetDescriptor Search tilesetDescriptor [TilesetDescriptor]
     * @param predictiveCacheLocationOptions [PredictiveCacheLocationOptions]
     *
     * @return [PredictiveCacheController]
     */
    override fun createSearchPredictiveCacheController(
        tileStore: TileStore,
        searchTilesetDescriptor: TilesetDescriptor,
        predictiveCacheLocationOptions: PredictiveCacheLocationOptions,
    ): PredictiveCacheController =
        navigator.createPredictiveCacheController(
            tileStore,
            listOf(searchTilesetDescriptor),
            predictiveCacheLocationOptions.toPredictiveLocationTrackerOptions(),
        )

    /**
     * Creates a Navigation [PredictiveCacheController]. Uses the option passed in
     * [RoutingTilesOptions] via [NavigationOptions].
     *
     * @param predictiveCacheLocationOptions [PredictiveCacheLocationOptions]
     *
     * @return [PredictiveCacheController]
     */
    override fun createNavigationPredictiveCacheController(
        predictiveCacheLocationOptions: PredictiveCacheLocationOptions,
    ): PredictiveCacheController =
        navigator.createPredictiveCacheController(
            predictiveCacheLocationOptions.toPredictiveLocationTrackerOptions(),
        )

    private fun PredictiveCacheLocationOptions.toPredictiveLocationTrackerOptions() =
        PredictiveLocationTrackerOptions(
            currentLocationRadiusInMeters,
            routeBufferRadiusInMeters,
            destinationLocationRadiusInMeters,
        )

    override fun updateExternalSensorData(
        data: SensorData,
        callback: UpdateExternalSensorDataCallback,
    ) {
        navigator.updateExternalSensorData(data, callback)
    }

    override fun setAdasisMessageCallback(
        callback: ADASISv2MessageCallback,
        adasisConfig: AdasisConfig,
    ) {
        adasisFacade.setAdasisMessageCallback(callback, adasisConfig)
    }

    override fun resetAdasisMessageCallback() {
        adasisFacade.resetAdasisMessageCallback()
    }

    override fun triggerResetOfEhProvider() {
        adasisFacade.triggerResetOfEhProvider()
    }

    override fun setUserLanguages(languages: List<String>) {
        navigator.config().mutableSettings().setUserLanguages(languages)
    }

    override fun reset(callback: ResetCallback?) {
        navigator.reset(callback)
    }

    override fun addRerouteObserver(nativeRerouteObserver: RerouteObserver) {
        navigator.addRerouteObserver(nativeRerouteObserver)
    }

    override fun removeRerouteObserver(nativeRerouteObserver: RerouteObserver) {
        navigator.removeRerouteObserver(nativeRerouteObserver)
    }

    private companion object {
        const val LOG_CATEGORY = "MapboxNativeNavigatorImpl"
    }
}
