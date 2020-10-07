package com.mapbox.navigation.base.trip.model

/**
 * Road name information
 *
 * Electronic Horizon is still **experimental**, which means that the design of the
 * APIs has open issues which may (or may not) lead to their changes in the future.
 * Roughly speaking, there is a chance that those declarations will be deprecated in the near
 * future or the semantics of their behavior may change in some way that may break some code.
 *
 * @param name road name
 * @param shielded is the road shielded?
 */
class NameInfo private constructor(
    val name: String,
    val shielded: Boolean
) {

    /**
     * @return the builder that created the [NameInfo]
     */
    fun toBuilder(): Builder = Builder().apply {
        name(name)
        shielded(shielded)
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NameInfo

        if (name != other.name) return false
        if (shielded != other.shielded) return false

        return true
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + shielded.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "NameInfo(" +
            "name=$name, " +
            "shielded=$shielded" +
            ")"
    }

    /**
     * Builder for [NameInfo].
     */
    class Builder {

        private var name: String = ""
        private var shielded: Boolean = false

        /**
         * Defines the road name
         */
        fun name(name: String): Builder =
            apply { this.name = name }

        /**
         * Defines if the road is shielded
         */
        fun shielded(shielded: Boolean): Builder =
            apply { this.shielded = shielded }

        /**
         * Build the [NameInfo]
         */
        fun build(): NameInfo {
            return NameInfo(
                name = name,
                shielded = shielded
            )
        }
    }
}
