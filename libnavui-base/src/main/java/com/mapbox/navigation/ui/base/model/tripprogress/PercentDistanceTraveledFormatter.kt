package com.mapbox.navigation.ui.base.model.tripprogress

import android.text.SpannableString
import com.mapbox.navigation.ui.base.formatter.ValueFormatter

/**
 * Formats trip related data for displaying in the UI
 */
class PercentDistanceTraveledFormatter : ValueFormatter<TripProgressUpdate, SpannableString> {

    /**
     * Formats an update to a [SpannableString] representing the percent distance traveled
     *
     * @param update a [TripProgressUpdate]
     * @return a formatted string
     */
    override fun format(update: TripProgressUpdate): SpannableString {
        return SpannableString("${update.percentRouteTraveled.toInt()}")
    }
}
