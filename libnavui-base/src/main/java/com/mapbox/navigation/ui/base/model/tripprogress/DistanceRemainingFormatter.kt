package com.mapbox.navigation.ui.base.model.tripprogress

import android.text.SpannableString
import com.mapbox.navigation.base.formatter.DistanceFormatterOptions
import com.mapbox.navigation.core.internal.formatter.MapboxDistanceFormatter
import com.mapbox.navigation.ui.base.formatter.ValueFormatter

/**
 * Formats trip related data for displaying in the UI
 *
 * @param distanceFormatterOptions to build the [DistanceRemainingFormatter]
 */
class DistanceRemainingFormatter(
    private val distanceFormatterOptions: DistanceFormatterOptions
) : ValueFormatter<TripProgressUpdate, SpannableString> {

    private val formatter = MapboxDistanceFormatter(distanceFormatterOptions)

    /**
     * Formats the data in the [TripProgressUpdate] for displaying the route distance remaining
     * in the UI
     *
     * @param t a [TripProgressUpdate]
     * @return a [SpannableString] representing the route distance remaining
     */
    override fun format(t: TripProgressUpdate): SpannableString {
        return formatter.formatDistance(t.distanceRemaining)
    }
}
