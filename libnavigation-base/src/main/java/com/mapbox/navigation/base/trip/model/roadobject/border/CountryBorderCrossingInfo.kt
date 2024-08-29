package com.mapbox.navigation.base.trip.model.roadobject.border

/**
 * Administrative information.
 *
 * @param from origin administrative info when crossing the country border
 * @param to destination administrative info when crossing the country border
 */
class CountryBorderCrossingInfo internal constructor(
    val from: CountryBorderCrossingAdminInfo,
    val to: CountryBorderCrossingAdminInfo,
) {

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CountryBorderCrossingInfo

        if (from != other.from) return false
        if (to != other.to) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = from.hashCode()
        result = 31 * result + (to.hashCode())
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "CountryBorderCrossingInfo(" +
            "from=$from, " +
            "to=$to" +
            ")"
    }
}
