package com.mapbox.navigation.base.trip.model.roadobject

/**
 * RoadObjectPassInfo contains id and type of the passed road object.
 *
 * @param roadObjectId road object id
 * @param type type of road object
 */
class RoadObjectPassInfo internal constructor(
    val roadObjectId: String,
    @RoadObjectType.Type val type: Int,
) {

    /**
     * Regenerate whenever a change is made
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RoadObjectPassInfo

        if (roadObjectId != other.roadObjectId) return false
        if (type != other.type) return false

        return true
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun hashCode(): Int {
        var result = roadObjectId.hashCode()
        result = 31 * result + type
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "RoadObjectPassInfo(" +
            "roadObjectId='$roadObjectId', " +
            "type=$type" +
            ")"
    }
}
