package com.mapbox.services.android.navigation.v5.utils.time

import java.util.Calendar
import java.util.Locale

internal class NoneSpecifiedTimeFormat(
    private val isDeviceTwentyFourHourFormat: Boolean
) : TimeFormatResolver {

    override fun nextChain(chain: TimeFormatResolver) {}

    override fun obtainTimeFormatted(type: Int, time: Calendar): String {
        return if (isDeviceTwentyFourHourFormat) {
            String.format(
                Locale.getDefault(),
                TwentyFourHoursTimeFormat.TWENTY_FOUR_HOURS_FORMAT,
                time,
                time
            )
        } else {
            String.format(
                Locale.getDefault(),
                TwelveHoursTimeFormat.TWELVE_HOURS_FORMAT,
                time,
                time,
                time
            )
        }
    }
}
