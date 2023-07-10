package com.mapbox.navigation.navigator.internal

import com.google.gson.JsonArray
import com.google.gson.JsonParser
import com.mapbox.api.directionsrefresh.v1.models.DirectionsRefreshResponse
import com.mapbox.api.directionsrefresh.v1.models.DirectionsRouteRefresh
import com.mapbox.api.directionsrefresh.v1.models.RouteLegRefresh
import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.common.TileStore
import com.mapbox.common.TilesetDescriptor
import com.mapbox.navigation.base.internal.route.nativeRoute
import com.mapbox.navigation.base.internal.utils.Constants
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.options.PredictiveCacheLocationOptions
import com.mapbox.navigation.base.options.RoutingTilesOptions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.utils.internal.ThreadController
import com.mapbox.navigation.utils.internal.logD
import com.mapbox.navigation.utils.internal.logE
import com.mapbox.navigator.CacheDataDomain
import com.mapbox.navigator.CacheHandle
import com.mapbox.navigator.ConfigHandle
import com.mapbox.navigator.ElectronicHorizonObserver
import com.mapbox.navigator.Experimental
import com.mapbox.navigator.FallbackVersionsObserver
import com.mapbox.navigator.FixLocation
import com.mapbox.navigator.GraphAccessor
import com.mapbox.navigator.HistoryRecorderHandle
import com.mapbox.navigator.NavigationStatus
import com.mapbox.navigator.Navigator
import com.mapbox.navigator.NavigatorObserver
import com.mapbox.navigator.PredictiveCacheController
import com.mapbox.navigator.PredictiveCacheControllerOptions
import com.mapbox.navigator.PredictiveLocationTrackerOptions
import com.mapbox.navigator.RefreshRouteResult
import com.mapbox.navigator.RoadObjectMatcher
import com.mapbox.navigator.RoadObjectsStore
import com.mapbox.navigator.RoadObjectsStoreObserver
import com.mapbox.navigator.RouteAlternative
import com.mapbox.navigator.RouteAlternativesControllerInterface
import com.mapbox.navigator.RouterInterface
import com.mapbox.navigator.SetRoutesParams
import com.mapbox.navigator.SetRoutesReason
import com.mapbox.navigator.SetRoutesResult
import com.mapbox.navigator.TilesConfig
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.concurrent.CopyOnWriteArraySet
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

/**
 * Default implementation of [MapboxNativeNavigator] interface.
 */
object MapboxNativeNavigatorImpl : MapboxNativeNavigator {

    private const val LOG_CATEGORY = "MapboxNativeNavigatorImpl"

    // TODO: What should be the default value? Should we expose it publicly?
    private const val MAX_NUMBER_TILES_LOAD_PARALLEL_REQUESTS = 2

    private var navigator: Navigator? = null
    override var graphAccessor: GraphAccessor? = null
    override var roadObjectMatcher: RoadObjectMatcher? = null
    override var roadObjectsStore: RoadObjectsStore? = null
    override lateinit var experimental: Experimental
    override lateinit var cache: CacheHandle
    override lateinit var routeAlternativesController: RouteAlternativesControllerInterface
    private val nativeNavigatorRecreationObservers =
        CopyOnWriteArraySet<NativeNavigatorRecreationObserver>()
    private lateinit var accessToken: String

    // Route following

    /**
     * Create or reset resources. This must be called before calling any
     * functions within [MapboxNativeNavigatorImpl]
     */
    override fun create(
        config: ConfigHandle,
        historyRecorderComposite: HistoryRecorderHandle?,
        tilesConfig: TilesConfig,
        accessToken: String,
        router: RouterInterface?,
    ): MapboxNativeNavigator {
        navigator?.shutdown()

        val nativeComponents = NavigatorLoader.createNavigator(
            config,
            historyRecorderComposite,
            tilesConfig,
            router,
        )
        navigator = nativeComponents.navigator
        graphAccessor = nativeComponents.graphAccessor
        roadObjectMatcher = nativeComponents.roadObjectMatcher
        roadObjectsStore = nativeComponents.navigator.roadObjectStore()
        experimental = nativeComponents.navigator.experimental
        cache = nativeComponents.cache
        routeAlternativesController = nativeComponents.routeAlternativesController
        this.accessToken = accessToken
        return this
    }

