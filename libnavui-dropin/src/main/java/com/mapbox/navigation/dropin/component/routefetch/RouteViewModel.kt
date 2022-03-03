package com.mapbox.navigation.dropin.component.routefetch

import android.util.Log
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.extensions.applyLanguageAndVoiceUnitOptions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.NavigationRouterCallback
import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.UICommandDispatcher
import com.mapbox.navigation.dropin.component.location.LocationBehavior
import com.mapbox.navigation.dropin.lifecycle.UICommand
import com.mapbox.navigation.dropin.lifecycle.UIViewModel
import com.mapbox.navigation.ui.utils.internal.ifNonNull
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class RouteViewModel(
    private val locationBehavior: LocationBehavior,
    val commandDispatcher: UICommandDispatcher
) : UIViewModel() {
    private var routeRequestId: Long? = null

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)
        mainJobControl.scope.launch {
            commandDispatcher.commandFlow.filterIsInstance<UICommand.RoutesCommand>().collect {
                when (it) {
                    is UICommand.RoutesCommand.FetchRouteFromCurrentLocation -> {
                        val currentLocation = locationBehavior.locationLiveData.value
                        ifNonNull(currentLocation) { lastLocation ->
                            val origin = Point.fromLngLat(
                                lastLocation.longitude,
                                lastLocation.latitude
                            )
                            val points = listOf<Point>(origin, it.destination)
                            val options = getDefaultOptions(mapboxNavigation, points)
                            fetchRoute(options, mapboxNavigation)
                        }
                    }
                    else -> {
                        // no impl
                    }
                }
            }
        }
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        super.onDetached(mapboxNavigation)
        routeRequestId?.let {
            mapboxNavigation.cancelRouteRequest(it)
        }
    }

    private fun fetchRoute(options: RouteOptions, mapboxNavigation: MapboxNavigation) {
        routeRequestId = mapboxNavigation.requestRoutes(
            options,
            object : NavigationRouterCallback {
                override fun onRoutesReady(
                    routes: List<NavigationRoute>,
                    routerOrigin: RouterOrigin
                ) {
                    mapboxNavigation.setNavigationRoutes(routes)
                }

                override fun onFailure(
                    reasons: List<RouterFailure>,
                    routeOptions: RouteOptions
                ) {
                    Log.e(TAG, "Failed to fetch route with reason(s):")
                    reasons.forEach {
                        Log.e(TAG, it.message)
                    }
                }

                override fun onCanceled(routeOptions: RouteOptions, routerOrigin: RouterOrigin) {
                    // no impl
                }
            }
        )
    }

    private fun getDefaultOptions(
        mapboxNavigation: MapboxNavigation,
        points: List<Point>
    ): RouteOptions {
        return RouteOptions.builder()
            .applyDefaultNavigationOptions()
            .applyLanguageAndVoiceUnitOptions(mapboxNavigation.navigationOptions.applicationContext)
            .layersList(listOf(mapboxNavigation.getZLevel(), null))
            .coordinatesList(points)
            .alternatives(true)
            .build()
    }

    private companion object {
        private val TAG = RouteViewModel::class.java.simpleName
    }
}
