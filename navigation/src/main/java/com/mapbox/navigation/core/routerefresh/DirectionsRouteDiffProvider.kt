package com.mapbox.navigation.core.routerefresh

import com.google.gson.JsonElement
import com.mapbox.api.directions.v5.models.LegAnnotation
import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.navigation.base.route.NavigationRoute
import kotlin.math.min

internal class DirectionsRouteDiffProvider {

    fun buildRouteDiffs(
        oldRoute: NavigationRoute,
        newRoute: NavigationRoute,
        currentLegIndex: Int,
    ): List<String> {
        val routeDiffs = arrayListOf<String>()
        val oldRouteLegs = oldRoute.directionsRoute.legs()
        val newRouteLegs = newRoute.directionsRoute.legs()
        if (oldRouteLegs != null && newRouteLegs != null) {
            for (legIndex in currentLegIndex until min(oldRouteLegs.size, newRouteLegs.size)) {
                val oldLeg = oldRouteLegs[legIndex]
                val newLeg = newRouteLegs[legIndex]
                val updatedAnnotations = getUpdatedData(oldLeg, newLeg)
                if (updatedAnnotations.isNotEmpty()) {
                    routeDiffs.add(
                        "Updated ${updatedAnnotations.joinToString()} at " +
                            "route ${newRoute.id} leg $legIndex",
                    )
                }
            }
        }
        if (oldRoute.waypoints != newRoute.waypoints) {
            routeDiffs.add("Updated waypoints at route ${newRoute.id}")
        }
        return routeDiffs
    }

    private fun getUpdatedData(oldRouteLeg: RouteLeg, newRouteLeg: RouteLeg): List<String> {
        val result = mutableListOf<String>()
        result.addAll(
            getUpdatedAnnotations(
                oldRouteLeg.annotation(),
                newRouteLeg.annotation(),
            ),
        )
        if (oldRouteLeg.incidents() != newRouteLeg.incidents()) {
            result.add("incidents")
        }
        if (oldRouteLeg.closures() != newRouteLeg.closures()) {
            result.add("closures")
        }
        return result
    }

    private fun getUpdatedAnnotations(
        oldLegAnnotation: LegAnnotation?,
        newLegAnnotation: LegAnnotation?,
    ): List<String> {
        val updatedAnnotations = arrayListOf<String>()
        if (oldLegAnnotation?.distance() != newLegAnnotation?.distance()) {
            updatedAnnotations.add("distance")
        }
        if (oldLegAnnotation?.duration() != newLegAnnotation?.duration()) {
            updatedAnnotations.add("duration")
        }
        if (oldLegAnnotation?.speed() != newLegAnnotation?.speed()) {
            updatedAnnotations.add("speed")
        }
        if (oldLegAnnotation?.maxspeed() != newLegAnnotation?.maxspeed()) {
            updatedAnnotations.add("maxSpeed")
        }
        if (oldLegAnnotation?.congestion() != newLegAnnotation?.congestion()) {
            updatedAnnotations.add("congestion")
        }
        if (oldLegAnnotation?.congestionNumeric() != newLegAnnotation?.congestionNumeric()) {
            updatedAnnotations.add("congestionNumeric")
        }
        if (oldLegAnnotation?.stateOfCharge() != newLegAnnotation?.stateOfCharge()) {
            updatedAnnotations.add("state_of_charge")
        }
        if (oldLegAnnotation?.currentSpeed() != newLegAnnotation?.currentSpeed()) {
            updatedAnnotations.add("current_speed")
        }
        if (oldLegAnnotation?.freeflowSpeed() != newLegAnnotation?.freeflowSpeed()) {
            updatedAnnotations.add("freeflow_speed")
        }
        return updatedAnnotations
    }

    private fun LegAnnotation.stateOfCharge(): JsonElement? =
        getUnrecognizedProperty("state_of_charge")
}
