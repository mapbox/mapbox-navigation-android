package com.mapbox.navigation.ui.maps.building

import com.mapbox.geojson.Point
import com.mapbox.maps.MapboxMap
import com.mapbox.navigation.base.trip.model.RouteProgress

internal sealed class BuildingAction {

    data class QueryBuilding(
        val point: Point,
        val mapboxMap: MapboxMap,
    ) : BuildingAction()

    data class QueryBuildingOnWaypoint(
        val progress: RouteProgress,
    ) : BuildingAction()

    data class QueryBuildingOnFinalDestination(
        val progress: RouteProgress,
    ) : BuildingAction()
}
