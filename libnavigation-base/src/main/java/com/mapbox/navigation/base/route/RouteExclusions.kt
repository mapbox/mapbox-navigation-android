@file:JvmName("RouteExclusions")

package com.mapbox.navigation.base.route

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.LegStep
import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.api.directions.v5.models.StepIntersection
import com.mapbox.api.directions.v5.utils.ParseUtils

private const val COMMA_DELIMETER = ","

/**
 * Violated road type.
 *
 * @param type see [RouteOptions.Builder.exclude] for possible types.
 * @param route [DirectionsRoute] that includes the exclusion violation.
 * @param legIndex index representing the leg that includes the exclusion violation.
 * @param leg [RouteLeg] that includes the exclusion violation.
 * @param stepIndex index representing the step that includes the exclusion violation.
 * @param step [LegStep] that includes the exclusion violation.
 * @param intersectionIndex index representing the intersection that includes the exclusion violation.
 * @param intersection [StepIntersection] that includes the exclusion violation.
 */
class ExclusionViolation(
    val type: String,
    val route: DirectionsRoute,
    val legIndex: Int,
    val leg: RouteLeg,
    val stepIndex: Int,
    val step: LegStep,
    val intersectionIndex: Int,
    val intersection: StepIntersection,
) {

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ExclusionViolation

        if (type != other.type) return false
        if (route != other.route) return false
        if (legIndex != other.legIndex) return false
        if (leg != other.leg) return false
        if (stepIndex != other.stepIndex) return false
        if (step != other.step) return false
        if (intersectionIndex != other.intersectionIndex) return false
        return intersection == other.intersection
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + route.hashCode()
        result = 31 * result + legIndex
        result = 31 * result + leg.hashCode()
        result = 31 * result + stepIndex
        result = 31 * result + step.hashCode()
        result = 31 * result + intersectionIndex
        result = 31 * result + intersection.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "ExclusionViolation(" +
            "type='$type', " +
            "route=$route, " +
            "legIndex=$legIndex, " +
            "leg=$leg, " +
            "stepIndex=$stepIndex, " +
            "step=$step, " +
            "intersectionIndex=$intersectionIndex, " +
            "intersection=$intersection," +
            ")"
    }
}

/**
 * Returns all violated exclusions for this route.
 *
 * @see [RouteOptions.Builder.exclude]
 */
fun NavigationRoute.exclusionViolations(): List<ExclusionViolation> {
    val exclusionViolations = mutableListOf<ExclusionViolation>()
    val excludeCriteria = ParseUtils.parseToStrings(routeOptions.exclude(), COMMA_DELIMETER)
    excludeCriteria?.let {
        directionsRoute.legs()?.forEachIndexed { legIndex, leg ->
            leg.steps()?.forEachIndexed { stepIndex, step ->
                step.intersections()?.forEachIndexed { intersectionIndex, intersection ->
                    intersection.classes()?.forEach { classes ->
                        if (excludeCriteria.contains(classes)) {
                            val exclusionViolation = ExclusionViolation(
                                classes,
                                directionsRoute,
                                legIndex,
                                leg,
                                stepIndex,
                                step,
                                intersectionIndex,
                                intersection,
                            )
                            exclusionViolations.add(exclusionViolation)
                        }
                    }
                }
            }
        }
    }
    return exclusionViolations
}
