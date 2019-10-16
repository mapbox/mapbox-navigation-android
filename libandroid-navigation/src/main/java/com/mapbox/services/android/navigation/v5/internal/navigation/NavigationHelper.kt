package com.mapbox.services.android.navigation.v5.internal.navigation

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.LegAnnotation
import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.core.constants.Constants.PRECISION_6
import com.mapbox.geojson.Point
import com.mapbox.geojson.utils.PolylineUtils
import com.mapbox.services.android.navigation.v5.milestone.Milestone
import com.mapbox.services.android.navigation.v5.routeprogress.CurrentLegAnnotation
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress
import com.mapbox.services.android.navigation.v5.utils.extensions.ifNonNull
import java.util.ArrayList

/**
 * This contains several single purpose methods that help out when a new location update occurs and
 * calculations need to be performed on it.
 */
internal object NavigationHelper {

    private const val INDEX_ZERO = 0
    private const val EMPTY_STRING = ""

    /**
     * When a milestones triggered, it's instruction needs to be built either using the provided
     * string or an empty string.
     */
    fun buildInstructionString(
        routeProgress: RouteProgress,
        milestone: Milestone
    ): String =
        milestone.instruction?.buildInstruction(routeProgress) ?: EMPTY_STRING

    /**
     * Takes in the leg distance remaining value already calculated and if additional legs need to be
     * traversed along after the current one, adds those distances and returns the new distance.
     * Otherwise, if the route only contains one leg or the users on the last leg, this value will
     * equal the leg distance remaining.
     */
    @JvmStatic
    fun routeDistanceRemaining(
        legDistanceRemaining: Double,
        legIndex: Int,
        directionsRoute: DirectionsRoute
    ): Double {
        var distanceRemaining = legDistanceRemaining
        directionsRoute.legs()?.let { legs ->
            val legsSize = legs.size
            if (legsSize < 2) {
                return distanceRemaining
            }

            for (i in legIndex + 1 until legsSize) {
                distanceRemaining += legs[i].distance() ?: 0.0
            }
        }
        return distanceRemaining
    }

    /**
     * Given the current [DirectionsRoute] and leg / step index,
     * return a list of [Point] representing the current step.
     *
     * This method is only used on a per-step basis as [PolylineUtils.decode]
     * can be a heavy operation based on the length of the step.
     *
     * Returns null if index is invalid.
     *
     * @param directionsRoute for list of steps
     * @param legIndex to get current step list
     * @param stepIndex to get current step
     * @return list of [Point] representing the current step
     */

    @JvmStatic
    fun decodeStepPoints(
        directionsRoute: DirectionsRoute,
        currentPoints: List<Point>?,
        legIndex: Int,
        stepIndex: Int
    ): List<Point>? {
        val legs = directionsRoute.legs()
        legs?.let { legs ->
            if (legs.isEmpty()) return currentPoints
            val steps = legs[legIndex].steps()
            steps?.let { steps ->
                if (steps.isEmpty()) return currentPoints
                val invalidStepIndex = stepIndex < 0 || stepIndex > steps.size - 1
                if (invalidStepIndex) return currentPoints
                val step = steps[stepIndex]
                step?.let { step ->
                    val stepGeometry = step.geometry()
                    stepGeometry?.let {
                        return PolylineUtils.decode(stepGeometry, PRECISION_6)
                    }
                }
            }
        }
        return currentPoints
    }

    /**
     * Given a list of distance annotations, find the current annotation index.  This index retrieves the
     * current annotation from any provided annotation list in [LegAnnotation].
     *
     * @param currentLegAnnotation current annotation being traveled along
     * @param leg holding each list of annotations
     * @param legDistanceRemaining to determine the new set of annotations
     * @return a current set of annotation data for the user's position along the route
     */
    @JvmStatic
    fun createCurrentAnnotation(
        currentLegAnnotation: CurrentLegAnnotation?,
        leg: RouteLeg,
        legDistanceRemaining: Double
    ): CurrentLegAnnotation? {
        leg.annotation()?.distance()?.let { distanceList ->
            if (distanceList.isEmpty()) return null

            val annotationBuilder = CurrentLegAnnotation.builder()
            val annotationIndex = findAnnotationIndex(
                currentLegAnnotation,
                annotationBuilder,
                leg,
                legDistanceRemaining,
                distanceList
            )
            annotationBuilder.distance(distanceList[annotationIndex])
            val durationList = leg.annotation()?.duration()
            durationList?.let {
                annotationBuilder.duration(durationList[annotationIndex])
            }
            val speedList = leg.annotation()?.speed()
            speedList?.let {
                annotationBuilder.speed(speedList[annotationIndex])
            }
            val maxSpeedList = leg.annotation()?.maxspeed()
            maxSpeedList?.let {
                annotationBuilder.maxspeed(maxSpeedList[annotationIndex])
            }
            val congestionList = leg.annotation()?.congestion()
            congestionList?.let {
                annotationBuilder.congestion(congestionList[annotationIndex])
            }
            annotationBuilder.index(annotationIndex)
            return annotationBuilder.build()
        } ?: return null
    }

    private fun findAnnotationIndex(
        currentLegAnnotation: CurrentLegAnnotation?,
        annotationBuilder: CurrentLegAnnotation.Companion.Builder,
        leg: RouteLeg,
        legDistanceRemaining: Double,
        distanceAnnotationList: List<Double?>
    ): Int {
        val legDistances = ArrayList(distanceAnnotationList)
        val totalLegDistance = leg.distance()
        totalLegDistance?.let {
            val distanceTraveled = totalLegDistance - legDistanceRemaining
            var distanceIndex = 0
            var annotationDistancesTraveled = 0.0
            ifNonNull(currentLegAnnotation) {
                distanceIndex = it.index
                annotationDistancesTraveled = it.distanceToAnnotation
            }
            for (i in distanceIndex until legDistances.size) {
                val distance = legDistances[i]
                distance?.let {
                    annotationDistancesTraveled += it
                    if (annotationDistancesTraveled > distanceTraveled) {
                        val distanceToAnnotation = annotationDistancesTraveled - it
                        annotationBuilder.distanceToAnnotation(distanceToAnnotation)
                        return i
                    }
                }
            }
        }
        return INDEX_ZERO
    }
}
