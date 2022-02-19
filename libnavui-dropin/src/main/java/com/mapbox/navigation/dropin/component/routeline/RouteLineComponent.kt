package com.mapbox.navigation.dropin.component.routeline

import android.util.Log
import com.mapbox.android.gestures.Utils
import com.mapbox.geojson.Point
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Style
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.route.toNavigationRoutes
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.dropin.component.map.MapEventProducer
import com.mapbox.navigation.dropin.component.routefetch.RoutesAction
import com.mapbox.navigation.dropin.component.routefetch.RoutesViewModel
import com.mapbox.navigation.dropin.extensions.flowRouteProgress
import com.mapbox.navigation.dropin.extensions.flowRoutesUpdated
import com.mapbox.navigation.ui.maps.route.line.MapboxRouteLineApiExtensions.findClosestRoute
import com.mapbox.navigation.ui.maps.route.line.MapboxRouteLineApiExtensions.setNavigationRouteLines
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineApi
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineView
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions
import com.mapbox.navigation.ui.maps.route.line.model.NavigationRouteLine
import com.mapbox.navigation.utils.internal.InternalJobControlFactory
import com.mapbox.navigation.utils.internal.ifNonNull
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
internal class RouteLineComponent(
    private val mapboxMap: MapboxMap,
    private val options: MapboxRouteLineOptions,
    private val mapEventProducer: MapEventProducer,
    private val routesViewModel: RoutesViewModel,
    private val routeLineApi: MapboxRouteLineApi = MapboxRouteLineApi(options),
    private val routeLineView: MapboxRouteLineView = MapboxRouteLineView(options)

) : MapboxNavigationObserver {

    private val jobControl = InternalJobControlFactory.createMainScopeJobControl()
    private var mapStyle: Style? = null
    private val routeClickPadding = Utils.dpToPx(30f)

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        jobControl.scope.launch {
            mapEventProducer.mapClicks.collect { point ->
                selectRoute(point)
            }
        }

        jobControl.scope.launch {
            mapEventProducer.mapStyleUpdates.collect {
                mapStyle = it
            }
        }

        jobControl.scope.launch {
            mapEventProducer.positionChanges.collect { point ->
                val result = routeLineApi.updateTraveledRouteLine(point)
                ifNonNull(mapStyle) { style ->
                    routeLineView.renderRouteLineUpdate(style, result)
                }
            }
        }

        jobControl.scope.launch {
            mapboxNavigation.flowRouteProgress().collect { routeProgress ->
                ifNonNull(mapStyle) { style ->
                    routeLineApi.updateWithRouteProgress(routeProgress) { result ->
                        routeLineView.renderRouteLineUpdate(style, result).also {
                            result.error?.let {
                                Log.e(TAG, it.errorMessage, it.throwable)
                            }
                        }
                    }
                }
            }
        }

        jobControl.scope.launch {
            mapboxNavigation.flowRoutesUpdated().collect { result ->
                val routeLines = result.navigationRoutes.map {
                    NavigationRouteLine(it, null)
                }
                routeLineApi.setNavigationRouteLines(routeLines).let { routeDrawData ->
                    ifNonNull(mapStyle) { style ->
                        routeLineView.renderRouteDrawData(style, routeDrawData)
                    }
                }
            }
        }
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        jobControl.job.cancelChildren()
        routeLineApi.cancel()
        routeLineView.cancel()
    }

    private fun selectRoute(point: Point) {
        jobControl.scope.launch {
            val result = routeLineApi.findClosestRoute(
                point,
                mapboxMap,
                routeClickPadding
            )

            result.onValue { resultValue ->
                if (resultValue.route != routeLineApi.getPrimaryRoute()) {
                    val reOrderedRoutes = routeLineApi.getRoutes()
                        .filter { it != resultValue.route }
                        .toMutableList()
                        .also {
                            it.add(0, resultValue.route)
                        }
                    routesViewModel.invoke(
                        RoutesAction.SetRoutes(reOrderedRoutes.toNavigationRoutes())
                    )
                }
            }
        }
    }

    private companion object {
        private val TAG = RouteLineComponent::class.java.simpleName
    }
}
