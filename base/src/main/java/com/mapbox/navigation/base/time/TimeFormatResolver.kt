package com.mapbox.navigation.base.time

import java.util.Calendar

internal fun interface TimeFormatResolver {

    fun obtainTimeFormatted(type: Int, time: Calendar): String
}
