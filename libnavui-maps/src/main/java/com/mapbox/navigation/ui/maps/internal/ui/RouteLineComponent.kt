package com.mapbox.navigation.ui.maps.internal.ui

import android.util.Log
import com.mapbox.android.gestures.Utils
import com.mapbox.geojson.Point
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.plugin.delegates.MapPluginProviderDelegate
import com.mapbox.maps.plugin.gestures.OnMapClickListener
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.internal.extensions.flowRouteProgress
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.ui.base.lifecycle.UIComponent
import com.mapbox.navigation.ui.maps.internal.ActiveRouteLineComponentContract
import com.mapbox.navigation.ui.maps.internal.RouteLineComponentContract
import com.mapbox.navigation.ui.maps.route.line.MapboxRouteLineApiExtensions.findClosestRoute
import com.mapbox.navigation.ui.maps.route.line.MapboxRouteLineApiExtensions.setNavigationRouteLines
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineApi
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineView
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions
import com.mapbox.navigation.ui.maps.route.line.model.NavigationRouteLine
import com.mapbox.navigation.utils.internal.ifNonNull
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@ExperimentalPreviewMapboxNavigationAPI
class RouteLineComponent(
    private val mapboxMap: MapboxMap,
    private val mapPlugins: MapPluginProviderDelegate,
    private val options: MapboxRouteLineOptions,
    private val routeLineApi: MapboxRouteLineApi = MapboxRouteLineApi(options),
    private val routeLineView: MapboxRouteLineView = MapboxRouteLineView(options),
    contract: RouteLineComponentContract? = null
) : UIComponent() {

    private val routeClickPadding = Utils.dpToPx(30f)
    val contract = contract ?: ActiveRouteLineComponentContract()

    private var onMapClickListener = OnMapClickListener { point ->
        selectRoute(point)
        false
    }

    private val onPositionChangedListener = OnIndicatorPositionChangedListener { point ->
        val result = routeLineApi.updateTraveledRouteLine(point)
        mapboxMap.getStyle()?.apply {
            routeLineView.renderRouteLineUpdate(this, result)
        }
    }

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)
        mapPlugins.gestures.addOnMapClickListener(onMapClickListener)
        mapPlugins.location.addOnIndicatorPositionChangedListener(onPositionChangedListener)

        when (contract) {
            is MapboxNavigationObserver -> {
                contract.onAttached(mapboxNavigation)
                coroutineScope.launch {
                    mapboxNavigation.flowRouteProgress().collect { routeProgress ->
                        ifNonNull(mapboxMap.getStyle()) { style ->
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
            }
        }

        coroutineScope.launch {
            contract.navigationRoutes.collect { navigationRoutes ->
                mapboxMap.getStyle()?.also { style ->
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
        when (contract) { is MapboxNavigationObserver -> contract.onDetached(mapboxNavigation) }
        mapPlugins.location.removeOnIndicatorPositionChangedListener(onPositionChangedListener)

        routeLineApi.cancel()
        routeLineView.cancel()
    }

    private fun selectRoute(point: Point) {
        coroutineScope.launch {
            val result = routeLineApi.findClosestRoute(
                point,
                mapboxMap,
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
                    contract.setRoutes(reOrderedRoutes)
                }
            }
        }
    }

    private companion object {
        private val TAG = RouteLineComponent::class.java.simpleName
    }
}