    /**
     * Recreate native objects and notify listeners.
     */
    override fun recreate(
        config: ConfigHandle,
        historyRecorderComposite: HistoryRecorderHandle?,
        tilesConfig: TilesConfig,
        accessToken: String,
        router: RouterInterface?
    ) {
        val storeNavSessionState = navigator!!.storeNavigationSession()
        create(config, historyRecorderComposite, tilesConfig, accessToken, router)
        navigator!!.restoreNavigationSession(storeNavSessionState)
        nativeNavigatorRecreationObservers.forEach {
            it.onNativeNavigatorRecreated()
        }
    }

    override suspend fun resetRideSession() = suspendCancellableCoroutine<Unit> {
        navigator!!.reset {
            it.resume(Unit)
        }
    }

    override fun startNavigationSession() {
        navigator!!.startNavigationSession()
    }

    override fun stopNavigationSession() {
        navigator!!.stopNavigationSession()
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
            navigator!!.updateLocation(rawLocation) {
                continuation.resume(it)
            }
        }

    override fun addNavigatorObserver(navigatorObserver: NavigatorObserver) =
        navigator!!.addObserver(navigatorObserver)

    override fun removeNavigatorObserver(navigatorObserver: NavigatorObserver) =
        navigator!!.removeObserver(navigatorObserver)

    // Routing

    override suspend fun setRoutes(
        primaryRoute: NavigationRoute?,
        startingLeg: Int,
        alternatives: List<NavigationRoute>,
        reason: SetRoutesReason,
    ): Expected<String, SetRoutesResult> = suspendCancellableCoroutine { continuation ->
        navigator!!.setRoutes(
            primaryRoute?.let { route ->
                SetRoutesParams(
                    route.nativeRoute(),
                    startingLeg,
                    alternatives.map { it.nativeRoute() }
                )
            },
            reason,
        ) { result ->
            result.onError {
                logE(
                    "Failed to set the primary route with alternatives, " +
                        "active guidance session will not function correctly. Reason: $it",
                    LOG_CATEGORY
                )
            }
            continuation.resume(result)
        }
    }

