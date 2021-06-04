package com.mapbox.navigation.navigator.internal

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.base.common.logger.Logger
import com.mapbox.bindgen.Expected
import com.mapbox.common.TileStore
import com.mapbox.navigation.base.options.DeviceProfile
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.options.PredictiveCacheLocationOptions
import com.mapbox.navigation.base.options.RoutingTilesOptions
import com.mapbox.navigator.BannerInstruction
import com.mapbox.navigator.CacheHandle
import com.mapbox.navigator.DumpHistoryCallback
import com.mapbox.navigator.ElectronicHorizonObserver
import com.mapbox.navigator.FallbackVersionsObserver
import com.mapbox.navigator.FixLocation
import com.mapbox.navigator.GraphAccessor
import com.mapbox.navigator.NavigationStatus
import com.mapbox.navigator.NavigatorConfig
import com.mapbox.navigator.PredictiveCacheController
import com.mapbox.navigator.RoadObjectMatcher
import com.mapbox.navigator.RoadObjectsStore
import com.mapbox.navigator.RoadObjectsStoreObserver
import com.mapbox.navigator.RouteInfo
import com.mapbox.navigator.RouterError
import com.mapbox.navigator.RouterResult
import com.mapbox.navigator.SensorData
import com.mapbox.navigator.TilesConfig
import com.mapbox.navigator.VoiceInstruction

/**
 * Provides API to work with native Navigator class. Exposed for internal usage only.
 */
interface MapboxNativeNavigator {

    private companion object {
        private const val INDEX_FIRST_LEG = 0
    }

    /**
     * Initialize the navigator with a device profile
     */
    fun create(
        deviceProfile: DeviceProfile,
        navigatorConfig: NavigatorConfig,
        tilesConfig: TilesConfig,
        historyDir: String?,
        logger: Logger
    ): MapboxNativeNavigator

    /**
     * Reinitialize the navigator with a device profile
     */
    fun recreate(
        deviceProfile: DeviceProfile,
        navigatorConfig: NavigatorConfig,
        tilesConfig: TilesConfig,
        historyDir: String?,
        logger: Logger
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

    /**
     * Passes in the current sensor data of the user.
     *
     * @param sensorData The current sensor data of user.
     *
     * @return true if the sensor data was usable, false if not.
     */
    fun updateSensorData(sensorData: SensorData): Boolean

    /**
     * Gets the status as an offset in time from the last fixed location. This
     * allows the caller to get predicted statuses in the future along the route if
     * the device is unable to get fixed locations. Poor reception would be one reason.
     *
     * This method uses previous fixes to snap the user's location to the route
     * and verify that the user is still on the route. This method also determines
     * if an instruction needs to be called out for the user.
     *
     * @param navigatorPredictionMillis millis for navigation status predictions.
     *
     * @return the last [TripStatus] as a result of fixed location updates. If the timestamp
     * is earlier than a previous call, the last status will be returned. The function does not support re-winding time.
     */
    suspend fun getStatus(navigatorPredictionMillis: Long): TripStatus

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
    suspend fun setRoute(
        route: DirectionsRoute?,
        legIndex: Int = INDEX_FIRST_LEG
    ): RouteInfo?

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
    suspend fun updateAnnotations(route: DirectionsRoute)

    /**
     * Gets the banner at a specific step index in the route. If there is no
     * banner at the specified index method return *null*.
     *
     * @param index Which step you want to get [BannerInstruction] for
     *
     * @return [BannerInstruction] for step index you passed
     */
    fun getBannerInstruction(index: Int): BannerInstruction?

    /**
     * Follows a new leg of the already loaded directions.
     * Returns an initialized navigation status if no errors occurred
     * otherwise, it returns an invalid navigation status state.
     *
     * @param legIndex new leg index
     *
     * @return an initialized [NavigationStatus] if no errors, invalid otherwise
     */
    fun updateLegIndex(legIndex: Int): Boolean

    // Offline

    /**
     * Uses valhalla and local tile data to generate mapbox-directions-api-like json.
     *
     * @param url the directions-based uri used when hitting the http service
     * @return a [RouterResult] object with the json and a success/fail boolean
     */
    suspend fun getRoute(url: String): Expected<RouterError, String>

    // History traces

    /**
     * Gets the history of state-changing calls to the navigator. This can be used to
     * replay a sequence of events for the purpose of bug fixing.
     *
     * @return a json representing the series of events that happened since the last time
     * the history was toggled on.
     */
    fun getHistory(result: DumpHistoryCallback)

    /**
     * Adds a custom event to the navigator's history. This can be useful to log things that
     * happen during navigation that are specific to your application.
     *
     * @param eventType the event type in the events log for your custom event
     * @param eventJsonProperties the json to attach to the "properties" key of the event
     */
    fun addHistoryEvent(eventType: String, eventJsonProperties: String)

    // Other

    /**
     * Gets the voice instruction at a specific step index in the route. If there is no
     * voice instruction at the specified index, *null* is returned.
     *
     * @param index Which step you want to get [VoiceInstruction] for
     *
     * @return [VoiceInstruction] for step index you passed
     */
    fun getVoiceInstruction(index: Int): VoiceInstruction?

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
    suspend fun isDifferentRoute(directionsRoute: DirectionsRoute): Boolean

    // EH

    /**
     * Sets the Electronic Horizon observer
     *
     * @param eHorizonObserver
     */
    fun setElectronicHorizonObserver(eHorizonObserver: ElectronicHorizonObserver?)

    /**
     * Sets the Road objects store observer
     *
     * @param roadObjectsStoreObserver
     */
    fun setRoadObjectsStoreObserver(roadObjectsStoreObserver: RoadObjectsStoreObserver?)

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
    fun createMapsPredictiveCacheController(
        tileStore: TileStore,
        tileVariant: String,
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

    val graphAccessor: GraphAccessor?

    val roadObjectsStore: RoadObjectsStore?

    val cache: CacheHandle

    val roadObjectMatcher: RoadObjectMatcher?
}
