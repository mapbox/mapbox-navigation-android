package com.mapbox.navigation.core.routerefresh

internal class EVDataHolder {

    private val currentData = mutableMapOf<String, String>()

    @Synchronized
    fun onEVDataUpdated(data: Map<String, String?>) {
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
