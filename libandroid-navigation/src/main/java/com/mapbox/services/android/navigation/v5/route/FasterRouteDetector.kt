package com.mapbox.services.android.navigation.v5.route

import android.location.Location
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.LegStep
import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.NAVIGATION_CHECK_FASTER_ROUTE_INTERVAL
import com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.NAVIGATION_MEDIUM_ALERT_DURATION
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress
import com.mapbox.services.android.navigation.v5.utils.extensions.ifNonNull
import java.util.Date
import java.util.concurrent.TimeUnit

class FasterRouteDetector : FasterRoute() {

    val VALID_ROUTE_DURATION_REMAINING = 600
    private var lastCheckedLocation: Location? = null

    override fun shouldCheckFasterRoute(location: Location, routeProgress: RouteProgress): Boolean {
        // On first pass through detector, last checked location will be null
        if (lastCheckedLocation == null) {
            lastCheckedLocation = location
        }
        // Check if the faster route time interval has been exceeded
        if (secondsSinceLastCheck(location) >= NAVIGATION_CHECK_FASTER_ROUTE_INTERVAL) {
            lastCheckedLocation = location
            // Check for both valid route and step durations remaining
            return validRouteDurationRemaining(routeProgress) && validStepDurationRemaining(routeProgress)
        }
        return false
    }

    override fun isFasterRoute(response: DirectionsResponse, routeProgress: RouteProgress): Boolean {
        if (validRouteResponse(response)) {
            ifNonNull(routeProgress.durationRemaining()) { currentDurationRemaining ->
                val newRoute = response.routes()[0]

                if (hasLegs(newRoute)) {
                    // Extract the first leg
                    newRoute.legs()?.let { routeLegList ->
                        val routeLeg = routeLegList[0]
                        if (hasAtLeastTwoSteps(routeLeg)) {
                            routeLeg.steps()?.let { stepList ->
                                // Extract the first two steps
                                val firstStep = stepList[0]
                                val secondStep = stepList[1]
                                // Check for valid first and second steps of the new route
                                if (!validFirstStep(firstStep) || !validSecondStep(secondStep, routeProgress)) {
                                    return false
                                }
                            }
                        }
                    }
                }
                newRoute.duration()?.let { duration ->
                    // New route must be at least 10% faster
                    if (duration <= (0.9 * currentDurationRemaining)) {
                        return true
                    }
                }
            }
        }
        return false
    }

    private fun hasLegs(newRoute: DirectionsRoute): Boolean {
        val routeLegList = newRoute.legs()
        return !routeLegList.isNullOrEmpty()
    }

    private fun hasAtLeastTwoSteps(routeLeg: RouteLeg): Boolean {
        val stepsList = routeLeg.steps()
        return stepsList != null && stepsList.size > 2
    }

    /**
     * The second step of the new route is valid if
     * it equals the current route upcoming step.
     *
     * @param secondStep of the new route
     * @param routeProgress current route progress
     * @return true if valid, false if not
     */
    private fun validSecondStep(secondStep: LegStep, routeProgress: RouteProgress): Boolean {
        return routeProgress.currentLegProgress()?.upComingStep()?.let { legStep ->
            legStep == secondStep
        } ?: false
    }

    /**
     * First step is valid if it is greater than
     * [com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.NAVIGATION_MEDIUM_ALERT_DURATION].
     *
     * @param firstStep of the new route
     * @return true if valid, false if not
     */
    private fun validFirstStep(firstStep: LegStep): Boolean {
        return firstStep.duration() > NAVIGATION_MEDIUM_ALERT_DURATION
    }

    /**
     * Checks if we have at least one [DirectionsRoute] in the given
     * [DirectionsResponse].
     *
     * @param response to be checked
     * @return true if valid, false if not
     */
    private fun validRouteResponse(response: DirectionsResponse?): Boolean {
        return response?.routes()?.isNotEmpty() ?: false
    }

    private fun validRouteDurationRemaining(routeProgress: RouteProgress) =
            // Total route duration remaining in seconds
            ifNonNull(routeProgress.durationRemaining()) { durationRemaining ->
                durationRemaining.toInt() > VALID_ROUTE_DURATION_REMAINING
            } ?: false

    private fun validStepDurationRemaining(routeProgress: RouteProgress) =
        // Current step duration remaining in seconds
        ifNonNull(routeProgress.currentLegProgress()?.currentStepProgress()?.durationRemaining()) { currentStepDurationRemaining ->
            currentStepDurationRemaining > NAVIGATION_MEDIUM_ALERT_DURATION
        } ?: false

    private fun secondsSinceLastCheck(location: Location): Long {
        return lastCheckedLocation?.let { loc ->
            dateDiff(Date(loc.time), Date(location.time), TimeUnit.SECONDS)
        } ?: 0L
    }

    private fun dateDiff(firstDate: Date, secondDate: Date, timeUnit: TimeUnit): Long {
        val diffInMillis = secondDate.time - firstDate.time
        return timeUnit.convert(diffInMillis, TimeUnit.MILLISECONDS)
    }
}
