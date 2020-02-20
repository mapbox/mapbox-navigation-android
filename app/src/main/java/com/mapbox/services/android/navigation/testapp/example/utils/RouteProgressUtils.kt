package com.mapbox.services.android.navigation.testapp.example.utils

import android.content.Context
import android.text.format.DateFormat
import com.mapbox.services.android.navigation.v5.navigation.NONE_SPECIFIED
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress
import com.mapbox.services.android.navigation.v5.utils.extensions.ifNonNull
import com.mapbox.services.android.navigation.v5.utils.time.TimeFormatter.formatTime
import java.util.Calendar
fun RouteProgress.formatArrivalTime(context: Context): String =
        ifNonNull(durationRemaining()) { durationRemaining ->
            // TODO localization 12 /24 time specify from settings
            formatTime(Calendar.getInstance(), durationRemaining, NONE_SPECIFIED, DateFormat.is24HourFormat(context))
        } ?: formatTime(Calendar.getInstance(), 0.0, NONE_SPECIFIED, DateFormat.is24HourFormat(context))
