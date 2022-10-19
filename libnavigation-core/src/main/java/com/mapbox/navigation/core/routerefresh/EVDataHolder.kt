package com.mapbox.navigation.core.routerefresh

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.EVDataObserver

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
internal class EVDataHolder : EVDataObserver {

    private val currentData = mutableMapOf<String, String>()

    @Synchronized
    override fun onEVDataUpdated(data: Map<String, String?>) {
        data.forEach { (key, value) ->
            if (value == null) {
                currentData.remove(key)
            } else {
                currentData[key] = value
            }
        }
    }

    @Synchronized
    fun currentData(): Map<String, String> = HashMap(currentData)
}
