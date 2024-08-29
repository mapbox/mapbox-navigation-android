package com.mapbox.navigation.ui.maps.building.api

import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.geojson.Point
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.QueriedRenderedFeature
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.ui.base.util.MapboxNavigationConsumer
import com.mapbox.navigation.ui.maps.building.BuildingAction
import com.mapbox.navigation.ui.maps.building.BuildingProcessor
import com.mapbox.navigation.ui.maps.building.model.BuildingError
import com.mapbox.navigation.ui.maps.building.model.BuildingValue
import com.mapbox.navigation.ui.maps.building.view.MapboxBuildingView
import com.mapbox.navigation.utils.internal.InternalJobControlFactory
import com.mapbox.navigation.utils.internal.ifNonNull
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch

/**
 * Mapbox Buildings Api allows you to query buildings on a [MapboxMap] that you would like
 * to highlight using the [MapboxBuildingView].
 * @property mapboxMap
 */
class MapboxBuildingsApi internal constructor(
    private val mapboxMap: MapboxMap,
    private val processor: BuildingProcessor,
) {

    constructor(mapboxMap: MapboxMap) : this(mapboxMap, BuildingProcessor)

    private val mainJobController by lazy { InternalJobControlFactory.createMainScopeJobControl() }

    /**
     * The API can be invoked to query a building on [MapboxMap] using the [point] provided as
     * an input. The API returns a [BuildingError] if there is an internal issue fetching the buildings.
     * Otherwise, it returns buildings wrapped inside [BuildingValue] in a form of list of [QueriedRenderedFeature].
     * This list is empty if the building is not found on the [MapboxMap]
     *
     * Note:
     *
     * There are two kind of points that can be used here to query a building.
     * - A point you can obtain where the actual building is also known as location point
     * - A point on a road snapped from the address also known as routable point
     *
     * For this method to work, the [point] passed to this API should be a "location point" and not a
     * "routing point".
     */
    fun queryBuildingToHighlight(
        point: Point,
        callback: MapboxNavigationConsumer<Expected<BuildingError, BuildingValue>>,
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
     * Otherwise, it returns buildings wrapped inside [BuildingValue] in a form of list of [QueriedRenderedFeature].
     * This list is empty if the building is not found on the [MapboxMap]
     *
     * Note:
     *
     * There are two kind of points that can be used here to query a building.
     * - A point you can obtain where the actual building is also known as location point
     * - A point on a road snapped from the address also known as routable point
     *
     * For this method to work, your route's original request should be a "location point" and not a
     * "routing point", otherwise, nothing will be returned. If the coordinate is a routing point
     * (which is the correct way to do it), you should define a "waypoint_targets" in the original
     * request, for each of the waypoints, and for the final destination.
     *
     * For example when making a route request, you can specify the [RouteOptions] as follows if you
     * choose the coordinate to be a routing point:
     *
     * ```
     * val routeOptions = RouteOptions.builder()
     *   .applyDefaultNavigationOptions()
     *   .applyLanguageAndVoiceUnitOptions(this)
     *   .coordinatesList(
     *     listOf(
     *       Point.fromLngLat(-122.4192, 37.7627), // origin
     *       Point.fromLngLat(-122.4182, 37.7651), // waypoint destination
     *       Point.fromLngLat(-122.4145, 37.7653)  // final destination
     *     )
     *   )
     *   .waypointTargetsList(
     *     listOf(
     *       Point.fromLngLat(-122.4192, 37.7627), // origin
     *       Point.fromLngLat(-122.4183, 37.7653), // waypoint destination
     *       Point.fromLngLat(-122.4146, 37.7655)  // final destination
     *     )
     *   )
     *   .build()
     * ```
     *
     * In this case the points mentioned in the [RouteOptions.coordinatesList] are routing points
     * where you would want the router to route you to whereas the points mentioned in the
     * [RouteOptions.waypointTargetsList] are the location points that would be highlighted when you
     * reach your waypoint or final destination. In case you don't specify the
     * [RouteOptions.waypointTargetsList] the logic will fallback to [RouteOptions.coordinatesList]
     * and would try to highlight these points. However the highlight will only work if they are
     * location points.
     */
    fun queryBuildingOnWaypoint(
        progress: RouteProgress,
        callback: MapboxNavigationConsumer<Expected<BuildingError, BuildingValue>>,
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
                BuildingError("waypoint inside $progress is null"),
            ),
        )
    }

    /**
     * The API can be invoked to query a building on [MapboxMap] using the [RouteProgress] provided as
     * an input. Use this function to query a building when you reach your destination.
     * The API returns a [BuildingError] if there is an internal issue fetching the buildings.
     * Otherwise, it returns buildings wrapped inside [BuildingValue] in a form of list of [QueriedRenderedFeature].
     * This list is empty if the building is not found on the [MapboxMap]
     *
     * Note:
     *
     * There are two kind of points that can be used here to query a building.
     * - A point you can obtain where the actual building is also known as location point
     * - A point on a road snapped from the address also known as routable point
     *
     * For this method to work, your route's original request should be a "location point" and not a
     * "routing point", otherwise, nothing will be returned. If the coordinate is a routing point
     * (which is the correct way to do it), you should define a "waypoint_targets" in the original
     * request, for each of the waypoints, and for the final destination.
     *
     * For example when making a route request, you can specify the [RouteOptions] as follows if you
     * choose the coordinate to be a routing point:
     *
     * ```
     * val routeOptions = RouteOptions.builder()
     *   .applyDefaultNavigationOptions()
     *   .applyLanguageAndVoiceUnitOptions(this)
     *   .coordinatesList(
     *     listOf(
     *       Point.fromLngLat(-122.4192, 37.7627), // origin
     *       Point.fromLngLat(-122.4145, 37.7653)  // final destination
     *     )
     *   )
     *   .waypointTargetsList(
     *     listOf(
     *       Point.fromLngLat(-122.4192, 37.7627), // origin
     *       Point.fromLngLat(-122.4146, 37.7655)  // final destination
     *     )
     *   )
     *   .build()
     * ```
     *
     * In this case the points mentioned in the [RouteOptions.coordinatesList] are routing points
     * where you would want the router to route you to whereas the points mentioned in the
     * [RouteOptions.waypointTargetsList] are the location points that would be highlighted when you
     * reach your final destination. In case you don't specify the [RouteOptions.waypointTargetsList]
     * the logic will fallback to [RouteOptions.coordinatesList] and would try to highlight these
     * points. However the highlight will only work if they are location points.
     */
    fun queryBuildingOnFinalDestination(
        progress: RouteProgress,
        callback: MapboxNavigationConsumer<Expected<BuildingError, BuildingValue>>,
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
                BuildingError("final destination point inside $progress is null"),
            ),
        )
    }

    /**
     * Cancels any/all background tasks that may be running.
     */
    fun cancel() {
        mainJobController.job.cancelChildren()
    }
}
