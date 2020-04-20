package com.mapbox.navigation.route.onboard

import com.mapbox.geojson.BoundingBox

/**
 * Listener that needs to be added to
 * [com.mapbox.navigation.route.onboard.task.RemoveTilesTask] to know when the routing
 * tiles within the provided [BoundingBox] have been removed
 */
internal interface OnOfflineTilesRemovedCallback {

    /**
     * Called when the routing tiles within the provided [BoundingBox] have been removed completely.
     *
     * @param numberOfTiles removed within the [BoundingBox] provided
     */
    fun onRemoved(numberOfTiles: Long)
}
