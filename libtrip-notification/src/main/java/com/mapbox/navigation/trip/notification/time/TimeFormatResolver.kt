package com.mapbox.navigation.trip.notification.time

import java.util.Calendar

internal interface TimeFormatResolver {

    fun obtainTimeFormatted(type: Int, time: Calendar): String
}
