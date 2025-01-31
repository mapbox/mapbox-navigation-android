package com.mapbox.navigation.base.trip.model.roadobject.distanceinfo

import com.mapbox.navigation.base.trip.model.roadobject.RoadObjectPosition

/**
 * Gate represents information about a particular exit or entrance.
 *
 * @param id of the [Gate], persistent to the same gate.
 * @param position on the road graph with coordinates
 * @param probability to enter/exit this gate, value in range [0, 1].
 * Warning: currently this field contains 1.0 for all gates, would be improved in the future.
 * @param distance distance to the gate in meters:
 * - if represents entrance outside the object - positive
 * - if represents entrance inside the object - negative
 * - if represents exit outside the object - zero
 * - if represents exit inside the object - positive
 */
class Gate internal constructor(
    val id: Int,
    val position: RoadObjectPosition,
    val probability: Double,
    val distance: Double,
) {

    /**
     * Regenerate whenever a change is made
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Gate

        if (id != other.id) return false
        if (position != other.position) return false
        if (probability != other.probability) return false
        if (distance != other.distance) return false

        return true
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + position.hashCode()
        result = 31 * result + probability.hashCode()
        result = 31 * result + distance.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "Gate(" +
            "id=$id, " +
            "position=$position, " +
            "probability=$probability, " +
            "distance=$distance" +
            ")"
    }
}
