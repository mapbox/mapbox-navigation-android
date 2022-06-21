package com.mapbox.navigation.navigator.internal

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.bindgen.Expected
import com.mapbox.common.TileStore
import com.mapbox.common.TilesetDescriptor
import com.mapbox.navigation.base.options.DeviceProfile
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.options.PredictiveCacheLocationOptions
import com.mapbox.navigation.base.options.RoutingTilesOptions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigator.BannerInstruction
import com.mapbox.navigator.CacheHandle
import com.mapbox.navigator.ElectronicHorizonObserver
import com.mapbox.navigator.Experimental
import com.mapbox.navigator.FallbackVersionsObserver
import com.mapbox.navigator.FixLocation
import com.mapbox.navigator.GraphAccessor
import com.mapbox.navigator.HistoryRecorderHandle
import com.mapbox.navigator.NavigationStatus
import com.mapbox.navigator.NavigatorConfig
import com.mapbox.navigator.NavigatorObserver
import com.mapbox.navigator.PredictiveCacheController
import com.mapbox.navigator.RoadObjectMatcher
import com.mapbox.navigator.RoadObjectsStore
import com.mapbox.navigator.RoadObjectsStoreObserver
import com.mapbox.navigator.RouteAlternative
import com.mapbox.navigator.RouteAlternativesControllerInterface
import com.mapbox.navigator.RouterError
import com.mapbox.navigator.RouterInterface
import com.mapbox.navigator.SetRoutesResult
import com.mapbox.navigator.TilesConfig

/**
 * Provides API to work with native Navigator class. Exposed for internal usage only.
 */
interface MapboxNativeNavigator {

    /**
     * Initialize the navigator with a device profile
     */
    fun create(
        deviceProfile: DeviceProfile,
        navigatorConfig: NavigatorConfig,
        tilesConfig: TilesConfig,
        historyDir: String?,
        accessToken: String,
        router: RouterInterface,
    ): MapboxNativeNavigator

    /**
     * Reinitialize the navigator with a device profile
     */
    fun recreate(
        deviceProfile: DeviceProfile,
        navigatorConfig: NavigatorConfig,
        tilesConfig: TilesConfig,
        historyDir: String?,
        accessToken: String,
        router: RouterInterface,
    )

    /**
     * Reset the navigator state with the same configuration. The location becomes unknown,
     * but the [NavigatorConfig] stays the same. This can be used to transport the
     * navigator to a new location.
     */
    fun resetRideSession()

    // Route following

    /**
     * Passes in the current raw location of the user.
     *
     * @param rawLocation The current raw [FixLocation] of user.
     *
     * @return true if the raw location was usable, false if not.
     */
    suspend fun updateLocation(rawLocation: FixLocation): Boolean

    fun addNavigatorObserver(navigatorObserver: NavigatorObserver)

    fun removeNavigatorObserver(navigatorObserver: NavigatorObserver)

    // Routing
    suspend fun setRoutes(
        primaryRoute: NavigationRoute?,
        startingLeg: Int = 0,
        alternatives: List<NavigationRoute> = emptyList(),
    ): Expected<String, SetRoutesResult>

    suspend fun setAlternativeRoutes(
        routes: List<NavigationRoute>
    ): List<RouteAlternative>

    /**
     * Updates annotations so that subsequent calls to getStatus will
     * reflect the most current annotations for the route.
     *
     * @param route [DirectionsRoute]
     */
    suspend fun refreshRoute(route: NavigationRoute): List<RouteAlternative>?

    /**
     * Gets the current banner. If there is no
     * banner, the method returns *null*.
     *
     * @return [BannerInstruction] for step index you passed
     */
    suspend fun getCurrentBannerInstruction(): BannerInstruction?

    /**
     * Follows a new leg of the already loaded directions.
     * Returns an initialized navigation status if no errors occurred
     * otherwise, it returns an invalid navigation status state.
     *
     * @param legIndex new leg index
     *
     * @return an initialized [NavigationStatus] if no errors, invalid otherwise
     */
    suspend fun updateLegIndex(legIndex: Int): Boolean

    // Offline

    /**
     * Uses valhalla and local tile data to generate mapbox-directions-api-like json.
     *
     * @param url the directions-based uri used when hitting the http service
     * @return a JSON route object or [RouterError]
     */
    suspend fun getRoute(url: String): Expected<RouterError, String>

    // History traces

    /**
     * Returns the native class that allows the sdk to record native history files.
     *
     * @return null when there is no directory to write files,
     *     or when the handle did not [HistoryRecorderHandle.build]
     */
    fun getHistoryRecorderHandle(): HistoryRecorderHandle?

    // EH

    /**
     * Sets the Electronic Horizon observer
     *
     * @param eHorizonObserver
     */
    fun setElectronicHorizonObserver(eHorizonObserver: ElectronicHorizonObserver?)

    fun addRoadObjectsStoreObserver(roadObjectsStoreObserver: RoadObjectsStoreObserver)

    fun removeRoadObjectsStoreObserver(roadObjectsStoreObserver: RoadObjectsStoreObserver)

    fun setFallbackVersionsObserver(fallbackVersionsObserver: FallbackVersionsObserver?)

    fun setNativeNavigatorRecreationObserver(
        nativeNavigatorRecreationObserver: NativeNavigatorRecreationObserver
    )

    /**
     * Unregister native observers
     */
    fun unregisterAllObservers()

    // Predictive cache

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
    fun createMapsPredictiveCacheControllerTileVariant(
        tileStore: TileStore,
        tileVariant: String,
        predictiveCacheLocationOptions: PredictiveCacheLocationOptions
    ): PredictiveCacheController

    /**
     * Creates a Maps [PredictiveCacheController].
     *
     * @param tileStore Maps [TileStore]
     * @param tilesetDescriptor Maps tilesetDescriptor
     * @param predictiveCacheLocationOptions [PredictiveCacheLocationOptions]
     *
     * @return [PredictiveCacheController]
     */
    fun createMapsPredictiveCacheController(
        tileStore: TileStore,
        tilesetDescriptor: TilesetDescriptor,
        predictiveCacheLocationOptions: PredictiveCacheLocationOptions
    ): PredictiveCacheController

    /**
     * Creates a Navigation [PredictiveCacheController]. Uses the option passed in
     * [RoutingTilesOptions] via [NavigationOptions].
     *
     * @param predictiveCacheLocationOptions [PredictiveCacheLocationOptions]
     *
     * @return [PredictiveCacheController]
     */
    fun createNavigationPredictiveCacheController(
        predictiveCacheLocationOptions: PredictiveCacheLocationOptions
    ): PredictiveCacheController

    val routeAlternativesController: RouteAlternativesControllerInterface

    val graphAccessor: GraphAccessor?

    val roadObjectsStore: RoadObjectsStore?

    val cache: CacheHandle

    val roadObjectMatcher: RoadObjectMatcher?

    val experimental: Experimental

    val router: RouterInterface
}
