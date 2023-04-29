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
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.trip.model.RouteProgressState
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.internal.AlternativeDataProvider
import com.mapbox.navigation.core.internal.RouteProgressData
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
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

interface RouteLineComponentContract {
    fun setRoutes(
        mapboxNavigation: MapboxNavigation,
        routes: List<NavigationRoute>,
        initialLegIndex: Int?
    )

    fun getRouteInPreview(): Flow<List<NavigationRoute>?>

    fun onMapClicked(point: Point)
}

internal class MapboxRouteLineComponentContract : RouteLineComponentContract {
    override fun setRoutes(
        mapboxNavigation: MapboxNavigation,
        routes: List<NavigationRoute>,
        initialLegIndex: Int?
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
    private val options: MapboxRouteLineOptions,
    private val routeLineApi: MapboxRouteLineApi = MapboxRouteLineApi(options),
    private val routeLineView: MapboxRouteLineView = MapboxRouteLineView(options),
    contractProvider: Provider<RouteLineComponentContract>? = null
) : UIComponent() {

    private var currentRouteProgressData: RouteProgressData? = null
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

        mapboxMap.getStyle {
            // initialize route line layers to ensure they exist before
            // both RouteLineComponent and RouteArrowComponent start rendering
            routeLineView.initializeLayers(it)
        }

        coroutineScope.launch {
            mapboxNavigation.flowRouteProgress().collect { routeProgress ->
                if (routeProgress.currentState == RouteProgressState.TRACKING) {
                    currentRouteProgressData = routeProgress.currentLegProgress?.let {
                        RouteProgressData(
                            it.legIndex,
                            routeProgress.currentRouteGeometryIndex,
                            it.geometryIndex
                        )
                    }
                }
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
                .onEach { currentRouteProgressData = null }
                .map { it.navigationRoutes to mapboxNavigation.currentLegIndex() }
                .stateIn(
                    this,
                    SharingStarted.WhileSubscribed(),
                    mapboxNavigation.getNavigationRoutes() to mapboxNavigation.currentLegIndex()
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
        currentRouteProgressData = null
        this.mapboxNavigation = null
    }

    private fun selectRoute(mapboxNavigation: MapboxNavigation, point: Point) {
        coroutineScope.launch {
            routeLineApi.findClosestRoute(point, mapboxMap, routeClickPadding).fold(
                { contractProvider.get().onMapClicked(point) },
                { result ->
                    if (result.navigationRoute != routeLineApi.getPrimaryNavigationRoute()) {
                        val reOrderedRoutes = arrayListOf(result.navigationRoute)
                        val legIndex = mapboxNavigation.getAlternativeMetadataFor(
                            result.navigationRoute
                        )?.let {
                            ifNonNull(currentRouteProgressData) { routeProgressData ->
                                AlternativeDataProvider.getAlternativeLegIndex(
                                    routeProgressData,
                                    it
                                )
                            }
                        }
                        routeLineApi.getNavigationRoutes().filterTo(reOrderedRoutes) { route ->
                            route != result.navigationRoute
                        }
                        contractProvider.get().setRoutes(
                            mapboxNavigation,
                            reOrderedRoutes,
                            legIndex
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
