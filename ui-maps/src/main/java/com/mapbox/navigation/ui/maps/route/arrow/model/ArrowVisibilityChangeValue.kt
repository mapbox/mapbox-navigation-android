package com.mapbox.navigation.ui.maps.route.arrow.model

import com.mapbox.maps.extension.style.layers.properties.generated.Visibility

/**
 * Represents data for rendering visibility changes.
 *
 * @param layerVisibilityModifications the layer visibility modifications
 */
class ArrowVisibilityChangeValue internal constructor(
    val layerVisibilityModifications: List<Pair<String, Visibility>>,
)