    override suspend fun setAlternativeRoutes(
        routes: List<NavigationRoute>
    ): List<RouteAlternative> = suspendCancellableCoroutine { continuation ->
        navigator!!.setAlternativeRoutes(
            routes.map { it.nativeRoute() }
        ) { result ->
            result.onError {
                logE(
                    "Failed to set alternative routes, " +
                        "alternatives will be ignored. Reason: $it",
                    LOG_CATEGORY
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
        route: NavigationRoute
    ): Expected<String, List<RouteAlternative>> {
        val refreshedLegs = route.directionsRoute.legs()?.map { routeLeg ->
            RouteLegRefresh.builder()
                .annotation(routeLeg.annotation())
                .incidents(routeLeg.incidents())
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
                        }
                    )
                }
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
                expected: Expected<String, RefreshRouteResult> ->
            expected.fold(
                { error ->
                    logE(
                        "Annotations update failed for route with ID '${route.id}'. " +
                            "Reason: $error",
                        LOG_CATEGORY
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
                        LOG_CATEGORY
                    )
                    continuation.resume(
                        ExpectedFactory.createValue(refreshRouteResult.alternatives)
                    )
                }
            )
        }
        return suspendCancellableCoroutine { continuation ->
            logD(
                "Refreshing native route ${route.nativeRoute().routeId} " +
                    "with generated refresh response: $refreshResponseJson",
                LOG_CATEGORY
            )
            navigator!!.refreshRoute(
                refreshResponseJson,
                route.nativeRoute().routeId
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
            navigator!!.changeLeg(legIndex) {
                continuation.resume(it)
            }
        }

    // EH

    /**
     * Sets the Electronic Horizon observer
     *
     * @param eHorizonObserver
     */
    override fun setElectronicHorizonObserver(eHorizonObserver: ElectronicHorizonObserver?) {
        navigator!!.setElectronicHorizonObserver(eHorizonObserver)
    }

    override fun addRoadObjectsStoreObserver(roadObjectsStoreObserver: RoadObjectsStoreObserver) {
        roadObjectsStore?.addObserver(roadObjectsStoreObserver)
    }

    override fun removeRoadObjectsStoreObserver(
        roadObjectsStoreObserver: RoadObjectsStoreObserver
    ) {
        roadObjectsStore?.removeObserver(roadObjectsStoreObserver)
    }

    override fun setFallbackVersionsObserver(fallbackVersionsObserver: FallbackVersionsObserver?) {
        navigator!!.setFallbackVersionsObserver(fallbackVersionsObserver)
    }

    override fun setNativeNavigatorRecreationObserver(
        nativeNavigatorRecreationObserver: NativeNavigatorRecreationObserver
    ) {
        nativeNavigatorRecreationObservers.add(nativeNavigatorRecreationObserver)
    }

    override fun unregisterAllObservers() {
        navigator!!.setElectronicHorizonObserver(null)
        navigator!!.setFallbackVersionsObserver(null)
        roadObjectsStore?.removeAllCustomRoadObjects()
        nativeNavigatorRecreationObservers.clear()
    }

    /**
     * Creates a Maps [PredictiveCacheController].
     *
     * @param tileStore Maps [TileStore]
     * @param tileVariant Maps tileset
     * @param predictiveCacheLocationOptions [PredictiveCacheLocationOptions]
     *
     * @return [PredictiveCacheController]
     */
    @Deprecated(
        "Use createMapsController(" +
            "mapboxMap, tileStore, tilesetDescriptor, predictiveCacheLocationOptions" +
            ") instead."
    )
    override fun createMapsPredictiveCacheControllerTileVariant(
        tileStore: TileStore,
        tileVariant: String,
        predictiveCacheLocationOptions: PredictiveCacheLocationOptions
    ): PredictiveCacheController =
        navigator!!.createPredictiveCacheController(
            tileStore,
            createDefaultMapsPredictiveCacheControllerOptions(tileVariant),
            predictiveCacheLocationOptions.toPredictiveLocationTrackerOptions()
        )

    /**
     * Creates a Maps [PredictiveCacheController].
     *
     * @param tileStore Maps [TileStore]
     * @param tilesetDescriptor Maps tilesetDescriptor
     * @param predictiveCacheLocationOptions [PredictiveCacheLocationOptions]
     *
     * @return [PredictiveCacheController]
     */
    override fun createMapsPredictiveCacheController(
        tileStore: TileStore,
        tilesetDescriptor: TilesetDescriptor,
        predictiveCacheLocationOptions: PredictiveCacheLocationOptions
    ): PredictiveCacheController =
        navigator!!.createPredictiveCacheController(
            tileStore,
            listOf(tilesetDescriptor),
            predictiveCacheLocationOptions.toPredictiveLocationTrackerOptions()
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
        predictiveCacheLocationOptions: PredictiveCacheLocationOptions
    ): PredictiveCacheController =
        navigator!!.createPredictiveCacheController(
            predictiveCacheLocationOptions.toPredictiveLocationTrackerOptions()
        )

    private fun PredictiveCacheLocationOptions.toPredictiveLocationTrackerOptions() =
        PredictiveLocationTrackerOptions(
            currentLocationRadiusInMeters,
            routeBufferRadiusInMeters,
            destinationLocationRadiusInMeters
        )

    private fun createDefaultMapsPredictiveCacheControllerOptions(tileVariant: String) =
        PredictiveCacheControllerOptions(
            "",
            tileVariant,
            CacheDataDomain.MAPS,
            MAX_NUMBER_TILES_LOAD_PARALLEL_REQUESTS,
            0
        )
}
