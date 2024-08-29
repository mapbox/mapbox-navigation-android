package com.mapbox.navigation.tripdata.maneuver.model

import com.mapbox.api.directions.v5.models.BannerComponents

/**
 * A simplified data structure representing an individual component inside [BannerComponents]
 * @property type String type of component.
 * @property node ComponentNode
 */
class Component(
    val type: String,
    val node: ComponentNode,
) {

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Component

        if (type != other.type) return false
        return node == other.node
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + node.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "Component(type='$type', node=$node)"
    }
}
