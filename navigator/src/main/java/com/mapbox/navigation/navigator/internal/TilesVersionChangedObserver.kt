package com.mapbox.navigation.navigator.internal

import com.mapbox.navigator.RoadGraphVersionInfo
import com.mapbox.navigator.TilesVersionChangeReason

/**
 * Notified after the navigator switched to another tiles version.
 * Called on the main thread.
 */
fun interface TilesVersionChangedObserver {

    fun onTilesVersionChanged(versionInfo: RoadGraphVersionInfo, reason: TilesVersionChangeReason)
}
