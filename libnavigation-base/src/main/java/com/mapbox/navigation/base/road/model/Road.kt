package com.mapbox.navigation.base.road.model

/**
 * Object that holds road properties
 * @property components list of the [RoadComponent]
 * @property name of the road if available otherwise null
 * @property shieldUrl url for the route shield if available otherwise null
 * @property shieldName name of the route shield if available otherwise null
 */
class Road internal constructor(
    val components: List<RoadComponent>,
    @Deprecated(
        message = "Use RoadComponent.text instead.",
        replaceWith = ReplaceWith("RoadComponent.text")
    )
    val name: String? = null,
    @Deprecated(
        message = "Use RoadComponent.shield.baseUrl() instead.",
        replaceWith = ReplaceWith("RoadComponent.shield.baseUrl()")
    )
    val shieldUrl: String? = null,
    @Deprecated(
        message = "Use RoadComponent.shield.name() instead.",
        replaceWith = ReplaceWith("RoadComponent.shield.name()")
    )
    val shieldName: String? = null,
) {

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Road

        if (components != other.components) return false
        if (name != other.name) return false
        if (shieldUrl != other.shieldUrl) return false
        if (shieldName != other.shieldName) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = components.hashCode()
        result = 31 * result + (name?.hashCode() ?: 0)
        result = 31 * result + (shieldUrl?.hashCode() ?: 0)
        result = 31 * result + (shieldName?.hashCode() ?: 0)
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "Road(" +
            "components=$components, " +
            "name=$name, " +
            "shieldUrl=$shieldUrl, " +
            "shieldName=$shieldName" +
            ")"
    }
}
