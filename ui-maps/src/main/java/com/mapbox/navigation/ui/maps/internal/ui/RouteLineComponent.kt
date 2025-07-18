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
import com.mapbox.navigation.base.internal.extensions.internalAlternativeRouteIndices
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.trip.model.RouteProgressState
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.internal.RouteProgressData
import com.mapbox.navigation.core.internal.RoutesProgressData
import com.mapbox.navigation.core.internal.extensions.flowRouteProgress
import com.mapbox.navigation.core.internal.extensions.flowRoutesUpdated
import com.mapbox.navigation.ui.base.lifecycle.UIComponent
import com.mapbox.navigation.ui.maps.route.line.MapboxRouteLineApiExtensions.findClosestRoute
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineApi
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineView
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineApiOptions
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineViewDynamicOptionsBuilderBlock
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineViewOptions
import com.mapbox.navigation.ui.utils.internal.Provider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

interface RouteLineComponentContract {
    fun setRoutes(
        mapboxNavigation: MapboxNavigation,
        routes: List<NavigationRoute>,
        initialLegIndex: Int?,
    )

    fun getRouteInPreview(): Flow<List<NavigationRoute>?>

    fun onMapClicked(point: Point)
}

internal class MapboxRouteLineComponentContract : RouteLineComponentContract {
    override fun setRoutes(
        mapboxNavigation: MapboxNavigation,
        routes: List<NavigationRoute>,
        initialLegIndex: Int?,
    ) {
        if (initialLegIndex != null) {
            mapboxNavigation.setNavigationRoutes(routes, initialLegIndex)
        } else {
            mapboxNavigation.setNavigationRoutes(routes)
        }
    }

    override fun getRouteInPreview(): Flow<List<NavigationRoute>?> {
        return flowOf(null)
    }

    override fun onMapClicked(point: Point) {
        // do nothing
    }
}

class RouteLineComponent(
    private val mapboxMap: MapboxMap,
    private val mapPlugins: MapPluginProviderDelegate,
    private val apiOptions: MapboxRouteLineApiOptions,
    private val viewOptions: MapboxRouteLineViewOptions,
    private val routeLineApi: MapboxRouteLineApi = MapboxRouteLineApi(apiOptions),
    private val routeLineView: MapboxRouteLineView = MapboxRouteLineView(viewOptions),
    private val viewOptionsUpdatesFlow: Flow<MapboxRouteLineViewDynamicOptionsBuilderBlock> =
        flowOf(),
    contractProvider: Provider<RouteLineComponentContract>? = null,
) : UIComponent() {

    private var currentRoutesProgressData: RoutesProgressData? = null
    private val contractProvider = contractProvider ?: Provider {
        MapboxRouteLineComponentContract()
    }

    private val routeClickPadding = Utils.dpToPx(30f)

    private val onMapClickListener = OnMapClickListener { point ->
        mapboxNavigation?.also { selectRoute(it, point) }
        false
    }

    private val onPositionChangedListener = OnIndicatorPositionChangedListener { point ->
        val result = routeLineApi.updateTraveledRouteLine(point)
        mapboxMap.style?.apply {
            routeLineView.renderRouteLineUpdate(this, result)
        }
    }

    private var mapboxNavigation: MapboxNavigation? = null

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)
        this.mapboxNavigation = mapboxNavigation
        mapPlugins.gestures.addOnMapClickListener(onMapClickListener)
        mapPlugins.location.addOnIndicatorPositionChangedListener(onPositionChangedListener)

        // initialize route line layers to ensure they exist before
        // both RouteLineComponent and RouteArrowComponent start rendering
        mapboxMap.getStyle {
            routeLineView.initializeLayers(it)
        }

        coroutineScope.launch {
            viewOptionsUpdatesFlow.collect { options ->
                mapboxMap.style?.let { style ->
                    routeLineView.updateDynamicOptions(style, options)
                    routeLineApi.getRouteDrawData {
                        routeLineView.renderRouteDrawData(style, it)
                    }
                }
            }
        }
        coroutineScope.launch {
            mapboxNavigation.flowRouteProgress().collect { routeProgress ->
                if (routeProgress.currentState == RouteProgressState.TRACKING) {
                    currentRoutesProgressData = routeProgress.currentLegProgress?.let {
                        RoutesProgressData(
                            RouteProgressData(
                                it.legIndex,
                                routeProgress.currentRouteGeometryIndex,
                                it.geometryIndex,
                            ),
                            routeProgress.internalAlternativeRouteIndices().mapValues { entry ->
                                RouteProgressData(
                                    entry.value.legIndex,
                                    entry.value.routeGeometryIndex,
                                    entry.value.legGeometryIndex,
                                )
                            },
                        )
                    }
                }
                mapboxMap.style?.let { style ->
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
                .onEach { currentRoutesProgressData = null }
                .map { it.navigationRoutes to mapboxNavigation.currentLegIndex() }
                .stateIn(
                    this,
                    SharingStarted.WhileSubscribed(),
                    mapboxNavigation.getNavigationRoutes() to mapboxNavigation.currentLegIndex(),
                )
            val routePreviewFlow = contractProvider.get().getRouteInPreview().map { it to 0 }
            combine(routesFlow, routePreviewFlow) { navigationRoutesPair, previewRoutesPair ->
                if (navigationRoutesPair.first.isNotEmpty()) {
                    navigationRoutesPair
                } else if (!previewRoutesPair.first.isNullOrEmpty()) {
                    previewRoutesPair
                } else {
                    emptyList<NavigationRoute>() to 0
                }
            }.collect { routesToLegIndex ->
                val routes = routesToLegIndex.first!!
                routeLineApi.setNavigationRoutes(
                    routes,
                    routesToLegIndex.second,
                    mapboxNavigation.getAlternativeMetadataFor(routes),
                ) { value ->
                    mapboxMap.style?.apply {
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
        currentRoutesProgressData = null
        this.mapboxNavigation = null
    }

    private fun selectRoute(mapboxNavigation: MapboxNavigation, point: Point) {
        coroutineScope.launch {
            routeLineApi.findClosestRoute(point, mapboxMap, routeClickPadding).fold(
                { contractProvider.get().onMapClicked(point) },
                { result ->
                    if (result.navigationRoute != routeLineApi.getPrimaryNavigationRoute()) {
                        val reOrderedRoutes = arrayListOf(result.navigationRoute)
                        val legIndex = currentRoutesProgressData?.alternatives
                            ?.get(result.navigationRoute.id)?.legIndex
                        routeLineApi.getNavigationRoutes().filterTo(reOrderedRoutes) { route ->
                            route != result.navigationRoute
                        }
                        contractProvider.get().setRoutes(
                            mapboxNavigation,
                            reOrderedRoutes,
                            legIndex,
                        )
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
