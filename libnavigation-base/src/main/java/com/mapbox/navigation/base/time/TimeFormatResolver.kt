package com.mapbox.navigation.base.time

import java.util.Calendar

internal interface TimeFormatResolver {

    fun obtainTimeFormatted(type: Int, time: Calendar): String
}
