package com.mapbox.navigation.ui.maps.route.line.api

import com.mapbox.maps.extension.observable.eventdata.SourceDataLoadedEventData
import com.mapbox.maps.plugin.delegates.listeners.OnSourceDataLoadedListener
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineFeatureId
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineSourceKey

internal class RoutesExpector(
    private val sourceToFeatureMap: MutableMap<RouteLineSourceKey, RouteLineFeatureId>
) {

    private var routeRenderCallbackData: RouteRenderCallbackData? = null

    fun expectNewRoutes(expectedRoutes: Set<String>, callback: RoutesRenderedCallback) {
        val renderedRoutes = mutableSetOf<String>()
        if (expectedRoutes.isEmpty()) {
            callback.onRoutesRendered(emptyList())
        } else {
            val map = callback.map
            routeRenderCallbackData?.let {
                map.removeOnSourceDataLoadedListener(it.listener)
                it.callback.onRoutesRenderingCancelled(it.expectedRoutes.toList())
            }
            routeRenderCallbackData = RouteRenderCallbackData(
                object : OnSourceDataLoadedListener {
                    override fun onSourceDataLoaded(eventData: SourceDataLoadedEventData) {
                        if (eventData.loaded == true) {
                            val routeId = sourceToFeatureMap[RouteLineSourceKey(eventData.id)]?.id()
                            routeId?.let { renderedRoutes.add(it) }
                            if (expectedRoutes == renderedRoutes) {
                                map.removeOnSourceDataLoadedListener(this)
                                routeRenderCallbackData = null
                                callback.onRoutesRendered(renderedRoutes.toList())
                            }
                        }
                    }
                }.also { map.addOnSourceDataLoadedListener(it) },
                callback,
                expectedRoutes
            )
        }
    }
}

private data class RouteRenderCallbackData(
    val listener: OnSourceDataLoadedListener,
    val callback: RoutesRenderedCallback,
    val expectedRoutes: Set<String>,
)
