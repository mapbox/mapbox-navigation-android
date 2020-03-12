package com.mapbox.navigation.base.options

import com.mapbox.navigation.base.formatter.DistanceFormatter
import com.mapbox.navigation.base.typedef.NONE_SPECIFIED
import com.mapbox.navigation.base.typedef.ROUNDING_INCREMENT_FIFTY
import com.mapbox.navigation.base.typedef.RoundingIncrement
import com.mapbox.navigation.base.typedef.TimeFormatType

const val DEFAULT_NAVIGATOR_POLLING_DELAY = 1500L

/**
 * Defines navigation options
 *
 * @param roundingIncrement defines the increment displayed on the instruction view
 * @param timeFormatType defines time format for calculation remaining trip time
 * @param navigatorPollingDelay defines approximate location engine interval lag in milliseconds
 *
 * This value will be used to offset the time at which the current location was calculated
 * in such a way as to project the location forward along the current trajectory so as to
 * appear more in sync with the users ground-truth location
 *
 * @param fasterRouteDetectorInterval defines time interval in milliseconds for detection is faster route available
 * @param distanceFormatter [DistanceFormatter] for format distances showing in notification during navigation
 * @param onboardRouterConfig [MapboxOnboardRouterConfig] defines configuration for the default on-board router
 */
data class NavigationOptions constructor(
    @RoundingIncrement val roundingIncrement: Int,
    @TimeFormatType val timeFormatType: Int,
    val navigatorPollingDelay: Long,
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
        .navigatorPollingDelay(navigatorPollingDelay)
        .distanceFormatter(distanceFormatter)
        .onboardRouterConfig(onboardRouterConfig)
        .isFromNavigationUi(isFromNavigationUi)

    class Builder {
        private var roundingIncrement: Int = ROUNDING_INCREMENT_FIFTY
        private var timeFormatType: Int = NONE_SPECIFIED
        private var navigatorPollingDelay: Long = DEFAULT_NAVIGATOR_POLLING_DELAY
        private var distanceFormatter: DistanceFormatter? = null
        private var onboardRouterConfig: MapboxOnboardRouterConfig? = null
        private var isFromNavigationUi: Boolean = false
        private var isDebugLoggingEnabled: Boolean = false

        fun roundingIncrement(roundingIncrement: Int) =
            apply { this.roundingIncrement = roundingIncrement }

        fun timeFormatType(type: Int) =
            apply { this.timeFormatType = type }

        fun navigatorPollingDelay(pollingDelay: Long) =
            apply { navigatorPollingDelay = pollingDelay }

        fun distanceFormatter(distanceFormatter: DistanceFormatter?) =
            apply { this.distanceFormatter = distanceFormatter }

        fun onboardRouterConfig(onboardRouterConfig: MapboxOnboardRouterConfig?) =
            apply { this.onboardRouterConfig = onboardRouterConfig }

        fun isFromNavigationUi(flag: Boolean) =
            apply { this.isFromNavigationUi = flag }

        fun isDebugLoggingEnabled(flag: Boolean) =
            apply { this.isDebugLoggingEnabled = flag }

        fun build(): NavigationOptions {
            return NavigationOptions(
                roundingIncrement = roundingIncrement,
                timeFormatType = timeFormatType,
                navigatorPollingDelay = navigatorPollingDelay,
                distanceFormatter = distanceFormatter,
                onboardRouterConfig = onboardRouterConfig,
                isFromNavigationUi = isFromNavigationUi,
                isDebugLoggingEnabled = isDebugLoggingEnabled
            )
        }
    }
}
