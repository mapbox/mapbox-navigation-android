package com.mapbox.navigation.navigator.internal

import android.location.Location
import android.os.SystemClock
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.geojson.Geometry
import com.mapbox.geojson.Point
import com.mapbox.geojson.gson.GeometryGeoJson
import com.mapbox.navigation.base.options.DeviceProfile
import com.mapbox.navigation.navigator.ActiveGuidanceOptionsMapper
import com.mapbox.navigation.navigator.toFixLocation
import com.mapbox.navigation.navigator.toLocation
import com.mapbox.navigation.utils.internal.ifNonNull
import com.mapbox.navigator.BannerInstruction
import com.mapbox.navigator.ElectronicHorizonObserver
import com.mapbox.navigator.NavigationStatus
import com.mapbox.navigator.Navigator
import com.mapbox.navigator.NavigatorConfig
import com.mapbox.navigator.RouteState
import com.mapbox.navigator.RouterResult
import com.mapbox.navigator.SensorData
import com.mapbox.navigator.TilesConfig
import com.mapbox.navigator.VoiceInstruction
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import java.lang.Error
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Default implementation of [MapboxNativeNavigator] interface.
 */
object MapboxNativeNavigatorImpl : MapboxNativeNavigator {

    private const val GRID_SIZE = 0.0025f
    private const val BUFFER_DILATION: Short = 1
    private const val PRIMARY_ROUTE_INDEX = 0
    private const val SINGLE_THREAD = 1

    private val NavigatorDispatcher: CoroutineDispatcher =
        Executors.newFixedThreadPool(SINGLE_THREAD).asCoroutineDispatcher()
    private var navigator: Navigator? = null
    private var route: DirectionsRoute? = null
    private var routeBufferGeoJson: Geometry? = null
    private val navigatorMapper = NavigatorMapper()

    // Route following

    /**
     * Create or reset resources. This must be called before calling any
     * functions within [MapboxNativeNavigatorImpl]
     */
    override fun create(
        deviceProfile: DeviceProfile,
        navigatorConfig: NavigatorConfig,
        tilesConfig: TilesConfig
    ): MapboxNativeNavigator {
        navigator = NavigatorLoader.createNavigator(
            deviceProfile,
            navigatorConfig,
            tilesConfig
        )
        route = null
        routeBufferGeoJson = null
        return this
    }

    override fun resetRideSession() {
        navigator!!.resetRideSession()
    }

