package com.mapbox.navigation.ui.maps.route.line.api

internal class ExpectedRoutesToRenderData {

    private val renderedSourceIdToDataIdAndRouteId = mutableMapOf<String, Pair<Int, String>>()
    private val clearedSourceIdToDataIdAndRouteId = mutableMapOf<String, Pair<Int, String>>()

    val allRenderedRouteIds: Set<String>
        get() = renderedSourceIdToDataIdAndRouteId.map { it.value.second }.toSet()
    val allClearedRouteIds: Set<String>
        get() = clearedSourceIdToDataIdAndRouteId.map { it.value.second }.toSet()

    fun addRenderedRoute(sourceId: String, dataId: Int, routeId: String?) {
        if (routeId != null) {
            renderedSourceIdToDataIdAndRouteId[sourceId] = dataId to routeId
        }
    }

    fun addClearedRoute(sourceId: String, dataId: Int, routeId: String?) {
        if (routeId != null) {
            clearedSourceIdToDataIdAndRouteId[sourceId] = dataId to routeId
        }
    }

    fun isEmpty(): Boolean = renderedSourceIdToDataIdAndRouteId.isEmpty() &&
        clearedSourceIdToDataIdAndRouteId.isEmpty()

    fun getRenderedRouteId(sourceId: String): String? =
        renderedSourceIdToDataIdAndRouteId[sourceId]?.second

    fun getClearedRouteId(sourceId: String): String? =
        clearedSourceIdToDataIdAndRouteId[sourceId]?.second

    fun getSourceAndDataIds(): List<Pair<String, Int>> =
        (renderedSourceIdToDataIdAndRouteId + clearedSourceIdToDataIdAndRouteId).map {
            it.key to it.value.first
        }
}
