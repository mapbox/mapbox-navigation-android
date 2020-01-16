package com.mapbox.navigation.base.options

import com.mapbox.navigation.base.formatter.DistanceFormatter
import com.mapbox.navigation.base.typedef.NONE_SPECIFIED
import com.mapbox.navigation.base.typedef.ROUNDING_INCREMENT_FIFTY
import com.mapbox.navigation.base.typedef.RoundingIncrement
import com.mapbox.navigation.base.typedef.TimeFormatType

class NavigationOptions private constructor(
    @RoundingIncrement private val roundingIncrement: Int,
    @TimeFormatType private val timeFormatType: Int,
    private val distanceFormatter: DistanceFormatter?,
    private val onboardRouterConfig: MapboxOnboardRouterConfig
) {

    fun roundingIncrement() = roundingIncrement

    fun timeFormatType() = timeFormatType

    fun distanceFormatter() = distanceFormatter

    fun onboardRouterConfig() = onboardRouterConfig

    data class Builder(
        private var timeFormatType: Int = NONE_SPECIFIED,
        private var roundingIncrement: Int = ROUNDING_INCREMENT_FIFTY,
        private var distanceFormatter: DistanceFormatter?,
        private var onboardRouterConfig: MapboxOnboardRouterConfig
    ) {

        fun roundingIncrement(roundingIncrement: Int) =
            apply { this.roundingIncrement = roundingIncrement }

        fun timeFormatType(type: Int) =
            apply { this.timeFormatType = type }

        fun distanceFormatter(distanceFormatter: DistanceFormatter) =
            apply { this.distanceFormatter = distanceFormatter }

        fun onboardRouterConfig(onboardRouterConfig: MapboxOnboardRouterConfig) =
            apply { this.onboardRouterConfig }

        fun build(): NavigationOptions {
            return NavigationOptions(
                roundingIncrement,
                timeFormatType,
                distanceFormatter,
                onboardRouterConfig
            )
        }
    }
}
