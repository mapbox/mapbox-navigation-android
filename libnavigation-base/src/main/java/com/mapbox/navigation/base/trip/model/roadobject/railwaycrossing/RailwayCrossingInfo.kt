package com.mapbox.navigation.base.trip.model.roadobject.railwaycrossing

/**
 * Railway crossing information.
 */
class RailwayCrossingInfo internal constructor() {

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RailwayCrossingInfo

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        return javaClass.hashCode()
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "RailwayCrossingInfo()"
    }
}
