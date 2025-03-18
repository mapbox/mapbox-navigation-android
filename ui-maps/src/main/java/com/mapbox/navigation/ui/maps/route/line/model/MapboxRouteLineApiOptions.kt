package com.mapbox.navigation.ui.maps.route.line.model

import android.util.Log
import androidx.annotation.Keep
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.internal.utils.Constants
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants

/**
 * Options for configuration of [MapboxRouteLineApi].
 *
 * @param lowCongestionRange the range for low congestion traffic, should be aligned with [TrafficOverrideOptions.lowCongestionRange]
 * @param moderateCongestionRange the range for moderate congestion traffic, should be aligned with [TrafficOverrideOptions.moderateCongestionRange]
 * @param heavyCongestionRange the range for heavy congestion traffic, should be aligned with [TrafficOverrideOptions.heavyCongestionRange]
 * @param severeCongestionRange the range for severe congestion traffic, should be aligned with [TrafficOverrideOptions.severeCongestionRange]
 * @param trafficBackfillRoadClasses for map styles that have been configured to substitute the low
 * traffic congestion color for unknown traffic conditions for specified road classes, the same
 * road classes can be specified here to make the same substitution on the route line
 * @param calculateRestrictedRoadSections indicates if the route line will display restricted
 * road sections with a dashed line. Note that in order for restricted sections to be displayed,
 * you also need to set [MapboxRouteLineViewOptions.displayRestrictedRoadSections] to true
 * for those views that will display restricted sections. Set [MapboxRouteLineApiOptions.calculateRestrictedRoadSections]
 * to true if at least one of your views will display them.
 * @param styleInactiveRouteLegsIndependently enabling this feature will change the color of the route
 * legs that aren't currently being navigated. See [RouteLineColorResources] to specify the color
 * used.
 * **Enabling this feature when [vanishingRouteLineEnabled] is true can have negative performance implications, especially for long routes.**
 * @param vanishingRouteLineEnabled indicates if the vanishing route line feature is enabled
 * @param vanishingRouteLineUpdateIntervalNano can be used to decrease the frequency of the vanishing route
 * line updates improving the performance at the expense of visual appearance of the vanishing point on the line during navigation.
 * @param isRouteCalloutsEnabled indicates if route callout feature is enabled so [MapboxRouteLineApi] will calculate data for it
 */
