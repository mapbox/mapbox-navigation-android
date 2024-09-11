package com.mapbox.navigation.base.options

import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.internal.utils.Constants

/**
 * Traffic override related config
 *
 * In order to update traffic it is necessary to request routes with specific [RouteOptions].
 * At a minimum the following options are necessary:
 *
 * ```kotlin
 *  val routeOptions = RouteOptions.builder()
 *      .baseUrl(Constants.BASE_API_URL)
 *      .user(Constants.MAPBOX_USER)
 *      .profile(DirectionsCriteria.PROFILE_DRIVING_TRAFFIC)
 *      .overview(DirectionsCriteria.OVERVIEW_FULL)
 *      .steps(true)
 *      .annotationsList(
 *          listOf(
 *              DirectionsCriteria.ANNOTATION_CONGESTION_NUMERIC,
 *              DirectionsCriteria.ANNOTATION_MAXSPEED,
 *              DirectionsCriteria.ANNOTATION_DISTANCE,
 *          )
 *      )
 *      .coordinatesList(listOf(origin, destination))
 *      .build()
 * ```
 * A good starting point might be RouteOptions.Builder.applyDefaultNavigationOptions() which will
 * include the options above.
 *
 * @param isEnabled defines if traffic override is enabled
 * @param highSpeedThresholdInKmPerHour defines what speed should be treated as a high speed
 * @param lowCongestionRange the range for low congestion traffic
 * @param moderateCongestionRange the range for moderate congestion traffic
 * @param heavyCongestionRange the range for heavy congestion traffic
 * @param severeCongestionRange the range for severe congestion traffic
 */
@ExperimentalPreviewMapboxNavigationAPI
class TrafficOverrideOptions private constructor(
    val isEnabled: Boolean,
    val highSpeedThresholdInKmPerHour: Int,
    val lowCongestionRange: IntRange,
    val moderateCongestionRange: IntRange,
    val heavyCongestionRange: IntRange,
    val severeCongestionRange: IntRange,
) {

    /**
     * Get a builder to customize a subset of current options.
     */
    fun toBuilder(): Builder = Builder().apply {
        isEnabled(isEnabled)
        highSpeedThresholdInKmPerHour(highSpeedThresholdInKmPerHour)
        lowCongestionRange(lowCongestionRange)
        moderateCongestionRange(moderateCongestionRange)
        heavyCongestionRange(heavyCongestionRange)
        severeCongestionRange(severeCongestionRange)
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TrafficOverrideOptions

        if (isEnabled != other.isEnabled) return false
        if (highSpeedThresholdInKmPerHour != other.highSpeedThresholdInKmPerHour) return false
        if (lowCongestionRange != other.lowCongestionRange) return false
        if (moderateCongestionRange != other.moderateCongestionRange) return false
        if (heavyCongestionRange != other.heavyCongestionRange) return false
        if (severeCongestionRange != other.severeCongestionRange) return false

        return true
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun hashCode(): Int {
        var result = isEnabled.hashCode()
        result = 31 * result + highSpeedThresholdInKmPerHour.hashCode()
        result = 31 * result + lowCongestionRange.hashCode()
        result = 31 * result + moderateCongestionRange.hashCode()
        result = 31 * result + heavyCongestionRange.hashCode()
        result = 31 * result + severeCongestionRange.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "TrafficOverrideOptions(" +
            "isEnabled=$isEnabled," +
            "highSpeedThresholdInKmPerHour=$highSpeedThresholdInKmPerHour" +
            "lowCongestionRange=$lowCongestionRange" +
            "moderateCongestionRange=$moderateCongestionRange" +
            "heavyCongestionRange=$heavyCongestionRange" +
            "severeCongestionRange=$severeCongestionRange" +
            ")"
    }

    /**
     * Build a new [TrafficOverrideOptions]
     */
    class Builder {
        private var isEnabled: Boolean = DEFAULT_IS_ENABLED
        private var lowCongestionRange: IntRange = Constants.CongestionRange.LOW_CONGESTION_RANGE
        private var moderateCongestionRange: IntRange =
            Constants.CongestionRange.MODERATE_CONGESTION_RANGE
        private var heavyCongestionRange: IntRange =
            Constants.CongestionRange.HEAVY_CONGESTION_RANGE
        private var severeCongestionRange: IntRange =
            Constants.CongestionRange.SEVERE_CONGESTION_RANGE
        private var highSpeedThresholdInKmPerHour: Int = DEFAULT_HIGH_SPEED_THRESHOLD_IN_KM_PER_HOUR

        /**
         * Defines if traffic override is enabled
         */
        fun isEnabled(flag: Boolean): Builder =
            apply { this.isEnabled = flag }

        /**
         * Defines what speed should be treated as a high speed
         */
        fun highSpeedThresholdInKmPerHour(value: Int): Builder =
            apply { highSpeedThresholdInKmPerHour = value }

        /**
         * Defines the range for low congestion traffic
         */
        fun lowCongestionRange(range: IntRange): Builder =
            apply { lowCongestionRange = range }

        /**
         * Defines the range for moderate congestion traffic
         */
        fun moderateCongestionRange(range: IntRange): Builder =
            apply { moderateCongestionRange = range }

        /**
         * Defines the range for heavy congestion traffic
         */
        fun heavyCongestionRange(range: IntRange): Builder =
            apply { heavyCongestionRange = range }

        /**
         * Defines the range for severe congestion traffic
         */
        fun severeCongestionRange(range: IntRange): Builder =
            apply { severeCongestionRange = range }

        /**
         * Build the [TrafficOverrideOptions]
         */
        fun build(): TrafficOverrideOptions {
            return TrafficOverrideOptions(
                isEnabled = isEnabled,
                highSpeedThresholdInKmPerHour = highSpeedThresholdInKmPerHour,
                lowCongestionRange = lowCongestionRange,
                moderateCongestionRange = moderateCongestionRange,
                heavyCongestionRange = heavyCongestionRange,
                severeCongestionRange = severeCongestionRange,
            )
        }

        private companion object {
            private const val DEFAULT_IS_ENABLED = false
            private const val DEFAULT_HIGH_SPEED_THRESHOLD_IN_KM_PER_HOUR = 80
        }
    }
}
