package com.mapbox.navigation.examples.util

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.geojson.Point
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.location.LocationComponentPlugin
import com.mapbox.maps.plugin.location.OnIndicatorPositionChangedListener
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.examples.core.R
import com.mapbox.navigation.ui.maps.internal.route.arrow.MapboxRouteArrowAPI
import com.mapbox.navigation.ui.maps.internal.route.arrow.MapboxRouteArrowActions
import com.mapbox.navigation.ui.maps.internal.route.arrow.MapboxRouteArrowView
import com.mapbox.navigation.ui.maps.internal.route.line.MapboxRouteLineAPI
import com.mapbox.navigation.ui.maps.internal.route.line.MapboxRouteLineActions
import com.mapbox.navigation.ui.maps.internal.route.line.MapboxRouteLineResourceProviderFactory.getRouteLineResourceProvider
import com.mapbox.navigation.ui.maps.internal.route.line.MapboxRouteLineView
import com.mapbox.navigation.ui.maps.route.RouteArrowLayerInitializer
import com.mapbox.navigation.ui.maps.route.RouteLineLayerInitializer
import com.mapbox.navigation.ui.maps.route.arrow.api.RouteArrowAPI
import com.mapbox.navigation.ui.maps.route.line.api.RouteLineAPI

class Slackline(private val activity: AppCompatActivity) : LifecycleObserver {

    private val routeLineView = MapboxRouteLineView()
    private val routeArrowView = MapboxRouteArrowView()
    private lateinit var mapView: MapView
    private lateinit var mapboxNavigation: MapboxNavigation

    private val routeStyleRes: Int by lazy {
        ThemeUtil.retrieveAttrResourceId(
            activity,
            R.attr.navigationViewRouteStyle,
            R.style.MapboxStyleNavigationMapRoute
        )
    }

    private val routeLineAPI: RouteLineAPI by lazy {
        val resourceProvider = getRouteLineResourceProvider(activity, routeStyleRes)
        MapboxRouteLineAPI(MapboxRouteLineActions(resourceProvider), routeLineView)
    }

    private val routeArrowAPI: RouteArrowAPI by lazy {
        MapboxRouteArrowAPI(MapboxRouteArrowActions(), routeArrowView)
    }

    private val routeLineLayerInitializer: RouteLineLayerInitializer by lazy {
        RouteLineLayerInitializer.Builder(activity).build()
    }

    private val routeArrowLayerInitializer: RouteArrowLayerInitializer by lazy {
        RouteArrowLayerInitializer.Builder(activity).build()
    }

    init {
        activity.lifecycle.addObserver(this)
    }

    fun initialize(mapView: MapView, mapboxNavigation: MapboxNavigation) {
        this.mapView = mapView
        this.mapboxNavigation = mapboxNavigation
        mapView.getMapboxMap().getStyle(
            object : Style.OnStyleLoaded {
                override fun onStyleLoaded(style: Style) {
                    routeLineLayerInitializer.initializeLayers(style)
                    routeArrowLayerInitializer.initializeLayers(style)
                    routeLineAPI.updateViewStyle(style)
                    routeArrowAPI.updateViewStyle(style)
                }
            }
        )
        getLocationComponent(mapView)!!.addOnIndicatorPositionChangedListener(
            onIndicatorPositionChangedListener
        )
        mapboxNavigation.registerRoutesObserver(routesObserver)
        mapboxNavigation.registerRouteProgressObserver(routeProgressObserver)
    }

    private fun getLocationComponent(mapView: MapView): LocationComponentPlugin? {
        return mapView.getPlugin<LocationComponentPlugin>(LocationComponentPlugin::class.java)
    }

    private val onIndicatorPositionChangedListener = object : OnIndicatorPositionChangedListener {
        override fun onIndicatorPositionChanged(point: Point) {
            routeLineAPI.updateTraveledRouteLine(point)
        }
    }

    private val routesObserver: RoutesObserver = object : RoutesObserver {
        override fun onRoutesChanged(routes: List<DirectionsRoute>) {
            routeLineAPI.setRoutes(listOf(routes[0]))
        }
    }

    private val routeProgressObserver: RouteProgressObserver = object : RouteProgressObserver {
        override fun onRouteProgressChanged(routeProgress: RouteProgress) {
            routeLineAPI.updateUpcomingRoutePointIndex(routeProgress)
            routeLineAPI.updateVanishingPointState(routeProgress.currentState)
            routeArrowAPI.addUpComingManeuverArrow(routeProgress)

            val currentRoute = routeProgress.route
            var hasGeometry = false
            if (currentRoute.geometry() != null && currentRoute.geometry()!!.isNotEmpty()) {
                hasGeometry = true
            }

            var isNewRoute = false
            if (hasGeometry && currentRoute !== routeLineAPI.getPrimaryRoute()) {
                isNewRoute = true
            }

            if (isNewRoute) {
                routeLineAPI.setRoutes(listOf(routeProgress.route))
            }
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    private fun onStop() {
        getLocationComponent(mapView)!!.removeOnIndicatorPositionChangedListener(
            onIndicatorPositionChangedListener
        )
        mapboxNavigation.unregisterRoutesObserver(routesObserver)
        mapboxNavigation.unregisterRouteProgressObserver(routeProgressObserver)
    }
}
