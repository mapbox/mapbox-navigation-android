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
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.internal.extensions.flowRouteProgress
import com.mapbox.navigation.core.internal.extensions.flowRoutesUpdated
import com.mapbox.navigation.ui.base.lifecycle.UIComponent
import com.mapbox.navigation.ui.maps.route.line.MapboxRouteLineApiExtensions.findClosestRoute
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineApi
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineView
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions
import com.mapbox.navigation.ui.utils.internal.Provider
import com.mapbox.navigation.utils.internal.ifNonNull
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@ExperimentalPreviewMapboxNavigationAPI
interface RouteLineComponentContract {
    fun setRoutes(mapboxNavigation: MapboxNavigation, routes: List<NavigationRoute>)

    fun getRouteInPreview(): Flow<List<NavigationRoute>?>

    fun onMapClicked(point: Point)
}

@ExperimentalPreviewMapboxNavigationAPI
internal class MapboxRouteLineComponentContract : RouteLineComponentContract {
    override fun setRoutes(mapboxNavigation: MapboxNavigation, routes: List<NavigationRoute>) {
        mapboxNavigation.setNavigationRoutes(routes)
    }

    override fun getRouteInPreview(): Flow<List<NavigationRoute>?> {
        return flowOf(null)
    }

    override fun onMapClicked(point: Point) {
        // do nothing
    }
}

@ExperimentalPreviewMapboxNavigationAPI
class RouteLineComponent(
    private val mapboxMap: MapboxMap,
    private val mapPlugins: MapPluginProviderDelegate,
    private val options: MapboxRouteLineOptions,
    private val routeLineApi: MapboxRouteLineApi = MapboxRouteLineApi(options),
    private val routeLineView: MapboxRouteLineView = MapboxRouteLineView(options),
    contractProvider: Provider<RouteLineComponentContract>? = null
) : UIComponent() {

    private val contractProvider: Provider<RouteLineComponentContract>

    init {
        this.contractProvider = contractProvider ?: Provider {
            MapboxRouteLineComponentContract()
        }
    }

    private val routeClickPadding = Utils.dpToPx(30f)

    private val onMapClickListener = OnMapClickListener { point ->
        mapboxNavigation?.also { selectRoute(it, point) }
        false
    }

    private val onPositionChangedListener = OnIndicatorPositionChangedListener { point ->
        val result = routeLineApi.updateTraveledRouteLine(point)
        mapboxMap.getStyle()?.apply {
            routeLineView.renderRouteLineUpdate(this, result)
        }
    }

    private var mapboxNavigation: MapboxNavigation? = null

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)
        this.mapboxNavigation = mapboxNavigation
        mapPlugins.gestures.addOnMapClickListener(onMapClickListener)
        mapPlugins.location.addOnIndicatorPositionChangedListener(onPositionChangedListener)

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

        coroutineScope.launch {
            val routesFlow = mapboxNavigation.flowRoutesUpdated()
                .map { it.navigationRoutes }
                .stateIn(
                    this,
                    SharingStarted.WhileSubscribed(),
                    mapboxNavigation.getNavigationRoutes()
                )
            val routePreviewFlow = contractProvider.get().getRouteInPreview()
            combine(routesFlow, routePreviewFlow) { navigationRoutes, previewRoutes ->
                if (navigationRoutes.isNotEmpty()) {
                    navigationRoutes
                } else if (!previewRoutes.isNullOrEmpty()) {
                    previewRoutes
                } else {
                    emptyList()
                }
            }.collect { routes ->
                routeLineApi.setNavigationRoutes(
                    routes,
                    mapboxNavigation.getAlternativeMetadataFor(routes)
                ) { value ->
                    mapboxMap.getStyle()?.apply {
                        routeLineView.renderRouteDrawData(this, value)
                    }
                }
            }
        }
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        super.onDetached(mapboxNavigation)
        mapPlugins.location.removeOnIndicatorPositionChangedListener(onPositionChangedListener)
        routeLineApi.cancel()
        routeLineView.cancel()
        this.mapboxNavigation = null
    }

    private fun selectRoute(mapboxNavigation: MapboxNavigation, point: Point) {
        coroutineScope.launch {
            routeLineApi.findClosestRoute(point, mapboxMap, routeClickPadding).fold(
                { contractProvider.get().onMapClicked(point) },
                { result ->
                    if (result.navigationRoute != routeLineApi.getPrimaryNavigationRoute()) {
                        val reOrderedRoutes = arrayListOf(result.navigationRoute)
                        routeLineApi.getNavigationRoutes().filterTo(reOrderedRoutes) { route ->
                            route != result.navigationRoute
                        }
                        contractProvider.get().setRoutes(mapboxNavigation, reOrderedRoutes)
                    } else {
                        contractProvider.get().onMapClicked(point)
                    }
                },
            )
        }
    }

    private companion object {
        private val TAG = RouteLineComponent::class.java.simpleName
    }
}
