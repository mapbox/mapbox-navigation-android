package com.mapbox.navigation.base.trip.model.eh

import com.mapbox.navigation.base.internal.extensions.notEquals

/**
 * [EHorizonGraphPath] defines a path on a map graph.
 *
 * @param edges Ids of edges on a road graph
 * @param percentAlongBegin fraction along edge shape (0-1) of a path begin point
 * @param percentAlongEnd  fraction along edge shape (0-1) of a path end point
 * @param length length of a path
 */
class EHorizonGraphPath internal constructor(
    val edges: List<Long>,
    val percentAlongBegin: Double,
    val percentAlongEnd: Double,
    val length: Double
) {

    /**
     * Regenerate whenever a change is made
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EHorizonGraphPath

        if (edges != other.edges) return false
        if (percentAlongBegin.notEquals(other.percentAlongBegin)) return false
        if (percentAlongEnd.notEquals(other.percentAlongEnd)) return false
        if (length.notEquals(other.length)) return false

        return true
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun hashCode(): Int {
        var result = edges.hashCode()
        result = 31 * result + percentAlongBegin.hashCode()
        result = 31 * result + percentAlongEnd.hashCode()
        result = 31 * result + length.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "EHorizonGraphPath(" +
            "edges=$edges, " +
            "percentAlongBegin=$percentAlongBegin, " +
            "percentAlongEnd=$percentAlongEnd, " +
            "length=$length" +
            ")"
    }
}
