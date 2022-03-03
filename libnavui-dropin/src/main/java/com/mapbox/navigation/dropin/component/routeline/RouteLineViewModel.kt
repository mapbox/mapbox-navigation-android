package com.mapbox.navigation.dropin.component.routeline

import com.mapbox.geojson.Point
import com.mapbox.maps.MapboxMap
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.UICommandDispatcher
import com.mapbox.navigation.dropin.lifecycle.UICommand
import com.mapbox.navigation.dropin.lifecycle.UIViewModel
import com.mapbox.navigation.ui.maps.route.line.MapboxRouteLineApiExtensions.findClosestRoute
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineApi
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch

class RouteLineViewModel(
    val commandDispatcher: UICommandDispatcher,
    private val routeLineOptions: MapboxRouteLineOptions,
    private val routeProgressBehavior: RouteProgressBehavior
): UIViewModel() {

    private val routeLineApi = MapboxRouteLineApi(routeLineOptions)

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)
        mainJobControl.scope.launch {
            commandDispatcher.commandFlow.filterIsInstance<UICommand.RouteLineCommand>().collect {
                when (it) {
                    is UICommand.RouteLineCommand.SelectRoute -> {
                        selectRoute(
                            point = it.point,
                            map = it.map,
                            clickPadding = it.clickPadding,
                            mapboxNavigation = mapboxNavigation
                        )
                    }
                }
            }
        }
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        super.onDetached(mapboxNavigation)
    }

    private suspend fun selectRoute(
        point: Point,
        map: MapboxMap,
        clickPadding: Float,
        mapboxNavigation: MapboxNavigation,
    ) {
        val result = routeLineApi.findClosestRoute(
            target = point,
            mapboxMap = map,
            padding = clickPadding
        )

        val routeFound = result.value?.route
        if (routeFound != null && routeFound != routeLineApi.getPrimaryRoute()) {
            val reOrderedRoutes = routeLineApi.getRoutes()
                .filter { it != routeFound }
                .toMutableList()
                .also {
                    it.add(0, routeFound)
                }
            mapboxNavigation.setRoutes(reOrderedRoutes)
        }
    }
}
