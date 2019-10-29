package com.mapbox.services.android.navigation.v5.utils.time

internal class TimeFormattingChain {

    fun setup(isDeviceTwentyFourHourFormat: Boolean): TimeFormatResolver {
        val noneSpecified = NoneSpecifiedTimeFormat(isDeviceTwentyFourHourFormat)
        val twentyFourHours = TwentyFourHoursTimeFormat(noneSpecified)
        return TwelveHoursTimeFormat(twentyFourHours)
    }
}
