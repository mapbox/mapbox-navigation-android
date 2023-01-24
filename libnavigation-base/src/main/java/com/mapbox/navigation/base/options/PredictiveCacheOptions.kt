package com.mapbox.navigation.base.options

/**
 * PredictiveCacheOptions
 *
 * @param predictiveCacheNavigationOptions [PredictiveCacheNavigationOptions] Predictive cache Navigation related options.
 * @param predictiveCacheMapsOptionsList List of predictive cache Maps related options ([PredictiveCacheMapsOptions]).
 *  Use this instead of [predictiveCacheMapsOptions] so that you can specify different
 *  [PredictiveCacheLocationOptions]s for different zoom level ranges.
 */
class PredictiveCacheOptions private constructor(
    val predictiveCacheNavigationOptions: PredictiveCacheNavigationOptions,
    val predictiveCacheMapsOptionsList: List<PredictiveCacheMapsOptions>
) {

    /**
     * Predictive cache Maps related options. If [Builder.predictiveCacheMapsOptionsList] was used,
     * returns first element from [predictiveCacheMapsOptionsList].
     * @deprecated use predictiveCacheMapsOptionsList instead to provide different
     * [PredictiveCacheLocationOptions]s for different zoom level ranges.
     */
    @Deprecated("Use predictiveCacheMapsOptionsList")
    val predictiveCacheMapsOptions: PredictiveCacheMapsOptions =
        predictiveCacheMapsOptionsList.first()

    /**
     * Get a builder to customize a subset of current options.
     */
    fun toBuilder(): Builder = Builder().apply {
        predictiveCacheNavigationOptions(predictiveCacheNavigationOptions)
        predictiveCacheMapsOptionsList(predictiveCacheMapsOptionsList)
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PredictiveCacheOptions

        if (predictiveCacheNavigationOptions != other.predictiveCacheNavigationOptions) return false
        if (predictiveCacheMapsOptionsList != other.predictiveCacheMapsOptionsList) return false

        return true
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun hashCode(): Int {
        var result = predictiveCacheNavigationOptions.hashCode()
        result = 31 * result + predictiveCacheMapsOptionsList.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "PredictiveCacheOptions(" +
            "predictiveCacheNavigationOptions=$predictiveCacheNavigationOptions, " +
            "predictiveCacheMapsOptionsList=$predictiveCacheMapsOptionsList" +
            ")"
    }

    /**
     * Build a new [PredictiveCacheOptions].
     */
    class Builder {
        private var predictiveCacheNavigationOptions =
            PredictiveCacheNavigationOptions.Builder().build()
        private var predictiveCacheMapsOptionsList =
            listOf(PredictiveCacheMapsOptions.Builder().build())

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
         * @deprecated use predictiveCacheMapsOptionsList instead to provide different
         * [PredictiveCacheLocationOptions]s for different zoom level ranges.
         */
        @Deprecated(
            "Use predictiveCacheMapsOptionsList",
            ReplaceWith("predictiveCacheMapsOptionsList(listOf(predictiveCacheMapsOptions))")
        )
        fun predictiveCacheMapsOptions(
            predictiveCacheMapsOptions: PredictiveCacheMapsOptions
        ): Builder = predictiveCacheMapsOptionsList(listOf(predictiveCacheMapsOptions))

        /**
         * List of predictive cache Maps related options ([PredictiveCacheMapsOptions]).
         * Use this instead of [predictiveCacheMapsOptions] so that you can specify different
         * [PredictiveCacheLocationOptions]s for different zoom level ranges.
         *
         * @throws IllegalArgumentException if [predictiveCacheMapsOptionsList] is empty.
         */
        @Throws(IllegalArgumentException::class)
        fun predictiveCacheMapsOptionsList(
            predictiveCacheMapsOptionsList: List<PredictiveCacheMapsOptions>
        ): Builder = apply {
            if (predictiveCacheMapsOptionsList.isEmpty()) {
                throw IllegalArgumentException("predictiveCacheMapsOptionsList must not be empty")
            }
            this.predictiveCacheMapsOptionsList = predictiveCacheMapsOptionsList.toList()
        }

        /**
         * Build [PredictiveCacheOptions].
         */
        fun build(): PredictiveCacheOptions = PredictiveCacheOptions(
            predictiveCacheNavigationOptions,
            predictiveCacheMapsOptionsList
        )
    }
}
