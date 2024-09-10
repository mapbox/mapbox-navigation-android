package com.mapbox.navigation.ui.maps.route.arrow.model

import com.mapbox.geojson.Feature
import com.mapbox.maps.extension.style.layers.properties.generated.Visibility

/**
 * Represents the data for rendering the maneuver arrow update
 *
 * @param layerVisibilityModifications visibility modifications
 * @param arrowShaftFeature a [Feature] for the arrow shafts source
 * @param arrowHeadFeature a [Feature] for the arrow head source
 */
class UpdateManeuverArrowValue internal constructor(
    val layerVisibilityModifications: List<Pair<String, Visibility>>,
    val arrowShaftFeature: Feature?,
    val arrowHeadFeature: Feature?,
)
