package com.mapbox.navigation.base.time

import java.util.Locale

internal class TimeFormattingChain {

    fun setup(
        isDeviceTwentyFourHourFormat: Boolean,
        locale: Locale = Locale.getDefault(),
    ): TimeFormatResolver {
        val noneSpecified = NoneSpecifiedTimeFormat(isDeviceTwentyFourHourFormat, locale)
        val twentyFourHours = TwentyFourHoursTimeFormat(noneSpecified, locale)
        return TwelveHoursTimeFormat(twentyFourHours, locale)
    }
}
