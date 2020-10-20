package com.mapbox.navigation.ui.route

import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.model.RouteProgressState
import com.mapbox.navigation.core.trip.session.RouteProgressObserver

/**
 * Upon receiving route progress events draws and/or updates the line on the map representing the
 * route as well as arrow(s) representing the next maneuver.
 *
 * @param routeLine the route to represent on the map
 * @param routeArrow the arrow representing the next maneuver
 */
internal class MapRouteProgressChangeListener(
    private val routeLine: MapRouteLine,
    private val routeArrow: MapRouteArrow
) : RouteProgressObserver {

    private var restoreRouteArrowVisibilityFun: DeferredRouteUpdateFun? = null
    private var shouldReInitializePrimaryRoute = false

    override fun onRouteProgressChanged(routeProgress: RouteProgress) {
        routeLine.updateUpcomingRoutePointIndex(routeProgress)

        val currentRoute = routeProgress.route
        val hasGeometry = currentRoute.geometry()?.isNotEmpty() ?: false
        if (hasGeometry && currentRoute != routeLine.getPrimaryRoute()) {
            routeLine.reinitializeWithRoutes(listOf(currentRoute))
            shouldReInitializePrimaryRoute = true

            restoreRouteArrowVisibilityFun = getRestoreArrowVisibilityFun(
                routeArrow.routeArrowIsVisible()
            )
            routeArrow.updateVisibilityTo(false)
        } else {
            restoreRouteArrowVisibilityFun?.invoke()
            restoreRouteArrowVisibilityFun = null

            if (routeArrow.routeArrowIsVisible()) {
                routeArrow.addUpcomingManeuverArrow(routeProgress)
            }

            if (hasGeometry) {
                if (shouldReInitializePrimaryRoute) {
                    routeLine.reinitializePrimaryRoute()
                    shouldReInitializePrimaryRoute = false
                }
            }
        }

        when (routeProgress.currentState) {
            RouteProgressState.LOCATION_TRACKING ->
                routeLine.inhibitAutomaticVanishingPointUpdate(false)
            RouteProgressState.ROUTE_COMPLETE -> {
                routeLine.inhibitAutomaticVanishingPointUpdate(true)
                routeLine.setVanishingOffset(1.0)
            }
            else -> routeLine.inhibitAutomaticVanishingPointUpdate(true)
        }
    }

    private fun getRestoreArrowVisibilityFun(isVisible: Boolean): DeferredRouteUpdateFun = {
        routeArrow.updateVisibilityTo(isVisible)
    }
}
internal typealias DeferredRouteUpdateFun = () -> Unit
