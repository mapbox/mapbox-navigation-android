package com.mapbox.navigation.base.internal.time

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import com.mapbox.navigation.base.R
import com.mapbox.navigation.base.TimeFormat
import com.mapbox.navigation.base.time.TimeFormattingChain
import com.mapbox.navigation.base.time.span.SpanItem
import com.mapbox.navigation.base.time.span.TextSpanItem
import com.mapbox.navigation.base.time.span.combineSpan
import java.util.ArrayList
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Default Time Formatter
 */
object TimeFormatter {

    private const val TIME_STRING_FORMAT = " %s "

    /**
     * Format time
     *
     * @param time Calendar
     * @param routeDuration duration in seconds
     * @param type [TimeFormat.Type]
     * @param isDeviceTwentyFourHourFormat *true* if 24-hour format, *false* otherwise
     * @return String
     */
    @JvmStatic
    fun formatTime(
        time: Calendar,
        routeDuration: Double,
        @TimeFormat.Type type: Int,
        isDeviceTwentyFourHourFormat: Boolean,
    ): String {
        time.add(Calendar.SECOND, routeDuration.toInt())
        return formatTime(time, type, isDeviceTwentyFourHourFormat)
    }

    /**
     * Format time
     *
     * @param time Calendar
     * @param type [TimeFormat.Type]
     * @param isDeviceTwentyFourHourFormat *true* if 24-hour format, *false* otherwise
     * @return String
     */
    @JvmStatic
    fun formatTime(
        time: Calendar,
        @TimeFormat.Type type: Int,
        isDeviceTwentyFourHourFormat: Boolean,
    ): String {
        val chain = TimeFormattingChain()
        return chain.setup(isDeviceTwentyFourHourFormat).obtainTimeFormatted(type, time)
    }

    /**
     * Format remaining time
     *
     * @param context Context
     * @param routeDuration route duration in seconds
     * @return SpannableStringBuilder
     */
    @JvmStatic
    fun formatTimeRemaining(
        context: Context,
        routeDuration: Double,
        locale: Locale?,
    ): SpannableStringBuilder {
        var seconds = routeDuration.toLong()

        if (seconds < 0) {
            seconds = 0L
        }

        val days = TimeUnit.SECONDS.toDays(seconds)
        seconds -= TimeUnit.DAYS.toSeconds(days)
        val hoursAndMinutes = getHoursAndMinutes(seconds)
        val hours = hoursAndMinutes.first
        val minutes = hoursAndMinutes.second

        val textSpanItems = ArrayList<SpanItem>()
        val resources = context.resourcesWithLocale(locale)
        formatDays(resources, days, textSpanItems)
        formatHours(resources, hours, textSpanItems)
        formatMinutes(resources, minutes, textSpanItems)
        formatNoData(resources, days, hours, minutes, textSpanItems)
        return textSpanItems.combineSpan()
    }

    private fun formatDays(resources: Resources, days: Long, textSpanItems: MutableList<SpanItem>) {
        if (days != 0L) {
            val dayQuantityString =
                resources.getQuantityString(R.plurals.mapbox_number_of_days, days.toInt())
            val dayString = String.format(TIME_STRING_FORMAT, dayQuantityString)
            textSpanItems.add(TextSpanItem(StyleSpan(Typeface.BOLD), days.toString()))
            textSpanItems.add(TextSpanItem(RelativeSizeSpan(1f), dayString))
        }
    }

    private fun formatHours(
        resources: Resources,
        hours: Long,
        textSpanItems: MutableList<SpanItem>,
    ) {
        if (hours != 0L) {
            val hourString =
                String.format(TIME_STRING_FORMAT, resources.getString(R.string.mapbox_unit_hr))
            textSpanItems.add(TextSpanItem(StyleSpan(Typeface.BOLD), hours.toString()))
            textSpanItems.add(TextSpanItem(RelativeSizeSpan(1f), hourString))
        }
    }

    private fun formatMinutes(
        resources: Resources,
        minutes: Long,
        textSpanItems: MutableList<SpanItem>,
    ) {
        if (minutes != 0L) {
            val minuteString =
                String.format(TIME_STRING_FORMAT, resources.getString(R.string.mapbox_unit_min))
            textSpanItems.add(TextSpanItem(StyleSpan(Typeface.BOLD), minutes.toString()))
            textSpanItems.add(TextSpanItem(RelativeSizeSpan(1f), minuteString))
        }
    }

    private fun formatNoData(
        resources: Resources,
        days: Long,
        hours: Long,
        minutes: Long,
        textSpanItems: MutableList<SpanItem>,
    ) {
        if (days == 0L && hours == 0L && minutes == 0L) {
            val minuteString =
                String.format(TIME_STRING_FORMAT, resources.getString(R.string.mapbox_unit_min))
            textSpanItems.add(TextSpanItem(RelativeSizeSpan(1f), "< "))
            textSpanItems.add(TextSpanItem(StyleSpan(Typeface.BOLD), "1"))
            textSpanItems.add(TextSpanItem(RelativeSizeSpan(1f), minuteString))
        }
    }

    private fun Context.resourcesWithLocale(locale: Locale?): Resources {
        val config = Configuration(this.resources.configuration).also {
            it.setLocale(locale)
        }
        return this.createConfigurationContext(config).resources
    }

    private fun getHoursAndMinutes(seconds: Long): Pair<Long, Long> {
        val initialHoursValue = TimeUnit.SECONDS.toHours(seconds)
        val leftOverSeconds = seconds - TimeUnit.HOURS.toSeconds(initialHoursValue)
        val minutes =
            TimeUnit.SECONDS.toMinutes(leftOverSeconds + TimeUnit.MINUTES.toSeconds(1) / 2)

        return if (minutes == 60L) {
            Pair(initialHoursValue + 1, 0)
        } else {
            Pair(
                initialHoursValue,
                minutes,
            )
        }
    }
}
