package com.mapbox.navigation.testing

import android.os.Build
import androidx.annotation.RequiresApi
import java.time.Month
import java.util.Calendar
import java.util.Calendar.MILLISECOND
import java.util.Date
import java.util.TimeZone

@RequiresApi(Build.VERSION_CODES.O)
fun utcToLocalTime(
    year: Int,
    month: Month,
    date: Int,
    hourOfDay: Int,
    minute: Int,
    second: Int,
    milliseconds: Int = 0,
):Date = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
    set(year, month.value - 1, date, hourOfDay, minute, second)
    set(MILLISECOND, milliseconds)
}.time

fun Date.add(
    hours: Int = 0,
    milliseconds: Long = 0
): Date {
    val calendar = Calendar.getInstance()
    calendar.time = this
    calendar.add(Calendar.HOUR_OF_DAY, hours)
    calendar.add(Calendar.MILLISECOND, milliseconds.toInt())
    return calendar.time
}