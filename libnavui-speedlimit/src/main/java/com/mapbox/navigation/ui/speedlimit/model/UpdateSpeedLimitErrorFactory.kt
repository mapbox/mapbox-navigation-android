package com.mapbox.navigation.ui.speedlimit.model

import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI

/**
 * A factory exposed to build a [UpdateSpeedLimitError] object.
 */
@ExperimentalMapboxNavigationAPI
object UpdateSpeedLimitErrorFactory {

    /**
     * Build [UpdateSpeedLimitError] given appropriate arguments
     */
    @JvmStatic
    fun buildSpeedLimitError(
        errorMessage: String,
        throwable: Throwable?
    ) = UpdateSpeedLimitError(errorMessage, throwable)
}
