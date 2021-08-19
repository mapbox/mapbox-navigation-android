package com.mapbox.navigation.base.trip.model.roadobject.location

import com.mapbox.geojson.Geometry

/**
 * SubgraphEdge represents an edge in the complex object which might be considered as a
 * directed graph. The graph might contain loops.
 * `innerEdgeIds` and `outerEdgeIds` properties contain edge ids, which allows to traverse the
 * graph, obtain geometry and calculate different distances inside it.
 *
 * @param id unique identifier of the edge.
 * @param innerEdgeIds the identifiers of edges in the subgraph from which the user could transition
 * to this edge.
 * @param outerEdgeIds the identifiers of edges in the subgraph to which the user could transition
 * from this edge.
 * @param shape the edge shape geometry.
 * @param length the length of the edge measured in meters.
 */
class SubgraphEdge internal constructor(
    val id: Long,
    val innerEdgeIds: List<Long>,
    val outerEdgeIds: List<Long>,
    val shape: Geometry,
    val length: Double,
) {

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SubgraphEdge

        if (id != other.id) return false
        if (innerEdgeIds != other.innerEdgeIds) return false
        if (outerEdgeIds != other.outerEdgeIds) return false
        if (shape != other.shape) return false
        if (length != other.length) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + innerEdgeIds.hashCode()
        result = 31 * result + outerEdgeIds.hashCode()
        result = 31 * result + shape.hashCode()
        result = 31 * result + length.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "SubgraphEdge(" +
            "id=$id, " +
            "innerEdgeIds=$innerEdgeIds, " +
            "outerEdgeIds=$outerEdgeIds, " +
            "shape=$shape, " +
            "length=$length" +
            ")"
    }
}
