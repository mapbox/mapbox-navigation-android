package com.mapbox.navigation.core.datainputs

import androidx.annotation.MainThread
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.navigator.internal.MapboxNativeNavigator

/**
 * Class that allows users to provide data from external sensors to the navigator.
 * Instance can be obtained via [MapboxNavigation.dataInputsManager].
 *
 * Not thread-safe, intended to be used from the main thread.
 */
@MainThread
@ExperimentalPreviewMapboxNavigationAPI
class DataInputsManager internal constructor(
    private val navigator: MapboxNativeNavigator,
) {

    /**
     * Updates the input service with the latest available odometry data.
     */
    fun updateOdometryData(data: OdometryData) {
        navigator.inputsService.updateOdometryData(data.mapToNative())
    }

    /**
     * Updates the input service with the latest available GNSS data.
     */
    fun updateRawGnssData(data: RawGnssData) {
        navigator.inputsService.updateRawGnssData(data.mapToNative())
    }

    /**
     * Updates the input service with the latest available compass data.
     */
    fun updateCompassData(data: CompassData) {
        navigator.inputsService.updateCompassData(data.mapToNative())
    }

    /**
     * Updates the input service with the latest available motion data.
     */
    fun updateMotionData(data: MotionData) {
        navigator.inputsService.updateMotionData(data.mapToNative())
    }

    /**
     * Updates the input service with the latest available altimeter data.
     */
    fun updateAltimeterData(data: AltimeterData) {
        navigator.inputsService.updateAltimeterData(data.mapToNative())
    }

    /**
     * Updates the input service with ETC gate information.
     */
    fun updateEtcGateInfo(info: EtcGateInfo) {
        navigator.inputsService.updateEtcGateInfo(info.mapToNative())
    }
}
