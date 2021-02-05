package com.mapbox.navigation.core.trip.model.eh

/**
 * The position on the current [EHorizon].
 *
 * @param edgeId the current Edge id
 * @param percentAlong the progress along the current edge [0,1)
 * @param
 */
class EHorizonGraphPosition internal constructor(
    val edgeId: Long,
    val percentAlong: Double
) {

    /**
     * Regenerate whenever a change is made
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EHorizonGraphPosition

        if (edgeId != other.edgeId) return false
        if (percentAlong != other.percentAlong) return false

        return true
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun hashCode(): Int {
        var result = edgeId.hashCode()
        result = 31 * result + percentAlong.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "EHorizonGraphPosition(" +
            "edgeId=$edgeId, " +
            "percentAlong=$percentAlong" +
            ")"
    }
}
