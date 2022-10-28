package com.mapbox.navigation.ui.maps.building

import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.maps.RenderedQueryOptions
import com.mapbox.navigation.base.internal.extensions.isLegWaypoint
import com.mapbox.navigation.base.internal.utils.internalWaypoints
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
        val waypoints = action.progress.navigationRoute.internalWaypoints()
        val waypointIndex = action.progress.currentLegProgress?.legIndex!! + 1
        val waypoint = waypoints.filter { it.isLegWaypoint() }.getOrNull(waypointIndex)
        return BuildingResult.GetDestination(waypoint?.target ?: waypoint?.location)
    }

    fun queryBuildingOnFinalDestination(
        action: BuildingAction.QueryBuildingOnFinalDestination
    ): BuildingResult.GetDestination {
        val lastWaypoint = action.progress.navigationRoute
            .internalWaypoints()
            .lastOrNull()
        return BuildingResult.GetDestination(lastWaypoint?.target ?: lastWaypoint?.location)
    }
}
