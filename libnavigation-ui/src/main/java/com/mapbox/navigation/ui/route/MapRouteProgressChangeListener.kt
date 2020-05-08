package com.mapbox.navigation.ui.route

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.ui.route.RouteConstants.VANISHING_ROUTE_LINE_UPDATE_DELAY
import com.mapbox.navigation.utils.internal.ThreadController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Upon receiving route progress events draws and/or updates the line on the map representing the
 * route as well as arrow(s) representing the next maneuver.
 *
 * @param routeLine the route to represent on the map
 * @param routeArrow the arrow representing the next maneuver
 * @param vanishRouteLineEnabled determines if the route line will vanish behind the puck as the
 * route progresses
 */
internal class MapRouteProgressChangeListener(
    private val routeLine: MapRouteLine,
    private val routeArrow: MapRouteArrow,
    private val vanishRouteLineEnabled: Boolean
) : RouteProgressObserver {

    /**
     * Upon receiving route progress events draws and/or updates the line on the map representing the
     * route as well as arrow(s) representing the next maneuver.
     *
     * @param routeLine the route to represent on the map
     * @param routeArrow the arrow representing the next maneuver
     */
    constructor(
        routeLine: MapRouteLine,
        routeArrow: MapRouteArrow
    ) : this(routeLine, routeArrow, false)

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
        val hasGeometry = currentRoute?.geometry()?.isNotEmpty() ?: false
        if (hasGeometry && currentRoute != directionsRoute) {
            routeLine.draw(currentRoute!!)
        } else {
            if (vanishRouteLineEnabled && hasGeometry && (job == null || !job!!.isActive)) {
                job = ThreadController.getMainScopeAndRootJob().scope.launch {
                    val totalDist =
                        (routeProgress.distanceRemaining() + routeProgress.distanceTraveled())
                    val dist = routeProgress.distanceTraveled() / totalDist
                    if (dist > 0) {
                        val deferredExpression = async(Dispatchers.Default) {
                            routeLine.getExpressionAtOffset(dist)
                        }
                        delay(VANISHING_ROUTE_LINE_UPDATE_DELAY)
                        routeLine.hideShieldLineAtOffset(dist)
                        routeLine.decorateRouteLine(deferredExpression.await())
                    }
                }
            }
        }
        routeArrow.addUpcomingManeuverArrow(routeProgress)
    }
}
