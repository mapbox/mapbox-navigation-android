package com.mapbox.navigation.base.options

import com.mapbox.navigation.base.formatter.DistanceFormatter
import com.mapbox.navigation.base.typedef.NONE_SPECIFIED
import com.mapbox.navigation.base.typedef.ROUNDING_INCREMENT_FIFTY
import com.mapbox.navigation.base.typedef.RoundingIncrement
import com.mapbox.navigation.base.typedef.TimeFormatType

const val DEFAULT_NAVIGATOR_POLLING_DELAY = 1500L

data class NavigationOptions constructor(
    @RoundingIncrement val roundingIncrement: Int,
    @TimeFormatType val timeFormatType: Int,
    val navigatorPollingDelay: Long,
    val distanceFormatter: DistanceFormatter?,
    val onboardRouterConfig: MapboxOnboardRouterConfig?,
    val isFromNavigationUi: Boolean = false
) {

    /**
     * Get a builder to customize a subset of current options.
     */
    fun toBuilder() = Builder(
        roundingIncrement,
        timeFormatType,
        navigatorPollingDelay,
        distanceFormatter,
        onboardRouterConfig
    )

    data class Builder(
        private var roundingIncrement: Int = ROUNDING_INCREMENT_FIFTY,
        private var timeFormatType: Int = NONE_SPECIFIED,
        private var navigatorPollingDelay: Long = DEFAULT_NAVIGATOR_POLLING_DELAY,
        private var distanceFormatter: DistanceFormatter? = null,
        private var onboardRouterConfig: MapboxOnboardRouterConfig? = null,
        private var isFromNavigationUi: Boolean = false
    ) {

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

        fun build(): NavigationOptions {
            return NavigationOptions(
                roundingIncrement,
                timeFormatType,
                navigatorPollingDelay,
                distanceFormatter,
                onboardRouterConfig,
                isFromNavigationUi
            )
        }
    }
}
