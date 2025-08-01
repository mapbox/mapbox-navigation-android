package com.mapbox.navigation.base.options

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI

/**
 * Predictive cache Navigation related options.
 *
 * @param tilesDataset String built out of `<account>[.<graph>]` variables. Account can be
 * `mapbox` for default datasets or your username for other.
 * @param tilesVersion Tiles version.
 * @param includeAdas Whether to include ADAS tiles.
 * @param predictiveCacheLocationOptions [PredictiveCacheLocationOptions] location configuration for guidance predictive caching
 *
 * Note: With the current implementation, if [includeAdas] is true,
 * [tilesDataset] and [tilesVersion] must not be null. Additionally, if either
 * [tilesDataset] or [tilesVersion] is specified, both must be provided; neither can be null.
 */
@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class PredictiveCacheNavigationOptions private constructor(
    @ExperimentalPreviewMapboxNavigationAPI
    val tilesDataset: String?,
    @ExperimentalPreviewMapboxNavigationAPI
    val tilesVersion: String?,
    @ExperimentalPreviewMapboxNavigationAPI
    val includeAdas: Boolean,
    val predictiveCacheLocationOptions: PredictiveCacheLocationOptions,
) {

    init {
        if (includeAdas) {
            checkNotNull(tilesDataset) {
                "tilesDataset can't be null if ADAS tiles included"
            }

            checkNotNull(tilesVersion) {
                "tilesVersion can't be null if ADAS tiles included"
            }
        }

        if (tilesDataset != null) {
            checkNotNull(tilesVersion) {
                "tilesVersion must be specified explicitly if tilesDataset specified"
            }
        }

        if (tilesVersion != null) {
            checkNotNull(tilesDataset) {
                "tilesDataset must be specified explicitly if tilesVersion specified"
            }
        }
    }

    /**
     * Get a builder to customize a subset of current options.
     */
    fun toBuilder(): Builder = Builder().apply {
        if (tilesDataset != null && tilesVersion != null) {
            tilesConfiguration(
                tilesDataset = tilesDataset,
                tilesVersion = tilesVersion,
                includeAdas = includeAdas,
            )
        }
        predictiveCacheLocationOptions(predictiveCacheLocationOptions)
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PredictiveCacheNavigationOptions

        if (includeAdas != other.includeAdas) return false
        if (tilesDataset != other.tilesDataset) return false
        if (tilesVersion != other.tilesVersion) return false
        if (predictiveCacheLocationOptions != other.predictiveCacheLocationOptions) return false

        return true
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun hashCode(): Int {
        var result = includeAdas.hashCode()
        result = 31 * result + (tilesDataset?.hashCode() ?: 0)
        result = 31 * result + (tilesVersion?.hashCode() ?: 0)
        result = 31 * result + predictiveCacheLocationOptions.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "PredictiveCacheNavigationOptions(" +
            "tilesDataset=$tilesDataset, " +
            "tilesVersion=$tilesVersion, " +
            "includeAdas=$includeAdas, " +
            "predictiveCacheLocationOptions=$predictiveCacheLocationOptions," +
            ")"
    }

    /**
     * Build a new [PredictiveCacheNavigationOptions].
     */
    class Builder {

        private var tilesDataset: String? = null
        private var tilesVersion: String? = null
        private var includeAdas: Boolean = false
        private var predictiveCacheLocationOptions: PredictiveCacheLocationOptions? = null

        /**
         * Location configuration for guidance predictive caching
         */
        fun predictiveCacheLocationOptions(
            predictiveCacheLocationOptions: PredictiveCacheLocationOptions,
        ): Builder = apply { this.predictiveCacheLocationOptions = predictiveCacheLocationOptions }

        /**
         * Tileset configuration.
         *
         * @param tilesDataset String built out of `<account>[.<graph>]` variables. Account can be
         * `mapbox` for default datasets or your username for other.
         * @param tilesVersion Tiles version.
         * @param includeAdas Whether to include ADAS tiles.
         *
         * Note: With the current implementation, if [includeAdas] is true,
         * [tilesDataset] and [tilesVersion] must not be null. Additionally, if either
         * [tilesDataset] or [tilesVersion] is specified, both must be provided; neither can be null.
         */
        @ExperimentalPreviewMapboxNavigationAPI
        fun tilesConfiguration(
            tilesDataset: String,
            tilesVersion: String,
            includeAdas: Boolean,
        ): Builder = apply {
            this.tilesDataset = tilesDataset
            this.tilesVersion = tilesVersion
            this.includeAdas = includeAdas
        }

        /**
         * Build [PredictiveCacheNavigationOptions].
         */
        fun build(): PredictiveCacheNavigationOptions = PredictiveCacheNavigationOptions(
            tilesDataset = tilesDataset,
            tilesVersion = tilesVersion,
            includeAdas = includeAdas,
            predictiveCacheLocationOptions = predictiveCacheLocationOptions
                ?: PredictiveCacheLocationOptions.Builder().build(),
        )
    }
}
