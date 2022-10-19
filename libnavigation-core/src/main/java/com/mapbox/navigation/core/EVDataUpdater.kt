package com.mapbox.navigation.core

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI

/**
 * Observer to be notified of EV data changes.
 * See [EVDataUpdater.registerEVDataObserver].
 */
@ExperimentalPreviewMapboxNavigationAPI
fun interface EVDataObserver {
    /**
     * Invoke when any component of EV data is changed.
     * Pass <b>only changed</b> components of EV data via [data].
     * Example: if you previously invoked this method with the following map:
     * <code>
     *     mapOf(
     *         "ev_initial_charge" to "90",
     *         "energy_consumption_curve" to "0,300;20,120;40,150",
     *         "auxiliary_consumption" to "300"
     *     )
     * </code>
     * and then the charge changes to 80, you should invoke this method with
     * <code>
     *     mapOf("ev_initial_charge" to "80")
     * </code>
     * as an argument. This way "ev_initial_charge" will be updated and the following parameters
     * will be used from the previous invocation.
     * If you want to remove a parameter, pass `null` for the corresponding key.
     * Example: for the case above if you want to remove "auxiliary_consumption", invoke this method
     * with
     * <code>
     *     mapOf("auxiliary_consumption" to null)
     * </code>
     * as an argument.
     *
     * @param data Map describing the changed EV data
     */
    fun onEVDataUpdated(data: Map<String, String?>)
}

/**
 * Objects implementing this interface should notify registered observers of EV data updates
 * so that they can be used in route refresh requests.
 * See [MapboxNavigation.setEVDataUpdater].
 */
@ExperimentalPreviewMapboxNavigationAPI
interface EVDataUpdater {
    /**
     * Register [EVDataObserver]. Objects implementing this interface should start sending
     * EV data updates to this observer.
     *
     * @param observer [EVDataObserver] to be added
     */
    fun registerEVDataObserver(observer: EVDataObserver)

    /**
     * Unregister [EVDataObserver]. Objects implementing this interface should stop sending
     * EV data updates to this observer.
     *
     * @param observer [EVDataObserver] to be removed
     */
    fun unregisterEVDataObserver(observer: EVDataObserver)
}
