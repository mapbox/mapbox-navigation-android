package com.mapbox.navigation.base.options

/**
 * PredictiveCacheOptions
 *
 * @param predictiveCacheNavigationOptions [PredictiveCacheNavigationOptions] Predictive cache Navigation related options.
 * @param predictiveCacheMapsOptionsList List of predictive cache Maps related options ([PredictiveCacheMapsOptions]).
 * @param predictiveCacheSearchOptionsList List of predictive cache Search related options ([PredictiveCacheSearchOptions]).
 */
class PredictiveCacheOptions private constructor(
    val predictiveCacheNavigationOptions: PredictiveCacheNavigationOptions,
    val predictiveCacheMapsOptionsList: List<PredictiveCacheMapsOptions>,
    val predictiveCacheSearchOptionsList: List<PredictiveCacheSearchOptions>?,
) {

    /**
     * Get a builder to customize a subset of current options.
     */
    fun toBuilder(): Builder = Builder().apply {
        predictiveCacheNavigationOptions(predictiveCacheNavigationOptions)
        predictiveCacheMapsOptionsList(predictiveCacheMapsOptionsList)

        predictiveCacheSearchOptionsList?.let {
            predictiveCacheSearchOptionsList(it)
        }
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
        return predictiveCacheSearchOptionsList == other.predictiveCacheSearchOptionsList
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun hashCode(): Int {
        var result = predictiveCacheNavigationOptions.hashCode()
        result = 31 * result + predictiveCacheMapsOptionsList.hashCode()
        result = 31 * result + predictiveCacheSearchOptionsList.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "PredictiveCacheOptions(" +
            "predictiveCacheNavigationOptions=$predictiveCacheNavigationOptions, " +
            "predictiveCacheMapsOptionsList=$predictiveCacheMapsOptionsList," +
            "predictiveCacheSearchOptionsList=$predictiveCacheSearchOptionsList" +
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

        private var predictiveCacheSearchOptionsList:
            List<PredictiveCacheSearchOptions>? = null

        /**
         * Predictive cache Navigation related options.
         */
        fun predictiveCacheNavigationOptions(
            predictiveCacheNavigationOptions: PredictiveCacheNavigationOptions,
        ): Builder = apply {
            this.predictiveCacheNavigationOptions = predictiveCacheNavigationOptions
        }

        /**
         * List of predictive cache Maps related options ([PredictiveCacheMapsOptions]).
         *
         * @throws IllegalArgumentException if [predictiveCacheMapsOptionsList] is empty.
         */
        @Throws(IllegalArgumentException::class)
        fun predictiveCacheMapsOptionsList(
            predictiveCacheMapsOptionsList: List<PredictiveCacheMapsOptions>,
        ): Builder = apply {
            if (predictiveCacheMapsOptionsList.isEmpty()) {
                throw IllegalArgumentException("predictiveCacheMapsOptionsList must not be empty")
            }
            this.predictiveCacheMapsOptionsList = predictiveCacheMapsOptionsList.toList()
        }

        /**
         * List of predictive cache search related options ([PredictiveCacheSearchOptions]).
         *
         * @throws IllegalArgumentException if [predictiveCacheSearchOptionsList] is empty.
         */
        @Throws(IllegalArgumentException::class)
        fun predictiveCacheSearchOptionsList(
            predictiveCacheSearchOptionsList:
                List<PredictiveCacheSearchOptions>,
        ): Builder = apply {
            if (predictiveCacheSearchOptionsList.isEmpty()) {
                throw IllegalArgumentException(
                    "predictiveCacheSearchOptionsList must not be empty",
                )
            }
            this.predictiveCacheSearchOptionsList =
                predictiveCacheSearchOptionsList.toList()
        }

        /**
         * Build [PredictiveCacheOptions].
         */
        fun build(): PredictiveCacheOptions = PredictiveCacheOptions(
            predictiveCacheNavigationOptions,
            predictiveCacheMapsOptionsList,
            predictiveCacheSearchOptionsList,
        )
    }
}
