package com.mapbox.navigation.navigator.internal

import androidx.annotation.RestrictTo
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.bindgen.Expected
import com.mapbox.common.TileStore
import com.mapbox.common.TilesetDescriptor
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.options.PredictiveCacheLocationOptions
import com.mapbox.navigation.base.options.RoutingTilesOptions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigator.ADASISv2MessageCallback
import com.mapbox.navigator.AdasisConfig
import com.mapbox.navigator.CacheHandle
import com.mapbox.navigator.ConfigHandle
import com.mapbox.navigator.ElectronicHorizonObserver
import com.mapbox.navigator.EventsMetadataInterface
import com.mapbox.navigator.Experimental
import com.mapbox.navigator.FallbackVersionsObserver
import com.mapbox.navigator.FixLocation
import com.mapbox.navigator.GraphAccessorInterface
import com.mapbox.navigator.InputsServiceHandleInterface
import com.mapbox.navigator.LaneSensorInfo
import com.mapbox.navigator.NavigationStatus
import com.mapbox.navigator.Navigator
import com.mapbox.navigator.NavigatorObserver
import com.mapbox.navigator.PredictiveCacheController
import com.mapbox.navigator.RerouteControllerInterface
import com.mapbox.navigator.RerouteDetectorInterface
import com.mapbox.navigator.ResetCallback
import com.mapbox.navigator.RoadObjectMatcher
import com.mapbox.navigator.RoadObjectsStore
import com.mapbox.navigator.RoadObjectsStoreObserver
import com.mapbox.navigator.RouteAlternative
import com.mapbox.navigator.RouteAlternativesControllerInterface
import com.mapbox.navigator.RouterInterface
import com.mapbox.navigator.SetRoutesReason
import com.mapbox.navigator.SetRoutesResult
import com.mapbox.navigator.Telemetry
import com.mapbox.navigator.TestingContext
import com.mapbox.navigator.TilesConfig
import com.mapbox.navigator.VehicleType
import com.mapbox.navigator.WeatherData

/**
 * Provides API to work with native Navigator class. Exposed for internal usage only.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
interface MapboxNativeNavigator : RerouteEventsProvider {

    /**
     * Reinitialize the navigator with a device profile
     */
    fun recreate(tilesConfig: TilesConfig)

    /**
     * Get router
     *
     * @return router [RouterInterface]
     */
    fun getRouter(): RouterInterface

    fun getRerouteDetector(): RerouteDetectorInterface?

    fun getRerouteController(): RerouteControllerInterface?

    suspend fun resetRideSession()

    fun startNavigationSession()

    fun stopNavigationSession()

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
        reason: SetRoutesReason,
    ): Expected<String, SetRoutesResult>

    suspend fun setAlternativeRoutes(
        routes: List<NavigationRoute>,
    ): List<RouteAlternative>

    /**
     * Updates annotations so that subsequent calls to getStatus will
     * reflect the most current annotations for the route.
     *
     * @param route [DirectionsRoute]
     */
    suspend fun refreshRoute(route: NavigationRoute): Expected<String, List<RouteAlternative>>

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

    // EV

    fun onEVDataUpdated(data: Map<String, String>)

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
        nativeNavigatorRecreationObserver: NativeNavigatorRecreationObserver,
    )

    /**
     * Unregister native observers
     */
    fun unregisterAllObservers()

    fun pause()
    fun resume()
    fun shutdown()

    // Predictive cache

    /**
     * Creates a Maps [PredictiveCacheController].
     *
     * @param tileStore Maps [TileStore]
     * @param tilesetDescriptor Maps tilesetDescriptor [TilesetDescriptor]
     * @param predictiveCacheLocationOptions [PredictiveCacheLocationOptions]
     *
     * @return [PredictiveCacheController]
     */
    fun createMapsPredictiveCacheController(
        tileStore: TileStore,
        tilesetDescriptor: TilesetDescriptor,
        predictiveCacheLocationOptions: PredictiveCacheLocationOptions,
    ): PredictiveCacheController

    /**
     * Creates a Search [PredictiveCacheController].
     *
     * @param tileStore Search [TileStore]
     * @param searchTilesetDescriptor Search tilesetDescriptor [TilesetDescriptor]
     * @param predictiveCacheLocationOptions [PredictiveCacheLocationOptions]
     *
     * @return [PredictiveCacheController]
     */
    fun createSearchPredictiveCacheController(
        tileStore: TileStore,
        searchTilesetDescriptor: TilesetDescriptor,
        predictiveCacheLocationOptions: PredictiveCacheLocationOptions,
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
        predictiveCacheLocationOptions: PredictiveCacheLocationOptions,
    ): PredictiveCacheController

    /**
     * Asynchronously passes in the current sensor data of the user.
     * The callback is scheduled using the `common::Scheduler` of the thread calling the `Navigator` constructor.
     *
     * @param data The current sensor data of user.
     */
    fun updateWeatherData(data: WeatherData)

    fun updateLaneSensorInfo(data: LaneSensorInfo)

    fun setVehicleType(type: VehicleType)

    /**
     * Sets a callback for ADASIS messages
     */
    fun setAdasisMessageCallback(callback: ADASISv2MessageCallback, adasisConfig: AdasisConfig)

    /**
     * Resets a callback for ADASIS messages
     */
    fun resetAdasisMessageCallback()

    /**
     * Trigger reset of EHP for the case when external reconstructor needs it.
     */
    fun triggerResetOfEhProvider()

    fun setUserLanguages(languages: List<String>)

    fun setTestingContext(testingContext: TestingContext)

    fun reset(callback: ResetCallback?)

    val config: ConfigHandle

    val eventsMetadataProvider: EventsMetadataInterface

    val navigator: Navigator

    val routeAlternativesController: RouteAlternativesControllerInterface

    val graphAccessor: GraphAccessorInterface

    val roadObjectsStore: RoadObjectsStore

    val cache: CacheHandle

    val roadObjectMatcher: RoadObjectMatcher

    val experimental: Experimental

    val inputsService: InputsServiceHandleInterface

    val telemetry: Telemetry
}
