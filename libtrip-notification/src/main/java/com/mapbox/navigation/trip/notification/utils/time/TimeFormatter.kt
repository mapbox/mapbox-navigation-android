package com.mapbox.navigation.trip.notification.utils.time

import android.content.Context
import android.content.res.Resources
import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import com.mapbox.navigation.base.typedef.TimeFormatType
import com.mapbox.navigation.trip.notification.R
import com.mapbox.navigation.utils.extensions.combineSpan
import com.mapbox.navigation.utils.span.SpanItem
import com.mapbox.navigation.utils.span.TextSpanItem
import java.util.ArrayList
import java.util.Calendar
import java.util.concurrent.TimeUnit

object TimeFormatter {

    private const val TIME_STRING_FORMAT = " %s "

    @JvmStatic
    fun formatTime(
        time: Calendar,
        routeDuration: Double,
        @TimeFormatType type: Int,
        isDeviceTwentyFourHourFormat: Boolean
    ): String {
        time.add(Calendar.SECOND, routeDuration.toInt())
        val chain = TimeFormattingChain()
        return chain.setup(isDeviceTwentyFourHourFormat).obtainTimeFormatted(type, time)
    }

    @JvmStatic
    fun formatTimeRemaining(context: Context, routeDuration: Double): SpannableStringBuilder {
        var seconds = routeDuration.toLong()

        if (seconds < 0) {
            seconds = 0L
        }

        val days = TimeUnit.SECONDS.toDays(seconds)
        seconds -= TimeUnit.DAYS.toSeconds(days)
        val hours = TimeUnit.SECONDS.toHours(seconds)
        seconds -= TimeUnit.HOURS.toSeconds(hours)
        val minutes =
            TimeUnit.SECONDS.toMinutes(seconds + TimeUnit.MINUTES.toSeconds(1) / 2) // round it to next minute if seconds is more or equal than 30

        val textSpanItems = ArrayList<SpanItem>()
        val resources = context.resources
        formatDays(resources, days, textSpanItems)
        formatHours(context, hours, textSpanItems)
        formatMinutes(context, minutes, textSpanItems)
        formatNoData(context, days, hours, minutes, textSpanItems)
        return textSpanItems.combineSpan()
    }

    private fun formatDays(resources: Resources, days: Long, textSpanItems: MutableList<SpanItem>) {
        if (days != 0L) {
            val dayQuantityString =
                resources.getQuantityString(R.plurals.numberOfDays, days.toInt())
            val dayString = String.format(TIME_STRING_FORMAT, dayQuantityString)
            textSpanItems.add(TextSpanItem(StyleSpan(Typeface.BOLD), days.toString()))
            textSpanItems.add(TextSpanItem(RelativeSizeSpan(1f), dayString))
        }
    }

    private fun formatHours(context: Context, hours: Long, textSpanItems: MutableList<SpanItem>) {
        if (hours != 0L) {
            val hourString = String.format(TIME_STRING_FORMAT, context.getString(R.string.hr))
            textSpanItems.add(TextSpanItem(StyleSpan(Typeface.BOLD), hours.toString()))
            textSpanItems.add(TextSpanItem(RelativeSizeSpan(1f), hourString))
        }
    }

    private fun formatMinutes(
        context: Context,
        minutes: Long,
        textSpanItems: MutableList<SpanItem>
    ) {
        if (minutes != 0L) {
            val minuteString = String.format(TIME_STRING_FORMAT, context.getString(R.string.min))
            textSpanItems.add(TextSpanItem(StyleSpan(Typeface.BOLD), minutes.toString()))
            textSpanItems.add(TextSpanItem(RelativeSizeSpan(1f), minuteString))
        }
    }

    private fun formatNoData(
        context: Context,
        days: Long,
        hours: Long,
        minutes: Long,
        textSpanItems: MutableList<SpanItem>
    ) {
        if (days == 0L && hours == 0L && minutes == 0L) {
            val minuteString = String.format(TIME_STRING_FORMAT, context.getString(R.string.min))
            textSpanItems.add(TextSpanItem(StyleSpan(Typeface.BOLD), 1.toString()))
            textSpanItems.add(TextSpanItem(RelativeSizeSpan(1f), minuteString))
        }
    }
}
