package com.mapbox.navigation.base.time

import java.util.Calendar
import java.util.Locale

internal class TwelveHoursTimeFormat(
    private val chain: TimeFormatResolver,
) : TimeFormatResolver {

    companion object {
        const val TWELVE_HOURS_FORMAT = "%tl:%tM %tp"
        private const val TWELVE_HOURS_TYPE = 0
    }

    override fun obtainTimeFormatted(type: Int, time: Calendar): String {
        return if (type == TWELVE_HOURS_TYPE) {
            String.format(Locale.getDefault(), TWELVE_HOURS_FORMAT, time, time, time)
        } else {
            chain.obtainTimeFormatted(type, time)
        }
    }
}
