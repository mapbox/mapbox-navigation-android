package com.mapbox.navigation.ui.base.model.maneuver

import com.mapbox.api.directions.v5.models.BannerComponents

/**
 * A simplified data structure representing an individual component inside [BannerComponents]
 * @property type String type of component.
 * @property node ComponentNode
 */
data class Component(
    val type: String,
    val node: ComponentNode
)
