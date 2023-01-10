package com.mapbox.navigation.ui.maps.internal.ui

import com.mapbox.maps.MapboxMap
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.internal.extensions.flowRouteProgress
import com.mapbox.navigation.core.internal.extensions.flowRoutesUpdated
import com.mapbox.navigation.ui.base.lifecycle.UIComponent
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowApi
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowView
import com.mapbox.navigation.ui.maps.route.arrow.model.RouteArrowOptions
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

class RouteArrowComponent(
    private val mapboxMap: MapboxMap,
    private val routeArrowOptions: RouteArrowOptions,
    private val routeArrowApi: MapboxRouteArrowApi = MapboxRouteArrowApi(),
    private val routeArrowView: MapboxRouteArrowView = MapboxRouteArrowView(routeArrowOptions),
) : UIComponent() {

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)
        coroutineScope.launch {
            mapboxNavigation.flowRouteProgress().collect { routeProgress ->
                val arrowUpdate = routeArrowApi.addUpcomingManeuverArrow(routeProgress)
                mapboxMap.getStyle()?.let { style ->
                    routeArrowView.renderManeuverUpdate(style, arrowUpdate)
                }
            }
        }

        coroutineScope.launch {
            mapboxNavigation.flowRoutesUpdated().filter { it.navigationRoutes.isEmpty() }.collect {
                mapboxMap.getStyle()?.let { style ->
                    routeArrowView.render(style, routeArrowApi.clearArrows())
                }
            }
        }
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        super.onDetached(mapboxNavigation)
        mapboxMap.getStyle()?.also { style ->
            routeArrowView.render(style, routeArrowApi.clearArrows())
        }
    }
}
