package com.mapbox.navigation.core.routeoptions

import android.location.Location
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.base.common.logger.Logger
import com.mapbox.base.common.logger.model.Message
import com.mapbox.base.common.logger.model.Tag
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.trip.model.RouteProgress

/**
 * Default implementation of [RouteOptionsProvider]
 */
internal class MapboxRouteOptionsProvider(
    private val logger: Logger
) : RouteOptionsProvider {

    private companion object {
        const val DEFAULT_REROUTE_BEARING_TOLERANCE = 90.0
    }

    /**
     * Provides a new instance of *RouteOptions* based on initial *RouteOptions*, *RouteProgress* and
     * current *Location*
     *
     * Returns *null* if a new [RouteOptions] instance cannot be combined based on the input given.
     */
    override fun update(
        routeOptions: RouteOptions?,
        routeProgress: RouteProgress?,
        location: Location?
    ): RouteOptionsProvider.RouteOptionsResult {
        if (routeOptions == null || routeProgress == null || location == null) {
            val msg = "Cannot combine RouteOptions, invalid inputs. routeOptions, " +
                "routeProgress, and location mustn't be null"
            logger.w(
                Tag("MapboxRouteOptionsProvider"),
                Message(msg)
            )
            return RouteOptionsProvider.RouteOptionsResult.Error(Throwable(msg))
        }

        val optionsBuilder = routeOptions.toBuilder()
        val coordinates = routeOptions.coordinates()

        routeProgress.currentLegProgress?.legIndex?.let { index ->
            optionsBuilder
                .coordinates(
                    coordinates.drop(index + 1).toMutableList().apply {
                        add(0, Point.fromLngLat(location.longitude, location.latitude))
                    }
                )
                .bearingsList(let {
                    val bearings = mutableListOf<List<Double>?>()

                    val originTolerance = routeOptions.bearingsList()?.getOrNull(0)?.getOrNull(1)
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
                })
                .radiusesList(let radiusesList@{
                    if (routeOptions.radiusesList().isNullOrEmpty()) {
                        return@radiusesList emptyList<Double>()
                    }
                    mutableListOf<Double>().also {
                        it.addAll(routeOptions.radiusesList()!!.subList(index, coordinates.size))
                    }
                })
                .approachesList(let approachesList@{
                    if (routeOptions.approachesList().isNullOrEmpty()) {
                        return@approachesList emptyList<String>()
                    }
                    mutableListOf<String>().also {
                        it.addAll(routeOptions.approachesList()!!.subList(index, coordinates.size))
                    }
                })
                .waypointIndicesList(let waypointIndicesList@{
                    if (routeOptions.waypointIndicesList().isNullOrEmpty()) {
                        return@waypointIndicesList emptyList<Int>()
                    }
                    mutableListOf<Int>().also {
                        it.addAll(
                            routeOptions.waypointIndicesList()!!.subList(
                                index,
                                coordinates.size
                            )
                        )
                    }
                })
                .waypointNamesList(let waypointNamesList@{
                    if (routeOptions.waypointNamesList().isNullOrEmpty()) {
                        return@waypointNamesList emptyList<String>()
                    }
                    mutableListOf<String>().also {
                        it.add("")
                        it.addAll(
                            routeOptions.waypointNamesList()!!.subList(
                                index + 1,
                                coordinates.size
                            )
                        )
                    }
                })
                .waypointTargetsList(let waypointTargetsList@{
                    if (routeOptions.waypointTargetsList().isNullOrEmpty()) {
                        return@waypointTargetsList emptyList<Point>()
                    }
                    mutableListOf<Point?>().also {
                        it.add(null)
                        it.addAll(
                            routeOptions.waypointTargetsList()!!.subList(
                                index + 1,
                                coordinates.size
                            )
                        )
                    }
                })
        }

        return RouteOptionsProvider.RouteOptionsResult.Success(optionsBuilder.build())
    }
}
