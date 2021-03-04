package com.mapbox.navigation.base.trip.model.roadobject

/**
 * Holds the road objects and the distance to the point where the alert occurs,
 * or start an alert if it has length.
 *
 * @param roadObject road object
 * @param distanceToStart distance to the start of the alert.
 * If the object has a length, and we've passed the start point,
 * **this value will be negative** until we cross the finish point of the objects's geometry.
 * This negative value, together with [RoadObjectGeometry.length]
 * can be used to calculate the distance since the start of an object.
 */
class UpcomingRoadObject private constructor(
    val roadObject: RoadObject,
    val distanceToStart: Double
) {

    /**
     * Transform this object into a builder to mutate the values.
     */
    fun toBuilder() = Builder(roadObject, distanceToStart)

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UpcomingRoadObject

        if (roadObject != other.roadObject) return false
        if (distanceToStart != other.distanceToStart) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = roadObject.hashCode()
        result = 31 * result + distanceToStart.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "UpcomingRoadObject(roadObject=$roadObject, distanceRemaining=$distanceToStart)"
    }

    /**
     * Use to create a new instance.
     *
     * @see UpcomingRoadObject
     */
    class Builder(
        private val roadObject: RoadObject,
        private val distanceRemaining: Double
    ) {

        /**
         * Build the object instance.
         */
        fun build() = UpcomingRoadObject(roadObject, distanceRemaining)
    }
}
