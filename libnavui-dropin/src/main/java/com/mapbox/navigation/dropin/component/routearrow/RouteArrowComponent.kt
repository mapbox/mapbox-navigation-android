package com.mapbox.navigation.dropin.component.routearrow

import com.mapbox.maps.MapView
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowApi
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowView
import com.mapbox.navigation.ui.maps.route.arrow.model.RouteArrowOptions
import com.mapbox.navigation.utils.internal.ifNonNull

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
internal class RouteArrowComponent(
    private val mapView: MapView,
    private val routeArrowOptions: RouteArrowOptions,
    private val routeArrowApi: MapboxRouteArrowApi = MapboxRouteArrowApi(),
    private val routeArrowView: MapboxRouteArrowView = MapboxRouteArrowView(routeArrowOptions),
) : MapboxNavigationObserver {

    private val routeProgressObserver = RouteProgressObserver { routeProgress ->
        ifNonNull(mapView.getMapboxMap().getStyle()) { style ->
            val arrowUpdate = routeArrowApi.addUpcomingManeuverArrow(routeProgress)
            routeArrowView.renderManeuverUpdate(style, arrowUpdate)
        }
    }

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        mapboxNavigation.registerRouteProgressObserver(routeProgressObserver)
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        mapboxNavigation.unregisterRouteProgressObserver(routeProgressObserver)
    }
}
