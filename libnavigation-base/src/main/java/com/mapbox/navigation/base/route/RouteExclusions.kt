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
data class ExclusionViolation(
    val type: String,
    val route: DirectionsRoute,
    val legIndex: Int,
    val leg: RouteLeg,
    val stepIndex: Int,
    val step: LegStep,
    val intersectionIndex: Int,
    val intersection: StepIntersection
)

/**
 * Returns all violated exclusions for this route.
 *
 * @see [RouteOptions.Builder.exclude]
 */
fun DirectionsRoute.exclusionViolations(): List<ExclusionViolation> {
    val exclusionViolations = mutableListOf<ExclusionViolation>()
    val excludeCriteria = ParseUtils.parseToStrings(this.routeOptions()?.exclude(), COMMA_DELIMETER)
    excludeCriteria?.let {
        this.legs()?.forEachIndexed { legIndex, leg ->
            leg.steps()?.forEachIndexed { stepIndex, step ->
                step.intersections()?.forEachIndexed { intersectionIndex, intersection ->
                    intersection.classes()?.forEach { classes ->
                        if (excludeCriteria.contains(classes)) {
                            val exclusionViolation = ExclusionViolation(
                                classes,
                                this,
                                legIndex,
                                leg,
                                stepIndex,
                                step,
                                intersectionIndex,
                                intersection
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
