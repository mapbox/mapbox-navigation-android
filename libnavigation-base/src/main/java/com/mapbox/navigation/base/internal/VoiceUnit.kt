package com.mapbox.navigation.base.internal

import androidx.annotation.StringDef

object VoiceUnit {
    /**
     * Change the units to imperial for voice and visual information. Note that this won't change
     * other results such as raw distance measurements which will always be returned in meters.
     */
    const val IMPERIAL = "imperial"

    /**
     * Change the units to metric for voice and visual information. Note that this won't change
     * other results such as raw distance measurements which will always be returned in meters.
     */
    const val METRIC = "metric"

    /**
     * Use to apply default units for a locale.
     */
    const val UNDEFINED = "undefined"

    /**
     * Retention policy for the various units of measurements.
     */
    @Retention(AnnotationRetention.SOURCE)
    @StringDef(IMPERIAL, METRIC, UNDEFINED)
    annotation class Type
}
