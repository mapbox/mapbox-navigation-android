package com.mapbox.navigation.ui.maps.roadname

import com.mapbox.navigation.base.road.model.Road

internal object RoadNameProcessor {

    fun process(action: RoadNameAction): RoadNameResult {
        when (action) {
            is RoadNameAction.GetRoadNameLabel -> {
                return processRoadNameLabel(action.road)
            }
        }
    }

    private fun processRoadNameLabel(road: Road): RoadNameResult {
        // Shield is being passed as null on purpose because this info is not yet available from
        // nav native. The ticket is being tracked here
        // https://github.com/mapbox/mapbox-navigation-native/issues/4325
        return RoadNameResult.RoadNameLabel(road.name, null, road.shieldName)
    }
}
