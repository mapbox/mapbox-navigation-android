package com.mapbox.navigation.core.trip.model.eh

/**
 * EHorizonObjectLocation represents location of road object on road graph.
 * For point-like objects will contain single edge with `percentAlongBegin == percentAlongEnd`
 *
 * @param edges list of edge ids belong to object
 * @param percentAlongBegin offset from the start of edge (0 - 1) pointing to the start of road
 * object on the very first edge
 * @param percentAlongEnd offset from the start of edge (0 - 1) pointing to the end of road object
 * on the very last edge
 */
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
