package com.mapbox.androidauto.car.preview

import com.mapbox.androidauto.car.MainCarContext
import com.mapbox.androidauto.car.internal.extensions.flowStyle
import com.mapbox.androidauto.car.internal.extensions.getStyleAsync
import com.mapbox.maps.MapboxExperimental
import com.mapbox.maps.extension.androidauto.MapboxCarMapObserver
import com.mapbox.maps.extension.androidauto.MapboxCarMapSurface
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.ui.maps.internal.ui.RouteArrowComponent
import com.mapbox.navigation.ui.maps.internal.ui.RouteLineComponent
import com.mapbox.navigation.ui.maps.internal.ui.RouteLineComponentContract
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowApi
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowView
import com.mapbox.navigation.ui.maps.route.arrow.model.RouteArrowOptions
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineApi
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineView
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

/**
 * This class is to simplify the interaction with [MapboxRouteLineApi], [MapboxRouteArrowView]
 * [MapboxRouteArrowApi], and [RouteProgressObserver] use cases that the app needs in the car.
 *
 * Anything for rendering the car's route line, is handled here at this point.
 */
@MapboxExperimental
@ExperimentalPreviewMapboxNavigationAPI
@OptIn(FlowPreview::class)
class CarRouteLine constructor(
    private val mainCarContext: MainCarContext,
    private val contract: RouteLineComponentContract = MapboxRouteLineComponentContract(),
    private val routeLineOptions: MapboxRouteLineOptions? = null,
    private val routeArrowOptions: RouteArrowOptions? = null,
) : MapboxCarMapObserver {

    private var routeLineComponent: RouteLineComponent? = null
    private var routeArrowComponent: RouteArrowComponent? = null
    private lateinit var scope: CoroutineScope

    override fun onAttached(mapboxCarMapSurface: MapboxCarMapSurface) {
        attachRouteLine(mapboxCarMapSurface)
        attachRouteArrow(mapboxCarMapSurface)
    }

    private fun attachRouteLine(mapboxCarMapSurface: MapboxCarMapSurface) {
        scope = MainScope()
        val options = routeLineOptions
            ?: MapboxRouteLineOptions.Builder(mapboxCarMapSurface.carContext)
                .withRouteLineBelowLayerId("road-label-navigation")
                .build()
        val routeLineView = MapboxRouteLineView(options)
        routeLineComponent = RouteLineComponent(
            mapboxMap = mapboxCarMapSurface.mapSurface.getMapboxMap(),
            mapPlugins = mapboxCarMapSurface.mapSurface,
            options = options,
            routeLineView = routeLineView,
            contractProvider = { contract }
        ).also { routeLineComponent ->
            mapboxCarMapSurface.getStyleAsync {
                MapboxNavigationApp.registerObserver(routeLineComponent)
            }

            scope.launch {
                combine(
                    mapboxCarMapSurface.flowStyle(),
                    mainCarContext.routeAlternativesEnabled
                ) { style, alternativesEnabled ->
                    if (alternativesEnabled) {
                        routeLineView.showAlternativeRoutes(style)
                    } else {
                        routeLineView.hideAlternativeRoutes(style)
                    }
                }.collect()
            }
        }
    }

    private fun attachRouteArrow(mapboxCarMapSurface: MapboxCarMapSurface) {
        routeArrowComponent = RouteArrowComponent(
            mapboxMap = mapboxCarMapSurface.mapSurface.getMapboxMap(),
            routeArrowOptions = routeArrowOptions
                ?: RouteArrowOptions.Builder(mapboxCarMapSurface.carContext)
                    .build(),
        ).also { routeArrowComponent ->
            mapboxCarMapSurface.getStyleAsync {
                MapboxNavigationApp.registerObserver(routeArrowComponent)
            }
        }
    }

    override fun onDetached(mapboxCarMapSurface: MapboxCarMapSurface) {
        scope.cancel()
        routeLineComponent?.let { MapboxNavigationApp.unregisterObserver(it) }
        routeLineComponent = null
        routeArrowComponent?.let { MapboxNavigationApp.unregisterObserver(it) }
        routeLineComponent = null
    }
}

// TODO this a is temporary duplication of ActiveRouteLineComponentContract
//   available from the RouteLineComponent
@ExperimentalPreviewMapboxNavigationAPI
class MapboxRouteLineComponentContract : RouteLineComponentContract {
    override fun setRoutes(mapboxNavigation: MapboxNavigation, routes: List<NavigationRoute>) {
        mapboxNavigation.setNavigationRoutes(routes)
    }

    override fun getRouteInPreview(): Flow<List<NavigationRoute>?> {
        return flowOf(null)
    }
}
