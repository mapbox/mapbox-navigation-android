@file:JvmName("NavigationRouteUtils")

package com.mapbox.navigation.base.utils.route

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.utils.DecodeUtils.stepGeometryToPoints
import com.mapbox.navigation.utils.internal.ifNonNull
import com.mapbox.navigation.utils.internal.logD
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val LOG_CATEGORY = "NavigationRouteUtils"

/**
 * This function checks whether the [NavigationRoute] has unexpected upcoming closures, which could be a reason to re-route.
 *
 * The algorithm does not take to account current closures, where the puck is on.
 *
 * `true` is returned whenever there's any upcoming [RouteLeg.closures] outside of a direct region around a coordinate from [RouteOptions.coordinatesList]
 * that was allowed for snapping to closures with [RouteOptions.snappingIncludeClosuresList] or [RouteOptions.snappingIncludeStaticClosuresList].
 * flags and the closure was not present in the original route response (in that case closure is considered expected).
 * Otherwise, `false` is returned.
 */
@ExperimentalPreviewMapboxNavigationAPI
suspend fun RouteProgress.hasUnexpectedUpcomingClosures(): Boolean =
    withContext(Dispatchers.Default) {
        val snappingResultList = navigationRoute.directionsRoute.getSnappingResultList()

        val routeProgressData = ifNonNull(
            currentLegProgress,
        ) { legProgress ->
            RouteProgressData(legProgress.legIndex, legProgress.geometryIndex)
        }

        var currentLegFirstWaypointIndex = 0
        navigationRoute.directionsRoute.legs()?.forEachIndexed { routeLegIndex, routeLeg ->

            val legLastGeometryIndex by lazy {
                navigationRoute.directionsRoute.stepsGeometryToPoints(routeLeg).lastIndex
            }

            val silentWaypoints = routeLeg.silentWaypoints()
            val unavoidableLegClosures = navigationRoute.unavoidableClosures
                .getOrNull(routeLegIndex).orEmpty()

            routeLeg.closures()?.forEach { closure ->
                if (closure in unavoidableLegClosures) {
                    // skipping expected closures
                    return@forEach
                }
                if (routeProgressData != null) {
                    if (routeProgressData.currentLegIndex > routeLegIndex) {
                        // skipping passed legs
                        return@forEach
                    } else if (
                        routeProgressData.currentLegIndex == routeLegIndex &&
                        routeProgressData.currentGeometryLegIndex >= closure.geometryIndexStart()
                    ) {
                        // skipping current and passed closures on the current leg
                        return@forEach
                    }
                }
                val silentWaypointsInClosureRange = silentWaypoints.inGeometryRange(
                    closure.geometryIndexStart(),
                    closure.geometryIndexEnd()
                )
                if (silentWaypointsInClosureRange.isNotEmpty()) {
                    silentWaypointsInClosureRange.forEach {
                        // (index + 1) is the first silent waypoint index of the leg
                        val silentWaypointIndex = currentLegFirstWaypointIndex + 1 + it
                        val isSnapAllowed =
                            snappingResultList.getOrNull(silentWaypointIndex) ?: false
                        if (!isSnapAllowed) {
                            logD(
                                "Route with id " +
                                    "[${this@hasUnexpectedUpcomingClosures.navigationRoute.id}] " +
                                    "has closure at leg index $routeLegIndex, that overlaps " +
                                    "silent (via) waypoint",
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
                        "Route with id " +
                            "[${this@hasUnexpectedUpcomingClosures.navigationRoute.id}] has " +
                            "closure at leg index $routeLegIndex",
                    )
                    return@withContext true
                }
                if (closure.geometryIndexStart() == 0 &&
                    snappingResultList.getOrNull(currentLegFirstWaypointIndex) != true
                ) {
                    logD(
                        "Route with id " +
                            "[${this@hasUnexpectedUpcomingClosures.navigationRoute.id}] has " +
                            "closure at the start of the leg, leg index $routeLegIndex",
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
                        "Route with id " +
                            "[${this@hasUnexpectedUpcomingClosures.navigationRoute.id}] has " +
                            "closure at the end of the leg, leg index $routeLegIndex",
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

private class RouteProgressData(
    val currentLegIndex: Int,
    val currentGeometryLegIndex: Int,
)

private fun DirectionsRoute.getSnappingResultList(): List<Boolean> {
    val snappingIncludeClosuresList = routeOptions()?.snappingIncludeClosuresList()
    val snappingIncludeStaticClosuresList = routeOptions()?.snappingIncludeStaticClosuresList()

    val snappingResultList = mutableListOf<Boolean>()
    val maxIndex = maxOf(
        snappingIncludeClosuresList?.size ?: 0,
        snappingIncludeStaticClosuresList?.size ?: 0
    )
    for (index in 0 until maxIndex) {
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
