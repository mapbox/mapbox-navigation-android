package com.mapbox.navigation.core.trip.model.eh

/**
 * EHorizonObjectEnterExitInfo
 *
 * @param roadObjectId road object id
 * @param enterFromStartOrExitFromEnd if object was entered via it's start for `onRoadObjectEnter`
 * or if object was exited via it's end for `onRoadObjectExit`
 * @param type type of road object
 */
class EHorizonObjectEnterExitInfo internal constructor(
    val roadObjectId: String,
    val enterFromStartOrExitFromEnd: Boolean,
    val type: String,
) {

    /**
     * Regenerate whenever a change is made
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EHorizonObjectEnterExitInfo

        if (roadObjectId != other.roadObjectId) return false
        if (enterFromStartOrExitFromEnd != other.enterFromStartOrExitFromEnd) return false
        if (type != other.type) return false

        return true
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun hashCode(): Int {
        var result = roadObjectId.hashCode()
        result = 31 * result + enterFromStartOrExitFromEnd.hashCode()
        result = 31 * result + type.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "EHorizonObjectEnterExitInfo(" +
            "roadObjectId='$roadObjectId', " +
            "enterFromStartOrExitFromEnd=$enterFromStartOrExitFromEnd, " +
            "type=$type" +
            ")"
    }
}