    /**
     * Passes in the current raw location of the user.
     *
     * @param rawLocation The current raw [Location] of user.
     *
     * @return true if the raw location was usable, false if not.
     */
    override suspend fun updateLocation(rawLocation: Location): Boolean =
        withContext(NavigatorDispatcher) {
            navigator!!.updateLocation(rawLocation.toFixLocation())
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
    override suspend fun getStatus(navigatorPredictionMillis: Long): TripStatus =
        withContext(NavigatorDispatcher) {
            val nanos = SystemClock.elapsedRealtimeNanos() + TimeUnit.MILLISECONDS.toNanos(
                navigatorPredictionMillis
            )
            val status = navigator!!.getStatus(nanos)
            TripStatus(
                status.location.toLocation(),
                status.key_points.map { it.toLocation() },
                navigatorMapper.getRouteProgress(
                    route,
                    routeBufferGeoJson,
                    status,
                    navigator!!.remainingWaypoints().size
                ),
                status.routeState == RouteState.OFF_ROUTE,
                status
            )
        }

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
    ): RouteInitInfo? =
        withContext(NavigatorDispatcher) {
            MapboxNativeNavigatorImpl.route = route
            val result = navigator!!.setRoute(
                route?.toJson()
                    ?: "{}",
                PRIMARY_ROUTE_INDEX,
                legIndex,
                ActiveGuidanceOptionsMapper.mapFrom(route)
            ).let { navigatorMapper.getRouteInitInfo(it.value) }

            val geometryWithBuffer = getRouteGeometryWithBuffer(GRID_SIZE, BUFFER_DILATION)
            routeBufferGeoJson = ifNonNull(geometryWithBuffer) {
                GeometryGeoJson.fromJson(it)
            }

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
    override fun updateAnnotations(
        legAnnotationJson: String,
        routeIndex: Int,
        legIndex: Int
    ): Boolean = navigator!!.updateAnnotations(legAnnotationJson, routeIndex, legIndex)

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
     * Gets a polygon around the currently loaded route. The method uses a bitmap approach
     * in which you specify a grid size (pixel size) and a dilation (how many pixels) to
     * expand the initial grid cells that are intersected by the route.
     *
     * @param gridSize the size of the individual grid cells
     * @param bufferDilation the number of pixels to dilate the initial intersection by it can
     * be thought of as controlling the halo thickness around the route
     *
     * @return a geojson as [String] representing the route buffer polygon
     */
    override fun getRouteGeometryWithBuffer(gridSize: Float, bufferDilation: Short): String? {
        return try {
            navigator!!.getRouteBufferGeoJson(gridSize, bufferDilation)
        } catch (error: Error) {
            // failed to obtain the route buffer
            // workaround for https://github.com/mapbox/mapbox-navigation-android/issues/2337
            null
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
    override fun updateLegIndex(legIndex: Int): Boolean =
        navigator!!.changeRouteLeg(PRIMARY_ROUTE_INDEX, legIndex)

    // Offline

    /**
     * Caches tiles around the last set route
     */
    override fun cacheLastRoute() {
        navigator!!.cacheLastRoute()
    }

    /**
     * Uses valhalla and local tile data to generate mapbox-directions-api-like json.
     *
     * @param url the directions-based uri used when hitting the http service
     * @return a [RouterResult] object with the json and a success/fail boolean
     */
    override fun getRoute(url: String): RouterResult = navigator!!.getRoute(url)

    /**
     * Passes in an input path to the tar file and output path.
     *
     * @param tarPath The path to the packed tiles.
     * @param destinationPath The path to the unpacked files.
     *
     * @return the number of unpacked tiles
     */
    override fun unpackTiles(tarPath: String, destinationPath: String): Long =
        navigator!!.unpackTiles(tarPath, destinationPath)

    /**
     * Removes tiles wholly within the supplied bounding box. If the tile is not
     * contained completely within the bounding box, it will remain in the cache.
     * After removing files from the cache, any routers should be reconfigured
     * to synchronize their in-memory cache with the disk.
     *
     * @param tilePath The path to the tiles.
     * @param southwest The lower left coord of the bounding box.
     * @param northeast The upper right coord of the bounding box.
     *
     * @return the number of tiles removed
     */
    override fun removeTiles(tilePath: String, southwest: Point, northeast: Point): Long =
        navigator!!.removeTiles(tilePath, southwest, northeast)

    // History traces

    /**
     * Gets the history of state-changing calls to the navigator. This can be used to
     * replay a sequence of events for the purpose of bug fixing.
     *
     * @return a json representing the series of events that happened since the last time
     * the history was toggled on.
     */
    override fun getHistory(): String = navigator!!.history

    /**
     * Toggles the recording of history on or off.
     * Toggling will reset all history calls [getHistory] first before toggling to retain a copy.
     *
     * @param isEnabled set this to true to turn on history recording and false to turn it off
     */
    override fun toggleHistory(isEnabled: Boolean) {
        navigator!!.toggleHistory(isEnabled)
    }

    /**
     * Adds a custom event to the navigator's history. This can be useful to log things that
     * happen during navigation that are specific to your application.
     *
     * @param eventType the event type in the events log for your custom event
     * @param eventJsonProperties the json to attach to the "properties" key of the event
     */
    override fun addHistoryEvent(eventType: String, eventJsonProperties: String) {
        navigator!!.pushHistory(eventType, eventJsonProperties)
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

    // EH

    /**
     * Sets the Electronic Horizon observer
     *
     * @param eHorizonObserver
     */
    override fun setElectronicHorizonObserver(eHorizonObserver: ElectronicHorizonObserver?) {
        navigator!!.setElectronicHorizonObserver(eHorizonObserver)
    }
}
