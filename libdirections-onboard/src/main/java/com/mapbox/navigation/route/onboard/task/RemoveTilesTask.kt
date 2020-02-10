package com.mapbox.navigation.route.onboard.task

import com.mapbox.geojson.Point
import com.mapbox.navigation.navigator.MapboxNativeNavigator
import com.mapbox.navigation.route.onboard.OnOfflineTilesRemovedCallback
import com.mapbox.navigation.utils.thread.ThreadController
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
// import kotlinx.coroutines.withContext

internal class RemoveTilesTask(
    private val navigator: MapboxNativeNavigator,
    private val tilePath: String,
    private val southwest: Point,
    private val northeast: Point,
    private val callback: OnOfflineTilesRemovedCallback
) {

    private val mainJobControl = ThreadController.getMainScopeAndRootJob()

    fun launch() {
        mainJobControl.scope.launch {
            navigator.removeTiles(tilePath, southwest, northeast) { numberOfTiles ->
                callback.onRemoved(numberOfTiles)
            }
            // val numberOfTiles = withContext(ThreadController.IODispatcher) {
            //     navigator.removeTiles(tilePath, southwest, northeast)
            // }
            //
            // callback.onRemoved(numberOfTiles)
        }
    }

    fun cancel() {
        mainJobControl.job.cancelChildren()
    }
}
