package com.mapbox.navigation.core.trip.model.eh

/**
 * EHorizonObjectLocation represents location of road object on road graph.
 * For point-like objects will contain single edge with `percentAlongBegin == percentAlongEnd`
 *
 * @param path represents location of line-like object, will be null point-like objects
 * @param position represents location of point-like object, will be null line-like objects
 */
class EHorizonObjectLocation internal constructor(
    val path: EHorizonGraphPath?,
    val position: EHorizonGraphPosition?
) {

    /**
     * Regenerate whenever a change is made
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EHorizonObjectLocation

        if (path != other.path) return false
        if (position != other.position) return false

        return true
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun hashCode(): Int {
        var result = path.hashCode()
        result = 31 * result + position.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "EHorizonObjectLocation(" +
            "path=$path, " +
            "position=$position" +
            ")"
    }
}
