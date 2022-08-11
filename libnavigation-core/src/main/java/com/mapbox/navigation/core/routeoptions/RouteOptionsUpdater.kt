package com.mapbox.navigation.core.routeoptions

import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.Bearing
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.reroute.MapboxRerouteController
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.core.trip.session.OffRouteObserver
import com.mapbox.navigation.utils.internal.logE
import kotlin.math.min

private const val DEFAULT_REROUTE_BEARING_TOLERANCE = 90.0
private const val LOG_CATEGORY = "RouteOptionsUpdater"

/**
 * The updater can be used to create a new set of route request options to the existing destination, accounting for the progress along the route.
 *
 * It's used by the default [MapboxRerouteController] (see [OffRouteObserver] and [MapboxNavigation.setRerouteController]).
 */
class RouteOptionsUpdater {

    /**
     * Provides a new [RouteOptions] instance based on the original request options, the current route progress and location matcher result.
     *
     * This carries over or adapts all of the request parameters to best fit the existing situation and remaining portion of the route.
     *
     * Notable adjustments:
     * - `snapping_include_closures=true` is set for the origin of the request to aid with potential need to navigate out of a closed section of the road
     * - `depart_at`/`arrive_by` parameters are cleared as they are not applicable in update/re-route scenario
     *
     * @return `RouteOptionsResult.Error` if a new [RouteOptions] instance cannot be combined based on the input given.
     * `RouteOptionsResult.Success` with a new [RouteOptions] instance if successfully combined.
     */
    fun update(
        routeOptions: RouteOptions?,
        routeProgress: RouteProgress?,
        locationMatcherResult: LocationMatcherResult?,
    ): RouteOptionsResult {
        if (routeOptions == null || routeProgress == null || locationMatcherResult == null) {
            val msg = "Cannot combine RouteOptions, invalid inputs. routeOptions, " +
                "routeProgress and locationMatcherResult cannot be null"
            logE(msg, LOG_CATEGORY)
            return RouteOptionsResult.Error(Throwable(msg))
        }

        val optionsBuilder = routeOptions.toBuilder()
        val coordinatesList = routeOptions.coordinatesList()
        val remainingWaypoints = routeProgress.remainingWaypoints

        if (remainingWaypoints == 0) {
            val msg = """
                Reroute failed. There are no remaining waypoints on the route.
                routeOptions=$routeOptions
                locationMatcherResult=$locationMatcherResult
                routeProgress=$routeProgress
            """.trimIndent()
            logE(msg, LOG_CATEGORY)
            return RouteOptionsResult.Error(Throwable(msg))
        }

        try {
            routeProgress.currentLegProgress?.legIndex?.let { index ->
                val location = locationMatcherResult.enhancedLocation
                optionsBuilder
                    .coordinatesList(
                        coordinatesList
                            .drop(coordinatesList.size - remainingWaypoints).toMutableList().apply {
                                add(0, Point.fromLngLat(location.longitude, location.latitude))
                            }
                    )
                    .bearingsList(
                        getUpdatedBearingList(
                            coordinatesList.size,
                            location.bearing.toDouble(),
                            routeOptions.bearingsList(),
                            remainingWaypoints
                        )
                    )
                    .radiusesList(
                        let radiusesList@{
                            val radiusesList = routeOptions.radiusesList()
                            if (radiusesList.isNullOrEmpty()) {
                                return@radiusesList emptyList<Double>()
                            }
                            mutableListOf<Double>().also {
                                it.addAll(radiusesList.subList(index, coordinatesList.size))
                            }
                        }
                    )
                    .approachesList(
                        let approachesList@{
                            val approachesList = routeOptions.approachesList()
                            if (approachesList.isNullOrEmpty()) {
                                return@approachesList emptyList<String>()
                            }
                            mutableListOf<String>().also {
                                it.addAll(approachesList.subList(index, coordinatesList.size))
                            }
                        }
                    ).apply {
                        if (routeOptions.profile() == DirectionsCriteria.PROFILE_DRIVING_TRAFFIC) {
                            snappingIncludeClosuresList(
                                let snappingClosures@{
                                    val snappingClosures =
                                        routeOptions.snappingIncludeClosuresList()
                                    mutableListOf<Boolean?>().apply {
                                        // append true for the origin of the re-route request
                                        add(true)
                                        if (snappingClosures.isNullOrEmpty()) {
                                            // create `null` value for each upcoming waypoint
                                            addAll(arrayOfNulls<Boolean>(remainingWaypoints))
                                        } else {
                                            // get existing values for each upcoming waypoint
                                            addAll(
                                                snappingClosures.subList(
                                                    index + 1,
                                                    coordinatesList.size
                                                )
                                            )
                                        }
                                    }
                                }
                            )
                        }
                    }
                    .waypointNamesList(
                        getUpdatedWaypointsList(
                            routeOptions.waypointNamesList(),
                            routeOptions.waypointIndicesList(),
                            coordinatesList.size - remainingWaypoints - 1
                        )
                    )
                    .waypointTargetsList(
                        getUpdatedWaypointsList(
                            routeOptions.waypointTargetsList(),
                            routeOptions.waypointIndicesList(),
                            coordinatesList.size - remainingWaypoints - 1
                        )
                    )
                    .waypointIndicesList(
                        getUpdatedWaypointIndicesList(
                            routeOptions.waypointIndicesList(),
                            coordinatesList.size - remainingWaypoints - 1
                        )
                    )
            }

            if (
                routeOptions.profile() == DirectionsCriteria.PROFILE_DRIVING ||
                routeOptions.profile() == DirectionsCriteria.PROFILE_DRIVING_TRAFFIC
            ) {
                optionsBuilder.layersList(
                    mutableListOf(locationMatcherResult.zLevel).apply {
                        val legacyLayerList = routeOptions.layersList()
                        if (legacyLayerList != null) {
                            addAll(legacyLayerList.takeLast(remainingWaypoints))
                        } else {
                            repeat(remainingWaypoints) { add(null) }
                        }
                    }
                )
            }

            optionsBuilder.arriveBy(null)
            optionsBuilder.departAt(null)
        } catch (e: Exception) {
            logE("routeOptions=[$routeOptions]", LOG_CATEGORY)
            logE("locationMatcherResult=[$locationMatcherResult]", LOG_CATEGORY)
            logE("routeProgress=[$routeProgress]", LOG_CATEGORY)
            throw e
        }

        return RouteOptionsResult.Success(optionsBuilder.build())
    }

