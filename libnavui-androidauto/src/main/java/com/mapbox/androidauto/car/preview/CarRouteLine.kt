package com.mapbox.androidauto.car.preview

import com.mapbox.androidauto.car.MainCarContext
import com.mapbox.maps.MapboxExperimental
import com.mapbox.maps.extension.androidauto.MapboxCarMapObserver
import com.mapbox.maps.extension.androidauto.MapboxCarMapSurface
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.ui.maps.internal.RouteLineComponentContract
import com.mapbox.navigation.ui.maps.internal.ui.RouteLineComponent
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowApi
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowView
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineApi
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineResources

/**
 * This class is to simplify the interaction with [MapboxRouteLineApi], [MapboxRouteArrowView]
 * [MapboxRouteArrowApi], and [RouteProgressObserver] use cases that the app needs in the car.
 *
 * Anything for rendering the car's route line, is handled here at this point.
 */
@MapboxExperimental
@ExperimentalPreviewMapboxNavigationAPI
class CarRouteLine(
    val mainCarContext: MainCarContext,
    private val options: MapboxRouteLineOptions? = null,
    private val contract: RouteLineComponentContract? = null
) : MapboxCarMapObserver {

    private var routeLineComponent: RouteLineComponent? = null

    override fun onAttached(mapboxCarMapSurface: MapboxCarMapSurface) {
        routeLineComponent = RouteLineComponent(
            mapboxMap = mapboxCarMapSurface.mapSurface.getMapboxMap(),
            mapPlugins = mapboxCarMapSurface.mapSurface,
            options = options ?: MapboxRouteLineOptions.Builder(mapboxCarMapSurface.carContext)
                .withRouteLineResources(RouteLineResources.Builder().build())
                .build(),
            contract = contract
        ).also {
            MapboxNavigationApp.registerObserver(it)
        }
    }

    override fun onDetached(mapboxCarMapSurface: MapboxCarMapSurface) {
        routeLineComponent?.let { MapboxNavigationApp.unregisterObserver(it) }
        routeLineComponent = null
    }
}
