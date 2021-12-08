package com.mapbox.navigation.qa_test_app.lifecycle

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.core.routealternatives.RouteAlternativesObserver

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class DropInContinuousRoutes : DefaultLifecycleObserver {

    private val routeAlternativesObserver =
        RouteAlternativesObserver { routeProgress, alternatives, _ ->
            val updatedRoutes = mutableListOf<DirectionsRoute>()
            updatedRoutes.add(routeProgress.route)
            updatedRoutes.addAll(alternatives)

            MapboxNavigationApp.current()?.apply {
                setRoutes(updatedRoutes)
                requestAlternativeRoutes()
            }
        }

    private val navigationObserver = object : MapboxNavigationObserver {
        override fun onAttached(mapboxNavigation: MapboxNavigation) {
            mapboxNavigation.registerRouteAlternativesObserver(routeAlternativesObserver)
        }

        override fun onDetached(mapboxNavigation: MapboxNavigation) {
            mapboxNavigation.unregisterRouteAlternativesObserver(routeAlternativesObserver)
        }
    }

    override fun onCreate(owner: LifecycleOwner) {
        MapboxNavigationApp.registerObserver(navigationObserver)
    }

    override fun onDestroy(owner: LifecycleOwner) {
        MapboxNavigationApp.unregisterObserver(navigationObserver)
    }
}
