package com.mapbox.navigation.base.options

/**
 * PredictiveCacheOptions
 *
 * @param predictiveCacheNavigationOptions [PredictiveCacheNavigationOptions] Predictive cache Navigation related options.
 * @param predictiveCacheMapsOptions [PredictiveCacheMapsOptions] Predictive cache Maps related options.
 */
class PredictiveCacheOptions private constructor(
    val predictiveCacheNavigationOptions: PredictiveCacheNavigationOptions,
    val predictiveCacheMapsOptions: PredictiveCacheMapsOptions,
) {

    /**
     * Get a builder to customize a subset of current options.
     */
    fun toBuilder(): Builder = Builder().apply {
        predictiveCacheNavigationOptions(predictiveCacheNavigationOptions)
        predictiveCacheMapsOptions(predictiveCacheMapsOptions)
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PredictiveCacheOptions

        if (predictiveCacheNavigationOptions != other.predictiveCacheNavigationOptions) return false
        if (predictiveCacheMapsOptions != other.predictiveCacheMapsOptions) return false

        return true
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun hashCode(): Int {
        var result = predictiveCacheNavigationOptions.hashCode()
        result = 31 * result + predictiveCacheMapsOptions.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "PredictiveCacheOptions(" +
            "predictiveCacheNavigationOptions=$predictiveCacheNavigationOptions, " +
            "predictiveCacheMapsOptions=$predictiveCacheMapsOptions" +
            ")"
    }

    /**
     * Build a new [PredictiveCacheOptions].
     */
    class Builder {
        private var predictiveCacheNavigationOptions: PredictiveCacheNavigationOptions? = null
        private var predictiveCacheMapsOptions: PredictiveCacheMapsOptions? = null

        /**
         * Predictive cache Navigation related options.
         */
        fun predictiveCacheNavigationOptions(
            predictiveCacheNavigationOptions: PredictiveCacheNavigationOptions
        ): Builder = apply {
            this.predictiveCacheNavigationOptions = predictiveCacheNavigationOptions
        }

        /**
         * Predictive cache Maps related options.
         */
        fun predictiveCacheMapsOptions(
            predictiveCacheMapsOptions: PredictiveCacheMapsOptions
        ): Builder = apply { this.predictiveCacheMapsOptions = predictiveCacheMapsOptions }

        /**
         * Build [PredictiveCacheOptions].
         */
        fun build(): PredictiveCacheOptions = PredictiveCacheOptions(
            predictiveCacheNavigationOptions ?: PredictiveCacheNavigationOptions.Builder().build(),
            predictiveCacheMapsOptions ?: PredictiveCacheMapsOptions.Builder().build(),
        )
    }
}
