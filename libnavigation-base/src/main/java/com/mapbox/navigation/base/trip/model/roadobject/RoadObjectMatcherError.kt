package com.mapbox.navigation.base.trip.model.roadobject

/**
 * RoadObjectMatcherError
 *
 * @param roadObjectId id of the road object
 * @param error description of the error
 */
class RoadObjectMatcherError internal constructor(
    val roadObjectId: String,
    val error: String,
) {

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RoadObjectMatcherError

        if (roadObjectId != other.roadObjectId) return false
        if (error != other.error) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = roadObjectId.hashCode()
        result = 31 * result + error.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "RoadObjectMatcherError(roadObjectId='$roadObjectId', error='$error')"
    }
}
