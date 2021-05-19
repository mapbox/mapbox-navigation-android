package com.mapbox.navigation.navigator.internal

import com.mapbox.api.directions.v5.models.DirectionsRoute
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
import com.mapbox.navigator.RouteInfo
import com.mapbox.navigator.Router
import com.mapbox.navigator.RouterError
import com.mapbox.navigator.RouterResult
import com.mapbox.navigator.SensorData
import com.mapbox.navigator.TilesConfig
import com.mapbox.navigator.VoiceInstruction
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Default implementation of [MapboxNativeNavigator] interface.
 */
object MapboxNativeNavigatorImpl : MapboxNativeNavigator {

    private const val PRIMARY_ROUTE_INDEX = 0
    private const val SINGLE_THREAD = 1
    private const val TAG = "MbxNativeNavigatorImpl"

    // TODO: What should be the default value? Should we expose it publicly?
    private const val MAX_NUMBER_TILES_LOAD_PARALLEL_REQUESTS = 2

    private val NavigatorDispatcher: CoroutineDispatcher =
        Executors.newFixedThreadPool(SINGLE_THREAD).asCoroutineDispatcher()
    private var navigator: Navigator? = null
    private var nativeRouter: Router? = null
    private var historyRecorderHandle: HistoryRecorderHandle? = null
    private var route: DirectionsRoute? = null
    override var graphAccessor: GraphAccessor? = null
    override var roadObjectMatcher: RoadObjectMatcher? = null
    override var roadObjectsStore: RoadObjectsStore? = null
    override lateinit var cache: CacheHandle
    private var logger: Logger? = null
    private val nativeNavigatorRecreationObservers =
        CopyOnWriteArraySet<NativeNavigatorRecreationObserver>()

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
        logger: Logger
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
        cache = nativeComponents.cache
        route = null
        this.logger = logger
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
        logger: Logger
    ) {
        create(deviceProfile, navigatorConfig, tilesConfig, historyDir, logger)
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
        withContext(NavigatorDispatcher) {
            navigator!!.updateLocation(rawLocation)
        }

    /**
     * Passes in the current sensor data of the user.
     *
     * @param sensorData The current sensor data of user.
     *
     * @return true if the sensor data was usable, false if not.
     */
    override fun updateSensorData(sensorData: SensorData): Boolean {
        return navigator!!.updateSensorData(sensorData)
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
     * @param route [DirectionsRoute] to follow.
     * @param legIndex Which leg to follow
     *
     * @return a [NavigationStatus] route state if no errors occurred.
     * Otherwise, it returns a invalid route state.
     */
    override suspend fun setRoute(
        route: DirectionsRoute?,
        legIndex: Int
    ): RouteInfo? =
        withContext(NavigatorDispatcher) {
            MapboxNativeNavigatorImpl.route = route
            val result = navigator!!.setRoute(
                route?.toJson() ?: "{}",
                PRIMARY_ROUTE_INDEX,
                legIndex,
                ActiveGuidanceOptionsMapper.mapFrom(route)
            ).let { it.value }

            result
        }

    /**
     * Updates annotations so that subsequent calls to getStatus will
     * reflect the most current annotations for the route.
     *
     * @param legAnnotationJson A string containing the json/pbf annotations
     * @param routeIndex Which route to apply the annotation update to
     * @param legIndex Which leg to apply the annotation update to
     *
     * @return True if the annotations could be updated false if not (wrong number of annotations)
     */
    override suspend fun updateAnnotations(route: DirectionsRoute): Unit =
        withContext(NavigatorDispatcher) {
            MapboxNativeNavigatorImpl.route = route
            route.legs()?.forEachIndexed { index, routeLeg ->
                routeLeg.annotation()?.toJson()?.let { annotations ->
                    navigator!!.updateAnnotations(annotations, PRIMARY_ROUTE_INDEX, index)
                        .let { success ->
                            logger?.d(
                                tag = Tag(TAG),
                                msg = Message(
                                    "Annotation updated successfully=$success, for leg " +
                                        "index $index, annotations: [$annotations]"
                                )
                            )
                        }
                }
            }
        }

    /**
     * Gets the banner at a specific step index in the route. If there is no
     * banner at the specified index method return *null*.
     *
     * @param index Which step you want to get [BannerInstruction] for
     *
     * @return [BannerInstruction] for step index you passed
     */
    override fun getBannerInstruction(index: Int): BannerInstruction? =
        navigator!!.getBannerInstruction(index)

    /**
     * Follows a new leg of the already loaded directions.
     * Returns an initialized navigation status if no errors occurred
     * otherwise, it returns an invalid navigation status state.
     *
     * @param legIndex new leg index
     *
     * @return an initialized [NavigationStatus] if no errors, invalid otherwise
     */
    override fun updateLegIndex(legIndex: Int): Boolean =
        navigator!!.changeRouteLeg(PRIMARY_ROUTE_INDEX, legIndex)

    // Offline

    /**
     * Uses valhalla and local tile data to generate mapbox-directions-api-like json.
     *
     * @param url the directions-based uri used when hitting the http service
     * @return a [RouterResult] object with the json and a success/fail boolean
     */
    override suspend fun getRoute(url: String): Expected<RouterError, String> {
        return suspendCoroutine { continuation ->
            nativeRouter!!.getRoute(url) {
                continuation.resume(it)
            }
        }
    }

    // History traces

    override fun getHistoryRecorderHandle(): HistoryRecorderHandle? {
        return historyRecorderHandle
    }

    // Other

    /**
     * Gets the voice instruction at a specific step index in the route. If there is no
     * voice instruction at the specified index, *null* is returned.
     *
     * @param index Which step you want to get [VoiceInstruction] for
     *
     * @return [VoiceInstruction] for step index you passed
     */
    override fun getVoiceInstruction(index: Int): VoiceInstruction? =
        navigator!!.getVoiceInstruction(index)

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

    /**
     * Sets the Road objects store observer
     *
     * @param roadObjectsStoreObserver
     */
    override fun setRoadObjectsStoreObserver(roadObjectsStoreObserver: RoadObjectsStoreObserver?) {
        roadObjectsStore?.setObserver(roadObjectsStoreObserver)
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
        roadObjectsStore?.setObserver(null)
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
}
