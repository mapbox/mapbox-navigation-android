package com.mapbox.navigation.ui.maps

import android.content.Context
import com.mapbox.maps.MapView
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.ui.base.ComponentConfig
import com.mapbox.navigation.ui.maps.internal.ui.RouteArrowComponent
import com.mapbox.navigation.ui.maps.internal.ui.RouteLineComponent
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants
import com.mapbox.navigation.ui.maps.route.arrow.model.RouteArrowOptions
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineResources

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
fun ComponentConfig.routeLineComponent(
    mapView: MapView,
    config: RouteLineComponentConfig.() -> Unit = {}
) {
    val componentConfig = RouteLineComponentConfig(mapView.context).apply(config)
    component(RouteLineComponent(mapView, componentConfig.options))
}

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
fun ComponentConfig.routeArrowComponent(
    mapView: MapView,
    config: RouteArrowComponentConfig.() -> Unit = {}
) {
    val componentConfig = RouteArrowComponentConfig(mapView.context).apply(config)
    component(RouteArrowComponent(mapView, componentConfig.options))
}

class RouteLineComponentConfig(context: Context) {
    var options = MapboxRouteLineOptions.Builder(context)
        .withRouteLineResources(RouteLineResources.Builder().build())
        .withRouteLineBelowLayerId("road-label-navigation")
        .withVanishingRouteLineEnabled(true)
        .build()
}

class RouteArrowComponentConfig internal constructor(context: Context) {
    var options = RouteArrowOptions.Builder(context)
        .withAboveLayerId(RouteLayerConstants.TOP_LEVEL_ROUTE_LINE_LAYER_ID)
        .build()
}
