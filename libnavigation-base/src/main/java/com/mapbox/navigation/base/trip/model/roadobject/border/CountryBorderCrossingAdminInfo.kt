package com.mapbox.navigation.base.trip.model.roadobject.border

/**
 * Administrative information.
 */
class CountryBorderCrossingAdminInfo internal constructor(
    /**
     * ISO 3166-1, 2 letter country code.
     */
    val code: String,
    /**
     * ISO 3166-1 alpha-3, 3 letter country code.
     */
    val codeAlpha3: String,
) {

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CountryBorderCrossingAdminInfo

        if (code != other.code) return false
        if (codeAlpha3 != other.codeAlpha3) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = code.hashCode()
        result = 31 * result + codeAlpha3.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "CountryBorderCrossingAdminInfo(" +
            "code='$code', " +
            "codeAlpha3='$codeAlpha3'" +
            ")"
    }
}
