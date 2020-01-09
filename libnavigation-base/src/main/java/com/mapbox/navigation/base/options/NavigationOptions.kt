package com.mapbox.navigation.base.options

import com.mapbox.navigation.base.typedef.NONE_SPECIFIED
import com.mapbox.navigation.base.typedef.ROUNDING_INCREMENT_FIFTY
import com.mapbox.navigation.base.typedef.RoundingIncrement
import com.mapbox.navigation.base.typedef.TimeFormatType

class NavigationOptions private constructor(
    @RoundingIncrement val roundingIncrement: Int,
    @TimeFormatType val timeFormatType: Int,
    var builder: Builder
) {

    fun roundingIncrement() = roundingIncrement

    fun timeFormatType() = timeFormatType

    class Builder {
        var timeFormatType: Int = NONE_SPECIFIED
        var roundingIncrement: Int = ROUNDING_INCREMENT_FIFTY

        fun roundingIncrement(roundingIncrement: Int) =
            apply { this.roundingIncrement = roundingIncrement }

        fun timeFormatType(type: Int) =
            apply { this.timeFormatType = type }

        fun build(): NavigationOptions {
            return NavigationOptions(
                roundingIncrement,
                timeFormatType,
                this
            )
        }
    }
}
