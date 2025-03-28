package com.mapbox.navigation.base.model

import androidx.annotation.IntDef
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI

/**
 * Weather condition type.
 */
@ExperimentalPreviewMapboxNavigationAPI
object WeatherCondition {

    /**
     * Rain weather condition
     */
    const val RAIN = 0

    /**
     * Snow weather condition
     */
    const val SNOW = 1

    /**
     * Fog weather condition
     */
    const val FOG = 2

    /**
     * Wet road weather condition
     */
    const val WET_ROAD = 3

    /**
     * Retention policy for the [WeatherCondition]
     */
    @Retention(AnnotationRetention.BINARY)
    @IntDef(
        RAIN,
        SNOW,
        FOG,
        WET_ROAD,
    )
    annotation class Type
}
