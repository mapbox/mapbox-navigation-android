@file: JvmName("RoadObjectEx")

package com.mapbox.navigation.core.trip.roadobject

import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.model.roadobject.UpcomingRoadObject
import com.mapbox.navigation.core.trip.roadobject.reststop.RestStopFactory
import com.mapbox.navigation.core.trip.roadobject.reststop.RestStopFromIntersection
import com.mapbox.navigation.core.trip.roadobject.reststop.RestStopFromRoadObject
import com.mapbox.navigation.utils.internal.ifNonNull

/**
 * Use the API to retrieve a list of all upcoming rest stops using [UpcomingRoadObject].
 * This is recommended to be used with
 * - [RouteProgress] during Active Navigation.
 */
fun List<UpcomingRoadObject>.getAllRestStops(): List<RestStopFromRoadObject> {
    return RestStopFactory.createWithUpcomingRoadObjects(this)
}

/**
 * Use the API to retrieve the first upcoming rest stop using [UpcomingRoadObject]. The API will
 * always find the first upcoming rest stop from the list of upcoming rest stops if available.
 * - returns null if route has no rest stop: In this case the API will return null
 * - returns [RestStopFromRoadObject] if route has 1 rest stop and hasn't been passed/reached
 * - returns null if route has 1 rest stop and it has already been passed/reached
 * - returns the first [RestStopFromRoadObject] that hasn't been passed/reached if route has multiple
 *   rest stops and you have passed a few of them
 * - returns null if route has multiple rest stops and all have been passed
 * This is recommended to be used with
 * - [RouteProgress] during Active Navigation.
 */
fun List<UpcomingRoadObject>.getFirstUpcomingRestStop(): RestStopFromRoadObject? {
    val restStops = this.getAllRestStops()
    if (restStops.isNotEmpty()) {
        val distanceToStart = restStops.first().distanceToStart
        if (distanceToStart != null && distanceToStart > 0) {
            return restStops.first()
        }
    }
    return null
}

/**
 * Use the API to retrieve the list of all rest stops using [NavigationRoute].
 * This is recommended to be used with:
 * - [NavigationRoute] during Route Preview
 */
fun NavigationRoute.getAllRestStops(): RoadObjectExtra<RestStopFromIntersection> {
    val legIndexToRestStops = ifNonNull(this.directionsRoute.legs()) { routeLegs ->
        val legToRestStops = hashMapOf<Int, List<RestStopFromIntersection>>()
        routeLegs.forEachIndexed { legIndex, routeLeg ->
            legToRestStops[legIndex] = ifNonNull(routeLeg.steps()) { steps ->
                val restStops = mutableListOf<RestStopFromIntersection>()
                steps.forEach { legStep ->
                    restStops.addAll(
                        RestStopFactory.createWithStepIntersection(
                            legStep.intersections()
                        )
                    )
                }
                restStops
            } ?: emptyList()
        }
        legToRestStops
    } ?: emptyMap()
    return RoadObjectExtra(this.id, legIndexToRestStops)
}
