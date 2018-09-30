package com.mapbox.services.android.navigation.testapp.example.utils

import android.content.Context
import android.text.format.DateFormat
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.services.android.navigation.v5.navigation.NavigationTimeFormat.NONE_SPECIFIED
import com.mapbox.services.android.navigation.v5.utils.time.TimeFormatter.formatTime
import java.util.*

fun DirectionsRoute.formatArrivalTime(context: Context): String {
  val time = Calendar.getInstance()
  val isTwentyFourHourFormat = DateFormat.is24HourFormat(context)
  // TODO localization 12 /24 time specify from settings
  return formatTime(time, duration()!!, NONE_SPECIFIED, isTwentyFourHourFormat)
}