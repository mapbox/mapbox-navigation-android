package com.mapbox.navigation.core.routeoptions

import android.location.Location
import com.mapbox.api.directions.v5.models.Bearing
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.base.common.logger.model.Message
import com.mapbox.base.common.logger.model.Tag
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.utils.internal.LoggerProvider
import kotlin.math.min

private const val DEFAULT_REROUTE_BEARING_TOLERANCE = 90.0
private const val TAG = "MbxRouteOptionsProvider"

/**
 * Default implementation of [RouteOptionsUpdater].
 */
class MapboxRouteOptionsUpdater : RouteOptionsUpdater {

    /**
     * Provides a new [RouteOptions] instance based on the original request options and the current route progress.
     *
     * Returns *null* if a new [RouteOptions] instance cannot be combined based on the input given. When *null*
     * is returned new route is not fetched.
     */
    override fun update(
        routeOptions: RouteOptions?,
        routeProgress: RouteProgress?,
        location: Location?
    ): RouteOptionsUpdater.RouteOptionsResult {
        if (routeOptions == null || routeProgress == null || location == null) {
            val msg = "Cannot combine RouteOptions, invalid inputs. routeOptions, " +
                "routeProgress, and location mustn't be null"
            LoggerProvider.logger.e(
                Tag(TAG),
                Message(msg)
            )
            return RouteOptionsUpdater.RouteOptionsResult.Error(Throwable(msg))
        }

        val optionsBuilder = routeOptions.toBuilder()
        val coordinatesList = routeOptions.coordinatesList()
        val remainingWaypoints = routeProgress.remainingWaypoints

        if (remainingWaypoints == 0) {
            val msg = """
                Reroute failed. There are no remaining waypoints on the route.
                routeOptions=$routeOptions
                location=$location
                routeProgress=$routeProgress
            """.trimIndent()
            LoggerProvider.logger.e(
                Tag(TAG),
                Message(msg)
            )
            return RouteOptionsUpdater.RouteOptionsResult.Error(Throwable(msg))
        }

        try {
            routeProgress.currentLegProgress?.legIndex?.let { index ->
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
                    )
                    .snappingIncludeClosuresList(
                        let snappingClosures@{
                            val snappingClosures = routeOptions.snappingIncludeClosuresList()
                            if (snappingClosures.isNullOrEmpty()) {
                                return@snappingClosures emptyList<Boolean>()
                            }
                            mutableListOf<Boolean>().also {
                                it.addAll(snappingClosures.subList(index, coordinatesList.size))
                            }
                        }
                    )
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

            optionsBuilder.arriveBy(null)
            optionsBuilder.departAt(null)
        } catch (e: Exception) {
            LoggerProvider.logger.e(
                Tag(TAG),
                Message("routeOptions=[$routeOptions]")
            )
            LoggerProvider.logger.e(
                Tag(TAG),
                Message("location=[$location]")
            )
            LoggerProvider.logger.e(
                Tag(TAG),
                Message("routeProgress=[$routeProgress]")
            )
            throw e
        }

        return RouteOptionsUpdater.RouteOptionsResult.Success(optionsBuilder.build())
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
}
