package com.mapbox.navigation.core.navigator

import com.mapbox.navigation.core.RoadGraphDataUpdateCallback
import com.mapbox.navigator.TilesManagerHandleInterface
import com.mapbox.navigation.core.RoadGraphVersionInfo as SDKRoadGraphVersionInfo

internal object TilesManagerHandleWrapper {

    fun requestRoadGraphDataUpdate(
        tilesManagerHandle: TilesManagerHandleInterface,
        callback: RoadGraphDataUpdateCallback,
    ) {
        tilesManagerHandle.checkUpdateAvailable { isUpdateAvailable, newVersionInfo ->
            callback.onRoadGraphDataUpdateInfoAvailable(
                isUpdateAvailable,
                newVersionInfo?.let { SDKRoadGraphVersionInfo(it.dataset, it.version) },
            )
        }
    }

    fun requestHDGraphDataUpdate(
        tilesManagerHandle: TilesManagerHandleInterface,
        callback: RoadGraphDataUpdateCallback,
    ) {
        tilesManagerHandle.checkHDUpdateAvailable { isUpdateAvailable, newVersionInfo ->
            callback.onRoadGraphDataUpdateInfoAvailable(
                isUpdateAvailable,
                newVersionInfo?.let { SDKRoadGraphVersionInfo(it.dataset, it.version) },
            )
        }
    }
}
