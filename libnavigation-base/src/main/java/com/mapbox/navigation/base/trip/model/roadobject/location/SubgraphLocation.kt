package com.mapbox.navigation.base.trip.model.roadobject.location

import com.mapbox.geojson.Geometry
import com.mapbox.navigation.base.trip.model.roadobject.RoadObjectPosition

/**
 * Location of an object represented as a subgraph.
 *
 * @param entries positions of the subgraph entries.
 * @param exits positions of the subgraph exits.
 * @param edges edges of the subgraph associated by id.
 */
class SubgraphLocation internal constructor(
    val entries: List<RoadObjectPosition>,
    val exits: List<RoadObjectPosition>,
    val edges: Map<Long, SubgraphEdge>,
    shape: Geometry,
) : RoadObjectLocation(RoadObjectLocationType.SUBGRAPH, shape) {

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as SubgraphLocation

        if (entries != other.entries) return false
        if (exits != other.exits) return false
        if (edges != other.edges) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + entries.hashCode()
        result = 31 * result + exits.hashCode()
        result = 31 * result + edges.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "SubgraphLocation(" +
            "entries=$entries, " +
            "exits=$exits, " +
            "edges=$edges" +
            ")"
    }
}
