package com.mapbox.navigation.base.options

/**
 * Predictive cache Navigation related options.
 *
 * @param predictiveCacheLocationOptions [PredictiveCacheLocationOptions] location configuration for guidance predictive caching
 */
class PredictiveCacheNavigationOptions private constructor(
    val predictiveCacheLocationOptions: PredictiveCacheLocationOptions,
) {

    /**
     * Get a builder to customize a subset of current options.
     */
    fun toBuilder(): Builder = Builder().apply {
        predictiveCacheLocationOptions(predictiveCacheLocationOptions)
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PredictiveCacheNavigationOptions

        if (predictiveCacheLocationOptions != other.predictiveCacheLocationOptions) return false

        return true
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun hashCode(): Int {
        return predictiveCacheLocationOptions.hashCode()
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "PredictiveCacheNavigationOptions(" +
            "predictiveCacheLocationOptions=$predictiveCacheLocationOptions" +
            ")"
    }

    /**
     * Build a new [PredictiveCacheZoomLevelOptions].
     */
    class Builder {
        private var predictiveCacheLocationOptions: PredictiveCacheLocationOptions? = null

        /**
         * Location configuration for guidance predictive caching
         */
        fun predictiveCacheLocationOptions(
            predictiveCacheLocationOptions: PredictiveCacheLocationOptions,
        ): Builder = apply { this.predictiveCacheLocationOptions = predictiveCacheLocationOptions }

        /**
         * Build [PredictiveCacheNavigationOptions].
         */
        fun build(): PredictiveCacheNavigationOptions = PredictiveCacheNavigationOptions(
            predictiveCacheLocationOptions ?: PredictiveCacheLocationOptions.Builder().build(),
        )
    }
}
