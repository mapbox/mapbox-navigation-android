package com.mapbox.navigation.base.trip.model.roadobject.incident

/**
 * Quantitative descriptor of congestion of [Incident].
 *
 * @param value quantitative descriptor of congestion. 0 to 100.
 */
class IncidentCongestion internal constructor(
    val value: Int?,
) {

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as IncidentCongestion

        if (value != other.value) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        return value.hashCode()
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "IncidentCongestion(value=$value)"
    }
}
