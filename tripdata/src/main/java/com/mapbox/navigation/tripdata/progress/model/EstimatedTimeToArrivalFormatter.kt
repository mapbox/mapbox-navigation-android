package com.mapbox.navigation.tripdata.progress.model

import android.content.Context
import android.text.SpannableString
import com.mapbox.navigation.base.TimeFormat
import com.mapbox.navigation.ui.base.formatter.ValueFormatter
import java.util.Calendar

/**
 * Formats trip related data for displaying in the UI
 *
 * @param context an application context instance
 * @param timeFormatType a value indicating whether the time should be formatted in 12 or 24 hour
 * format
 */
@Deprecated(
    "This formatter is unable to format ETA with respect " +
        "to destination time zone, use EstimatedTimeOfArrivalFormatter instead",
)
class EstimatedTimeToArrivalFormatter(
    context: Context,
    @TimeFormat.Type private val timeFormatType: Int = TimeFormat.NONE_SPECIFIED,
) : ValueFormatter<Long, SpannableString> {

    private val formatter = EstimatedTimeOfArrivalFormatter(context, timeFormatType)

    /**
     * Formats an update to a [SpannableString] representing the estimated time to arrival
     *
     * @param update represents the estimated time to arrival value
     * @return a formatted string
     */
    override fun format(update: Long): SpannableString {
        val etaAsCalendar = Calendar.getInstance()
        etaAsCalendar.timeInMillis = update
        return formatter.format(etaAsCalendar)
    }
}