    private fun getUpdatedBearingList(
        coordinates: Int,
        currentAngle: Double,
        legacyBearingList: List<Bearing?>?,
        remainingWaypoints: Int
    ): MutableList<Bearing?> {
        return ArrayList<Bearing?>().also { newList ->
            val originTolerance = legacyBearingList?.getOrNull(0)
                ?.degrees()
                ?: DEFAULT_REROUTE_BEARING_TOLERANCE
            newList.add(Bearing.builder().angle(currentAngle).degrees(originTolerance).build())

            if (legacyBearingList != null) {
                newList.addAll(
                    legacyBearingList.subList(
                        coordinates - remainingWaypoints,
                        min(legacyBearingList.size, coordinates)
                    )
                )
            }

            while (newList.size < remainingWaypoints + 1) {
                newList.add(null)
            }
        }
    }

    private fun getUpdatedWaypointIndicesList(
        waypointIndicesList: List<Int>?,
        lastPassedWaypointIndex: Int
    ): MutableList<Int> {
        if (waypointIndicesList.isNullOrEmpty()) {
            return mutableListOf()
        }
        return mutableListOf<Int>().also { updatedWaypointIndicesList ->
            val updatedStartWaypointIndicesIndex = getUpdatedStartWaypointsListIndex(
                waypointIndicesList,
                lastPassedWaypointIndex
            )
            updatedWaypointIndicesList.add(0)
            updatedWaypointIndicesList.addAll(
                waypointIndicesList.subList(
                    updatedStartWaypointIndicesIndex + 1,
                    waypointIndicesList.size
                ).map { it - lastPassedWaypointIndex }
            )
        }
    }

    private fun <T> getUpdatedWaypointsList(
        waypointsList: List<T>?,
        waypointIndicesList: List<Int>?,
        lastPassedWaypointIndex: Int
    ): MutableList<T> {
        if (waypointsList.isNullOrEmpty()) {
            return mutableListOf()
        }
        return mutableListOf<T>().also { updatedWaypointsList ->
            val updatedStartWaypointsListIndex = getUpdatedStartWaypointsListIndex(
                waypointIndicesList,
                lastPassedWaypointIndex
            )
            updatedWaypointsList.add(waypointsList[updatedStartWaypointsListIndex])
            updatedWaypointsList.addAll(
                waypointsList.subList(
                    updatedStartWaypointsListIndex + 1,
                    waypointsList.size
                )
            )
        }
    }

    private fun getUpdatedStartWaypointsListIndex(
        waypointIndicesList: List<Int>?,
        lastPassedWaypointIndex: Int
    ): Int {
        var updatedStartWaypointIndicesIndex = 0
        waypointIndicesList?.forEachIndexed { indx, waypointIndex ->
            if (waypointIndex <= lastPassedWaypointIndex) {
                updatedStartWaypointIndicesIndex = indx
            }
        }
        return updatedStartWaypointIndicesIndex
    }

    /**
     * Describes a result of generating new options from the original request and the current route progress.
     */
    sealed class RouteOptionsResult {
        /**
         * Successful operation.
         *
         * @param routeOptions the recreated route option from the current route progress
         */
        data class Success(val routeOptions: RouteOptions) : RouteOptionsResult()

        /**
         * Failed operation.
         *
         * @param error reason
         */
        data class Error(val error: Throwable) : RouteOptionsResult()
    }
}
