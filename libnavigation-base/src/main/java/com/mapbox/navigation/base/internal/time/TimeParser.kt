package com.mapbox.navigation.base.internal.time

import com.google.gson.internal.bind.util.ISO8601Utils
import java.text.ParsePosition
import java.util.Date

fun parseSO8061DateToLocalTimeOrNull(date: String?): Date? {
    if (date == null) return null
    return try {
        ISO8601Utils.parse(date, ParsePosition(0))
    } catch (t: Throwable) {
        null
    }
}