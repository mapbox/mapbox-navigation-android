package com.mapbox.navigation.ui.maps.building

import com.mapbox.bindgen.Expected
import com.mapbox.geojson.Point
import com.mapbox.navigation.ui.maps.building.model.BuildingError
import com.mapbox.navigation.ui.maps.building.model.BuildingValue

internal sealed class BuildingResult {

    data class QueriedBuildings(
        val queriedBuildings: Expected<BuildingError, BuildingValue>,
    ) : BuildingResult()

    data class GetDestination(
        val point: Point?,
    ) : BuildingResult()
}
