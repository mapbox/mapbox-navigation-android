package com.mapbox.navigation.utils.formatter.time

import java.util.Calendar

internal interface TimeFormatResolver {
    fun nextChain(chain: TimeFormatResolver)

    fun obtainTimeFormatted(type: Int, time: Calendar): String
}
