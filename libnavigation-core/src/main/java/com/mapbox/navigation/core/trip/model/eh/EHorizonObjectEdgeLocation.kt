package com.mapbox.navigation.core.trip.model.eh

/**
 * EHorizonObjectEdgeLocation represents location of road object on the edge
 *
 * @param percentAlongBegin offset from the start of edge (0 - 1) pointing to the beginning of road
 * object on this edge. Will be 0 for all edges in the line-like road object except the very first
 * one in the case of point-like object percentAlongBegin == percentAlongEnd.
 * @param percentAlongEnd offset from the start of edge (0 - 1) pointing to the end of road object
 * on this edge. Will be 1 for all edges in the line-like road object except the very first one in
 * the case of point-like object percentAlongBegin == percentAlongEnd.
 */
class EHorizonObjectEdgeLocation internal constructor(
    val percentAlongBegin: Double,
    val percentAlongEnd: Double,
) {

    /**
     * Regenerate whenever a change is made
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EHorizonObjectEdgeLocation

        if (percentAlongBegin != other.percentAlongBegin) return false
        if (percentAlongEnd != other.percentAlongEnd) return false

        return true
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun hashCode(): Int {
        var result = percentAlongBegin.hashCode()
        result = 31 * result + percentAlongEnd.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "EHorizonObjectEdgeLocation(" +
            "percentAlongBegin=$percentAlongBegin, " +
            "percentAlongEnd=$percentAlongEnd" +
            ")"
    }
}
