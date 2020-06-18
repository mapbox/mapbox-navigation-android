package com.mapbox.navigation.ui.route

import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.ui.internal.route.RouteConstants.MINIMUM_ROUTE_LINE_OFFSET
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
    private val routeArrow: MapRouteArrow,
    private val vanishingLineAnimator: ValueAnimator?
) : RouteProgressObserver {

    private val jobControl by lazy { ThreadController.getMainScopeAndRootJob() }
    private var lastDistanceValue = 0f

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

    private val routeLineAnimatorUpdateCallback = ValueAnimator.AnimatorUpdateListener {
        val animationDistanceValue = it.animatedValue as Float
        if (animationDistanceValue > MINIMUM_ROUTE_LINE_OFFSET) {
            val expression = routeLine.getExpressionAtOffset(animationDistanceValue)
            routeLine.hideShieldLineAtOffset(animationDistanceValue)
            routeLine.hideRouteLineAtOffset(animationDistanceValue)
            routeLine.decorateRouteLine(expression)
        }
    }

    init {
        this.lastDistanceValue = routeLine.vanishPointOffset
        this.vanishingLineAnimator?.addUpdateListener(routeLineAnimatorUpdateCallback)
    }

    override fun onRouteProgressChanged(routeProgress: RouteProgress) {
        onProgressChange(routeProgress)
    }

    private fun onProgressChange(routeProgress: RouteProgress) {
        val directionsRoute = routeLine.getPrimaryRoute()
        updateRoute(directionsRoute, routeProgress)
    }

    private fun updateRoute(directionsRoute: DirectionsRoute?, routeProgress: RouteProgress) {
        if (routeArrow.routeArrowIsVisible()) {
            routeArrow.addUpcomingManeuverArrow(routeProgress)
        }
        val currentRoute = routeProgress.route
        val hasGeometry = currentRoute.geometry()?.isNotEmpty() ?: false
        if (hasGeometry && currentRoute != directionsRoute) {
            vanishingLineAnimator?.cancel()
            routeLine.draw(currentRoute)
            this.lastDistanceValue = routeLine.vanishPointOffset
        } else {
            // if there is no geometry then the session is in free drive and the vanishing
            // route line code should not execute.
            if (vanishingLineAnimator != null && hasGeometry) {
                jobControl.scope.launch {
                    val percentDistanceTraveled = getPercentDistanceTraveled(routeProgress)
                    if (percentDistanceTraveled > 0) {
                        animateVanishRouteLineUpdate(lastDistanceValue, percentDistanceTraveled)
                        lastDistanceValue = percentDistanceTraveled
                    }
                }
            }
        }
    }

    private fun animateVanishRouteLineUpdate(
        startingDistanceValue: Float,
        percentDistanceTraveled: Float
    ) {
        vanishingLineAnimator?.setValues(PropertyValuesHolder.ofFloat(
            "",
            startingDistanceValue,
            percentDistanceTraveled
        ))
        vanishingLineAnimator?.start()
    }

    private fun getPercentDistanceTraveled(routeProgress: RouteProgress): Float {
        val totalDist =
            (routeProgress.distanceRemaining + routeProgress.distanceTraveled)
        return routeProgress.distanceTraveled / totalDist
    }
}
