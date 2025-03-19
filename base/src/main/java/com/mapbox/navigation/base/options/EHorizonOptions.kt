package com.mapbox.navigation.base.options

import com.mapbox.navigation.base.trip.model.eh.EHorizon

/**
 * Defines options for [EHorizon].
 *
 * **NOTE**: The Mapbox Electronic Horizon feature of the Mapbox Navigation SDK is in public beta
 * and is subject to changes, including its pricing. Use of the feature is subject to the beta
 * product restrictions in the Mapbox Terms of Service.
 * Mapbox reserves the right to eliminate any free tier or free evaluation offers at any time and
 * require customers to place an order to purchase the Mapbox Electronic Horizon feature,
 * regardless of the level of use of the feature.
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
 * @param alertServiceOptions control how Navigation SDK extracts road objects from the road graph and which objects are collected.
 * @param enableEnhancedDataAlongEH controls access to enhanced data attributes (from ADAS data layer).
 * Note: using enhanced data will require additional resources: bandwidth, disk space, and RAM.
 * Note: regardless whether the flag is set, enhanced data will be accessible if ADASIS callback is set.
 * Default value is false.
 */
class EHorizonOptions private constructor(
    val length: Double,
    val expansion: Int,
    val branchLength: Double,
    val minTimeDeltaBetweenUpdates: Double?,
    val alertServiceOptions: AlertServiceOptions,
    val enableEnhancedDataAlongEH: Boolean,
) {

    /**
     * Get a builder to customize a subset of current options.
     */
    fun toBuilder(): Builder = Builder().apply {
        length(length)
        expansion(expansion)
        branchLength(branchLength)
        minTimeDeltaBetweenUpdates(minTimeDeltaBetweenUpdates)
        alertServiceOptions(alertServiceOptions)
        enableEnhancedDataAlongEH(enableEnhancedDataAlongEH)
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
        if (alertServiceOptions != other.alertServiceOptions) return false
        if (enableEnhancedDataAlongEH != other.enableEnhancedDataAlongEH) return false

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
        result = 31 * result + alertServiceOptions.hashCode()
        result = 31 * result + enableEnhancedDataAlongEH.hashCode()
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
            "minTimeDeltaBetweenUpdates=$minTimeDeltaBetweenUpdates, " +
            "alertServiceOptions=$alertServiceOptions, " +
            "enableEnhancedDataAlongEH=$enableEnhancedDataAlongEH" +
            ")"
    }

    /**
     * Build a new [EHorizonOptions]
     */
    class Builder {

        private var length: Double = DEFAULT_LENGTH
        private var expansion: Int = DEFAULT_EXPANSION
        private var branchLength: Double = DEFAULT_BRANCH_LENGTH
        private var minTimeDeltaBetweenUpdates: Double? = DEFAULT_MIN_DELTA
        private var alertServiceOptions: AlertServiceOptions = AlertServiceOptions.Builder().build()
        private var enableEnhancedDataAlongEH: Boolean = DEFAULT_ENABLE_ENHANCED_DATA_ALONG_EH

        /**
         * Override the minimum length of the EHorizon ahead of the current position.
         * If negative exception will be thrown.
         */
        fun length(length: Double): Builder =
            apply {
                if (length < 0) {
                    throw IllegalArgumentException(
                        "EHorizonOptions.length can't be negative.",
                    )
                }
                this.length = length
            }

        /**
         * Override the number of levels to expand.
         * If negative exception will be thrown.
         */
        fun expansion(expansion: Int): Builder =
            apply {
                if (expansion < 0) {
                    throw IllegalArgumentException(
                        "EHorizonOptions.expansion can't be negative.",
                    )
                }
                this.expansion = expansion
            }

        /**
         * Override the minimum length to expand branches.
         * If negative exception will be thrown.
         */
        fun branchLength(branchLength: Double): Builder =
            apply {
                if (branchLength < 0) {
                    throw IllegalArgumentException(
                        "EHorizonOptions.branchLength can't be negative.",
                    )
                }
                this.branchLength = branchLength
            }

        /**
         * Override the minimum time delta between EHorizon updates.
         * If negative exception will be thrown.
         */
        fun minTimeDeltaBetweenUpdates(minTimeDeltaBetweenUpdates: Double?): Builder =
            apply {
                minTimeDeltaBetweenUpdates?.let {
                    if (it < 0) {
                        throw IllegalArgumentException(
                            "EHorizonOptions.minTimeDeltaBetweenUpdates can't be negative.",
                        )
                    }
                }
                this.minTimeDeltaBetweenUpdates = minTimeDeltaBetweenUpdates
            }

        /**
         * Control how Navigation SDK extracts road objects from the road graph and which objects are collected.
         */
        fun alertServiceOptions(alertServiceOptions: AlertServiceOptions): Builder =
            apply {
                this.alertServiceOptions = alertServiceOptions
            }

        /**
         * Control access to enhanced data attributes (from ADAS data layer).
         * Note: using enhanced data will require additional resources: bandwidth, disk space, and RAM.
         * Note: regardless whether the flag is set, enhanced data will be accessible if ADASIS callback is set.
         * Default value is false.
         */
        fun enableEnhancedDataAlongEH(enableEnhancedDataAlongEH: Boolean): Builder =
            apply {
                this.enableEnhancedDataAlongEH = enableEnhancedDataAlongEH
            }

        /**
         * Build the [EHorizonOptions]
         */
        fun build(): EHorizonOptions {
            return EHorizonOptions(
                length = length,
                expansion = expansion,
                branchLength = branchLength,
                minTimeDeltaBetweenUpdates = minTimeDeltaBetweenUpdates,
                alertServiceOptions = alertServiceOptions,
                enableEnhancedDataAlongEH = enableEnhancedDataAlongEH,
            )
        }

        private companion object {
            private const val DEFAULT_LENGTH = 500.0
            private const val DEFAULT_EXPANSION = 0
            private const val DEFAULT_BRANCH_LENGTH = 50.0
            private val DEFAULT_MIN_DELTA = null
            private const val DEFAULT_ENABLE_ENHANCED_DATA_ALONG_EH = false
        }
    }
}
