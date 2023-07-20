package com.mapbox.navigation.core.routeoptions

import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.Bearing
import com.mapbox.api.directions.v5.models.DirectionsWaypoint
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.internal.extensions.indexOfNextWaypoint
import com.mapbox.navigation.base.internal.route.getChargingStationCurrentType
import com.mapbox.navigation.base.internal.route.getChargingStationId
import com.mapbox.navigation.base.internal.route.getChargingStationPowerKw
import com.mapbox.navigation.base.internal.route.getWaypointMetadataOrEmpty
import com.mapbox.navigation.base.internal.utils.internalWaypoints
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.reroute.MapboxRerouteController
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.core.trip.session.OffRouteObserver
import com.mapbox.navigation.utils.internal.logE

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
     * - `snapping_include_closures=true` and `snapping_include_static_closures=true` are set for the origin of the request to aid with potential need to navigate out of a closed section of the road
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

        val routeOptions = if (routeOptions.isEVRoute() && routeOptions.waypointIndices() != null) {
            if (!allCoordinatesAreWaypoints(routeOptions)) {
                return RouteOptionsResult.Error(
                    Throwable("Silent waypoints aren't supported in EV routing")
                )
            } else {
                // getting rid of waypoints indices to simplify the rest of the logic
                routeOptions.toBuilder().waypointIndicesList(null).build()
            }
        } else {
            routeOptions
        }

        val currentNavigationRoute = routeProgress.navigationRoute
        val serverAddedChargingStationWithIndex =
            currentNavigationRoute.waypoints?.mapIndexedNotNull { index, waypoint ->
                val metadata = waypoint.getWaypointMetadataOrEmpty()
                if (metadata.isServerProvided() && !metadata.wasRequestedAsUserProvided()) {
                    Pair(index, waypoint)
                } else null
            } ?: emptyList()
        val coordinatesList = routeOptions.coordinatesList().toMutableList().apply {
            serverAddedChargingStationWithIndex.forEach { (indexOfWaypoint, waypoint) ->
                this.add(indexOfWaypoint, waypoint.location())
            }
        }

        val (nextCoordinateIndex, remainingCoordinates) = indexOfNextWaypoint(
            routeProgress.navigationRoute.internalWaypoints(),
            routeProgress.remainingWaypoints,
        ).let {
            if (it == null) {
                val msg = "Index of next coordinate is not defined"
                logE(msg, LOG_CATEGORY)
                return RouteOptionsResult.Error(Throwable(msg))
            } else if (coordinatesList.lastIndex < it) {
                val msg = "Index of next coordinate is out of range of coordinates"
                logE(msg, LOG_CATEGORY)
                return RouteOptionsResult.Error(Throwable(msg))
            } else {
                return@let it to coordinatesList.size - it
            }
        }

        val optionsBuilder = routeOptions.toBuilder()

        try {
            val location = locationMatcherResult.enhancedLocation
            optionsBuilder
                .coordinatesList(
                    coordinatesList
                        .subList(nextCoordinateIndex, coordinatesList.size)
                        .toMutableList()
                        .apply {
                            add(0, Point.fromLngLat(location.longitude, location.latitude))
                        }
                )
                .bearingsList(
                    getUpdatedBearingList(
                        remainingCoordinates,
                        nextCoordinateIndex,
                        location.bearing.toDouble(),
                        routeOptions.bearingsList()?.updateWithValueForIndexes(
                            serverAddedChargingStationWithIndex,
                            null
                        )
                    )
                )
                .radiusesList(
                    let radiusesList@{
                        val radiusesList = routeOptions.radiusesList()
                            ?.updateWithValueForIndexes(
                                serverAddedChargingStationWithIndex,
                                null
                            )
                        if (radiusesList.isNullOrEmpty()) {
                            return@radiusesList emptyList<Double?>()
                        }
                        radiusesList.subList(
                            nextCoordinateIndex - 1,
                            coordinatesList.size
                        )
                    }
                )
                .approachesList(
                    let approachesList@{
                        val approachesList = routeOptions.approachesList()
                            ?.updateWithValueForIndexes(serverAddedChargingStationWithIndex, null)
                        if (approachesList.isNullOrEmpty()) {
                            return@approachesList emptyList<String>()
                        }
                        mutableListOf<String?>() +
                            null +
                            approachesList.takeLast(remainingCoordinates)
                    }
                )
                .apply {
                    if (routeOptions.profile() == DirectionsCriteria.PROFILE_DRIVING_TRAFFIC) {
                        snappingIncludeClosuresList(
                            routeOptions.snappingIncludeClosuresList()
                                ?.toMutableList()
                                ?.updateWithValueForIndexes(
                                    serverAddedChargingStationWithIndex,
                                    null
                                )
                                .withFirstTrue(remainingCoordinates)
                        )
                        snappingIncludeStaticClosuresList(
                            routeOptions.snappingIncludeStaticClosuresList()
                                ?.updateWithValueForIndexes(
                                    serverAddedChargingStationWithIndex,
                                    null
                                )
                                .withFirstTrue(remainingCoordinates)
                        )
                    }
                }
                .waypointNamesList(
                    getUpdatedWaypointsList(
                        routeOptions.waypointNamesList()
                            ?.toMutableList()
                            ?.updateWithValueForIndexes(serverAddedChargingStationWithIndex, null),
                        routeOptions.waypointIndicesList(),
                        nextCoordinateIndex
                    )
                )
                .waypointTargetsList(
                    getUpdatedWaypointsList(
                        routeOptions.waypointTargetsList()
                            ?.toMutableList()
                            ?.updateWithValueForIndexes(serverAddedChargingStationWithIndex, null),
                        routeOptions.waypointIndicesList(),
                        nextCoordinateIndex
                    )
                )
                .waypointIndicesList(
                    getUpdatedWaypointIndicesList(
                        routeOptions.waypointIndicesList(),
                        nextCoordinateIndex
                    )
                )
                .unrecognizedJsonProperties(
                    getUpdatedUnrecognizedJsonProperties(
                        routeOptions.unrecognizedJsonProperties,
                        nextCoordinateIndex,
                        serverAddedChargingStationWithIndex,
                        coordinatesList.size
                    )
                )

            if (
                routeOptions.profile() == DirectionsCriteria.PROFILE_DRIVING ||
                routeOptions.profile() == DirectionsCriteria.PROFILE_DRIVING_TRAFFIC
            ) {
                optionsBuilder.layersList(
                    mutableListOf(locationMatcherResult.zLevel).apply {
                        val legacyLayerList = routeOptions.layersList()
                            ?.toMutableList()
                            ?.updateWithValueForIndexes(serverAddedChargingStationWithIndex, null)
                        if (legacyLayerList != null) {
                            addAll(
                                legacyLayerList.subList(nextCoordinateIndex, coordinatesList.size)
                            )
                        } else {
                            while (this.size < remainingCoordinates + 1) {
                                add(null)
                            }
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

    private fun allCoordinatesAreWaypoints(routeOptions: RouteOptions) =
        routeOptions.waypointIndicesList() == (0 until routeOptions.coordinatesList()
            .count()).toList()

    private fun <T> List<T>.updateWithValueForIndexes(
        serverAddedChargingStationWithIndex: List<Pair<Int, DirectionsWaypoint>>,
        value: T
    ): List<T> {
        val result = this.toMutableList()
        serverAddedChargingStationWithIndex.forEach { (indexOfWaypoint, _) ->
            result.add(indexOfWaypoint, value)
        }
        return result
    }

    private fun getUpdatedBearingList(
        remainingCoordinates: Int,
        nextCoordinateIndex: Int,
        currentAngle: Double,
        legacyBearingList: List<Bearing?>?,
    ): MutableList<Bearing?> {
        return ArrayList<Bearing?>().also { newList ->
            val originTolerance = legacyBearingList?.getOrNull(0)
                ?.degrees()
                ?: DEFAULT_REROUTE_BEARING_TOLERANCE
            newList.add(Bearing.builder().angle(currentAngle).degrees(originTolerance).build())

            if (legacyBearingList != null) {
                for (idx in nextCoordinateIndex..legacyBearingList.lastIndex) {
                    newList.add(legacyBearingList[idx])
                }
            }

            while (newList.size < remainingCoordinates + 1) {
                newList.add(null)
            }
        }
    }

    private fun getUpdatedWaypointIndicesList(
        waypointIndicesList: List<Int>?,
        nextCoordinateIndex: Int
    ): MutableList<Int> {
        if (waypointIndicesList.isNullOrEmpty()) {
            return mutableListOf()
        }
        return mutableListOf<Int>().apply {
            add(0)
            waypointIndicesList.forEach { value ->
                val newVal = value - nextCoordinateIndex + 1
                if (newVal > 0) {
                    add(newVal)
                }
            }
        }
    }

    private fun getUpdatedUnrecognizedJsonProperties(
        originalUnrecognizedJsonProperties: Map<String, JsonElement>?,
        nextCoordinateIndex: Int,
        serverAddedChargingStationWithIndex: List<Pair<Int, DirectionsWaypoint>>,
        waypointsCount: Int
    ): Map<String, JsonElement>? {
        if (originalUnrecognizedJsonProperties == null) {
            return null
        }
        if (originalUnrecognizedJsonProperties.isEmpty()) {
            return emptyMap()
        }
        val newUnrecognizedJsonProperties = originalUnrecognizedJsonProperties.toMutableMap()
        if (originalUnrecognizedJsonProperties.isEVRoute()) {
            listOf(
                "waypoints.charging_station_id" to serverAddedChargingStationWithIndex
                    .map { (index, waypoint) ->
                        Pair(
                            index,
                            waypoint.getChargingStationId() ?: ""
                        )
                    },
                "waypoints.charging_station_power" to serverAddedChargingStationWithIndex
                    .map { (index, waypoint) ->
                        Pair(
                            index,
                            waypoint.getChargingStationPowerKw()?.let { (it * 1000).toString() }
                                ?: "")
                    },
                "waypoints.charging_station_current_type" to serverAddedChargingStationWithIndex
                    .map { (index, waypoint) ->
                        Pair(
                            index,
                            waypoint.getChargingStationCurrentType() ?: ""
                        )
                    },
            ).forEach { (param, serverAddedValues) ->
                updateCoordinateRelatedListProperty(
                    newUnrecognizedJsonProperties,
                    originalUnrecognizedJsonProperties,
                    param,
                    nextCoordinateIndex,
                    serverAddedValues,
                    waypointsCount
                )
            }
            newUnrecognizedJsonProperties["ev_add_charging_stops"] = JsonPrimitive(false)
        }
        return newUnrecognizedJsonProperties
    }

    private fun updateCoordinateRelatedListProperty(
        newMap: MutableMap<String, JsonElement>,
        oldMap: Map<String, JsonElement>,
        key: String,
        nextCoordinateIndex: Int,
        serverAddedWaypointParametersWithIndex: List<Pair<Int, String>>,
        waypointsCount: Int
    ) {
        if (key !in oldMap && serverAddedWaypointParametersWithIndex.isEmpty()) {
            return
        }
        val elements: List<String> = oldMap[key]?.asString?.let {
            it.split(";").toMutableList().apply {
                serverAddedWaypointParametersWithIndex.forEach { (index, param) ->
                    this.add(index, param)
                }
            }
        } ?: MutableList(waypointsCount) { "" }.apply {
            serverAddedWaypointParametersWithIndex.forEach { (index, param) ->
                this[index] = param
            }
        }
        val remainingElements = elements.drop(nextCoordinateIndex)
        if (remainingElements.all { it.isEmpty() }) {
            return
        }
        val newValue = ";" + remainingElements.joinToString(";")
        newMap[key] = JsonPrimitive(newValue)
    }

    private fun <T> getUpdatedWaypointsList(
        waypointsList: List<T>?,
        waypointIndicesList: List<Int>?,
        nextCoordinateIndex: Int
    ): MutableList<T> {
        if (waypointsList.isNullOrEmpty()) {
            return mutableListOf()
        }
        return mutableListOf<T>().also { updatedWaypointsList ->
            val updatedStartWaypointsListIndex = getUpdatedStartWaypointsListIndex(
                waypointIndicesList,
                nextCoordinateIndex
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
        nextCoordinateIndex: Int
    ): Int {
        if (waypointIndicesList == null) {
            return nextCoordinateIndex - 1
        }
        var updatedStartWaypointIndicesIndex = 0
        waypointIndicesList.forEachIndexed { indx, waypointIndex ->
            if (waypointIndex < nextCoordinateIndex) {
                updatedStartWaypointIndicesIndex = indx
            }
        }
        return updatedStartWaypointIndicesIndex
    }

    private fun List<Boolean?>?.withFirstTrue(
        remainingCoordinates: Int,
    ): List<Boolean?> {
        return mutableListOf<Boolean?>().also { newList ->
            // append true for the origin of the re-route request
            newList.add(true)
            if (isNullOrEmpty()) {
                // create `null` value for each upcoming waypoint
                newList.addAll(arrayOfNulls<Boolean>(remainingCoordinates))
            } else {
                // get existing values for each upcoming waypoint
                newList.addAll(takeLast(remainingCoordinates))
            }
        }
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