@Keep
@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class MapboxRouteLineApiOptions private constructor(
    val lowCongestionRange: IntRange,
    val moderateCongestionRange: IntRange,
    val heavyCongestionRange: IntRange,
    val severeCongestionRange: IntRange,
    val trafficBackfillRoadClasses: List<String>,
    val calculateRestrictedRoadSections: Boolean,
    val styleInactiveRouteLegsIndependently: Boolean,
    val vanishingRouteLineEnabled: Boolean,
    val vanishingRouteLineUpdateIntervalNano: Long,
    @ExperimentalPreviewMapboxNavigationAPI
    val isRouteCalloutsEnabled: Boolean,
) {

    /**
     * Creates a builder matching this instance.
     */
    fun toBuilder(): Builder {
        return Builder()
            .lowCongestionRange(lowCongestionRange)
            .moderateCongestionRange(moderateCongestionRange)
            .heavyCongestionRange(heavyCongestionRange)
            .severeCongestionRange(severeCongestionRange)
            .trafficBackfillRoadClasses(trafficBackfillRoadClasses)
            .calculateRestrictedRoadSections(calculateRestrictedRoadSections)
            .styleInactiveRouteLegsIndependently(styleInactiveRouteLegsIndependently)
            .vanishingRouteLineEnabled(vanishingRouteLineEnabled)
            .vanishingRouteLineUpdateIntervalNano(vanishingRouteLineUpdateIntervalNano)
            .isRouteCalloutsEnabled(isRouteCalloutsEnabled)
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MapboxRouteLineApiOptions

        if (lowCongestionRange != other.lowCongestionRange) return false
        if (moderateCongestionRange != other.moderateCongestionRange) return false
        if (heavyCongestionRange != other.heavyCongestionRange) return false
        if (severeCongestionRange != other.severeCongestionRange) return false
        if (trafficBackfillRoadClasses != other.trafficBackfillRoadClasses) return false
        if (calculateRestrictedRoadSections != other.calculateRestrictedRoadSections) return false
        if (styleInactiveRouteLegsIndependently != other.styleInactiveRouteLegsIndependently) {
            return false
        }
        if (vanishingRouteLineEnabled != other.vanishingRouteLineEnabled) return false
        if (vanishingRouteLineUpdateIntervalNano != other.vanishingRouteLineUpdateIntervalNano) {
            return false
        }
        if (isRouteCalloutsEnabled != other.isRouteCalloutsEnabled) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = lowCongestionRange.hashCode()
        result = 31 * result + moderateCongestionRange.hashCode()
        result = 31 * result + heavyCongestionRange.hashCode()
        result = 31 * result + severeCongestionRange.hashCode()
        result = 31 * result + trafficBackfillRoadClasses.hashCode()
        result = 31 * result + calculateRestrictedRoadSections.hashCode()
        result = 31 * result + styleInactiveRouteLegsIndependently.hashCode()
        result = 31 * result + vanishingRouteLineEnabled.hashCode()
        result = 31 * result + vanishingRouteLineUpdateIntervalNano.hashCode()
        result = 31 * result + isRouteCalloutsEnabled.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "MapboxRouteLineApiOptions(" +
            "lowCongestionRange=$lowCongestionRange, " +
            "moderateCongestionRange=$moderateCongestionRange, " +
            "heavyCongestionRange=$heavyCongestionRange, " +
            "severeCongestionRange=$severeCongestionRange, " +
            "trafficBackfillRoadClasses=$trafficBackfillRoadClasses, " +
            "calculateRestrictedRoadSections=$calculateRestrictedRoadSections, " +
            "styleInactiveRouteLegsIndependently=$styleInactiveRouteLegsIndependently, " +
            "vanishingRouteLineEnabled=$vanishingRouteLineEnabled, " +
            "vanishingRouteLineUpdateIntervalNano=$vanishingRouteLineUpdateIntervalNano" +
            "isRouteCalloutsEnabled=$isRouteCalloutsEnabled" +
            ")"
    }

    /**
     * A builder used to create instance of [MapboxRouteLineApiOptions].
     */
    class Builder {

        private var lowCongestionRange: IntRange = Constants.CongestionRange.LOW_CONGESTION_RANGE
        private var moderateCongestionRange: IntRange =
            Constants.CongestionRange.MODERATE_CONGESTION_RANGE
        private var heavyCongestionRange: IntRange =
            Constants.CongestionRange.HEAVY_CONGESTION_RANGE
        private var severeCongestionRange: IntRange =
            Constants.CongestionRange.SEVERE_CONGESTION_RANGE
        private var trafficBackfillRoadClasses: List<String> = emptyList()
        private var vanishingRouteLineEnabled: Boolean = false
        private var calculateRestrictedRoadSections = false
        private var styleInactiveRouteLegsIndependently: Boolean = false
        private var isRouteCalloutsEnabled: Boolean = false
        private var vanishingRouteLineUpdateIntervalNano: Long =
            RouteLayerConstants.DEFAULT_VANISHING_POINT_MIN_UPDATE_INTERVAL_NANO

        /**
         * The range for low traffic congestion
         *
         * @param range the range to be used
         *
         * @return the builder
         * @throws IllegalArgumentException if the range includes values less than 0 or greater than 100
         */
        @Throws(IllegalArgumentException::class)
        fun lowCongestionRange(range: IntRange): Builder {
            if (!congestionRangeInBounds(range)) {
                throw IllegalArgumentException(
                    "Ranges containing values less than 0 or greater than 100 are invalid.",
                )
            } else {
                this.lowCongestionRange = range
            }

            return this
        }

        /**
         * The range for moderate traffic congestion
         *
         * @param range the range to be used
         *
         * @return the builder
         * @throws IllegalArgumentException if the range includes values less than 0 or greater than 100
         */
        @Throws(IllegalArgumentException::class)
        fun moderateCongestionRange(range: IntRange): Builder {
            if (!congestionRangeInBounds(range)) {
                throw IllegalArgumentException(
                    "Ranges containing values less than 0 or greater than 100 are invalid.",
                )
            } else {
                this.moderateCongestionRange = range
            }
            return this
        }

        /**
         * The range for heavy traffic congestion
         *
         * @param range the range to be used
         *
         * @return the builder
         * @throws IllegalArgumentException if the range includes values less than 0 or greater than 100
         */
        @Throws(IllegalArgumentException::class)
        fun heavyCongestionRange(range: IntRange): Builder {
            if (!congestionRangeInBounds(range)) {
                throw IllegalArgumentException(
                    "Ranges containing values less than 0 or greater than 100 are invalid.",
                )
            } else {
                this.heavyCongestionRange = range
            }

            return this
        }

        /**
         * The range for severe traffic congestion
         *
         * @param range the range to be used
         *
         * @return the builder
         * @throws IllegalArgumentException if the range includes values less than 0 or greater than 100
         */
        @Throws(IllegalArgumentException::class)
        fun severeCongestionRange(range: IntRange): Builder {
            if (!congestionRangeInBounds(range)) {
                throw IllegalArgumentException(
                    "Ranges containing values less than 0 or greater than 100 are invalid.",
                )
            } else {
                this.severeCongestionRange = range
            }
            return this
        }

        /**
         * For map styles that have been configured to substitute the low
         * traffic congestion color for unknown traffic conditions for specified road classes, the same
         * road classes can be specified here to make the same substitution on the route line.
         *
         * @param roadClasses a list of roadClasses
         * @return the builder
         */
        fun trafficBackfillRoadClasses(roadClasses: List<String>): Builder =
            apply { this.trafficBackfillRoadClasses = roadClasses }

        /**
         * Indicates if the vanishing route line feature is enabled.
         *
         * @param isEnabled whether vanishing route line is enabled
         * @return the builder
         */
        fun vanishingRouteLineEnabled(isEnabled: Boolean): Builder =
            apply { this.vanishingRouteLineEnabled = isEnabled }

        /**
         * Indicates if the route line will display restricted
         * road sections with a dashed line. Note that in order for restricted sections to be displayed,
         * you also need to set [MapboxRouteLineViewOptions.displayRestrictedRoadSections] to true
         * for those views that will display restricted sections. Set [MapboxRouteLineApiOptions.calculateRestrictedRoadSections]
         * to true if at least one of your views will display them.
         *
         * @param calculateRestrictedRoadSections whether the data for restricted sections should be calculated
         * @return the builder
         */
        fun calculateRestrictedRoadSections(calculateRestrictedRoadSections: Boolean): Builder =
            apply { this.calculateRestrictedRoadSections = calculateRestrictedRoadSections }

        /**
         * Enabling this feature will change the color of the route
         * legs that aren't currently being navigated. See [RouteLineColorResources] to specify the color
         * used.
         * **Enabling this feature when [vanishingRouteLineEnabled] is true can have negative performance implications, especially for long routes.**
         *
         * @param enable whether inactive legs should be styled independently
         * @return the builder
         */
        fun styleInactiveRouteLegsIndependently(enable: Boolean): Builder =
            apply { this.styleInactiveRouteLegsIndependently = enable }

        /**
         * When enabled [MapboxRouteLineApi] calculates data for route callouts.
         *
         * @param enable whether route callout feature is enabled
         * @return the builder
         */
        @ExperimentalPreviewMapboxNavigationAPI
        fun isRouteCalloutsEnabled(enable: Boolean): Builder =
            apply { this.isRouteCalloutsEnabled = enable }

        /**
         * Can be used to decrease the frequency of the vanishing route
         * line updates improving the performance at the expense of visual appearance
         * of the vanishing point on the line during navigation.
         *
         * @param interval vanishing route line update interval, in nanoseconds
         * @return the builder
         */
        fun vanishingRouteLineUpdateIntervalNano(
            interval: Long,
        ): Builder = apply { this.vanishingRouteLineUpdateIntervalNano = interval }

        /**
         * Builds an instance of [MapboxRouteLineApiOptions].
         *
         * @return an instance of [MapboxRouteLineApiOptions].
         * @throws IllegalStateException if congestion ranges overlap
         */
        @Throws(IllegalStateException::class)
        fun build(): MapboxRouteLineApiOptions {
            if (rangesOverlap()) {
                throw IllegalStateException(
                    "Traffic congestion ranges should not overlap each other.",
                )
            }
            return MapboxRouteLineApiOptions(
                lowCongestionRange,
                moderateCongestionRange,
                heavyCongestionRange,
                severeCongestionRange,
                trafficBackfillRoadClasses,
                calculateRestrictedRoadSections,
                styleInactiveRouteLegsIndependently,
                vanishingRouteLineEnabled,
                vanishingRouteLineUpdateIntervalNano,
                isRouteCalloutsEnabled,
            )
        }

        private fun rangesOverlap(): Boolean {
            val logTag = "Mbx${RouteLineColorResources::class.java.canonicalName}"
            return when {
                lowCongestionRange.intersect(moderateCongestionRange).isNotEmpty() -> {
                    Log.e(logTag, "Low and moderate ranges are overlapping.")
                    true
                }

                lowCongestionRange.intersect(heavyCongestionRange).isNotEmpty() -> {
                    Log.e(logTag, "Low and moderate ranges are overlapping.")
                    true
                }

                lowCongestionRange.intersect(severeCongestionRange).isNotEmpty() -> {
                    Log.e(logTag, "Low and severe ranges are overlapping.")
                    true
                }

                moderateCongestionRange.intersect(heavyCongestionRange).isNotEmpty() -> {
                    Log.e(logTag, "Moderate and heavy ranges are overlapping.")
                    true
                }

                moderateCongestionRange.intersect(severeCongestionRange).isNotEmpty() -> {
                    Log.e(logTag, "Moderate and severe ranges are overlapping.")
                    true
                }

                heavyCongestionRange.intersect(severeCongestionRange).isNotEmpty() -> {
                    Log.e(logTag, "Heavy and severe ranges are overlapping.")
                    true
                }

                else -> false
            }
        }
    }

    private companion object {
        private fun congestionRangeInBounds(range: IntRange): Boolean {
            return range.first >= 0 && range.last <= 100
        }
    }
}
