package com.mapbox.navigation.ui.maps.installer

import android.content.Context
import com.mapbox.maps.MapView
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.ui.base.installer.ComponentInstaller
import com.mapbox.navigation.ui.base.installer.Installation
import com.mapbox.navigation.ui.maps.internal.ui.RouteArrowComponent
import com.mapbox.navigation.ui.maps.internal.ui.RouteLineComponent
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowView
import com.mapbox.navigation.ui.maps.route.arrow.model.RouteArrowOptions
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineApi
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineView
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineResources

/**
 * Install component that renders route line on the map.
 *
 * The installed component:
 * - renders route lines returned by [RoutesObserver]
 * - vanishes the traveled portion of the route line (if enabled via [MapboxRouteLineOptions] in the configuration)
 * - selects alternative route on map click
 */
@ExperimentalPreviewMapboxNavigationAPI
fun ComponentInstaller.routeLine(
    mapView: MapView,
    config: RouteLineComponentConfig.() -> Unit = {}
): Installation {
    val componentConfig = RouteLineComponentConfig(mapView.context).apply(config)
    return component(RouteLineComponent(mapView.getMapboxMap(), mapView, componentConfig.options))
}

/**
 * Install component that renders route line arrows on the map.
 *
 * The installed component registers itself as a [RouteProgressObserver] and
 * renders upcoming maneuver arrows on the map.
 */
@ExperimentalPreviewMapboxNavigationAPI
fun ComponentInstaller.routeArrow(
    mapView: MapView,
    config: RouteArrowComponentConfig.() -> Unit = {}
): Installation {
    val componentConfig = RouteArrowComponentConfig(mapView.context).apply(config)
    return component(RouteArrowComponent(mapView, componentConfig.options))
}

/**
 * Route line component configuration class.
 */
@ExperimentalPreviewMapboxNavigationAPI
class RouteLineComponentConfig internal constructor(context: Context) {
    /**
     * Options used to create [MapboxRouteLineApi] and [MapboxRouteLineView] instance.
     */
    var options = MapboxRouteLineOptions.Builder(context)
        .withRouteLineResources(RouteLineResources.Builder().build())
        .build()
}

/**
 * Route arrow component configuration class.
 */
@ExperimentalPreviewMapboxNavigationAPI
class RouteArrowComponentConfig internal constructor(context: Context) {
    /**
     * Options used to create [MapboxRouteArrowView] instance.
     */
    var options = RouteArrowOptions.Builder(context).build()
}
