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
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.dropin.UICommandDispatcher
import com.mapbox.navigation.dropin.lifecycle.UICommand
import com.mapbox.navigation.ui.maps.route.line.MapboxRouteLineApiExtensions.findClosestRoute
import com.mapbox.navigation.ui.maps.route.line.MapboxRouteLineApiExtensions.setRoutes
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineApi
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineView
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions
import com.mapbox.navigation.ui.maps.route.line.model.RouteLine
import com.mapbox.navigation.utils.internal.InternalJobControlFactory
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class RouteLineComponent(
    private val mapView: MapView,
    private val dispatcher: UICommandDispatcher,
    private val options: MapboxRouteLineOptions,
) : MapboxNavigationObserver {

    private val routeClickPadding = Utils.dpToPx(30f)
    private val jobControl = InternalJobControlFactory.createMainScopeJobControl()

    private val routeLineView by lazy {
        MapboxRouteLineView(options)
    }

    private val routeLineApi: MapboxRouteLineApi by lazy {
        MapboxRouteLineApi(options)
    }

    private val routesObserver = RoutesObserver { result ->
        val routeLines = result.routes.map { RouteLine(it, null) }
        jobControl.scope.launch {
            routeLineApi.setRoutes(routeLines).let { routeDrawData ->
                mapView.getMapboxMap().getStyle { style ->
                    routeLineView.renderRouteDrawData(style, routeDrawData)
                }
            }
        }
    }

    private val onMapClickListener = OnMapClickListener { point ->
        dispatcher.dispatch(
            UICommand.MapCommand.OnMapClicked(
                point = point,
                map = mapView.getMapboxMap(),
                padding = routeClickPadding
            )
        )
        false
    }

    private val routeProgressObserver = RouteProgressObserver { routeProgress ->
        mapView.getMapboxMap().getStyle { style ->
            routeLineApi.updateWithRouteProgress(routeProgress) { result ->
                routeLineView.renderRouteLineUpdate(style, result).also {
                    result.error?.let {
                        Log.e(TAG, it.errorMessage, it.throwable)
                    }
                }
            }
        }
    }

    private val onPositionChangedListener = OnIndicatorPositionChangedListener { point ->
        val result = routeLineApi.updateTraveledRouteLine(point)
        mapView.getMapboxMap().getStyle()?.apply {
            routeLineView.renderRouteLineUpdate(this, result)
        }
    }

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        // Setup the map press to select alternative routes.
        mapView.gestures.addOnMapClickListener(onMapClickListener)

        mapboxNavigation.registerRoutesObserver(routesObserver)
        mapboxNavigation.registerRouteProgressObserver(routeProgressObserver)
        mapView.location.addOnIndicatorPositionChangedListener(onPositionChangedListener)
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        mapView.location.removeOnIndicatorPositionChangedListener(onPositionChangedListener)
        mapboxNavigation.unregisterRouteProgressObserver(routeProgressObserver)
        mapboxNavigation.unregisterRoutesObserver(routesObserver)
        jobControl.job.cancelChildren()
        routeLineApi.cancel()
        routeLineView.cancel()
    }

    private companion object {
        private val TAG = RouteLineComponent::class.java.simpleName
    }
}
