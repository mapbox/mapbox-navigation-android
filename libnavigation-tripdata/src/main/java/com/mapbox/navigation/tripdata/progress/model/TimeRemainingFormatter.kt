package com.mapbox.navigation.tripdata.progress.model

import android.content.Context
import android.text.Spannable
import android.text.SpannableString
import android.text.style.RelativeSizeSpan
import com.mapbox.navigation.base.internal.time.TimeFormatter.formatTimeRemaining
import com.mapbox.navigation.ui.base.formatter.ValueFormatter
import java.util.Locale

/**
 * Formats trip related data for displaying in the UI
 *
 * @param context an application context instance
 * @param locale an optional [Locale], if not provided the locale will be derived from the context
 */
class TimeRemainingFormatter(
    context: Context,
    var locale: Locale? = null,
) : ValueFormatter<Double, SpannableString> {

    private val appContext: Context = context.applicationContext

    /**
     * * Formats an update to a [SpannableString] representing the remaining travel time
     *
     * @param update the time remaining value
     * @return a formatted string
     */
    override fun format(update: Double): SpannableString {
        val timeRemainingSpan = formatTimeRemaining(
            appContext,
            update,
            locale,
        )
        val spaceIndex = if (timeRemainingSpan.startsWith("<")) {
            val firstSpaceIndex = timeRemainingSpan.indexOfFirst { it == ' ' }
            timeRemainingSpan.indexOf(' ', firstSpaceIndex + 1)
        } else {
            timeRemainingSpan.indexOfFirst { it == ' ' }
        }
        val secondSpaceIndex = timeRemainingSpan.trim().indexOf(' ', spaceIndex + 1)
        val thirdSpaceIndex = timeRemainingSpan.trim().indexOf(' ', secondSpaceIndex + 1)

        timeRemainingSpan.setSpan(
            RelativeSizeSpan(2f),
            0,
            spaceIndex,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
        )

        if (secondSpaceIndex > 0 && thirdSpaceIndex > 0) {
            timeRemainingSpan.setSpan(
                RelativeSizeSpan(2f),
                secondSpaceIndex + 1,
                thirdSpaceIndex,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
        }

        return SpannableString(timeRemainingSpan.trim())
    }
}
