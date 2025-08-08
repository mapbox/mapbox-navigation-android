package com.mapbox.navigation.ui.maps.route.arrow.model

import com.mapbox.geojson.FeatureCollection

/**
 * Represents the data for rendering the adding of route arrow(s)
 *
 * @param arrowShaftFeatureCollection a [FeatureCollection] for the arrow shafts source
 * @param arrowHeadFeatureCollection a [FeatureCollection] for the arrow head source
 */
class ArrowAddedValue internal constructor(
    val arrowShaftFeatureCollection: FeatureCollection,
    val arrowHeadFeatureCollection: FeatureCollection,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ArrowAddedValue

        if (arrowShaftFeatureCollection != other.arrowShaftFeatureCollection) return false
        if (arrowHeadFeatureCollection != other.arrowHeadFeatureCollection) return false

        return true
    }

    override fun hashCode(): Int {
        var result = arrowShaftFeatureCollection.hashCode()
        result = 31 * result + arrowHeadFeatureCollection.hashCode()
        return result
    }

    override fun toString(): String {
        return "ArrowAddedValue(" +
            "arrowShaftFeatureCollection=$arrowShaftFeatureCollection, " +
            "arrowHeadFeatureCollection=$arrowHeadFeatureCollection" +
            ")"
    }
}
