package com.mapbox.services.android.navigation.v5.utils.time

import java.util.Calendar

internal interface TimeFormatResolver {
    fun nextChain(chain: TimeFormatResolver)

    fun obtainTimeFormatted(type: Int, time: Calendar): String
}
