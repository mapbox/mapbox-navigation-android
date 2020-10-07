package com.mapbox.navigation.base.options

/**
 * Defines options for [EHorizon].
 *
 * Electronic Horizon is still **experimental**, which means that the design of the
 * APIs has open issues which may (or may not) lead to their changes in the future.
 * Roughly speaking, there is a chance that those declarations will be deprecated in the near
 * future or the semantics of their behavior may change in some way that may break some code.
 *
 * @param length the minimum length of the MPP in meters. This does not include the trailingLength.
 * The actual MPP length may be bigger
 * @param expansion the number of branches to include from the MPP. When set to 0 only the MPP
 * is returned. Higher values will result in deeper nesting
 * @param branchLength when expansion is set to anything but 0, this specifies the minimum length
 * in meters branches will be expanded from the MPP
 * @param includeGeometries will geometries be included for edges. Excluding the edge shapes may
 * save some processing time on shape extraction and decoding
 */
class EHorizonOptions private constructor(
    val length: Double,
    val expansion: Int,
    val branchLength: Double,
    val includeGeometries: Boolean
) {

    /**
     * Get a builder to customize a subset of current options.
     */
    fun toBuilder(): Builder = Builder().apply {
        length(length)
        expansion(expansion)
        branchLength(branchLength)
        includeGeometries(includeGeometries)
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EHorizonOptions

        if (length != other.length) return false
        if (expansion != other.expansion) return false
        if (branchLength != other.branchLength) return false
        if (includeGeometries != other.includeGeometries) return false

        return true
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun hashCode(): Int {
        var result = length.hashCode()
        result = 31 * result + expansion.hashCode()
        result = 31 * result + branchLength.hashCode()
        result = 31 * result + includeGeometries.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "EHorizonOptions(" +
            "length=$length, " +
            "expansion=$expansion, " +
            "branchLength=$branchLength, " +
            "includeGeometries=$includeGeometries" +
            ")"
    }

    /**
     * Build a new [EHorizonOptions]
     */
    class Builder {

        private var length: Double = 500.0
        private var expansion: Int = 0
        private var branchLength: Double = 50.0
        private var includeGeometries: Boolean = false

        /**
         * Override he minimum length of the EHorizon ahead of the current position.
         */
        fun length(length: Double): Builder =
            apply { this.length = length }

        /**
         * Override the number of levels to expand.
         */
        fun expansion(expansion: Int): Builder =
            apply { this.expansion = expansion }

        /**
         * Override the minimum length to expand branches.
         */
        fun branchLength(branchLength: Double): Builder =
            apply { this.branchLength = branchLength }

        /**
         * Override the flag to enable/disable geometries in the EHorizon.
         */
        fun includeGeometries(includeGeometries: Boolean): Builder =
            apply { this.includeGeometries = includeGeometries }

        /**
         * Build the [EHorizonOptions]
         */
        fun build(): EHorizonOptions {
            return EHorizonOptions(
                length = length,
                expansion = expansion,
                branchLength = branchLength,
                includeGeometries = includeGeometries
            )
        }
    }
}
