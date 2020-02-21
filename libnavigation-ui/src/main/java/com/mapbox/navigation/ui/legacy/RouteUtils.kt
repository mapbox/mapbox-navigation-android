package com.mapbox.navigation.ui.legacy

import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.api.directions.v5.models.LegStep
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.model.RouteProgressState
import timber.log.Timber

class RouteUtils {

    companion object {
        private const val FIRST_INSTRUCTION = 0
        private const val ORIGIN_WAYPOINT_NAME_THRESHOLD = 1
        private const val ORIGIN_WAYPOINT_INDEX_THRESHOLD = 1
        private const val ORIGIN_WAYPOINT_INDEX = 0
        private const val ORIGIN_APPROACH_THRESHOLD = 1
        private const val ORIGIN_APPROACH_INDEX = 0
        private const val FIRST_POSITION = 0
        private const val SECOND_POSITION = 1
        private const val SEMICOLON = ";"
    }

    /**
     * Looks at the current [RouteProgressState] and returns if
     * is [RouteProgressState.ROUTE_ARRIVED].
     *
     * @param routeProgress the current route progress
     * @return true if in arrival state, false if not
     */
    fun isArrivalEvent(routeProgress: RouteProgress): Boolean =
        routeProgress.currentState()?.let { currentState ->
            currentState == RouteProgressState.ROUTE_ARRIVED
        } ?: false

    /**
     * Looks at the current [RouteProgress] list of legs and
     * checks if the current leg is the last leg.
     *
     * @param routeProgress the current route progress
     * @return true if last leg, false if not
     * @since 0.8.0
     */
    fun isLastLeg(routeProgress: RouteProgress): Boolean {
        val legs = routeProgress.route()?.legs()
        val currentLeg = routeProgress.currentLegProgress()?.routeLeg()
        return legs?.let { legList ->
            currentLeg == legList[legList.size - 1]
        } ?: false
    }

    /**
     * Given a [RouteProgress], this method will calculate the remaining waypoint coordinates
     * along the given route based on total route coordinates and the progress remaining waypoints.
     *
     * If the coordinate size is less than the remaining waypoints, this method
     * will return null.
     *
     * @param routeProgress for route coordinates and remaining waypoints
     * @return list of remaining waypoints as [Point]s
     * @since 0.10.0
     */
    fun calculateRemainingWaypoints(routeProgress: RouteProgress?): List<Point>? {
        val routeOptions = routeProgress?.route()?.routeOptions()
            ?: return null
        val coordinates = ArrayList(routeOptions.coordinates())
        val coordinatesSize = coordinates.size
        val remainingWaypointsCount = routeProgress.remainingWaypoints()
            ?: return null
        if (coordinatesSize < remainingWaypointsCount) {
            return null
        }
        val waypointIndices = routeOptions.waypointIndices()
            ?: return coordinates.subList(
                coordinatesSize - remainingWaypointsCount,
                coordinatesSize
            )

        val allWaypointIndices = waypointIndices.split(SEMICOLON).toTypedArray()
        val remainingWaypointIndices: Array<String> =
            allWaypointIndices.copyOfRange(
                allWaypointIndices.size - remainingWaypointsCount,
                allWaypointIndices.size
            )
        return try {
            val firstRemainingWaypointIndex = remainingWaypointIndices[FIRST_POSITION].toInt()
            coordinates.subList(firstRemainingWaypointIndex, coordinatesSize)
        } catch (ex: NumberFormatException) {
            Timber.e("Fail to convert waypoint index to integer")
            null
        }
    }

    /**
     * Given a [RouteProgress], this method will calculate the waypoint indices
     * along the given route based on route option waypoint indices and the progress remaining waypoint coordinates.
     * Remaining waypoint indices are recalculated based on count of already achieved waypoints.
     *
     * If the waypoint indices are empty, this method will return null.
     *
     * @param routeProgress for route waypoint indices and remaining coordinates
     * @return Integer array including the origin waypoint index and the recalculated remaining ones
     * @since 0.43.0
     */
    fun calculateRemainingWaypointIndices(routeProgress: RouteProgress): IntArray? {
        val routeOptions = routeProgress.route()?.routeOptions()
        if (routeOptions == null || routeOptions.waypointIndices().isNullOrEmpty()) {
            return null
        }
        val waypointIndices = routeOptions.waypointIndices()
            ?: return null
        val remainingWaypointsCount = routeProgress.remainingWaypoints()
            ?: return null

        val allWaypointIndices = waypointIndices.split(SEMICOLON).toTypedArray()
        val remainingWaypointIndices: Array<String> =
            allWaypointIndices.copyOfRange(
                allWaypointIndices.size - remainingWaypointsCount,
                allWaypointIndices.size
            )
        return try {
            val firstRemainingWaypointIndex = remainingWaypointIndices[FIRST_POSITION].toInt()
            val traveledCoordinatesCount =
                firstRemainingWaypointIndex - ORIGIN_WAYPOINT_INDEX_THRESHOLD
            val resultWaypointIndices =
                IntArray(remainingWaypointIndices.size + ORIGIN_WAYPOINT_NAME_THRESHOLD)
            resultWaypointIndices[ORIGIN_WAYPOINT_INDEX] =
                allWaypointIndices[ORIGIN_WAYPOINT_INDEX].toInt()
            for (i in remainingWaypointIndices.indices) {
                resultWaypointIndices[i + 1] =
                    remainingWaypointIndices[i].toInt() - traveledCoordinatesCount
            }
            resultWaypointIndices
        } catch (ex: NumberFormatException) {
            Timber.e("Fail to convert waypoint index to integer")
            null
        }
    }

