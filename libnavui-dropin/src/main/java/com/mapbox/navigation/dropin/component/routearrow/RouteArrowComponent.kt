package com.mapbox.navigation.dropin.component.routearrow

import com.mapbox.maps.MapView
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.internal.extensions.flowRouteProgress
import com.mapbox.navigation.ui.base.lifecycle.UIComponent
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowApi
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowView
import com.mapbox.navigation.ui.maps.route.arrow.model.RouteArrowOptions
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@ExperimentalPreviewMapboxNavigationAPI
internal class RouteArrowComponent(
    private val mapView: MapView,
    private val routeArrowOptions: RouteArrowOptions,
    private val routeArrowApi: MapboxRouteArrowApi = MapboxRouteArrowApi(),
    private val routeArrowView: MapboxRouteArrowView = MapboxRouteArrowView(routeArrowOptions),
) : UIComponent() {

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)
        coroutineScope.launch {
            mapboxNavigation.flowRouteProgress().collect { routeProgress ->
                mapView.getMapboxMap().getStyle()?.also { style ->
                    val arrowUpdate = routeArrowApi.addUpcomingManeuverArrow(routeProgress)
                    routeArrowView.renderManeuverUpdate(style, arrowUpdate)
                }
            }
        }
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        super.onDetached(mapboxNavigation)
        mapView.getMapboxMap().getStyle()?.also { style ->
            routeArrowView.render(style, routeArrowApi.clearArrows())
        }
    }
}
