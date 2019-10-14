package com.mapbox.services.android.navigation.v5.utils.time

import java.util.Calendar
import java.util.Locale

internal class TwentyFourHoursTimeFormat(
    private var chain: TimeFormatResolver
) : TimeFormatResolver {

    companion object {
        const val TWENTY_FOUR_HOURS_FORMAT = "%tk:%tM"
        private const val TWENTY_FOUR_HOURS_TYPE = 1
    }

    override fun nextChain(chain: TimeFormatResolver) {
        this.chain = chain
    }

    override fun obtainTimeFormatted(type: Int, time: Calendar): String {
        return if (type == TWENTY_FOUR_HOURS_TYPE) {
            String.format(Locale.getDefault(), TWENTY_FOUR_HOURS_FORMAT, time, time)
        } else {
            chain.obtainTimeFormatted(type, time)
        }
    }
}
