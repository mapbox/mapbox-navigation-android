package com.mapbox.navigation.dropin.component.routeline

import com.mapbox.geojson.Point
import com.mapbox.maps.MapView
import com.mapbox.maps.extension.observable.eventdata.StyleLoadedEventData
import com.mapbox.maps.plugin.delegates.listeners.OnStyleLoadedListener
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.directions.session.RoutesUpdatedResult
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.dropin.component.UIComponent
import com.mapbox.navigation.dropin.component.navigationstate.NavigationState

internal sealed interface RouteLineUIComponent : UIComponent

internal class MapboxRouteLineUIComponent(
    val view: MapView,
    val viewModel: RouteLineViewModel
) : RouteLineUIComponent,
    OnStyleLoadedListener,
    RoutesObserver,
    RouteProgressObserver,
    OnIndicatorPositionChangedListener {

    override fun onNavigationStateChanged(state: NavigationState) {
        // no impl
    }

    override fun onStyleLoaded(eventData: StyleLoadedEventData) {
        view.getMapboxMap().getStyle { style ->
            viewModel.mapStyleUpdated(style)
        }
    }

    override fun onRoutesChanged(result: RoutesUpdatedResult) {
        view.getMapboxMap().getStyle()?.let { style ->
            viewModel.routesUpdated(result, style)
        }
    }

    override fun onRouteProgressChanged(routeProgress: RouteProgress) {
        view.getMapboxMap().getStyle()?.let { style ->
            viewModel.routeProgressUpdated(routeProgress, style)
        }
    }

    override fun onIndicatorPositionChanged(point: Point) {
        view.getMapboxMap().getStyle()?.let { style ->
            viewModel.positionChanged(point, style)
        }
    }
}
