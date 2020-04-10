package com.mapbox.navigation.ui.route

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
        val currentRoute = routeProgress.route()
        val directionsRoutes = routeLine.retrieveDirectionsRoutes()
        val primaryRouteIndex = routeLine.retrievePrimaryRouteIndex()

        val currentRouteGeometry = currentRoute?.geometry()
        val directionsGeometry = directionsRoutes.getOrNull(primaryRouteIndex)?.geometry()
        val hasGeometry = !(currentRouteGeometry.isNullOrEmpty() || directionsGeometry.isNullOrEmpty())
        if (hasGeometry && currentRouteGeometry != directionsGeometry) {
            routeLine.draw(currentRoute)
        }
        routeArrow.addUpcomingManeuverArrow(routeProgress)
    }
}
