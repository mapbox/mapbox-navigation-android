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
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UpdateManeuverArrowValue

        if (layerVisibilityModifications != other.layerVisibilityModifications) return false
        if (arrowShaftFeature != other.arrowShaftFeature) return false
        if (arrowHeadFeature != other.arrowHeadFeature) return false

        return true
    }

    override fun hashCode(): Int {
        var result = layerVisibilityModifications.hashCode()
        result = 31 * result + (arrowShaftFeature?.hashCode() ?: 0)
        result = 31 * result + (arrowHeadFeature?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "UpdateManeuverArrowValue(" +
            "layerVisibilityModifications=$layerVisibilityModifications, " +
            "arrowShaftFeature=$arrowShaftFeature, " +
            "arrowHeadFeature=$arrowHeadFeature" +
            ")"
    }
}
