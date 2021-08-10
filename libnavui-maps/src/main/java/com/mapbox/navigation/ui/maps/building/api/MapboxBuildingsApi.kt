package com.mapbox.navigation.ui.maps.building.api

import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.geojson.Point
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.QueriedFeature
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.ui.base.util.MapboxNavigationConsumer
import com.mapbox.navigation.ui.maps.building.BuildingAction
import com.mapbox.navigation.ui.maps.building.BuildingProcessor
import com.mapbox.navigation.ui.maps.building.model.BuildingError
import com.mapbox.navigation.ui.maps.building.model.BuildingValue
import com.mapbox.navigation.ui.maps.building.view.MapboxBuildingView
import com.mapbox.navigation.utils.internal.JobControl
import com.mapbox.navigation.utils.internal.ThreadController
import com.mapbox.navigation.utils.internal.ifNonNull
import kotlinx.coroutines.launch

/**
 * Mapbox Buildings Api allows you to query buildings on a [MapboxMap] that you would like
 * to highlight using the [MapboxBuildingView].
 * @property mapboxMap
 */
class MapboxBuildingsApi internal constructor(
    private val mapboxMap: MapboxMap,
    private val processor: BuildingProcessor
) {

    constructor(mapboxMap: MapboxMap) : this(mapboxMap, BuildingProcessor)

    private val mainJobController: JobControl by lazy { ThreadController.getMainScopeAndRootJob() }

    /**
     * The API can be invoked to query a building on [MapboxMap] using the [point] provided as
     * an input. The API returns a [BuildingError] if there is an internal issue fetching the buildings.
     * Otherwise, it returns buildings wrapped inside [BuildingValue] in a form of list of [QueriedFeature].
     * This list is empty if the building is not found on the [MapboxMap]
     *
     * Note:
     *
     * There are two kind of points that can be used here to query a building.
     * - A point you can obtain that lives on a building also known as location point
     * - A point you would route to on the road also known as routable point
     *
     * For this method to work, your route's original request should be a "location point" and not a
     * "routing point", otherwise, nothing will be returned. If the coordinate is a routing point
     * (which is the correct way to do it), you should define a "waypoint_targets" in the original
     * request, for each of the waypoints, and for the final destination.
     */
    fun queryBuildingToHighlight(
        point: Point,
        callback: MapboxNavigationConsumer<Expected<BuildingError, BuildingValue>>
    ) {
        val action = BuildingAction.QueryBuilding(point, mapboxMap)
        mainJobController.scope.launch {
            val result = processor.queryBuilding(action)
            callback.accept(result.queriedBuildings)
        }
    }

    /**
     * The API can be invoked to query a building on [MapboxMap] using the [RouteProgress] provided as
     * an input. Use this function to query a building when you reach a waypoint in a multi-leg trip.
     * The API returns a [BuildingError] if there is an internal issue fetching the buildings.
     * Otherwise, it returns buildings wrapped inside [BuildingValue] in a form of list of [QueriedFeature].
     * This list is empty if the building is not found on the [MapboxMap]
     *
     * Note:
     *
     * There are two kind of points that can be used here to query a building.
     * - A point you can obtain that lives on a building also known as location point
     * - A point you would route to on the road also known as routable point
     *
     * For this method to work, your route's original request should be a "location point" and not a
     * "routing point", otherwise, nothing will be returned. If the coordinate is a routing point
     * (which is the correct way to do it), you should define a "waypoint_targets" in the original
     * request, for each of the waypoints, and for the final destination.
     */
    fun queryBuildingOnWaypoint(
        progress: RouteProgress,
        callback: MapboxNavigationConsumer<Expected<BuildingError, BuildingValue>>
    ) {
        val action = BuildingAction.QueryBuildingOnWaypoint(progress)
        val destination = processor.queryBuildingOnWaypoint(action)
        ifNonNull(destination.point) { point ->
            mainJobController.scope.launch {
                val pointAction = BuildingAction.QueryBuilding(point, mapboxMap)
                val result = processor.queryBuilding(pointAction)
                callback.accept(result.queriedBuildings)
            }
        } ?: callback.accept(
            ExpectedFactory.createError(
                BuildingError("waypoint inside $progress is null")
            )
        )
    }

    /**
     * The API can be invoked to query a building on [MapboxMap] using the [RouteProgress] provided as
     * an input. Use this function to query a building when you reach your destination.
     * The API returns a [BuildingError] if there is an internal issue fetching the buildings.
     * Otherwise, it returns buildings wrapped inside [BuildingValue] in a form of list of [QueriedFeature].
     * This list is empty if the building is not found on the [MapboxMap]
     *
     * Note:
     *
     * There are two kind of points that can be used here to query a building.
     * - A point you can obtain that lives on a building also known as location point
     * - A point you would route to on the road also known as routable point
     *
     * For this method to work, your route's original request should be a "location point" and not a
     * "routing point", otherwise, nothing will be returned. If the coordinate is a routing point
     * (which is the correct way to do it), you should define a "waypoint_targets" in the original
     * request, for each of the waypoints, and for the final destination.
     */
    fun queryBuildingOnFinalDestination(
        progress: RouteProgress,
        callback: MapboxNavigationConsumer<Expected<BuildingError, BuildingValue>>
    ) {
        val action = BuildingAction.QueryBuildingOnFinalDestination(progress)
        val destination = processor.queryBuildingOnFinalDestination(action)
        ifNonNull(destination.point) { point ->
            mainJobController.scope.launch {
                val pointAction = BuildingAction.QueryBuilding(point, mapboxMap)
                val result = processor.queryBuilding(pointAction)
                callback.accept(result.queriedBuildings)
            }
        } ?: callback.accept(
            ExpectedFactory.createError(
                BuildingError("final destination point inside $progress is null")
            )
        )
    }
}
