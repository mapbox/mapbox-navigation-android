package com.mapbox.navigation.ui.maps.roadname.api

import com.mapbox.navigation.base.road.model.Road
import com.mapbox.navigation.ui.maps.roadname.RoadNameAction
import com.mapbox.navigation.ui.maps.roadname.RoadNameProcessor
import com.mapbox.navigation.ui.maps.roadname.RoadNameResult
import com.mapbox.navigation.ui.maps.roadname.model.RoadLabel

/**
 * The API allows you to generate data to be rendered as a part of road name labels.
 */
class MapboxRoadNameLabelApi {

    /**
     * The method takes in [Road] and uses the model to extract the road name, shield name and
     * request the route shield based on the [Road.shieldUrl].
     * @param road Road
     * @return RoadLabel
     */
    fun getRoadNameLabel(road: Road): RoadLabel {
        val action = RoadNameAction.GetRoadNameLabel(road)
        val result = RoadNameProcessor.process(action) as RoadNameResult.RoadNameLabel
        return RoadLabel(result.name, result.shield, result.shieldName)
    }
}
