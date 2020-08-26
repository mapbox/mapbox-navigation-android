package com.mapbox.navigation.core.routeoptions

import android.location.Location
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.base.common.logger.Logger
import com.mapbox.base.common.logger.model.Message
import com.mapbox.base.common.logger.model.Tag
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.trip.model.RouteProgress

private const val DEFAULT_REROUTE_BEARING_TOLERANCE = 90.0

/**
 * Default implementation of [RouteOptionsUpdater].
 */
class MapboxRouteOptionsUpdater(
    private val logger: Logger? = null
) : RouteOptionsUpdater {

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
            logger?.w(
                Tag("MapboxRouteOptionsProvider"),
                Message(msg)
            )
            return RouteOptionsUpdater.RouteOptionsResult.Error(Throwable(msg))
        }

        val optionsBuilder = routeOptions.toBuilder()
        val coordinates = routeOptions.coordinates()
        val remainingWaypoints = routeProgress.remainingWaypoints

        routeProgress.currentLegProgress?.legIndex?.let { index ->
            optionsBuilder
                .coordinates(
                    coordinates
                        .drop(coordinates.size - remainingWaypoints).toMutableList().apply {
                            add(0, Point.fromLngLat(location.longitude, location.latitude))
                        }
                )
                .bearingsList(
                    let {
                        val bearings = mutableListOf<List<Double>?>()

                        val originTolerance =
                            routeOptions.bearingsList()?.getOrNull(0)?.getOrNull(1)
                                ?: DEFAULT_REROUTE_BEARING_TOLERANCE
                        val currentAngle = location.bearing.toDouble()

                        bearings.add(listOf(currentAngle, originTolerance))
                        val originalBearings = routeOptions.bearingsList()
                        if (originalBearings != null) {
                            bearings.addAll(originalBearings.subList(index + 1, coordinates.size))
                        } else {
                            while (bearings.size < coordinates.size) {
                                bearings.add(null)
                            }
                        }
                        bearings
                    }
                )
                .radiusesList(
                    let radiusesList@{
                        val radiusesList = routeOptions.radiusesList()
                        if (radiusesList.isNullOrEmpty()) {
                            return@radiusesList emptyList<Double>()
                        }
                        mutableListOf<Double>().also {
                            it.addAll(radiusesList.subList(index, coordinates.size))
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
                            it.addAll(approachesList.subList(index, coordinates.size))
                        }
                    }
                )
                .waypointNamesList(
                    getUpdatedWaypointsList(
                        routeOptions.waypointNamesList(),
                        routeOptions.waypointIndicesList(),
                        coordinates.size - remainingWaypoints - 1
                    )
                )
                .waypointTargetsList(
                    getUpdatedWaypointsList(
                        routeOptions.waypointTargetsList(),
                        routeOptions.waypointIndicesList(),
                        coordinates.size - remainingWaypoints - 1
                    )
                )
                .waypointIndicesList(
                    getUpdatedWaypointIndicesList(
                        routeOptions.waypointIndicesList(),
                        coordinates.size - remainingWaypoints - 1
                    )
                )
        }

        return RouteOptionsUpdater.RouteOptionsResult.Success(optionsBuilder.build())
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
