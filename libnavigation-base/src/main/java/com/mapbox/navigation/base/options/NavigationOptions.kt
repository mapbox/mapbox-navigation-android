package com.mapbox.navigation.base.options

import com.mapbox.navigation.base.formatter.DistanceFormatter
import com.mapbox.navigation.base.typedef.NONE_SPECIFIED
import com.mapbox.navigation.base.typedef.ROUNDING_INCREMENT_FIFTY
import com.mapbox.navigation.base.typedef.RoundingIncrement
import com.mapbox.navigation.base.typedef.TimeFormatType

/**
 * Default navigator approximate prediction in milliseconds
 *
 * This value will be used to offset the time at which the current location was calculated
 * in such a way as to project the location forward along the current trajectory so as to
 * appear more in sync with the users ground-truth location
 */
const val DEFAULT_NAVIGATOR_PREDICTION_MILLIS = 1100L

/**
 * Defines navigation options
 *
 * @param roundingIncrement defines the increment displayed on the instruction view
 * @param timeFormatType defines time format for calculation remaining trip time
 * @param navigatorPredictionMillis defines approximate navigator prediction in milliseconds
 *
 * This value will be used to offset the time at which the current location was calculated
 * in such a way as to project the location forward along the current trajectory so as to
 * appear more in sync with the users ground-truth location
 *
 * @param distanceFormatter [DistanceFormatter] for format distances showing in notification during navigation
 * @param onboardRouterConfig [MapboxOnboardRouterConfig] defines configuration for the default on-board router
 * @param isFromNavigationUi Boolean *true* if is called from UI, otherwise *false*
 * @param isDebugLoggingEnabled Boolean
 */
data class NavigationOptions constructor(
    @RoundingIncrement val roundingIncrement: Int,
    @TimeFormatType val timeFormatType: Int,
    val navigatorPredictionMillis: Long,
    val distanceFormatter: DistanceFormatter?,
    val onboardRouterConfig: MapboxOnboardRouterConfig?,
    val isFromNavigationUi: Boolean = false,
    val isDebugLoggingEnabled: Boolean = false
) {

    /**
     * Get a builder to customize a subset of current options.
     */
    fun toBuilder() = Builder()
        .roundingIncrement(roundingIncrement)
        .timeFormatType(timeFormatType)
        .navigatorPredictionMillis(navigatorPredictionMillis)
        .distanceFormatter(distanceFormatter)
        .onboardRouterConfig(onboardRouterConfig)
        .isFromNavigationUi(isFromNavigationUi)

    /**
     * Build a new [NavigationOptions]
     */
    class Builder {
        private var roundingIncrement: Int = ROUNDING_INCREMENT_FIFTY
        private var timeFormatType: Int = NONE_SPECIFIED
        private var navigatorPredictionMillis: Long = DEFAULT_NAVIGATOR_PREDICTION_MILLIS
        private var distanceFormatter: DistanceFormatter? = null
        private var onboardRouterConfig: MapboxOnboardRouterConfig? = null
        private var isFromNavigationUi: Boolean = false
        private var isDebugLoggingEnabled: Boolean = false

        /**
         * Defines the increment displayed on the instruction view
         */
        fun roundingIncrement(roundingIncrement: Int) =
            apply { this.roundingIncrement = roundingIncrement }

        /**
         * Defines time format for calculation remaining trip time
         */
        fun timeFormatType(type: Int) =
            apply { this.timeFormatType = type }

        /**
         * Defines approximate navigator prediction in milliseconds
         */
        fun navigatorPredictionMillis(predictionMillis: Long) =
            apply { navigatorPredictionMillis = predictionMillis }

        /**
         *  Defines format distances showing in notification during navigation
         */
        fun distanceFormatter(distanceFormatter: DistanceFormatter?) =
            apply { this.distanceFormatter = distanceFormatter }

        /**
         * Defines configuration for the default on-board router
         */
        fun onboardRouterConfig(onboardRouterConfig: MapboxOnboardRouterConfig?) =
            apply { this.onboardRouterConfig = onboardRouterConfig }

        /**
         * Defines if the builder instance is created from the Navigation UI
         */
        fun isFromNavigationUi(flag: Boolean) =
            apply { this.isFromNavigationUi = flag }

        /**
         * Defines if debug logging is enabled
         */
        fun isDebugLoggingEnabled(flag: Boolean) =
            apply { this.isDebugLoggingEnabled = flag }

        /**
         * Build a new instance of [NavigationOptions]
         * @return NavigationOptions
         */
        fun build(): NavigationOptions {
            return NavigationOptions(
                roundingIncrement = roundingIncrement,
                timeFormatType = timeFormatType,
                navigatorPredictionMillis = navigatorPredictionMillis,
                distanceFormatter = distanceFormatter,
                onboardRouterConfig = onboardRouterConfig,
                isFromNavigationUi = isFromNavigationUi,
                isDebugLoggingEnabled = isDebugLoggingEnabled
            )
        }
    }
}
