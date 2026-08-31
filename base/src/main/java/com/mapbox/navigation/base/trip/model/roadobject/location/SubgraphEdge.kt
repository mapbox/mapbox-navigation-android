package com.mapbox.navigation.base.trip.model.roadobject.location

import com.mapbox.geojson.Geometry

/**
 * SubgraphEdge represents an edge in the complex object which might be considered as a
 * directed graph. The graph might contain loops.
 * `innerEdgeIds` and `outerEdgeIds` properties contain edge ids, which allows to traverse the
 * graph, obtain geometry and calculate different distances inside it.
 *
 * @param id unique identifier of the edge.
 * @param innerEdgeIdsArray the identifiers of edges in the subgraph from which the user could transition
 * to this edge.
 * @param outerEdgeIdsArray the identifiers of edges in the subgraph to which the user could transition
 * from this edge.
 * @param shape the edge shape geometry.
 * @param length the length of the edge measured in meters.
 */
class SubgraphEdge internal constructor(
    val id: Long,
    val innerEdgeIdsArray: LongArray,
    val outerEdgeIdsArray: LongArray,
    val shape: Geometry,
    val length: Double,
) {

    /**
     * @return the identifiers of edges in the subgraph from which the user could transition to
     * this edge.
     */
    @Deprecated(
        message = "Use innerEdgeIdsArray instead - avoids per-element boxing. " +
            "This getter rebuilds the list on every call.",
        replaceWith = ReplaceWith("innerEdgeIdsArray"),
    )
    val innerEdgeIds: List<Long>
        get() = innerEdgeIdsArray.toList()

    /**
     * @return the identifiers of edges in the subgraph to which the user could transition from
     * this edge.
     */
    @Deprecated(
        message = "Use outerEdgeIdsArray instead - avoids per-element boxing. " +
            "This getter rebuilds the list on every call.",
        replaceWith = ReplaceWith("outerEdgeIdsArray"),
    )
    val outerEdgeIds: List<Long>
        get() = outerEdgeIdsArray.toList()

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SubgraphEdge

        if (id != other.id) return false
        if (!innerEdgeIdsArray.contentEquals(other.innerEdgeIdsArray)) return false
        if (!outerEdgeIdsArray.contentEquals(other.outerEdgeIdsArray)) return false
        if (shape != other.shape) return false
        if (length != other.length) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + innerEdgeIdsArray.contentHashCode()
        result = 31 * result + outerEdgeIdsArray.contentHashCode()
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
            "innerEdgeIdsArray=${innerEdgeIdsArray.contentToString()}, " +
            "outerEdgeIdsArray=${outerEdgeIdsArray.contentToString()}, " +
            "shape=$shape, " +
            "length=$length" +
            ")"
    }
}
