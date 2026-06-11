package com.mapbox.navigation.base.time

import java.util.Calendar
import java.util.Locale

internal class NoneSpecifiedTimeFormat(
    private val isDeviceTwentyFourHourFormat: Boolean,
    private val locale: Locale = Locale.getDefault(),
) : TimeFormatResolver {

    override fun obtainTimeFormatted(type: Int, time: Calendar): String {
        return if (isDeviceTwentyFourHourFormat) {
            String.format(
                locale,
                TwentyFourHoursTimeFormat.TWENTY_FOUR_HOURS_FORMAT,
                time,
                time,
            )
        } else {
            String.format(
                locale,
                TwelveHoursTimeFormat.TWELVE_HOURS_FORMAT,
                time,
                time,
                time,
            )
        }
    }
}
