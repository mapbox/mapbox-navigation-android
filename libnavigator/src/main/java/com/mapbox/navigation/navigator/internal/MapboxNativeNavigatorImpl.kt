package com.mapbox.navigation.navigator.internal

import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.DirectionsWaypoint
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.base.common.logger.Logger
import com.mapbox.base.common.logger.model.Message
import com.mapbox.base.common.logger.model.Tag
import com.mapbox.bindgen.Expected
import com.mapbox.common.TileStore
import com.mapbox.navigation.base.options.DeviceProfile
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.options.PredictiveCacheLocationOptions
import com.mapbox.navigation.base.options.RoutingTilesOptions
import com.mapbox.navigator.BannerInstruction
import com.mapbox.navigator.CacheDataDomain
import com.mapbox.navigator.CacheHandle
import com.mapbox.navigator.ElectronicHorizonObserver
import com.mapbox.navigator.Experimental
import com.mapbox.navigator.FallbackVersionsObserver
import com.mapbox.navigator.FixLocation
import com.mapbox.navigator.GraphAccessor
import com.mapbox.navigator.HistoryRecorderHandle
import com.mapbox.navigator.NavigationStatus
import com.mapbox.navigator.Navigator
import com.mapbox.navigator.NavigatorConfig
import com.mapbox.navigator.NavigatorObserver
import com.mapbox.navigator.PredictiveCacheController
import com.mapbox.navigator.PredictiveCacheControllerOptions
import com.mapbox.navigator.PredictiveLocationTrackerOptions
import com.mapbox.navigator.RoadObjectMatcher
import com.mapbox.navigator.RoadObjectsStore
import com.mapbox.navigator.RoadObjectsStoreObserver
import com.mapbox.navigator.RouteAlternativesControllerInterface
import com.mapbox.navigator.RouteInfo
import com.mapbox.navigator.Router
import com.mapbox.navigator.RouterError
import com.mapbox.navigator.SensorData
import com.mapbox.navigator.TilesConfig
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.Executors
import kotlin.coroutines.resume

/**
 * Default implementation of [MapboxNativeNavigator] interface.
 */
object MapboxNativeNavigatorImpl : MapboxNativeNavigator {

    const val PRIMARY_ROUTE_INDEX = 0

    private const val SINGLE_THREAD = 1
    private const val TAG = "MbxNativeNavigatorImpl"

    // TODO: What should be the default value? Should we expose it publicly?
    private const val MAX_NUMBER_TILES_LOAD_PARALLEL_REQUESTS = 2

    private val NavigatorDispatcher: CoroutineDispatcher =
        Executors.newFixedThreadPool(SINGLE_THREAD).asCoroutineDispatcher()
    private var navigator: Navigator? = null
    // TODO migrate to RouterInterface
    private var nativeRouter: Router? = null
    private var historyRecorderHandle: HistoryRecorderHandle? = null
    override var graphAccessor: GraphAccessor? = null
    override var roadObjectMatcher: RoadObjectMatcher? = null
    override var roadObjectsStore: RoadObjectsStore? = null
    override lateinit var experimental: Experimental
    override lateinit var cache: CacheHandle
    private var logger: Logger? = null
    private val nativeNavigatorRecreationObservers =
        CopyOnWriteArraySet<NativeNavigatorRecreationObserver>()
    private lateinit var accessToken: String

    // todo move to native
    const val OFFLINE_UUID = "offline"

    // Route following

    /**
     * Create or reset resources. This must be called before calling any
     * functions within [MapboxNativeNavigatorImpl]
     */
    override fun create(
        deviceProfile: DeviceProfile,
        navigatorConfig: NavigatorConfig,
        tilesConfig: TilesConfig,
        historyDir: String?,
        logger: Logger,
        accessToken: String,
    ): MapboxNativeNavigator {
        navigator?.shutdown()

        val nativeComponents = NavigatorLoader.createNavigator(
            deviceProfile,
            navigatorConfig,
            tilesConfig,
            historyDir,
        )
        navigator = nativeComponents.navigator
        nativeRouter = nativeComponents.nativeRouter
        historyRecorderHandle = nativeComponents.historyRecorderHandle
        graphAccessor = nativeComponents.graphAccessor
        roadObjectMatcher = nativeComponents.roadObjectMatcher
        roadObjectsStore = nativeComponents.navigator.roadObjectStore()
        experimental = nativeComponents.navigator.experimental
        cache = nativeComponents.cache
        this.logger = logger
        this.accessToken = accessToken
        return this
    }

    /**
     * Recreate native objects and notify listeners.
     */
    override fun recreate(
        deviceProfile: DeviceProfile,
        navigatorConfig: NavigatorConfig,
        tilesConfig: TilesConfig,
        historyDir: String?,
        logger: Logger,
        accessToken: String,
    ) {
        create(deviceProfile, navigatorConfig, tilesConfig, historyDir, logger, accessToken)
        nativeNavigatorRecreationObservers.forEach {
            it.onNativeNavigatorRecreated()
        }
    }

