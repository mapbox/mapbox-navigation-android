package com.mapbox.navigation.base.formatter

import android.text.SpannableString

/**
 * An interface, which provides correctly formatted distances.
 */
fun interface DistanceFormatter {

    /**
     * Returns a formatted SpannableString
     *
     * @param distance in meters
     * @return [SpannableString] representation which allows to change the style of the String to be
     * displayed in the view
     */
    fun formatDistance(distance: Double): SpannableString
}
