package com.mapbox.navigation.base.trip.model.alert

/**
 * Administrative information.
 */
class CountryBorderCrossingAdminInfo private constructor(
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
     * Transform this object into a builder to mutate the values.
     */
    fun toBuilder(): Builder = Builder(code, codeAlpha3)

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
        return "CountryBorderCrossingAdminInfo(code='$code', " +
            "codeAlpha3='$codeAlpha3')"
    }

    /**
     * Use to create a new instance.
     *
     * @see CountryBorderCrossingAdminInfo
     */
    class Builder(
        private val code: String,
        private val codeAlpha3: String
    ) {

        /**
         * Build the object instance.
         */
        fun build() =
            CountryBorderCrossingAdminInfo(
                code,
                codeAlpha3
            )
    }
}
