package com.mapbox.navigation.dropin.component.routeline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mapbox.geojson.Point
import com.mapbox.maps.Style
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.directions.session.RoutesUpdatedResult
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineApi
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineView
import com.mapbox.navigation.ui.maps.route.line.model.RouteLine
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineError
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

internal class RouteLineViewModel(
    private val routeLineApi: MapboxRouteLineApi,
    private val routeLineView: MapboxRouteLineView
) : ViewModel() {

    private val _routeLineErrors: MutableSharedFlow<RouteLineError> = MutableSharedFlow()
    val routeLineErrors: Flow<RouteLineError> = _routeLineErrors

    fun mapStyleUpdated(style: Style) {
        routeLineView.initializeLayers(style)
        routeLineApi.getRouteDrawData { result ->
            routeLineView.renderRouteDrawData(style, result).also {
                result.error?.let {
                    viewModelScope.launch { _routeLineErrors.emit(it) }
                }
            }
        }
    }

    fun routesUpdated(update: RoutesUpdatedResult, style: Style) {
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

    fun routeProgressUpdated(routeProgress: RouteProgress, style: Style) {
        routeLineApi.updateWithRouteProgress(routeProgress) { result ->
            routeLineView.renderRouteLineUpdate(style, result).also {
                result.error?.let {
                    viewModelScope.launch { _routeLineErrors.emit(it) }
                }
            }
        }
    }

    fun positionChanged(point: Point, style: Style) {
        routeLineApi.updateTraveledRouteLine(point).also {
            routeLineView.renderRouteLineUpdate(style, it)
            it.error?.let {
                viewModelScope.launch { _routeLineErrors.emit(it) }
            }
        }
    }
}
