package com.mapbox.navigation.base.internal.time

import java.text.ParsePosition
import java.util.Date

/***
 * Parses a ISO8601 date. Converts parsed date to local time zone.
 */
fun parseISO8601DateToLocalTimeOrNull(date: String?): Date? {
    if (date == null) return null
    return try {
        ISO8601Utils.parse(date, ParsePosition(0))
    } catch (t: Throwable) {
        null
    }
}
