package com.mapbox.navigation.base.road.model

/**
 * Object that holds road properties
 * @property components list of the [RoadComponent]
 */
class Road internal constructor(
    val components: List<RoadComponent>,
) {

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
        return "Road(" +
            "components=$components" +
            ")"
    }
}
