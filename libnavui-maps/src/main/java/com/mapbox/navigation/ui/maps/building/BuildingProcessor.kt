package com.mapbox.navigation.ui.maps.building

import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.geojson.Point
import com.mapbox.maps.RenderedQueryOptions
import com.mapbox.navigation.base.internal.extensions.isLegWaypoint
import com.mapbox.navigation.base.internal.extensions.isRequestedWaypoint
import com.mapbox.navigation.base.internal.utils.internalWaypoints
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.ui.maps.building.model.BuildingError
import com.mapbox.navigation.ui.maps.building.model.BuildingValue
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

internal object BuildingProcessor {

    /**
     * Source layer that includes building height.
     */
    private const val BUILDING_LAYER_ID = "building"
    private const val BUILDING_EXTRUSION_LAYER_ID = "building-extrusion"

    suspend fun queryBuilding(
        action: BuildingAction.QueryBuilding
    ): BuildingResult.QueriedBuildings = suspendCoroutine { continuation ->
        val screenCoordinate = action.mapboxMap.pixelForCoordinate(action.point)
        val queryOptions = RenderedQueryOptions(
            listOf(BUILDING_LAYER_ID, BUILDING_EXTRUSION_LAYER_ID),
            null
        )
        action.mapboxMap.queryRenderedFeatures(screenCoordinate, queryOptions) { expected ->
            expected.fold(
                { error ->
                    continuation.resume(
                        BuildingResult.QueriedBuildings(
                            ExpectedFactory.createError(BuildingError(error))
                        )
                    )
                },
                { value ->
                    continuation.resume(
                        BuildingResult.QueriedBuildings(
                            ExpectedFactory.createValue(BuildingValue(value))
                        )
                    )
                }
            )
        }
    }

    fun queryBuildingOnWaypoint(
        action: BuildingAction.QueryBuildingOnWaypoint
    ): BuildingResult.GetDestination {
        val waypointIndex = action.progress.currentLegProgress?.legIndex!! + 1
        val buildingLocation = getBuildingLocation(waypointIndex, action.progress)
        return BuildingResult.GetDestination(buildingLocation)
    }

    fun queryBuildingOnFinalDestination(
        action: BuildingAction.QueryBuildingOnFinalDestination
    ): BuildingResult.GetDestination {
        val waypointIndex = action.progress.navigationRoute.internalWaypoints()
            .indexOfLast { it.isLegWaypoint() }
        val buildingLocation = getBuildingLocation(waypointIndex, action.progress)
        return BuildingResult.GetDestination(buildingLocation)
    }

    private fun getBuildingLocation(legWaypointIndex: Int, progress: RouteProgress): Point? {
        val waypoints = progress.navigationRoute.internalWaypoints()
        val legWaypoints = waypoints.filter { it.isLegWaypoint() }
        val waypoint = legWaypoints.getOrNull(legWaypointIndex)
        if (waypoint == null) return null
        if (waypoint.target != null) {
            return waypoint.target
        }
        if (!waypoint.isRequestedWaypoint()) {
            return waypoint.location
        }
        val nonRequestedWaypointsCount = legWaypoints.take(legWaypointIndex + 1)
            .count { !it.isRequestedWaypoint() }
        return progress.navigationRoute.routeOptions.coordinatesList()
            .getOrNull(legWaypointIndex - nonRequestedWaypointsCount)
    }
}
