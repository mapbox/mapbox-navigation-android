package com.mapbox.navigation.tripdata.progress.model

import android.text.SpannableString
import com.mapbox.navigation.ui.base.formatter.ValueFormatter

/**
 * Formats trip related data for displaying in the UI
 */
class PercentDistanceTraveledFormatter : ValueFormatter<Double, SpannableString> {

    /**
     * Formats an update to a [SpannableString] representing the percent distance traveled
     *
     * @param update a [TripProgressUpdateValue]
     * @return a formatted string
     */
    override fun format(update: Double): SpannableString {
        return SpannableString("${update.toInt()}")
    }
}
