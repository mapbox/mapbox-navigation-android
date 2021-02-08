package com.mapbox.navigation.core.trip.model.eh

/**
 * EHorizonPosition
 *
 * @param eHorizonGraphPosition current graph position
 * @param eHorizon tree of edges
 * @param eHorizonResultType result type. see [EHorizonResultType]
 */
class EHorizonPosition internal constructor(
    val eHorizonGraphPosition: EHorizonGraphPosition,
    val eHorizon: EHorizon,
    val eHorizonResultType: String
) {

    /**
     * Regenerate whenever a change is made
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EHorizonPosition

        if (eHorizonGraphPosition != other.eHorizonGraphPosition) return false
        if (eHorizon != other.eHorizon) return false
        if (eHorizonResultType != other.eHorizonResultType) return false

        return true
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun hashCode(): Int {
        var result = eHorizonGraphPosition.hashCode()
        result = 31 * result + eHorizon.hashCode()
        result = 31 * result + eHorizonResultType.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "EHorizonPosition(" +
            "eHorizonGraphPosition=$eHorizonGraphPosition, " +
            "eHorizon=$eHorizon, " +
            "eHorizonResultType=$eHorizonResultType" +
            ")"
    }
}
