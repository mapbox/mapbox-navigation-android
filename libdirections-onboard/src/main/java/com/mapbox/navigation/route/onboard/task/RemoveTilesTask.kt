package com.mapbox.navigation.route.onboard.task

import com.mapbox.geojson.Point
import com.mapbox.navigation.navigator.MapboxNativeNavigator
import com.mapbox.navigation.route.onboard.OnOfflineTilesRemovedCallback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class RemoveTilesTask(
    private val navigator: MapboxNativeNavigator,
    private val tilePath: String,
    private val southwest: Point,
    private val northeast: Point,
    private val callback: OnOfflineTilesRemovedCallback
) {

    private val scope = MainScope()

    fun launch() {
        scope.launch {
            val numberOfTiles = withContext(Dispatchers.Default) {
                navigator.removeTiles(tilePath, southwest, northeast)
            }

            callback.onRemoved(numberOfTiles)
        }
    }

    fun cancel() {
        scope.cancel()
    }
}
