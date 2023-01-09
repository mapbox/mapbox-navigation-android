package com.mapbox.navigation.copilot

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal object DateUtils {

    private const val DATE_AND_TIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSSZ"
    private val dateFormat = SimpleDateFormat(DATE_AND_TIME_PATTERN, Locale.US)

    fun obtainCurrentDate(): String = dateFormat.format(Date())
}
