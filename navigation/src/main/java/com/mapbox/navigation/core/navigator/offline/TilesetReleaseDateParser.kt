package com.mapbox.navigation.core.navigator.offline

import com.mapbox.navigation.utils.internal.logE
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

internal object TilesetReleaseDateParser {

    fun parseReleaseDate(versionName: String): Date? {
        return try {
            VERSION_NAME_DATE_FORMATTER.parse(versionName)
        } catch (e: Exception) {
            logE(TilesetVersionManagerImpl.LOG_CATEGORY) {
                "Unable to parse date from version name $versionName: $e"
            }
            null
        }
    }

    private val VERSION_NAME_DATE_FORMATTER = SimpleDateFormat(
        "yyyy_MM_dd-HH_mm_ss",
        Locale.US,
    ).apply {
        timeZone = TimeZone.getTimeZone("UTC")
        isLenient = false
    }
}
