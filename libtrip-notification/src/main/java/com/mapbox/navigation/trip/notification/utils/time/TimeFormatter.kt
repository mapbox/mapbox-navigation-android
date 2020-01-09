package com.mapbox.navigation.trip.notification.utils.time

import com.mapbox.navigation.base.typedef.TimeFormatType
import java.util.Calendar

object TimeFormatter {

    private const val TIME_STRING_FORMAT = " %s "

    @JvmStatic
    fun formatTime(
        time: Calendar,
        routeDuration: Double,
        @TimeFormatType type: Int,
        isDeviceTwentyFourHourFormat: Boolean
    ): String {
        time.add(Calendar.SECOND, routeDuration.toInt())
        val chain = TimeFormattingChain()
        return chain.setup(isDeviceTwentyFourHourFormat).obtainTimeFormatted(type, time)
    }
}
