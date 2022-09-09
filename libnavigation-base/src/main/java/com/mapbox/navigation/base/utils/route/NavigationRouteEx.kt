@file:JvmName("NavigationRouteUtils")

package com.mapbox.navigation.base.utils.route

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.utils.DecodeUtils.stepGeometryToPoints
import com.mapbox.navigation.utils.internal.logD
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val LOG_CATEGORY = "NavigationRouteUtils"

/**
 * This function checks whether the [NavigationRoute] has unexpected closures, which could be a reason to re-route.
 *
 * `true` is returned whenever there's any [RouteLeg.closures] outside of a direct region around a coordinate from [RouteOptions.coordinatesList]
 * that was allowed for snapping to closures with [RouteOptions.snappingIncludeClosuresList] or [RouteOptions.snappingIncludeStaticClosuresList].
 * flags. Otherwise, `false` is returned.
 */
@ExperimentalPreviewMapboxNavigationAPI
suspend fun NavigationRoute.hasUnexpectedClosures(): Boolean =
    withContext(Dispatchers.Default) {
        val snappingResultList = directionsRoute.getSnappingResultList()

        var currentLegFirstWaypointIndex = 0
        directionsRoute.legs()?.forEachIndexed { routeLegIndex, routeLeg ->

            val legLastGeometryIndex by lazy {
                directionsRoute.stepsGeometryToPoints(routeLeg).lastIndex
            }

            val silentWaypoints = routeLeg.silentWaypoints()

            routeLeg.closures()?.forEach { closure ->
                val silentWaypointsInClosureRange = silentWaypoints.inGeometryRange(
                    closure.geometryIndexStart(), closure.geometryIndexEnd()
                )
                if (silentWaypointsInClosureRange.isNotEmpty()) {
                    silentWaypointsInClosureRange.forEach {
                        // (index + 1) is the first silent waypoint index of the leg
                        val silentWaypointIndex = currentLegFirstWaypointIndex + 1 + it
                        val isSnapAllowed =
                            snappingResultList.getOrNull(silentWaypointIndex) ?: false
                        if (!isSnapAllowed) {
                            logD(
                                "Route with id [${this@hasUnexpectedClosures.id}] has closure " +
                                    "at leg index $routeLegIndex, that overlaps silent (via) " +
                                    "waypoint",
                                LOG_CATEGORY
                            )
                            return@withContext true
                        }
                    }
                }
                if (closure.geometryIndexStart() != 0 &&
                    closure.geometryIndexEnd() != legLastGeometryIndex &&
                    silentWaypointsInClosureRange.isEmpty()
                ) {
                    logD(
                        "Route with id [${this@hasUnexpectedClosures.id}] has closure at leg " +
                            "index $routeLegIndex",
                    )
                    return@withContext true
                }
                if (closure.geometryIndexStart() == 0 &&
                    snappingResultList.getOrNull(currentLegFirstWaypointIndex) != true
                ) {
                    logD(
                        "Route with id [${this@hasUnexpectedClosures.id}] has closure at the " +
                            "start of the leg, leg index $routeLegIndex",
                        LOG_CATEGORY
                    )
                    return@withContext true
                }
                if (closure.geometryIndexEnd() == legLastGeometryIndex &&
                    snappingResultList.getOrNull(
                            currentLegFirstWaypointIndex + silentWaypoints.size + 1
                        ) != true
                ) {
                    logD(
                        "Route with id [${this@hasUnexpectedClosures.id}] has closure at " +
                            "the end of the leg, leg index $routeLegIndex",
                        LOG_CATEGORY
                    )
                    return@withContext true
                }
            }
            currentLegFirstWaypointIndex += silentWaypoints.size
            currentLegFirstWaypointIndex++
        }

        return@withContext false
    }

private fun DirectionsRoute.getSnappingResultList(): List<Boolean> {
    val snappingIncludeClosuresList = routeOptions()?.snappingIncludeClosuresList()
    val snappingIncludeStaticClosuresList = routeOptions()?.snappingIncludeStaticClosuresList()

    val snappingResultList = mutableListOf<Boolean>()
    for (
        index in 0 until maxOf(
            snappingIncludeClosuresList?.size ?: 0,
            snappingIncludeStaticClosuresList?.size ?: 0
        )
    ) {
        snappingResultList.add(
            snappingIncludeClosuresList?.getOrNull(index) ?: false ||
                snappingIncludeStaticClosuresList?.getOrNull(index) ?: false
        )
    }
    return snappingResultList
}

/**
 * return the list of indexes of List<LegSilentWaypoints> which are at closure area
 */
private fun List<LegSilentWaypoints>.inGeometryRange(
    geometryIndexStart: Int,
    geometryIndexEnd: Int
): List<Int> {
    return mapIndexedNotNull { index, legSilentWaypoints ->
        if (legSilentWaypoints.geometryIndex in geometryIndexStart..geometryIndexEnd) {
            index
        } else {
            null
        }
    }
}

private fun DirectionsRoute.stepsGeometryToPoints(
    leg: RouteLeg,
): List<Point> {
    val points = mutableListOf<Point>()

    leg.steps()?.forEachIndexed { stepIndex, legStep ->
        stepGeometryToPoints(legStep).let {
            // step with 2 coordinate might have duplicated values
            val squashed = if (it.size == 2) {
                it.toSet()
            } else {
                it
            }
            if (stepIndex != 0) {
                // removing duplicate points for adjacent steps
                squashed.drop(1)
            } else {
                squashed
            }
        }.let {
            points.addAll(it)
        }
    }
    return points
}

private fun RouteLeg.silentWaypoints(): List<LegSilentWaypoints> =
    viaWaypoints()?.map {
        LegSilentWaypoints(
            it.geometryIndex()
        )
    } ?: emptyList()

private class LegSilentWaypoints(
    val geometryIndex: Int,
)
