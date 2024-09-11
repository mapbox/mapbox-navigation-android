package com.mapbox.navigation.core.navigator

import com.mapbox.navigation.core.RoadGraphDataUpdateCallback
import com.mapbox.navigator.CacheHandleInterface

private typealias SDKRoadGraphVersionInfo = com.mapbox.navigation.core.RoadGraphVersionInfo

internal object CacheHandleWrapper {

    fun requestRoadGraphDataUpdate(
        cache: CacheHandleInterface,
        callback: RoadGraphDataUpdateCallback,
    ) {
        cache.isRoadGraphDataUpdateAvailable { isUpdateAvailable, newVersionInfo ->
            callback.onRoadGraphDataUpdateInfoAvailable(
                isUpdateAvailable,
                newVersionInfo?.let { SDKRoadGraphVersionInfo(it.dataset, it.version) },
            )
        }
    }
}
