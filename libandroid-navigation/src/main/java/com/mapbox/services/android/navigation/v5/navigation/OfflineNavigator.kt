package com.mapbox.services.android.navigation.v5.navigation

import com.mapbox.geojson.Point
import com.mapbox.navigator.Navigator
import com.mapbox.navigator.TileEndpointConfiguration

internal class OfflineNavigator @JvmOverloads constructor(
    private val navigator: Navigator,
    private val version: String = "",
    private val host: String = "",
    private val accessToken: String = ""
) {

    /**
     * Configures the navigator for getting offline routes
     *
     * @param tilePath directory path where the tiles are located
     * @param callback a callback that will be fired when the offline data is initialized and
     * [MapboxOfflineRouter.findRoute]
     * can be called safely
     */
    fun configure(tilePath: String, callback: OnOfflineTilesConfiguredCallback) {
        ConfigureRouterTask(
            navigator,
            tilePath,
            TileEndpointConfiguration(host, version, accessToken, ""),
            callback
        ).execute()
    }

    /**
     * Uses libvalhalla and local tile data to generate mapbox-directions-api-like json
     *
     * @param offlineRoute an offline navigation route
     * @param callback which receives a RouterResult object with the json and a success/fail bool
     */
    fun retrieveRouteFor(offlineRoute: OfflineRoute, callback: OnOfflineRouteFoundCallback) {
        OfflineRouteRetrievalTask(navigator, callback).execute(offlineRoute)
    }

    /**
     * Unpacks tar file into a specified destination path.
     *
     * @param tarPath to find file to be unpacked
     * @param destinationPath where the tar will be unpacked
     */
    fun unpackTiles(tarPath: String, destinationPath: String) {
        navigator.unpackTiles(tarPath, destinationPath)
    }

    /**
     * Removes tiles within / intersected by a bounding box
     *
     * @param tilePath directory path where the tiles are located
     * @param southwest lower left [Point] of the bounding box
     * @param northeast upper right [Point] of the bounding box
     * @param callback a callback that will be fired when the routing tiles have been removed completely
     */
    fun removeTiles(
        tilePath: String,
        southwest: Point,
        northeast: Point,
        callback: OnOfflineTilesRemovedCallback
    ) {
        RemoveTilesTask(navigator, tilePath, southwest, northeast, callback).execute()
    }
}
