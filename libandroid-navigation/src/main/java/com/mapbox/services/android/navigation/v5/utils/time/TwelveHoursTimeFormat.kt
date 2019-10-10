package com.mapbox.services.android.navigation.v5.utils.time

import java.util.Calendar
import java.util.Locale

internal class TwelveHoursTimeFormat(
    private var chain: TimeFormatResolver
) : TimeFormatResolver {

    companion object {
        const val TWELVE_HOURS_FORMAT = "%tl:%tM %tp"
        private const val TWELVE_HOURS_TYPE = 0
    }

    override fun nextChain(chain: TimeFormatResolver) {
        this.chain = chain
    }

    override fun obtainTimeFormatted(type: Int, time: Calendar): String {
        return if (type == TWELVE_HOURS_TYPE) {
            String.format(Locale.getDefault(), TWELVE_HOURS_FORMAT, time, time, time)
        } else {
            chain.obtainTimeFormatted(type, time)
        }
    }
}
