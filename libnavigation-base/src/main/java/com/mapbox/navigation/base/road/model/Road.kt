package com.mapbox.navigation.base.road.model

import com.mapbox.api.directions.v5.models.MapboxShield

/**
 * Object that holds road properties
 * @property components list of the [RoadComponent]
 */
class Road internal constructor(
    val components: List<RoadComponent>
) {

    /**
     * Name of the road.
     */
    @Deprecated(
        message = "Use RoadComponent.text instead.",
        replaceWith = ReplaceWith("RoadComponent.text")
    )
    val name: String? = null

    /**
     * URL for the route shield.
     */
    @Deprecated(
        message = "Use RoadComponent.shield.baseUrl() instead.",
        replaceWith = ReplaceWith("RoadComponent.shield.baseUrl()")
    )
    val shieldUrl: String? = null

    /**
     * Name of the route shield.
     */
    @Deprecated(
        message = "Use RoadComponent.shield.name() instead.",
        replaceWith = ReplaceWith("RoadComponent.shield.name()")
    )
    val shieldName: String? = null

    /**
     * Mapbox designed shield.
     */
    @Deprecated(
        message = "Use RoadComponent.shield instead.",
        replaceWith = ReplaceWith("RoadComponent.shield")
    )
    val mapboxShield: List<MapboxShield>? = null

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Road

        if (components != other.components) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        return components.hashCode()
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "Road(components=$components)"
    }
}
