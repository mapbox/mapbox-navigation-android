package com.mapbox.navigation.ui.maps.roadname

import com.mapbox.navigation.base.road.model.Road

internal sealed class RoadNameAction {
    data class GetRoadNameLabel(val road: Road) : RoadNameAction()
}
