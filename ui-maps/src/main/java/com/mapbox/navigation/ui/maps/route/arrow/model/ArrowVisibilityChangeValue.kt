package com.mapbox.navigation.ui.maps.route.arrow.model

import com.mapbox.maps.extension.style.layers.properties.generated.Visibility

/**
 * Represents data for rendering visibility changes.
 *
 * @param layerVisibilityModifications the layer visibility modifications
 */
class ArrowVisibilityChangeValue internal constructor(
    val layerVisibilityModifications: List<Pair<String, Visibility>>,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ArrowVisibilityChangeValue

        if (layerVisibilityModifications != other.layerVisibilityModifications) return false

        return true
    }

    override fun hashCode(): Int {
        return layerVisibilityModifications.hashCode()
    }

    override fun toString(): String {
        return "ArrowVisibilityChangeValue(" +
            "layerVisibilityModifications=$layerVisibilityModifications" +
            ")"
    }
}
