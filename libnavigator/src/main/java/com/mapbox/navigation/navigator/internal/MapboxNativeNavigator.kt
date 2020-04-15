package com.mapbox.navigation.navigator.internal

import android.location.Location
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.base.common.logger.Logger
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.options.DeviceProfile
import com.mapbox.navigator.BannerInstruction
import com.mapbox.navigator.NavigationStatus
import com.mapbox.navigator.NavigatorConfig
import com.mapbox.navigator.RouterParams
import com.mapbox.navigator.RouterResult
import com.mapbox.navigator.SensorData
import com.mapbox.navigator.VoiceInstruction
import java.util.Date

/**
 * Provides API to work with native Navigator class. Exposed for internal usage only.
 */
interface MapboxNativeNavigator {

    companion object {
        private const val INDEX_FIRST_LEG = 0
        private const val GRID_SIZE = 0.0025f
        private const val BUFFER_DILATION: Short = 1
    }

    /**
     * Initialize the navigator with a device profile
     */
    fun create(deviceProfile: DeviceProfile, logger: Logger?): MapboxNativeNavigator

    // Route following

    /**
     * Passes in the current raw location of the user.
     *
     * @param rawLocation The current raw [Location] of user.
     *
     * @return true if the raw location was usable, false if not.
     */
    suspend fun updateLocation(rawLocation: Location, date: Date): Boolean

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
     * @param date the point in time to receive the status for.
     *
     * @return the last [TripStatus] as a result of fixed location updates. If the timestamp
     * is earlier than a previous call, the last status will be returned. The function does not support re-winding time.
     */
    suspend fun getStatus(date: Date): TripStatus

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
    ): NavigationStatus

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
    fun updateAnnotations(legAnnotationJson: String, routeIndex: Int, legIndex: Int): Boolean

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
    fun getRouteGeometryWithBuffer(
        gridSize: Float = GRID_SIZE,
        bufferDilation: Short = BUFFER_DILATION
    ): String?

    /**
     * Follows a new leg of the already loaded directions.
     * Returns an initialized navigation status if no errors occurred
     * otherwise, it returns an invalid navigation status state.
     *
     * @param legIndex new leg index
     *
     * @return an initialized [NavigationStatus] if no errors, invalid otherwise
     */
    fun updateLegIndex(legIndex: Int): NavigationStatus

    // Free Drive

    /**
     * Toggles Electronic Horizon on or off.
     *
     * @param isEnabled set this to true to turn on Electronic Horizon and false to turn it off
     */
    fun toggleElectronicHorizon(isEnabled: Boolean)

    // Offline

    /**
     * Caches tiles around the last set route
     */
    fun cacheLastRoute()

    /**
     * Configures routers for getting routes offline.
     *
     * @param routerParams Optional [RouterParams] object which contains router configurations for
     * getting routes offline.
     *
     * @return number of tiles founded in the directory
     */
    fun configureRouter(routerParams: RouterParams): Long

    /**
     * Uses valhalla and local tile data to generate mapbox-directions-api-like json.
     *
     * @param url the directions-based uri used when hitting the http service
     * @return a [RouterResult] object with the json and a success/fail boolean
     */
    fun getRoute(url: String): RouterResult

    /**
     * Passes in an input path to the tar file and output path.
     *
     * @param tarPath The path to the packed tiles.
     * @param destinationPath The path to the unpacked files.
     *
     * @return the number of unpacked tiles
     */
    fun unpackTiles(tarPath: String, destinationPath: String): Long

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
    fun removeTiles(tilePath: String, southwest: Point, northeast: Point): Long

    // History traces

    /**
     * Gets the history of state-changing calls to the navigator. This can be used to
     * replay a sequence of events for the purpose of bug fixing.
     *
     * @return a json representing the series of events that happened since the last time
     * the history was toggled on.
     */
    fun getHistory(): String

    /**
     * Toggles the recording of history on or off.
     * Toggling will reset all history calls [getHistory] first before toggling to retain a copy.
     *
     * @param isEnabled set this to true to turn on history recording and false to turn it off
     */
    fun toggleHistory(isEnabled: Boolean)

    /**
     * Adds a custom event to the navigator's history. This can be useful to log things that
     * happen during navigation that are specific to your application.
     *
     * @param eventType the event type in the events log for your custom event
     * @param eventJsonProperties the json to attach to the "properties" key of the event
     */
    fun addHistoryEvent(eventType: String, eventJsonProperties: String)

    // Configuration

    /**
     * Gets the current configuration used for navigation.
     *
     * @return the [NavigatorConfig] used for navigation.
     */
    fun getConfig(): NavigatorConfig

    /**
     * Updates the configuration used for navigation. Passing null resets the config.
     *
     * @param config the new [NavigatorConfig]
     */
    fun setConfig(config: NavigatorConfig?)

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
}