    override fun resetRideSession() {
        navigator!!.resetRideSession()
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

    /**
     * Passes in the current sensor data of the user.
     *
     * @param sensorData The current sensor data of user.
     *
     * @return true if the sensor data was usable, false if not.
     */
    override suspend fun updateSensorData(sensorData: SensorData): Boolean =
        suspendCancellableCoroutine { continuation ->
            navigator!!.updateSensorData(sensorData) {
                continuation.resume(it)
            }
        }

    override fun addNavigatorObserver(navigatorObserver: NavigatorObserver) =
        navigator!!.addObserver(navigatorObserver)

    override fun removeNavigatorObserver(navigatorObserver: NavigatorObserver) =
        navigator!!.removeObserver(navigatorObserver)

    // Routing

    /**
     * Sets the route path for the navigator to process.
     * Returns initialized route state if no errors occurred.
     * Otherwise, it returns a invalid route state.
     *
     * @param routes [DirectionsRoute]s to follow.
     * @param legIndex Which leg to follow
     *
     * @return a [RouteInfo] route state if no errors occurred.
     * Otherwise, it returns null.
     */
    override suspend fun setRoute(
        routes: List<DirectionsRoute>,
        legIndex: Int
    ): RouteInfo? =
        suspendCancellableCoroutine { continuation ->
            if (routes.isNotEmpty()) {
                checkNotNull(routes[PRIMARY_ROUTE_INDEX].routeOptions()) {
                    "The route set must include RouteOptions"
                }
                val routeOptions = routes[PRIMARY_ROUTE_INDEX].routeOptions()!!
                val directionsResponse = mapToDirectionsResponse(routes, routeOptions)
                navigator!!.setRoute(
                    directionsResponse?.toJson(),
                    PRIMARY_ROUTE_INDEX,
                    legIndex,
                    routeOptions.toUrl(accessToken).toString()
                ) {
                    continuation.resume(it.value)
                }
            } else {
                navigator!!.setRoute(null, 0, 0, null) {
                    continuation.resume(it.value)
                }
            }
        }

    /**
     * https://github.com/mapbox/mapbox-navigation-native/issues/4296
     * Nav native requires a DirectionsResponse so we are drafting a fake one.
     * In order to preserve the original request, will require api changes.
     */
    private fun mapToDirectionsResponse(
        routes: List<DirectionsRoute>,
        routeOptions: RouteOptions?
    ): DirectionsResponse? = routeOptions?.run {
        DirectionsResponse.builder()
            .routes(routes.toMutableList())
            .code("Ok")
            .waypoints(
                coordinatesList().mapIndexed { index, point ->
                    val waypointBuilder = DirectionsWaypoint.builder()
                        .rawLocation(doubleArrayOf(point.longitude(), point.latitude()))
                    waypointNamesList()?.getOrNull(index)?.let { name ->
                        waypointBuilder.name(name)
                    }
                    waypointBuilder.build()
                }
            )
            .build()
    }

    /**
     * Updates annotations so that subsequent calls to getStatus will
     * reflect the most current annotations for the route.
     *
     * @param route [DirectionsRoute]
     */
    override suspend fun updateAnnotations(route: DirectionsRoute) {
        route.legs()?.forEachIndexed { index, routeLeg ->
            suspendCancellableCoroutine<Unit> { continuation ->
                routeLeg.annotation()?.toJson()?.let { annotations ->
                    navigator!!.updateAnnotations(annotations, PRIMARY_ROUTE_INDEX, index) {
                        logger?.d(
                            tag = Tag(TAG),
                            msg = Message(
                                "Annotation updated successfully=$it, for leg " +
                                    "index $index, annotations: [$annotations]"
                            )
                        )

                        continuation.resume(Unit)
                    }
                }
            }
        }
    }

    /**
     * Gets the current banner. If there is no
     * banner, the method returns *null*.
     *
     * @return [BannerInstruction] for step index you passed
     */
    override suspend fun getCurrentBannerInstruction(): BannerInstruction? =
        suspendCancellableCoroutine { continuation ->
            navigator!!.getBannerInstruction {
                continuation.resume(it)
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
            navigator!!.changeRouteLeg(PRIMARY_ROUTE_INDEX, legIndex) {
                continuation.resume(it)
            }
        }

    // Offline

    /**
     * Uses valhalla and local tile data to generate mapbox-directions-api-like json.
     *
     * @param url the directions-based uri used when hitting the http service
     * @return a JSON route object or [RouterError]
     */
    override suspend fun getRoute(url: String): Expected<RouterError, String> =
        suspendCancellableCoroutine { continuation ->
            nativeRouter!!.getRoute(url) { expected, _ ->
                continuation.resume(expected)
            }
        }

    // History traces

    override fun getHistoryRecorderHandle(): HistoryRecorderHandle? {
        return historyRecorderHandle
    }

    // Other

    /**
     * Compare given route with current route.
     * Routes are considered the same if one of the routes is a suffix of another
     * without the first and last intersection.
     *
     * If we don't have an active route, return `true`.
     * If given route has less or equal 2 intersections we consider them different
     *
     * @param directionsRoute the route to compare
     *
     * @return `true` if route is different, `false` otherwise.
     */
    // TODO make async after https://github.com/mapbox/mapbox-navigation-native/issues/4164
    override suspend fun isDifferentRoute(
        directionsRoute: DirectionsRoute
    ): Boolean = withContext(NavigatorDispatcher) {
        val alternativeJson = directionsRoute.toJson()
        val guidanceGeometry = ActiveGuidanceOptionsMapper
            .mapToActiveGuidanceGeometry(directionsRoute.routeOptions()?.geometries())
        navigator!!.isDifferentRoute(alternativeJson, guidanceGeometry)
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
    override fun createMapsPredictiveCacheController(
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

    override fun createRouteAlternativesController(): RouteAlternativesControllerInterface {
        return navigator!!.createRouteAlternativesController()
    }
}
