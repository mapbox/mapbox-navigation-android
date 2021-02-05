package com.mapbox.navigation.core.trip.model.eh

/**
 * Basic Edge class
 *
 * @param id identifier of the directed edge (it's unique)
 * @param level the level of the Edge (0 being the mpp, 1 branches of the mpp,
 * 2 branches of level 1 branches, etc)
 * @param probability the probability for this edge in percentage to be taken by the driver.
 * The probabilities of all outgoing edges on a single intersection sum up to 1.
 * @param out the outgoing Edges
 * @param parent the parent Edge
 */
class Edge internal constructor(
    val id: Long,
    val level: Byte,
    val probability: Double,
    val out: List<Edge>,
    val parent: Edge?,
) {

    /**
     * @return true if the Edge is the most probable path (MPP), false if not
     */
    fun isMpp(): Boolean {
        return level == 0.toByte()
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Edge

        if (id != other.id) return false
        if (level != other.level) return false
        if (probability != other.probability) return false
        if (out != other.out) return false
        if (parent != other.parent) return false

        return true
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + level
        result = 31 * result + probability.hashCode()
        result = 31 * result + out.hashCode()
        result = 31 * result + (parent?.hashCode() ?: 0)
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "Edge(" +
            "id=$id, " +
            "level=$level, " +
            "probability=$probability, " +
            "out=$out, " +
            "parent=$parent" +
            ")"
    }
}
