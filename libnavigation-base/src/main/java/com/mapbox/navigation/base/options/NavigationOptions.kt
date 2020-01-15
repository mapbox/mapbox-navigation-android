package com.mapbox.navigation.base.options

import com.mapbox.navigation.base.formatter.DistanceFormatter
import com.mapbox.navigation.base.typedef.NONE_SPECIFIED
import com.mapbox.navigation.base.typedef.ROUNDING_INCREMENT_FIFTY
import com.mapbox.navigation.base.typedef.RoundingIncrement
import com.mapbox.navigation.base.typedef.TimeFormatType

class NavigationOptions private constructor(
    @RoundingIncrement private val roundingIncrement: Int,
    @TimeFormatType private val timeFormatType: Int,
    private val distanceFormatter: DistanceFormatter
) {

    fun roundingIncrement() = roundingIncrement

    fun timeFormatType() = timeFormatType

    fun distanceFormatter() = distanceFormatter

    data class Builder(
        private var timeFormatType: Int = NONE_SPECIFIED,
        private var roundingIncrement: Int = ROUNDING_INCREMENT_FIFTY,
        private var distanceFormatter: DistanceFormatter
    ) {

        fun roundingIncrement(roundingIncrement: Int) =
            apply { this.roundingIncrement = roundingIncrement }

        fun timeFormatType(type: Int) =
            apply { this.timeFormatType = type }

        fun distanceFormatter(distanceFormatter: DistanceFormatter) =
            apply { this.distanceFormatter = distanceFormatter }

        fun build(): NavigationOptions {
            return NavigationOptions(
                roundingIncrement,
                timeFormatType,
                distanceFormatter
            )
        }
    }
}
