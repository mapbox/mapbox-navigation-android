package com.mapbox.navigation.trip.notification.utils.time

internal class TimeFormattingChain {

    fun setup(isDeviceTwentyFourHourFormat: Boolean): TimeFormatResolver {
        val noneSpecified = NoneSpecifiedTimeFormat(isDeviceTwentyFourHourFormat)
        val twentyFourHours = TwentyFourHoursTimeFormat(noneSpecified)
        return TwelveHoursTimeFormat(twentyFourHours)
    }
}
