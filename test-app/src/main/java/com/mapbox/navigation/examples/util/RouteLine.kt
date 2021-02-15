package com.mapbox.navigation.examples.util

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.locationcomponent.getLocationComponentPlugin
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowApi
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowView
import com.mapbox.navigation.ui.maps.route.arrow.model.RouteArrowOptions
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineApi
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineView
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions
import com.mapbox.navigation.ui.maps.route.line.model.RouteLine
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineResources

class RouteLine(private val activity: AppCompatActivity) : LifecycleObserver {
    private lateinit var mapView: MapView
    private lateinit var mapboxNavigation: MapboxNavigation
    lateinit var style: Style

    private val routeLineResources: RouteLineResources by lazy {
        RouteLineResources.Builder().build()
    }

    private val options: MapboxRouteLineOptions by lazy {
        MapboxRouteLineOptions.Builder(activity)
            .withRouteLineResources(routeLineResources)
            .withRouteLineBelowLayerId("road-label")
            .build()
    }

    private val routeLineView by lazy {
        MapboxRouteLineView(options)
    }

    private val routeLineApi: MapboxRouteLineApi by lazy {
        MapboxRouteLineApi(options)
    }

    private val routeArrowApi: MapboxRouteArrowApi by lazy {
        MapboxRouteArrowApi()
    }

    private val routeArrowView: MapboxRouteArrowView by lazy {
        MapboxRouteArrowView(RouteArrowOptions.Builder(activity).build())
    }

    init {
        activity.lifecycle.addObserver(this)
    }

    fun initialize(mapView: MapView, mapboxNavigation: MapboxNavigation) {
        this.mapView = mapView
        this.mapboxNavigation = mapboxNavigation

        mapView.getMapboxMap().getStyle { style -> this@RouteLine.style = style }
    }

    private val onIndicatorPositionChangedListener = OnIndicatorPositionChangedListener { point ->
        routeLineApi.updateTraveledRouteLine(point)?.apply {
            routeLineView.render(style, this)
        }
    }

    private val routesObserver: RoutesObserver = object : RoutesObserver {
        override fun onRoutesChanged(routes: List<DirectionsRoute>) {
            routeLineApi.setRoutes(listOf(RouteLine(routes[0], null))).apply {
                routeLineView.render(style, this)
            }
        }
    }

    private val routeProgressObserver: RouteProgressObserver = object : RouteProgressObserver {
        override fun onRouteProgressChanged(routeProgress: RouteProgress) {
            routeLineApi.updateWithRouteProgress(routeProgress)
            routeArrowApi.updateUpcomingManeuverArrow(routeProgress).apply {
                routeArrowView.render(style, this)
            }

            val currentRoute = routeProgress.route
            var hasGeometry = false
            if (currentRoute.geometry() != null && currentRoute.geometry()!!.isNotEmpty()) {
                hasGeometry = true
            }

            var isNewRoute = false
            if (hasGeometry && currentRoute !== routeLineApi.getPrimaryRoute()) {
                isNewRoute = true
            }

            if (isNewRoute) {
                routeLineApi.setRoutes(listOf(RouteLine(routeProgress.route, null))).apply {
                    routeLineView.render(style, this)
                }
            }
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    private fun onStart() {
        mapView.getLocationComponentPlugin().addOnIndicatorPositionChangedListener(
            onIndicatorPositionChangedListener
        )
        mapboxNavigation.registerRoutesObserver(routesObserver)
        mapboxNavigation.registerRouteProgressObserver(routeProgressObserver)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    private fun onStop() {
        mapView.getLocationComponentPlugin().removeOnIndicatorPositionChangedListener(
            onIndicatorPositionChangedListener
        )
        mapboxNavigation.unregisterRoutesObserver(routesObserver)
        mapboxNavigation.unregisterRouteProgressObserver(routeProgressObserver)
    }
}
