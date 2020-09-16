package com.mapbox.navigation.ui.route

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.ui.internal.route.RouteConstants.MINIMUM_ROUTE_LINE_OFFSET
import com.mapbox.navigation.ui.internal.utils.RouteLineValueAnimator
import com.mapbox.navigation.ui.internal.utils.RouteLineValueAnimatorHandler
import com.mapbox.navigation.utils.internal.ThreadController
import kotlinx.coroutines.launch

/**
 * Upon receiving route progress events draws and/or updates the line on the map representing the
 * route as well as arrow(s) representing the next maneuver.
 *
 * @param routeLine the route to represent on the map
 * @param routeArrow the arrow representing the next maneuver
 * @param vanishingLineAnimator the animator to use for altering the appearance of the route line
 * behind the puck. Also determines if the route line will vanish behind the puck as the
 * route progresses. If no animator is provided the route line will remain solid and not updated
 * during the route progress.
 */
internal class MapRouteProgressChangeListener(
    private val routeLine: MapRouteLine,
    private val routeArrow: MapRouteArrow?,
    private val vanishingLineAnimator: RouteLineValueAnimator?
) : RouteProgressObserver {

    private val jobControl by lazy { ThreadController.getMainScopeAndRootJob() }
    private var lastDistanceValue = 0f
    private var restoreRouteArrowVisibilityFun: DeferredRouteUpdateFun? = null
    private var shouldReInitializePrimaryRoute = false

    /**
     * Returns the percentage of the distance traveled that was last calculated. This is only
     * calculated if the @param vanishingLineAnimator passed into the constructor was non-null.
     *
     * @return the value calculated during the last progress update event or 0 if not enabled.
     */
    fun getPercentDistanceTraveled(): Float {
        return lastDistanceValue
    }

    /**
     * Sets the value for the distance traveled which is used in animating the section of the route
     * line behind the puck when the vanishing route line feature is enabled.
     *
     * @param distance a value representing the percentage of the distance traveled.
     */
    fun updatePercentDistanceTraveled(distance: Float) {
        lastDistanceValue = distance
    }

    private val routeLineAnimatorUpdateHandler: RouteLineValueAnimatorHandler = { animationValue ->
        if (animationValue > MINIMUM_ROUTE_LINE_OFFSET) {
            val expression = routeLine.getExpressionAtOffset(animationValue)
            routeLine.hideCasingLineAtOffset(animationValue)
            routeLine.hideRouteLineAtOffset(animationValue)
            routeLine.decorateRouteLine(expression)
            lastDistanceValue = animationValue
        }
    }

    init {
        this.lastDistanceValue = routeLine.vanishPointOffset
        this.vanishingLineAnimator?.valueAnimatorHandler = routeLineAnimatorUpdateHandler
    }

    override fun onRouteProgressChanged(routeProgress: RouteProgress) {
        onProgressChange(routeProgress)
    }

    private fun onProgressChange(routeProgress: RouteProgress) {
        val directionsRoute = routeLine.getPrimaryRoute()
        updateRoute(directionsRoute, routeProgress)
    }

    private fun updateRoute(directionsRoute: DirectionsRoute?, routeProgress: RouteProgress) {
        vanishingLineAnimator?.cancelAnimationCallbacks()
        val currentRoute = routeProgress.route
        val hasGeometry = currentRoute.geometry()?.isNotEmpty() ?: false
        if (hasGeometry && currentRoute != directionsRoute) {
            routeLine.reinitializeWithRoutes(listOf(currentRoute))
            shouldReInitializePrimaryRoute = true

            restoreRouteArrowVisibilityFun =
                getRestoreArrowVisibilityFun(routeArrow?.routeArrowIsVisible() ?: false)
            routeArrow?.updateVisibilityTo(false)

            this.lastDistanceValue = routeLine.vanishPointOffset
        } else {
            restoreRouteArrowVisibilityFun?.invoke()
            restoreRouteArrowVisibilityFun = null

            if (routeArrow?.routeArrowIsVisible() ?: false) {
                routeArrow?.addUpcomingManeuverArrow(routeProgress)
            }
            // if there is no geometry then the session is in free drive and the vanishing
            // route line code should not execute.
            if (hasGeometry) {
                if (shouldReInitializePrimaryRoute) {
                    routeLine.reinitializePrimaryRoute()
                    shouldReInitializePrimaryRoute = false
                }

                if (vanishingLineAnimator != null)
                    jobControl.scope.launch {
                        val percentDistanceTraveled = getPercentDistanceTraveled(routeProgress)
                        if (percentDistanceTraveled > 0) {
                            vanishingLineAnimator.start(lastDistanceValue, percentDistanceTraveled)
                        }
                    }
            }
        }
    }

    private fun getPercentDistanceTraveled(routeProgress: RouteProgress): Float {
        val totalDist =
            (routeProgress.distanceRemaining + routeProgress.distanceTraveled)
        return routeProgress.distanceTraveled / totalDist
    }

    private fun getRestoreArrowVisibilityFun(isVisible: Boolean): DeferredRouteUpdateFun = {
        routeArrow?.updateVisibilityTo(isVisible)
    }
}
internal typealias DeferredRouteUpdateFun = () -> Unit
