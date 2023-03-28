package com.mapbox.navigation.ui.maps.route.line.api

internal class DataIdHolder {

    private val sourceIdToDataIdAndRouteId = mutableMapOf<String, Int>()
    fun incrementDataId(sourceId: String): Int {
        val newDataId = (sourceIdToDataIdAndRouteId[sourceId] ?: 0) + 1
        sourceIdToDataIdAndRouteId[sourceId] = newDataId
        return newDataId
    }
}
