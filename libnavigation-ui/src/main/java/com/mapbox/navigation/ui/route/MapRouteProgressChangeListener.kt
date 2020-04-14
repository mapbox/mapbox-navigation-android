package com.mapbox.navigation.ui.route

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.trip.session.RouteProgressObserver

internal class MapRouteProgressChangeListener(
    private val routeLine: MapRouteLine,
    private val routeArrow: MapRouteArrow
) : RouteProgressObserver {

    private var isVisible = true

    override fun onRouteProgressChanged(routeProgress: RouteProgress) {
        if (!isVisible) return

        onProgressChange(routeProgress)
    }

    fun updateVisibility(isVisible: Boolean) {
        this.isVisible = isVisible
    }

    private fun onProgressChange(routeProgress: RouteProgress) {
        val directionsRoutes = routeLine.retrieveDirectionsRoutes()
        val primaryRouteIndex = routeLine.retrievePrimaryRouteIndex()
        val directionsRoute = directionsRoutes.getOrNull(primaryRouteIndex)

        updateRoute(directionsRoute, routeProgress)
    }

    private fun updateRoute(directionsRoute: DirectionsRoute?, routeProgress: RouteProgress) {
        val currentRoute = routeProgress.route()
        val hasGeometry = !(directionsRoute?.geometry().isNullOrEmpty() ||
            currentRoute?.geometry().isNullOrEmpty())
        if (currentRoute != null && hasGeometry && currentRoute != directionsRoute) {
            routeLine.draw(currentRoute)
            routeArrow.addUpcomingManeuverArrow(routeProgress)
        }
    }
}
