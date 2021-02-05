package com.mapbox.navigation.core.trip.model.eh

class EHorizonObjectDistanceInfo internal constructor(
    val distanceToEntry: Double,
    val distanceToEnd: Double,
    val entryFromStart: Boolean,
    val length: Double?,
    val type: String
) {

    /**
     * Regenerate whenever a change is made
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EHorizonObjectDistanceInfo

        if (distanceToEntry != other.distanceToEntry) return false
        if (distanceToEnd != other.distanceToEnd) return false
        if (entryFromStart != other.entryFromStart) return false
        if (length != other.length) return false
        if (type != other.type) return false

        return true
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun hashCode(): Int {
        var result = distanceToEntry.hashCode()
        result = 31 * result + distanceToEnd.hashCode()
        result = 31 * result + entryFromStart.hashCode()
        result = 31 * result + (length?.hashCode() ?: 0)
        result = 31 * result + type.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "EHorizonObjectDistanceInfo(" +
            "distanceToEntry=$distanceToEntry, " +
            "distanceToEnd=$distanceToEnd, " +
            "entryFromStart=$entryFromStart, " +
            "length=$length, " +
            "type=$type" +
            ")"
    }
}
