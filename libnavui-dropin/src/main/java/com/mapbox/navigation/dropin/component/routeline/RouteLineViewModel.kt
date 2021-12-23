package com.mapbox.navigation.dropin.component.routeline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mapbox.android.gestures.Utils
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.geojson.Point
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.layers.properties.generated.Visibility
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.directions.session.RoutesUpdatedResult
import com.mapbox.navigation.ui.maps.route.line.MapboxRouteLineApiExtensions.findClosestRoute
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineApi
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineView
import com.mapbox.navigation.ui.maps.route.line.model.RouteLine
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineError
import com.mapbox.navigation.ui.maps.route.line.model.RouteNotFound
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

internal class RouteLineViewModel(
    private val routeLineApi: MapboxRouteLineApi,
    private val routeLineView: MapboxRouteLineView
) : ViewModel() {

    private val _routeLineErrors: MutableSharedFlow<RouteLineError> = MutableSharedFlow()
    internal val routeLineErrors: Flow<RouteLineError> = _routeLineErrors

    private val _routeResets: MutableSharedFlow<List<DirectionsRoute>> = MutableSharedFlow()
    internal val routeResets: Flow<List<DirectionsRoute>> = _routeResets

    private val _routeNotFoundErrors: MutableSharedFlow<RouteNotFound> = MutableSharedFlow()
    internal val routeNotFoundErrors: Flow<RouteNotFound> = _routeNotFoundErrors

    internal fun mapStyleUpdated(style: Style) {
        routeLineView.initializeLayers(style)
        routeLineApi.getRouteDrawData { result ->
            routeLineView.renderRouteDrawData(style, result).also {
                result.error?.let {
                    viewModelScope.launch { _routeLineErrors.emit(it) }
                }
            }
        }
    }

    internal fun routesUpdated(update: RoutesUpdatedResult, style: Style) {
        update.routes.map {
            RouteLine(it, null)
        }.apply {
            routeLineApi.setRoutes(this) { result ->
                routeLineView.renderRouteDrawData(style, result).also {
                    result.error?.let {
                        viewModelScope.launch { _routeLineErrors.emit(it) }
                    }
                }
            }
        }
    }

    internal fun routeProgressUpdated(routeProgress: RouteProgress, style: Style) {
        routeLineApi.updateWithRouteProgress(routeProgress) { result ->
            routeLineView.renderRouteLineUpdate(style, result).also {
                result.error?.let {
                    viewModelScope.launch { _routeLineErrors.emit(it) }
                }
            }
        }
    }

    internal fun positionChanged(point: Point, style: Style) {
        routeLineApi.updateTraveledRouteLine(point).also {
            routeLineView.renderRouteLineUpdate(style, it)
            it.error?.let {
                viewModelScope.launch { _routeLineErrors.emit(it) }
            }
        }
    }

    internal fun mapClick(point: Point, mapboxMap: MapboxMap) {
        mapboxMap.getStyle()?.let { style ->
            val primaryLineVisibility = routeLineView.getPrimaryRouteVisibility(style)
            val alternativeRouteLinesVisibility =
                routeLineView.getAlternativeRoutesVisibility(style)
            if (
                primaryLineVisibility == Visibility.VISIBLE &&
                alternativeRouteLinesVisibility == Visibility.VISIBLE
            ) {
                viewModelScope.launch {
                    val routeClickPadding = Utils.dpToPx(30f)
                    routeLineApi.findClosestRoute(
                        point,
                        mapboxMap,
                        routeClickPadding
                    ).fold(
                        { routeNotFound ->
                            this.launch {
                                _routeNotFoundErrors.emit(routeNotFound)
                            }
                        },
                        { value ->
                            if (value.route != routeLineApi.getPrimaryRoute()) {
                                val reOrderedRoutes = routeLineApi.getRoutes()
                                    .filter { it != value.route }
                                    .toMutableList()
                                    .also {
                                        it.add(0, value.route)
                                    }
                                this.launch {
                                    _routeResets.emit(reOrderedRoutes)
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}
