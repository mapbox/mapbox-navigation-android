package com.mapbox.navigation.core.geodeeplink

import com.mapbox.geojson.Point

/**
 * @param point location coordinates specified by the deeplink, null when unknown
 * @param placeQuery human readable description of the place
 */
class GeoDeeplink internal constructor(
    val point: Point?,
    val placeQuery: String?,
) {

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GeoDeeplink

        if (point != other.point) return false
        if (placeQuery != other.placeQuery) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = (point?.hashCode() ?: 0)
        result = 31 * result + (placeQuery?.hashCode() ?: 0)
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "GeoDeeplink(point=$point, placeQuery=$placeQuery)"
    }
}
