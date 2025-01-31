package com.mapbox.navigation.base.trip.model.eh

/**
 * Basic Edge class
 *
 * **NOTE**: The Mapbox Electronic Horizon feature of the Mapbox Navigation SDK is in public beta
 * and is subject to changes, including its pricing. Use of the feature is subject to the beta
 * product restrictions in the Mapbox Terms of Service.
 * Mapbox reserves the right to eliminate any free tier or free evaluation offers at any time and
 * require customers to place an order to purchase the Mapbox Electronic Horizon feature,
 * regardless of the level of use of the feature.
 *
 * @param id identifier of the directed edge (it's unique)
 * @param level the level of the Edge (0 being the mpp, 1 branches of the mpp,
 * 2 branches of level 1 branches, etc)
 * @param probability the probability for this edge in percentage to be taken by the driver.
 * The probabilities of all outgoing edges on a single intersection sum up to 1.
 * @param isOnRoute Whether this edge is on primary route used for active guidance. Always false in free drive.
 * @param out the outgoing Edges
 */
class EHorizonEdge internal constructor(
    val id: Long,
    val level: Byte,
    val probability: Double,
    val isOnRoute: Boolean,
    val out: List<EHorizonEdge>,
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

        other as EHorizonEdge

        if (id != other.id) return false
        if (level != other.level) return false
        if (probability != other.probability) return false
        if (isOnRoute != other.isOnRoute) return false
        if (out != other.out) return false

        return true
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + level
        result = 31 * result + probability.hashCode()
        result = 31 * result + isOnRoute.hashCode()
        result = 31 * result + out.hashCode()
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
            "isOnRoute=$isOnRoute, " +
            "out=$out" +
            ")"
    }
}
