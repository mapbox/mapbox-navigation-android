package com.mapbox.navigation.base.formatter

import java.util.Calendar

/**
 * Interface that provides formatted time.
 */
fun interface TimeFormatter {

    /**
     * Returns a formatted string representing input time.
     */
    fun formatTime(time: Calendar): String
}
