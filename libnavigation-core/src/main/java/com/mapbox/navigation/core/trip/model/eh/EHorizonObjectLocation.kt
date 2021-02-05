package com.mapbox.navigation.core.trip.model.eh

class EHorizonObjectLocation internal constructor(
    val edges: List<Long>,
    val percentAlongBegin: Double,
    val percentAlongEnd: Double,
) {

    /**
     * Regenerate whenever a change is made
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EHorizonObjectLocation

        if (edges != other.edges) return false
        if (percentAlongBegin != other.percentAlongBegin) return false
        if (percentAlongEnd != other.percentAlongEnd) return false

        return true
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun hashCode(): Int {
        var result = edges.hashCode()
        result = 31 * result + percentAlongBegin.hashCode()
        result = 31 * result + percentAlongEnd.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "EHorizonObjectLocation(" +
            "edges=$edges, " +
            "percentAlongBegin=$percentAlongBegin, " +
            "percentAlongEnd=$percentAlongEnd" +
            ")"
    }
}
