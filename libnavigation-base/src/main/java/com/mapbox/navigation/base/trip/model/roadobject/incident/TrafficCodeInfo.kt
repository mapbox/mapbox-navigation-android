package com.mapbox.navigation.base.trip.model.roadobject.incident

/**
 * Holds information about traffic code. See [IncidentInfo.trafficCodes].
 *
 * @param value traffic code value
 */
class TrafficCodeInfo internal constructor(
    val value: Int
){

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TrafficCodeInfo

        if (value != other.value) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        return value
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "TrafficCodeInfo(value=$value)"
    }
}
