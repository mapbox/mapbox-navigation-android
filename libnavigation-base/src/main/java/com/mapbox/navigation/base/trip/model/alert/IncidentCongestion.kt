package com.mapbox.navigation.base.trip.model.alert

/**
 * Quantitative descriptor of congestion of [IncidentAlert].
 *
 * @param value transform this object into a builder to mutate the values.
 */
class IncidentCongestion private constructor(
    val value: Int?
) {

    /**
     * Transform this object into a builder to mutate the values.
     */
    fun toBuilder(): Builder = Builder()
        .value(value)

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

    /**
     * Use to create a new instance.
     *
     * @see IncidentCongestion
     */
    class Builder {

        private var value: Int? = null

        /**
         * Quantitative descriptor of congestion. 0 to 100.
         */
        fun value(value: Int?): Builder = also {
            this.value = value
        }

        /**
         * Build the object instance.
         */
        fun build(): IncidentCongestion = IncidentCongestion(value)
    }
}
