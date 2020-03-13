package com.mapbox.navigation.base.formatter

import android.text.SpannableString

interface DistanceFormatter {

    /**
     * Returns a formatted SpannableString
     *
     * @param distance in meters
     * @return [SpannableString] representation which allows to change the style of the String to be
     * displayed in the view
     */
    fun formatDistance(distance: Double): SpannableString
}
