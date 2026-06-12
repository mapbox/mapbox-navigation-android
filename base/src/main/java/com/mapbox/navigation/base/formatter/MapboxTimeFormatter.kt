package com.mapbox.navigation.base.formatter

import android.content.Context
import android.text.format.DateFormat
import com.mapbox.navigation.base.TimeFormat
import java.util.Calendar

/**
 * Default implementation of [TimeFormatter] interface. It uses default locale-aware time formatting.
 */
class MapboxTimeFormatter @JvmOverloads constructor(
    private val applicationContext: Context,
    @TimeFormat.Type private val timeFormatType: Int = TimeFormat.NONE_SPECIFIED,
) : TimeFormatter {

    /**
     * Formats time in a locale-aware manner.
     */
    override fun formatTime(time: Calendar): String {
        return com.mapbox.navigation.base.internal.time.TimeFormatter.formatTime(
            time,
            timeFormatType,
            DateFormat.is24HourFormat(applicationContext),
        )
    }
}
