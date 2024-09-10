package com.mapbox.navigation.core.routeoptions

import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.Bearing
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.internal.extensions.indexOfNextRequestedCoordinate
import com.mapbox.navigation.base.internal.utils.internalWaypoints
import com.mapbox.navigation.base.options.NavigateToFinalDestination
import com.mapbox.navigation.base.options.RerouteDisabled
import com.mapbox.navigation.base.options.RerouteStrategyForMapMatchedRoutes
import com.mapbox.navigation.base.route.ResponseOriginAPI
import com.mapbox.navigation.base.route.ResponseOriginAPI.Companion.DIRECTIONS_API
import com.mapbox.navigation.base.route.ResponseOriginAPI.Companion.MAP_MATCHING_API
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.reroute.MapboxRerouteController
import com.mapbox.navigation.core.reroute.PreRouterFailure
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.core.trip.session.OffRouteObserver
import com.mapbox.navigation.utils.internal.logE

private const val DEFAULT_REROUTE_BEARING_TOLERANCE = 90.0
private const val LOG_CATEGORY = "RouteOptionsUpdater"

/**
 * The updater can be used to create a new set of route request options to the existing destination, accounting for the progress along the route.
 *
 * It's used by the default [MapboxRerouteController] (see [OffRouteObserver] and [MapboxNavigation.setRerouteEnabled]).
 */
