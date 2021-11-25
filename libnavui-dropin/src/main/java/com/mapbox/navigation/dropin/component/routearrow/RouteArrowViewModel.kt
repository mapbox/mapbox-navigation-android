package com.mapbox.navigation.dropin.component.routearrow

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mapbox.maps.Style
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowApi
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowView
import com.mapbox.navigation.ui.maps.route.arrow.model.InvalidPointError
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

internal class RouteArrowViewModel(
    private val routeArrowApi: MapboxRouteArrowApi,
    private val routeArrowView: MapboxRouteArrowView
) : ViewModel() {

    private val _routeArrowErrors: MutableSharedFlow<InvalidPointError> = MutableSharedFlow()
    val routeArrowErrors: Flow<InvalidPointError> = _routeArrowErrors

    fun routeProgressUpdated(routeProgress: RouteProgress, style: Style) {
        routeArrowApi.addUpcomingManeuverArrow(routeProgress).apply {
            routeArrowView.renderManeuverUpdate(style, this)
            this.error?.let {
                viewModelScope.launch { _routeArrowErrors.emit(it) }
            }
        }
    }
}
