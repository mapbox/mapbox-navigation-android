package com.mapbox.navigation.qa_test_app.lifecycle

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.core.routealternatives.RouteAlternativesObserver

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class DropInContinuousRoutes : MapboxNavigationObserver {

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

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        mapboxNavigation.registerRouteAlternativesObserver(routeAlternativesObserver)
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        mapboxNavigation.unregisterRouteAlternativesObserver(routeAlternativesObserver)
    }
}