@OptIn(ExperimentalMapboxNavigationAPI::class)
internal class RouteOptionsUpdater {

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
        @ResponseOriginAPI responseOriginAPI: String = DIRECTIONS_API,
        rerouteStrategyForMapMatchedRoutes: RerouteStrategyForMapMatchedRoutes = RerouteDisabled,
    ): RouteOptionsResult {
        if (routeOptions == null) {
            val msg = "Cannot reroute as there is no active route available."
            logE(msg, LOG_CATEGORY)
            return RouteOptionsResult.Error(Throwable(msg), reason = PreRouterFailure(msg, false))
        }

        if (routeProgress == null || locationMatcherResult == null) {
            val msg = "Cannot combine RouteOptions, " +
                "routeProgress and locationMatcherResult cannot be null."
            logE(msg, LOG_CATEGORY)
            return RouteOptionsResult.Error(Throwable(msg), reason = PreRouterFailure(msg, true))
        }

        val coordinatesData = getCoordinatesData(
            routeProgress,
            routeOptions,
            rerouteStrategyForMapMatchedRoutes,
            responseOriginAPI,
        )

        val (coordinatesList, nextCoordinateIndex, remainingCoordinates) = coordinatesData.value
            ?: return RouteOptionsResult.Error(Throwable(coordinatesData.error))

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
                        },
                )
                .bearingsList(
                    getUpdatedBearingList(
                        remainingCoordinates,
                        nextCoordinateIndex,
                        location.bearing,
                        routeOptions.bearingsList(),
                    ),
                )
                .radiusesList(
                    let radiusesList@{
                        val radiusesList = routeOptions.radiusesList()
                        if (radiusesList.isNullOrEmpty()) {
                            return@radiusesList emptyList<Double?>()
                        }
                        radiusesList.subList(
                            nextCoordinateIndex - 1,
                            coordinatesList.size,
                        )
                    },
                )
                .approachesList(
                    let approachesList@{
                        val approachesList = routeOptions.approachesList()
                        if (approachesList.isNullOrEmpty()) {
                            return@approachesList emptyList<String>()
                        }
                        mutableListOf<String?>() +
                            null +
                            approachesList.takeLast(remainingCoordinates - 1)
                    },
                )
                .apply {
                    if (routeOptions.profile() == DirectionsCriteria.PROFILE_DRIVING_TRAFFIC) {
                        snappingIncludeClosuresList(
                            routeOptions.snappingIncludeClosuresList()
                                .withFirstTrue(remainingCoordinates),
                        )
                        snappingIncludeStaticClosuresList(
                            routeOptions.snappingIncludeStaticClosuresList()
                                .withFirstTrue(remainingCoordinates),
                        )
                    }
                }
                .waypointNamesList(
                    getUpdatedWaypointsList(
                        routeOptions.waypointNamesList(),
                        routeOptions.waypointIndicesList(),
                        nextCoordinateIndex,
                    ),
                )
                .waypointTargetsList(
                    getUpdatedWaypointsList(
                        routeOptions.waypointTargetsList(),
                        routeOptions.waypointIndicesList(),
                        nextCoordinateIndex,
                    ),
                )
                .waypointIndicesList(
                    getUpdatedWaypointIndicesList(
                        routeOptions.waypointIndicesList(),
                        nextCoordinateIndex,
                        responseOriginAPI,
                    ),
                )
                .unrecognizedJsonProperties(
                    getUpdatedUnrecognizedJsonProperties(
                        routeOptions.unrecognizedJsonProperties,
                        nextCoordinateIndex,
                        responseOriginAPI,
                    ),
                )

            if (
                routeOptions.profile() == DirectionsCriteria.PROFILE_DRIVING ||
                routeOptions.profile() == DirectionsCriteria.PROFILE_DRIVING_TRAFFIC
            ) {
                optionsBuilder.layersList(
                    mutableListOf(locationMatcherResult.zLevel).apply {
                        val legacyLayerList = routeOptions.layersList()
                        if (legacyLayerList != null) {
                            addAll(
                                legacyLayerList.subList(nextCoordinateIndex, coordinatesList.size),
                            )
                        } else {
                            while (this.size < remainingCoordinates + 1) {
                                add(null)
                            }
                        }
                    },
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

    private fun getCoordinatesData(
        routeProgress: RouteProgress,
        routeOptions: RouteOptions,
        rerouteStrategyForMapMatchedRoutes: RerouteStrategyForMapMatchedRoutes,
        @ResponseOriginAPI responseOriginAPI: String,
    ): Expected<String, CoordinatesData> {
        fun createError(message: String): Expected<String, CoordinatesData> =
            ExpectedFactory.createError(message)

        return when (responseOriginAPI) {
            MAP_MATCHING_API -> {
                when (rerouteStrategyForMapMatchedRoutes) {
                    NavigateToFinalDestination -> {
                        val coordinatesList =
                            routeProgress.navigationRoute.waypoints?.map { it.location() }

                        if (coordinatesList == null) {
                            val msg = "NavigationRoute.waypoints are null."
                            logE(msg, LOG_CATEGORY)
                            createError(msg)
                        } else {
                            ExpectedFactory.createValue(
                                CoordinatesData(coordinatesList, coordinatesList.size - 1, 1),
                            )
                        }
                    }

                    RerouteDisabled -> {
                        val msg = "Reroute disabled for the current map matched route."
                        logE(msg, LOG_CATEGORY)
                        createError(msg)
                    }

                    else -> {
                        val msg = "Invalid rerouteStrategyForMapMatchedRoute = " +
                            "$rerouteStrategyForMapMatchedRoutes"
                        logE(msg, LOG_CATEGORY)
                        createError(msg)
                    }
                }
            }

            DIRECTIONS_API -> indexOfNextRequestedCoordinate(
                routeProgress.navigationRoute.internalWaypoints(),
                routeProgress.remainingWaypoints,
            ).let { nextCoordinateIndex ->
                val coordinatesList = routeOptions.coordinatesList()

                if (nextCoordinateIndex == null) {
                    val msg = "Index of next coordinate is not defined"
                    logE(msg, LOG_CATEGORY)
                    createError(msg)
                } else if (coordinatesList.lastIndex < nextCoordinateIndex) {
                    val msg = "Index of next coordinate is out of range of coordinates"
                    logE(msg, LOG_CATEGORY)
                    createError(msg)
                } else {
                    ExpectedFactory.createValue(
                        CoordinatesData(
                            coordinatesList,
                            nextCoordinateIndex,
                            coordinatesList.size - nextCoordinateIndex,
                        ),
                    )
                }
            }

            else -> {
                createError("Invalid responseOriginAPI = $responseOriginAPI")
            }
        }
    }

    private fun getUpdatedBearingList(
        remainingCoordinates: Int,
        nextCoordinateIndex: Int,
        currentAngle: Double?,
        legacyBearingList: List<Bearing?>?,
    ): MutableList<Bearing?> {
        return ArrayList<Bearing?>().also { newList ->
            val originTolerance = legacyBearingList?.getOrNull(0)
                ?.degrees()
                ?: DEFAULT_REROUTE_BEARING_TOLERANCE
            newList.add(
                currentAngle?.let { Bearing.builder().angle(it).degrees(originTolerance).build() },
            )

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
        nextCoordinateIndex: Int,
        @ResponseOriginAPI responseOriginAPI: String,
    ): List<Int> {
        if (waypointIndicesList.isNullOrEmpty()) {
            return listOf()
        }
        if (responseOriginAPI == MAP_MATCHING_API) {
            return listOf(0, 1)
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
        @ResponseOriginAPI responseOriginAPI: String,
    ): Map<String, JsonElement>? {
        if (responseOriginAPI == MAP_MATCHING_API) {
            return emptyMap()
        }
        if (originalUnrecognizedJsonProperties == null) {
            return null
        }
        val newUnrecognizedJsonProperties = originalUnrecognizedJsonProperties.toMutableMap()
        if (originalUnrecognizedJsonProperties.isEVRoute()) {
            listOf(
                "waypoints.charging_station_id",
                "waypoints.charging_station_power",
                "waypoints.charging_station_current_type",
            ).forEach {
                updateCoordinateRelatedListProperty(
                    newUnrecognizedJsonProperties,
                    originalUnrecognizedJsonProperties,
                    it,
                    nextCoordinateIndex,
                )
            }
        }
        return newUnrecognizedJsonProperties
    }

    private fun updateCoordinateRelatedListProperty(
        newMap: MutableMap<String, JsonElement>,
        oldMap: Map<String, JsonElement>,
        key: String,
        nextCoordinateIndex: Int,
    ) {
        if (key !in oldMap) {
            return
        }
        val oldValue = oldMap[key]!!.asString
        val elements = oldValue.split(";")
        val newValue = ";" + elements.drop(nextCoordinateIndex).joinToString(";")
        newMap[key] = JsonPrimitive(newValue)
    }

    private fun <T> getUpdatedWaypointsList(
        waypointsList: List<T?>?,
        waypointIndicesList: List<Int>?,
        nextCoordinateIndex: Int,
    ): List<T?> {
        if (waypointsList.isNullOrEmpty()) {
            return mutableListOf()
        }
        val explicitWaypointsIndicesList = if (waypointIndicesList.isNullOrEmpty()) {
            waypointsList.indices.toList()
        } else {
            waypointIndicesList
        }
        return mutableListOf<T?>().also { updatedWaypointsList ->
            val updatedStartWaypointsListIndex = getUpdatedStartWaypointsListIndex(
                explicitWaypointsIndicesList,
                nextCoordinateIndex,
            )
            updatedWaypointsList.add(null)
            updatedWaypointsList.addAll(
                waypointsList.subList(
                    updatedStartWaypointsListIndex + 1,
                    waypointsList.size,
                ),
            )
        }
    }

    private fun getUpdatedStartWaypointsListIndex(
        waypointIndicesList: List<Int>?,
        nextCoordinateIndex: Int,
    ): Int {
        var updatedStartWaypointIndicesIndex = 0
        waypointIndicesList?.forEachIndexed { indx, waypointIndex ->
            if (waypointIndex < nextCoordinateIndex) {
                updatedStartWaypointIndicesIndex = indx
            }
        }
        return updatedStartWaypointIndicesIndex
    }

    private fun List<Boolean>?.withFirstTrue(
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
        class Success(val routeOptions: RouteOptions) : RouteOptionsResult() {

            /**
             * Indicates whether some other object is "equal to" this one.
             */
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false

                other as Success

                return routeOptions == other.routeOptions
            }

            /**
             * Returns a hash code value for the object.
             */
            override fun hashCode(): Int {
                return routeOptions.hashCode()
            }

            /**
             * Returns a string representation of the object.
             */
            override fun toString(): String {
                return "Success(routeOptions=$routeOptions)"
            }
        }

        /**
         * Failed operation.
         *
         * @param error reason
         */
        class Error(
            val error: Throwable,
            val reason: PreRouterFailure? = null,
        ) : RouteOptionsResult() {

            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false

                other as Error

                if (error != other.error) return false
                if (reason != other.reason) return false

                return true
            }

            override fun hashCode(): Int {
                var result = error.hashCode()
                result = 31 * result + (reason?.hashCode() ?: 0)
                return result
            }

            override fun toString(): String {
                return "Error(error=$error, reason=$reason)"
            }
        }
    }

    private data class CoordinatesData(
        val coordinates: List<Point>,
        val nextCoordinateIndex: Int,
        val remainingCoordinates: Int,
    )
}
