package com.mapbox.navigation.dropin.component.routefetch

import android.content.Context
import android.util.Log
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.extensions.applyLanguageAndVoiceUnitOptions
import com.mapbox.navigation.base.route.RouterCallback
import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.utils.internal.InternalJobControlFactory
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
internal class RouteFetchComponent(private val context: Context) : MapboxNavigationObserver {

    private var routeRequestId: Long? = null
    private val jobControl = InternalJobControlFactory.createMainScopeJobControl()

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        jobControl.scope.launch {
            MapboxDropInRouteRequester.setRouteRequests.collect {
                mapboxNavigation.setRoutes(it)
            }
        }

        jobControl.scope.launch {
            MapboxDropInRouteRequester.routeRequests.collect {
                val routeOptions = getDefaultOptions(mapboxNavigation, it)
                fetchRoute(routeOptions, mapboxNavigation)
            }
        }

        jobControl.scope.launch {
            MapboxDropInRouteRequester.routeOptionsRequests.collect {
                fetchRoute(it, mapboxNavigation)
            }
        }
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        jobControl.job.cancelChildren()
        routeRequestId?.let {
            mapboxNavigation.cancelRouteRequest(it)
        }
    }

    private fun fetchRoute(options: RouteOptions, mapboxNavigation: MapboxNavigation) {
        routeRequestId = mapboxNavigation.requestRoutes(
            options,
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
            .applyLanguageAndVoiceUnitOptions(context)
            .layersList(listOf(mapboxNavigation.getZLevel(), null))
            .coordinatesList(points)
            .alternatives(true)
            .build()
    }

    private companion object {
        private val TAG = RouteFetchComponent::class.java.simpleName
    }
}
