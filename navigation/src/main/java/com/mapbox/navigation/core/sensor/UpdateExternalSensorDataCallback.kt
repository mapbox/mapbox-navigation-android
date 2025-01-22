package com.mapbox.navigation.core.sensor

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI

/**
 * Callback which is getting called to report
 * [com.mapbox.navigation.core.MapboxNavigation.updateExternalSensorData] result
 */
@ExperimentalPreviewMapboxNavigationAPI
internal fun interface UpdateExternalSensorDataCallback {

    /**
     * Called when [com.mapbox.navigation.core.MapboxNavigation.updateExternalSensorData]
     * function call result is available
     *
     * @param result true if the sensor data was usable, false otherwise
     */
    fun onResult(result: Boolean)
}
