package com.mapbox.navigation.base.options

import com.mapbox.navigation.base.formatter.DistanceFormatter
import com.mapbox.navigation.base.typedef.NONE_SPECIFIED
import com.mapbox.navigation.base.typedef.ROUNDING_INCREMENT_FIFTY
import com.mapbox.navigation.base.typedef.RoundingIncrement
import com.mapbox.navigation.base.typedef.TimeFormatType

private const val DEFAULT_NAVIGATOR_POLLING_DELAY = 1500L

class NavigationOptions private constructor(
    @RoundingIncrement private val roundingIncrement: Int,
    @TimeFormatType private val timeFormatType: Int,
    private val navigatorPollingDelay: Long,
    private val distanceFormatter: DistanceFormatter?,
    private val onboardRouterConfig: MapboxOnboardRouterConfig?
) {

    fun roundingIncrement() = roundingIncrement

    fun timeFormatType() = timeFormatType

    fun distanceFormatter() = distanceFormatter

    fun onboardRouterConfig() = onboardRouterConfig

    fun navigatorPollingDelay() = navigatorPollingDelay

    data class Builder(
        private var timeFormatType: Int = NONE_SPECIFIED,
        private var roundingIncrement: Int = ROUNDING_INCREMENT_FIFTY,
        private var navigatorPollingDelay: Long = DEFAULT_NAVIGATOR_POLLING_DELAY,
        private var distanceFormatter: DistanceFormatter? = null,
        private var onboardRouterConfig: MapboxOnboardRouterConfig? = null
    ) {

        fun roundingIncrement(roundingIncrement: Int) =
            apply { this.roundingIncrement = roundingIncrement }

        fun timeFormatType(type: Int) =
            apply { this.timeFormatType = type }

        fun distanceFormatter(distanceFormatter: DistanceFormatter) =
            apply { this.distanceFormatter = distanceFormatter }

        fun onboardRouterConfig(onboardRouterConfig: MapboxOnboardRouterConfig) =
            apply { this.onboardRouterConfig }

        fun navigatorPollingDelay(pollingDelay: Long) =
            apply { navigatorPollingDelay = pollingDelay }

        fun build(): NavigationOptions {
            return NavigationOptions(
                roundingIncrement,
                timeFormatType,
                navigatorPollingDelay,
                distanceFormatter,
                onboardRouterConfig
            )
        }
    }
}
