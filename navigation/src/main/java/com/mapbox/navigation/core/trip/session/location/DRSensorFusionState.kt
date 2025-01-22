package com.mapbox.navigation.core.trip.session.location

import androidx.annotation.StringDef
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI

/**
 * Dead-reckoning sensor fuser state
 */
@ExperimentalMapboxNavigationAPI
object DRSensorFusionState {

    /**
     * Disabled status
     */
    const val DISABLED = "DISABLED"

    /**
     * Cold start status
     */
    const val COLD_START = "COLD_START"

    /**
     * Initialization status
     */
    const val INITIALIZATION = "INITIALIZATION"

    /**
     * Normal operation status
     */
    const val NORMAL_OPERATION = "NORMAL_OPERATION"

    /**
     * Failure status
     */
    const val FAILURE = "FAILURE"

    /**
     * Retention policy for the [DRSensorFusionState]
     */
    @Retention(AnnotationRetention.BINARY)
    @StringDef(
        DISABLED,
        COLD_START,
        INITIALIZATION,
        NORMAL_OPERATION,
        FAILURE,
    )
    annotation class State

    @JvmSynthetic
    @State
    internal fun createFromNativeObject(
        nativeState: com.mapbox.navigator.DRSensorFusionState,
    ) = when (nativeState) {
        com.mapbox.navigator.DRSensorFusionState.DISABLED -> DISABLED
        com.mapbox.navigator.DRSensorFusionState.COLD_START -> COLD_START
        com.mapbox.navigator.DRSensorFusionState.INITIALIZATION -> INITIALIZATION
        com.mapbox.navigator.DRSensorFusionState.NORMAL_OPERATION -> NORMAL_OPERATION
        com.mapbox.navigator.DRSensorFusionState.FAILURE -> FAILURE
    }
}
