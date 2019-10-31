package com.mapbox.navigation.utils.formatter.time

internal class TimeFormattingChain {

    fun setup(isDeviceTwentyFourHourFormat: Boolean): TimeFormatResolver {
        val noneSpecified = NoneSpecifiedTimeFormat(isDeviceTwentyFourHourFormat)
        val twentyFourHours = TwentyFourHoursTimeFormat(noneSpecified)
        return TwelveHoursTimeFormat(twentyFourHours)
    }
}
