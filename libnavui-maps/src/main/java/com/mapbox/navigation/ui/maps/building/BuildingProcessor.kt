package com.mapbox.navigation.ui.maps.building

import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.maps.RenderedQueryOptions
import com.mapbox.navigation.ui.maps.building.model.BuildingError
import com.mapbox.navigation.ui.maps.building.model.BuildingValue
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

internal object BuildingProcessor {

    /**
     * Source layer that includes building height.
     */
    private const val BUILDING_LAYER_ID = "building"

    suspend fun queryBuilding(
        action: BuildingAction.QueryBuilding
    ): BuildingResult.QueriedBuildings = suspendCoroutine { continuation ->
        val screenCoordinate = action.mapboxMap.pixelForCoordinate(action.point)
        val queryOptions = RenderedQueryOptions(listOf(BUILDING_LAYER_ID), null)
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
        val routeOptions = action.progress.route.routeOptions()
        val coordinateIndex = action.progress.currentLegProgress?.legIndex!! + 1
        return routeOptions?.coordinatesList()?.getOrNull(coordinateIndex)?.let { point ->
            BuildingResult.GetDestination(point)
        } ?: BuildingResult.GetDestination(null)
    }

    fun queryBuildingOnFinalDestination(
        action: BuildingAction.QueryBuildingOnFinalDestination
    ): BuildingResult.GetDestination {
        val point = action.progress.route.routeOptions()?.coordinatesList()?.lastOrNull()
        return BuildingResult.GetDestination(point)
    }
}
