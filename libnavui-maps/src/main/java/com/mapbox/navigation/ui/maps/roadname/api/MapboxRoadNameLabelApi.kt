package com.mapbox.navigation.ui.maps.roadname.api

import com.mapbox.navigation.base.road.model.Road
import com.mapbox.navigation.ui.maps.roadname.model.RoadLabel

/**
 * The API allows you to generate data to be rendered as a part of road name labels.
 */
@Deprecated(
    message = "The API will not be supported and will be removed."
)
class MapboxRoadNameLabelApi {

    /**
     * The method takes in [Road] and uses the model to extract the road name, shield name and
     * request the route shield based on the [Road.shieldUrl].
     * @param road Road
     * @return RoadLabel
     */
    @Deprecated(
        message = "The API is redundant. To render road name instantiate the MapboxRoadNameView " +
            "and invoke renderRoadName by passing Road object that can be obtained via location " +
            "observer using onNewLocationMatcherResult. The method will not be supported and " +
            "will be removed."
    )
    fun getRoadNameLabel(road: Road): RoadLabel {
        return RoadLabel(road.name, null, road.shieldName)
    }
}
