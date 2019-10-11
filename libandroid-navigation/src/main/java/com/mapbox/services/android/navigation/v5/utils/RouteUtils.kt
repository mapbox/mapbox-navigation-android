package com.mapbox.services.android.navigation.v5.utils

import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.api.directions.v5.models.LegStep
import com.mapbox.core.utils.TextUtils
import com.mapbox.geojson.Point
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgressState
import java.util.ArrayList
import java.util.Arrays
import java.util.Comparator

class RouteUtils {

    /**
     * Looks at the current [RouteProgressState] and returns if
     * is [RouteProgressState.ROUTE_ARRIVED].
     *
     * @param routeProgress the current route progress
     * @return true if in arrival state, false if not
     */
    fun isArrivalEvent(routeProgress: RouteProgress): Boolean {
        val currentState = routeProgress.currentState()
        return currentState != null && currentState == RouteProgressState.ROUTE_ARRIVED
    }

    /**
     * Looks at the current [RouteProgress] list of legs and
     * checks if the current leg is the last leg.
     *
     * @param routeProgress the current route progress
     * @return true if last leg, false if not
     * @since 0.8.0
     */
    fun isLastLeg(routeProgress: RouteProgress): Boolean {
        val legs = routeProgress.directionsRoute().legs()
        val currentLeg = routeProgress.currentLeg()
        legs?.let { legsList ->
            return currentLeg == legsList[legsList.size - 1]
        } ?: return false
    }

    /**
     * Given a [RouteProgress], this method will calculate the remaining coordinates
     * along the given route based on total route coordinates and the progress remaining waypoints.
     *
     *
     * If the coordinate size is less than the remaining waypoints, this method
     * will return null.
     *
     * @param routeProgress for route coordinates and remaining waypoints
     * @return list of remaining waypoints as [Point]s
     * @since 0.10.0
     */
    fun calculateRemainingWaypoints(routeProgress: RouteProgress): List<Point>? {
        val routeOptions = routeProgress.directionsRoute().routeOptions() ?: return null
        val coordinates = ArrayList(routeOptions.coordinates())
        val coordinatesSize = coordinates.size
        val remainingWaypoints = routeProgress.remainingWaypoints()
        return when (coordinatesSize < remainingWaypoints) {
            true -> null
            false -> coordinates.subList(coordinatesSize - remainingWaypoints, coordinatesSize)
        }
    }

    /**
     * Given a [RouteProgress], this method will calculate the remaining waypoint names
     * along the given route based on route option waypoint names and the progress remaining coordinates.
     *
     *
     * If the waypoint names are empty, this method will return null.
     *
     * @param routeProgress for route waypoint names and remaining coordinates
     * @return String array including the origin waypoint name and the remaining ones
     * @since 0.19.0
     */
    fun calculateRemainingWaypointNames(routeProgress: RouteProgress): Array<String?>? {
        val routeOptions = routeProgress.directionsRoute().routeOptions() ?: return null
        val allWaypointNames = routeOptions.waypointNames()
        if (allWaypointNames == null || TextUtils.isEmpty(allWaypointNames)) {
            return null
        }
        val names = allWaypointNames.split(SEMICOLON.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val coordinatesSize = routeOptions.coordinates().size
        val remainingWaypointNames = Arrays.copyOfRange(names,
                coordinatesSize - routeProgress.remainingWaypoints(), coordinatesSize)
        val waypointNames = arrayOfNulls<String>(remainingWaypointNames.size + ORIGIN_WAYPOINT_NAME_THRESHOLD)
        waypointNames[ORIGIN_WAYPOINT_NAME] = names[ORIGIN_WAYPOINT_NAME]
        System.arraycopy(remainingWaypointNames, FIRST_POSITION, waypointNames, SECOND_POSITION,
                remainingWaypointNames.size)
        return waypointNames
    }

    /**
     * Given the current step / current step distance remaining, this function will
     * find the current instructions to be shown.
     *
     * @param currentStep holding the current banner instructions
     * @param stepDistanceRemaining to determine progress along the currentStep
     * @return the current banner instructions based on the current distance along the step
     * @since 0.13.0
     */
    fun findCurrentBannerInstructions(currentStep: LegStep?, stepDistanceRemaining: Double): BannerInstructions? {
        currentStep?.let { currStep ->
            val instructions = currStep.bannerInstructions()
            instructions?.let { instructionList ->
                when (instructionList.isEmpty()) {
                    true -> return null
                    false -> {
                        val sortedInstructions = sortBannerInstructions(instructionList)
                        for (instruction in sortedInstructions) {
                            val distanceAlongGeometry = instruction.distanceAlongGeometry().toInt()
                            if (distanceAlongGeometry >= stepDistanceRemaining.toInt()) {
                                return instruction
                            }
                        }
                        return instructions[FIRST_INSTRUCTION]
                    }
                }
            } ?: return null
        } ?: return null
    }

    private fun sortBannerInstructions(instructions: List<BannerInstructions>): List<BannerInstructions> {
        val sortedInstructions = ArrayList(instructions)
        sortedInstructions.sortWith(Comparator { instruction, nextInstructions ->
            java.lang.Double.compare(instruction.distanceAlongGeometry(),
                    nextInstructions.distanceAlongGeometry()
            )
        })
        return sortedInstructions
    }

    companion object {
        private const val FIRST_INSTRUCTION = 0
        private const val ORIGIN_WAYPOINT_NAME_THRESHOLD = 1
        private const val ORIGIN_WAYPOINT_NAME = 0
        private const val FIRST_POSITION = 0
        private const val SECOND_POSITION = 1
        private const val SEMICOLON = ";"
    }
}
