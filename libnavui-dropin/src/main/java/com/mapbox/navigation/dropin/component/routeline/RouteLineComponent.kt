package com.mapbox.navigation.dropin.component.routeline

import android.util.Log
import com.mapbox.android.gestures.Utils
import com.mapbox.geojson.Point
import com.mapbox.maps.MapView
import com.mapbox.maps.plugin.gestures.OnMapClickListener
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.internal.extensions.flowRouteProgress
import com.mapbox.navigation.core.internal.extensions.flowRoutesUpdated
import com.mapbox.navigation.dropin.component.routefetch.RoutesAction
import com.mapbox.navigation.dropin.model.Store
import com.mapbox.navigation.ui.base.lifecycle.UIComponent
import com.mapbox.navigation.ui.maps.route.line.MapboxRouteLineApiExtensions.findClosestRoute
import com.mapbox.navigation.ui.maps.route.line.MapboxRouteLineApiExtensions.setNavigationRouteLines
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineApi
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineView
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions
import com.mapbox.navigation.ui.maps.route.line.model.NavigationRouteLine
import com.mapbox.navigation.utils.internal.ifNonNull
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@ExperimentalPreviewMapboxNavigationAPI
internal class RouteLineComponent(
    private val store: Store,
    private val mapView: MapView,
    private val options: MapboxRouteLineOptions,
    private val routeLineApi: MapboxRouteLineApi = MapboxRouteLineApi(options),
    private val routeLineView: MapboxRouteLineView = MapboxRouteLineView(options)
) : UIComponent() {

    private val routeClickPadding = Utils.dpToPx(30f)

    private val onMapClickListener = OnMapClickListener { point ->
        selectRoute(point)
        false
    }

    private val onPositionChangedListener = OnIndicatorPositionChangedListener { point ->
        val result = routeLineApi.updateTraveledRouteLine(point)
        mapView.getMapboxMap().getStyle()?.apply {
            routeLineView.renderRouteLineUpdate(this, result)
        }
    }

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)
        mapView.gestures.addOnMapClickListener(onMapClickListener)
        mapView.location.addOnIndicatorPositionChangedListener(onPositionChangedListener)

        coroutineScope.launch {
            mapboxNavigation.flowRouteProgress().collect { routeProgress ->
                ifNonNull(mapView.getMapboxMap().getStyle()) { style ->
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

        coroutineScope.launch {
            val routesFlow = mapboxNavigation.flowRoutesUpdated()
                .map { it.navigationRoutes }
                .stateIn(
                    this,
                    SharingStarted.WhileSubscribed(),
                    mapboxNavigation.getNavigationRoutes()
                )

            routesFlow.collect { navigationRoutes ->
                mapView.getMapboxMap().getStyle()?.also { style ->
                    val routeLines = navigationRoutes.map {
                        NavigationRouteLine(it, null)
                    }
                    val routeDrawData = routeLineApi.setNavigationRouteLines(routeLines)
                    routeLineView.renderRouteDrawData(style, routeDrawData)
                }
            }
        }
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        super.onDetached(mapboxNavigation)
        mapView.location.removeOnIndicatorPositionChangedListener(onPositionChangedListener)
        routeLineApi.cancel()
        routeLineView.cancel()
    }

    private fun selectRoute(point: Point) {
        coroutineScope.launch {
            val result = routeLineApi.findClosestRoute(
                point,
                mapView.getMapboxMap(),
                routeClickPadding
            )

            result.onValue { resultValue ->
                if (resultValue.navigationRoute != routeLineApi.getPrimaryNavigationRoute()) {
                    val reOrderedRoutes = routeLineApi.getNavigationRoutes()
                        .filter { it != resultValue.navigationRoute }
                        .toMutableList()
                        .also {
                            it.add(0, resultValue.navigationRoute)
                        }
                    store.dispatch(RoutesAction.SetRoutes(reOrderedRoutes))
                }
            }
        }
    }

    private companion object {
        private val TAG = RouteLineComponent::class.java.simpleName
    }
}
