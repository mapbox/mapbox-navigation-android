package com.mapbox.navigation.core.trip.model.eh

/**
 * Road name information
 *
 * @param name road name
 * @param shielded is the road shielded?
 */
class RoadName internal constructor(
    val name: String,
    val shielded: Boolean
) {

    /**
     * Regenerate whenever a change is made
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RoadName

        if (name != other.name) return false
        if (shielded != other.shielded) return false

        return true
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + shielded.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "NameInfo(" +
            "name=$name, " +
            "shielded=$shielded" +
            ")"
    }
}
