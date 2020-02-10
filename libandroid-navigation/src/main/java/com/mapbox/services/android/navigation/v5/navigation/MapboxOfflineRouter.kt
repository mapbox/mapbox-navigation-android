package com.mapbox.services.android.navigation.v5.navigation

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.geojson.BoundingBox
import com.mapbox.navigator.Navigator
import java.io.File

/**
 * Class used for offline routing.
 */
class MapboxOfflineRouter {
    private val tilePath: String
    private val offlineNavigator: OfflineNavigator
    private val offlineTileVersions: OfflineTileVersions

    /**
     * Creates an offline router which uses the specified offline path for storing and retrieving
     * data.
     *
     * @param offlinePath directory path where the offline data is located
     */
    constructor(offlinePath: String) {
        val tileDir = File(offlinePath, TILE_PATH_NAME)
        if (!tileDir.exists()) {
            tileDir.mkdirs()
        }

        this.tilePath = tileDir.absolutePath
        offlineNavigator = OfflineNavigator(Navigator(null, null, null))
        offlineTileVersions = OfflineTileVersions()
    }

    // Package private for testing purposes
    internal constructor(
        tilePath: String,
        offlineNavigator: OfflineNavigator,
        offlineTileVersions: OfflineTileVersions
    ) {
        this.tilePath = tilePath
        this.offlineNavigator = offlineNavigator
        this.offlineTileVersions = offlineTileVersions
    }

    companion object {
        init {
            NavigationLibraryLoader.load()
        }

        private const val TILE_PATH_NAME = "tiles"
    }

    /**
     * Configures the navigator for getting offline routes.
     *
     * @param version version of offline tiles to use
     * @param callback a callback that will be fired when the offline data is configured and
     * [MapboxOfflineRouter.findRoute]
     * can be called safely
     */
    fun configure(version: String, callback: OnOfflineTilesConfiguredCallback) {
        offlineNavigator.configure(File(tilePath, version).absolutePath, callback)
    }

    /**
     * Uses libvalhalla and local tile data to generate mapbox-directions-api-like JSON.
     *
     * @param route the [OfflineRoute] to get a [DirectionsRoute] from
     * @param callback a callback to pass back the result
     */
    fun findRoute(route: OfflineRoute, callback: OnOfflineRouteFoundCallback) {
        offlineNavigator.retrieveRouteFor(route, callback)
    }

    /**
     * Starts the download of tiles specified by the provided [OfflineTiles] object.
     *
     * @param offlineTiles object specifying parameters for the tile request
     * @param listener which is updated on error, on progress update and on completion
     */
    fun downloadTiles(offlineTiles: OfflineTiles, listener: RouteTileDownloadListener) {
        RouteTileDownloader(offlineNavigator, tilePath, listener).startDownload(offlineTiles)
    }

    /**
     * Call this method to fetch the latest available offline tile versions that
     * can be used with [MapboxOfflineRouter.downloadTiles].
     *
     * @param accessToken Mapbox access token to call the version API
     * @param callback with the available versions
     */
    fun fetchAvailableTileVersions(accessToken: String, callback: OnTileVersionsFoundCallback) {
        offlineTileVersions.fetchRouteTileVersions(accessToken, callback)
    }

    /**
     * Removes tiles within / intersected by a bounding box
     *
     *
     * Note that calling [MapboxOfflineRouter.findRoute] while
     * [MapboxOfflineRouter.removeTiles] could lead
     * to undefine behavior
     *
     *
     * @param version version of offline tiles to use
     * @param boundingBox bounding box within which routing tiles should be removed
     * @param callback a callback that will be fired when the routing tiles have been removed completely
     */
    fun removeTiles(
        version: String,
        boundingBox: BoundingBox,
        callback: OnOfflineTilesRemovedCallback
    ) {
        offlineNavigator.removeTiles(
            File(tilePath, version).absolutePath, boundingBox.southwest(),
            boundingBox.northeast(), callback
        )
    }
}
