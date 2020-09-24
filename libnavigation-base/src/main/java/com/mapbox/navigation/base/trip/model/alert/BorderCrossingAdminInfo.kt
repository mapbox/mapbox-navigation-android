package com.mapbox.navigation.base.trip.model.alert

/**
 * Administrative information.
 */
class BorderCrossingAdminInfo private constructor(
    /**
     * ISO 3166-1, 2 letter country code.
     */
    val countryCode: String,
    /**
     * ISO 3166-1 alpha-3, 3 letter country code.
     */
    val countryCodeAlpha3: String,
) {

    /**
     * Transform this object into a builder to mutate the values.
     */
    fun toBuilder(): Builder = Builder(countryCode, countryCodeAlpha3)

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BorderCrossingAdminInfo

        if (countryCode != other.countryCode) return false
        if (countryCodeAlpha3 != other.countryCodeAlpha3) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = countryCode.hashCode()
        result = 31 * result + countryCodeAlpha3.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "BorderCrossingAdminInfo(countryCode='$countryCode', " +
            "countryCodeAlpha3='$countryCodeAlpha3')"
    }

    /**
     * Use to create a new instance.
     *
     * @see BorderCrossingAdminInfo
     */
    class Builder(
        private val countryCode: String,
        private val countryCodeAlpha3: String
    ) {
        /**
         * Build the object instance.
         */
        fun build() =
            BorderCrossingAdminInfo(
                countryCode,
                countryCodeAlpha3
            )
    }
}
