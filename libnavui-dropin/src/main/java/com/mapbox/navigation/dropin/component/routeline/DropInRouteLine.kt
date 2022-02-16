package com.mapbox.navigation.dropin.component.routeline

import android.location.Location
import android.util.Log
import com.mapbox.android.gestures.Utils
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.maps.MapView
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.extensions.applyLanguageAndVoiceUnitOptions
import com.mapbox.navigation.base.route.RouterCallback
import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.dropin.component.location.DropInLocationState
import com.mapbox.navigation.ui.maps.route.line.MapboxRouteLineApiExtensions.findClosestRoute
import com.mapbox.navigation.ui.maps.route.line.MapboxRouteLineApiExtensions.setRoutes
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineApi
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineView
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions
import com.mapbox.navigation.ui.maps.route.line.model.RouteLine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
internal class DropInRouteLine(
    private val mapView: MapView,
    private val options: MapboxRouteLineOptions,
    val dropInLocationState: DropInLocationState,
) : MapboxNavigationObserver {

    private val routeClickPadding = Utils.dpToPx(30f)

    private val routeLineView by lazy {
        MapboxRouteLineView(options)
    }

    private val routeLineApi: MapboxRouteLineApi by lazy {
        MapboxRouteLineApi(options)
    }

    private val routesObserver = RoutesObserver { result ->
        val routeLines = result.routes.map { RouteLine(it, null) }
        // TODO this should be attached to a lifecycle scope
        CoroutineScope(Dispatchers.Main).launch {
            routeLineApi.setRoutes(routeLines).let { routeDrawData ->
                mapView.getMapboxMap().getStyle { style ->
                    routeLineView.renderRouteDrawData(style, routeDrawData)
                }
            }
        }
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
        // Setup a long press to find a route.
        mapView.gestures.addOnMapLongClickListener { point ->
            val originLocation = dropInLocationState.locationLiveData.value
            findRoute(originLocation, point)
            false
        }

        // Setup the map press to select alternative routes.
        mapView.gestures.addOnMapClickListener { point ->
            selectRoute(point)
            false
        }

        mapboxNavigation.registerRoutesObserver(routesObserver)
        mapboxNavigation.registerRouteProgressObserver(routeProgressObserver)
        mapView.location.addOnIndicatorPositionChangedListener(onPositionChangedListener)
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        mapView.location.removeOnIndicatorPositionChangedListener(onPositionChangedListener)
        mapboxNavigation.unregisterRouteProgressObserver(routeProgressObserver)
        mapboxNavigation.unregisterRoutesObserver(routesObserver)
        routeLineApi.cancel()
        routeLineView.cancel()
    }

    fun selectRoute(point: Point) {
        // TODO this should be attached to a lifecycle scope
        CoroutineScope(Dispatchers.Main).launch {
            val result = routeLineApi.findClosestRoute(
                point,
                mapView.getMapboxMap(),
                routeClickPadding
            )

            val routeFound = result.value?.route
            if (routeFound != null && routeFound != routeLineApi.getPrimaryRoute()) {
                val reOrderedRoutes = routeLineApi.getRoutes()
                    .filter { it != routeFound }
                    .toMutableList()
                    .also {
                        it.add(0, routeFound)
                    }
                MapboxNavigationApp.current()?.setRoutes(reOrderedRoutes)
            }
        }
    }

    fun findRoute(originLocation: Location?, destination: Point) {
        val origin = originLocation?.run { Point.fromLngLat(longitude, latitude) }
            ?: return

        val mapboxNavigation: MapboxNavigation = MapboxNavigationApp.current() ?: return
        val routeOptions = RouteOptions.builder()
            .applyDefaultNavigationOptions()
            .applyLanguageAndVoiceUnitOptions(mapView.context)
            .coordinatesList(listOf(origin, destination))
            .layersList(listOf(mapboxNavigation.getZLevel(), null))
            .alternatives(true)
            .build()
        mapboxNavigation.requestRoutes(
            routeOptions,
            object : RouterCallback {
                override fun onRoutesReady(
                    routes: List<DirectionsRoute>,
                    routerOrigin: RouterOrigin
                ) {
                    mapboxNavigation.setRoutes(routes.reversed())
                }

                override fun onFailure(
                    reasons: List<RouterFailure>,
                    routeOptions: RouteOptions
                ) {
                    // no impl
                }

                override fun onCanceled(routeOptions: RouteOptions, routerOrigin: RouterOrigin) {
                    // no impl
                }
            }
        )
    }

    private companion object {
        private val TAG = DropInRouteLine::class.java.simpleName
    }
}
