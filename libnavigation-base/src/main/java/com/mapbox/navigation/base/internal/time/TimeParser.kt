package com.mapbox.navigation.base.internal.time

import com.google.gson.GsonBuilder
import java.util.Date

/***
 * Parses a ISO8601 date. Converts parsed date to local time zone.
 */
fun parseISO8601DateToLocalTimeOrNull(date: String?): Date? {
    if (date == null) return null
    return try {
        GsonBuilder().create().getAdapter(Date::class.java).fromJson("\"$date\"")
    } catch (t: Throwable) {
        null
    }
}
