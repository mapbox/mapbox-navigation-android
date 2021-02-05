package com.mapbox.navigation.base.options

/**
 * Defines options for [EHorizon].
 *
 * @param length the minimum length of the MPP in meters. This does not include the trailingLength.
 * The actual MPP length may be bigger. Double in range [1.0, 20000.0]. Default value 500.0
 * @param expansion the number of branches to include from the MPP. When set to 0 only the MPP
 * is returned. Higher values will result in deeper nesting. Int in range [0, 2]. Default value 0
 * @param branchLength when expansion is set to anything but 0, this specifies the minimum length
 * in meters branches will be expanded from the MPP. Double in range [1.0, 5000.0].
 * Default value 50.0
 * @param minTimeDeltaBetweenUpdates the minimum time which should pass between consecutive
 * navigation statuses to update electronic horizon (seconds). If null electronic horizon will be
 * updated on each navigation status. Default value null
 */
class EHorizonOptions private constructor(
    val length: Double,
    val expansion: Int,
    val branchLength: Double,
    val minTimeDeltaBetweenUpdates: Double?
) {

    /**
     * Get a builder to customize a subset of current options.
     */
    fun toBuilder(): Builder = Builder().apply {
        length(length)
        expansion(expansion)
        branchLength(branchLength)
        minTimeDeltaBetweenUpdates(minTimeDeltaBetweenUpdates)
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
        if (minTimeDeltaBetweenUpdates != other.minTimeDeltaBetweenUpdates) return false

        return true
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun hashCode(): Int {
        var result = length.hashCode()
        result = 31 * result + expansion.hashCode()
        result = 31 * result + branchLength.hashCode()
        result = 31 * result + minTimeDeltaBetweenUpdates.hashCode()
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
            "minTimeDeltaBetweenUpdates=$minTimeDeltaBetweenUpdates" +
            ")"
    }

    /**
     * Build a new [EHorizonOptions]
     */
    class Builder {

        private var length: Double = 500.0
        private var expansion: Int = 0
        private var branchLength: Double = 50.0
        private var minTimeDeltaBetweenUpdates: Double? = null

        /**
         * Override the minimum length of the EHorizon ahead of the current position.
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
         * Override the minimum time delta between EHorizon updates.
         */
        fun minTimeDeltaBetweenUpdates(minTimeDeltaBetweenUpdates: Double?): Builder =
            apply { this.minTimeDeltaBetweenUpdates = minTimeDeltaBetweenUpdates }

        /**
         * Build the [EHorizonOptions]
         */
        fun build(): EHorizonOptions {
            return EHorizonOptions(
                length = length,
                expansion = expansion,
                branchLength = branchLength,
                minTimeDeltaBetweenUpdates = minTimeDeltaBetweenUpdates
            )
        }
    }
}