    /**
     * Given a [RouteProgress], this method will calculate the remaining waypoint names
     * along the given route based on route option waypoint names and the progress remaining waypoint coordinates.
     *
     * If the waypoint names are empty, this method will return null.
     *
     * @param routeProgress for route waypoint names and remaining coordinates
     * @return String array including the origin waypoint name and the remaining ones
     * @since 0.19.0
     */
    fun calculateRemainingWaypointNames(routeProgress: RouteProgress): Array<String>? {
        val routeOptions = routeProgress.route()?.routeOptions()
        if (routeOptions == null || routeOptions.waypointNames().isNullOrEmpty()) {
            return null
        }
        val waypointNames = routeOptions.waypointNames()
            ?: return null
        val remainingWaypointsCount = routeProgress.remainingWaypoints()
            ?: return null

        val allWaypointNames = waypointNames.split(SEMICOLON).toTypedArray()
        val remainingWaypointNames: Array<String> =
            allWaypointNames.copyOfRange(
                allWaypointNames.size - remainingWaypointsCount,
                allWaypointNames.size
            )
        val resultWaypointNames =
            arrayOfNulls<String>(remainingWaypointNames.size + ORIGIN_WAYPOINT_NAME_THRESHOLD)
        resultWaypointNames[ORIGIN_WAYPOINT_INDEX] = allWaypointNames[ORIGIN_WAYPOINT_INDEX]
        System.arraycopy(
            remainingWaypointNames,
            FIRST_POSITION,
            resultWaypointNames,
            SECOND_POSITION,
            remainingWaypointNames.size
        )
        return resultWaypointNames.map { it ?: "" }.toTypedArray()
    }

    /**
     * Given a [RouteProgress], this method will calculate the remaining approaches
     * along the given route based on route option approaches and the progress remaining approaches.
     *
     * If the approaches are empty, this method will return null.
     *
     * @param routeProgress for route approaches and remaining coordinates
     * @return String array including the origin approach and the remaining ones
     * @since 0.19.0
     */
    fun calculateRemainingApproaches(routeProgress: RouteProgress): Array<String>? {
        val routeOptions = routeProgress.route()?.routeOptions()
        if (routeOptions == null || routeOptions.approaches().isNullOrEmpty()) {
            return null
        }
        val approaches = routeOptions.approaches()
            ?: return null
        val remainingWaypointsCount = routeProgress.remainingWaypoints()
            ?: return null

        val allApproaches = approaches.split(SEMICOLON).toTypedArray()
        val remainingApproaches: Array<String> = allApproaches.copyOfRange(
            allApproaches.size - remainingWaypointsCount,
            allApproaches.size
        )
        val resultApproaches =
            arrayOfNulls<String>(remainingApproaches.size + ORIGIN_APPROACH_THRESHOLD)
        resultApproaches[ORIGIN_APPROACH_INDEX] = allApproaches[ORIGIN_APPROACH_INDEX]
        System.arraycopy(
            remainingApproaches,
            FIRST_POSITION,
            resultApproaches,
            SECOND_POSITION,
            remainingApproaches.size
        )
        return resultApproaches.map { it ?: "" }.toTypedArray()
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
    fun findCurrentBannerInstructions(
        currentStep: LegStep?,
        stepDistanceRemaining: Double
    ): BannerInstructions? = currentStep?.bannerInstructions()?.let {
        val instructions: List<BannerInstructions> = sortBannerInstructions(it)
        instructions.firstOrNull { instruction ->
            instruction.distanceAlongGeometry().toInt() >= stepDistanceRemaining.toInt()
        } ?: when (instructions.isNotEmpty()) {
            true -> instructions[FIRST_INSTRUCTION]
            else -> null
        }
    }

    private fun sortBannerInstructions(instructions: List<BannerInstructions>): List<BannerInstructions> =
        instructions.toMutableList()
            .sortedBy { it.distanceAlongGeometry() }
}
