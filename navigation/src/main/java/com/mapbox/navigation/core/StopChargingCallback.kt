package com.mapbox.navigation.core

import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI

/**
 * Interface definition for a callback to be invoked when [MapboxNavigation.stopCharging]
 * finishes resolving whether the next route leg was started.
 */
@ExperimentalMapboxNavigationAPI
fun interface StopChargingCallback {

    /**
     * Called once [MapboxNavigation.stopCharging] is resolved (or immediately if changing leg
     * isn't possible).
     *
     * @param data describes the outcome of the operation
     */
    fun onFinished(data: ChargingFinishedData)
}
