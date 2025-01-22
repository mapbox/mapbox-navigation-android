package com.mapbox.navigation.tripdata.progress.model

import android.content.Context
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.format.DateFormat
import android.text.style.StyleSpan
import com.mapbox.navigation.base.TimeFormat
import com.mapbox.navigation.base.internal.time.TimeFormatter.formatTime
import com.mapbox.navigation.ui.base.formatter.ValueFormatter
import java.util.Calendar

/**
 * Formats trip related data for displaying in the UI
 *
 * @param context an application context instance
 * @param timeFormatType a value indicating whether the time should be formatted in 12 or 24 hour
 * format
 */
class EstimatedTimeOfArrivalFormatter(
    context: Context,
    @TimeFormat.Type private val timeFormatType: Int = TimeFormat.NONE_SPECIFIED,
) : ValueFormatter<Calendar, SpannableString> {

    private val appContext = context.applicationContext

    /**
     * Formats an update to a [SpannableString] representing the estimated time to arrival
     *
     * @param update represents the estimated time to arrival value
     * @return a formatted string
     */
    override fun format(update: Calendar): SpannableString {
        val is24HourFormat = DateFormat.is24HourFormat(appContext)
        val spannableString = SpannableString(formatTime(update, timeFormatType, is24HourFormat))
        val spaceIndex = spannableString.indexOf(char = ' ')
        if (spaceIndex > 0) {
            val span = StyleSpan(Typeface.BOLD)
            spannableString.setSpan(span, 0, spaceIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        return spannableString
    }
}
