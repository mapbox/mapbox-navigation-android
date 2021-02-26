package com.mapbox.navigation.ui.base.model.tripprogress

import android.text.SpannableString
import com.mapbox.navigation.ui.base.formatter.ValueFormatter

/**
 * Formats trip related data for displaying in the UI
 */
class PercentDistanceTraveledFormatter : ValueFormatter<Double, SpannableString> {

    /**
     * Formats an update to a [SpannableString] representing the percent distance traveled
     *
     * @param value a [TripProgressUpdate]
     * @return a formatted string
     */
    override fun format(value: Double): SpannableString {
        return SpannableString("${value.toInt()}")
    }
}
