package com.mapbox.navigation.ui.route

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.geojson.LineString
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.ui.route.MapRouteLine.MapRouteLineSupport.buildRouteLineExpression
import com.mapbox.navigation.utils.internal.ThreadController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

internal class MapRouteProgressChangeListener(
    private val routeLine: MapRouteLine,
    private val routeArrow: MapRouteArrow
) : RouteProgressObserver {

    private var job: Job? = null
    private var isVisible = true

    override fun onRouteProgressChanged(routeProgress: RouteProgress) {
        if (!isVisible) return

        onProgressChange(routeProgress)
    }

    fun updateVisibility(isVisible: Boolean) {
        this.isVisible = isVisible
    }

    private fun onProgressChange(routeProgress: RouteProgress) {
        val directionsRoute = routeLine.getPrimaryRoute()
        updateRoute(directionsRoute, routeProgress)
    }

    private fun updateRoute(directionsRoute: DirectionsRoute?, routeProgress: RouteProgress) {
        val currentRoute = routeProgress.route()
        val hasGeometry = !(directionsRoute?.geometry().isNullOrEmpty() ||
            currentRoute?.geometry().isNullOrEmpty())
        if (currentRoute != null && hasGeometry && currentRoute != directionsRoute) {
            routeLine.draw(currentRoute)
            routeArrow.addUpcomingManeuverArrow(routeProgress)
        } else {
            if (job == null || !job!!.isActive && currentRoute != null && hasGeometry) {
                job = ThreadController.getMainScopeAndRootJob().scope.launch {
                    val totalDist =
                        (routeProgress.distanceRemaining() + routeProgress.distanceTraveled())
                    val dist = routeProgress.distanceTraveled() / totalDist
                    if (dist > 0) {
                        val deferredExpression = async(Dispatchers.Default) {
                            val lineString: LineString = routeLine.getLineStringForRoute(currentRoute!!)
                            buildRouteLineExpression(
                                currentRoute,
                                lineString,
                                true,
                                dist.toDouble(),
                                routeLine::getRouteColorForCongestion
                            )
                        }
                        routeArrow.addUpcomingManeuverArrow(routeProgress)
                        routeLine.hideShieldLineAtOffset(dist)
                        routeLine.decorateRouteLine(deferredExpression.await())
                    }
                }
            }
        }
    }
}
