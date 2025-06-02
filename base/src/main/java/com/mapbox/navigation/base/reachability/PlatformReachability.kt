package com.mapbox.navigation.base.reachability

import com.mapbox.common.NetworkStatus.NOT_REACHABLE
import com.mapbox.common.NetworkStatus.REACHABLE_VIA_ETHERNET
import com.mapbox.common.NetworkStatus.REACHABLE_VIA_WI_FI
import com.mapbox.common.NetworkStatus.REACHABLE_VIA_WWAN
import com.mapbox.common.ReachabilityChanged
import com.mapbox.common.ReachabilityFactory
import com.mapbox.common.ReachabilityInterface
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI

/**
 * Interacts directly with the platform's reachability API to monitor network status.
 *
 * This class provides functionality for checking network reachability, getting the current
 * network status, and adding or removing listeners for network status changes.
 * It uses the provided [ReachabilityInterface] to interface with the platform's reachability API.
 *
 * @param reachability The interface responsible for checking network reachability and notifying
 *                     about network status changes.
 */
@ExperimentalPreviewMapboxNavigationAPI
class PlatformReachability(
    private val reachability: ReachabilityInterface,
) {

    private var listeners: MutableMap<PlatformReachabilityChanged, Long> = mutableMapOf()

    private val reachabilityChangedListener = ReachabilityChanged { status ->
        listeners.forEach { it.key.onChanged(toLocalNetworkStatus(status)) }
    }

    /**
     * Returns the current network status based on the platform's reachability API.
     *
     * This method translates the platform-specific [NetworkStatus] into a local enum that can
     * be used in the app. The status may be one of the following:
     * - NotReachable
     * - ReachableViaWiFi
     * - ReachableViaEthernet
     * - ReachableViaWWAN
     *
     * @return The current network status as a [NetworkStatus] enum.
     */
    fun currentNetworkStatus(): NetworkStatus {
        return toLocalNetworkStatus(reachability.currentNetworkStatus())
    }

    /**
     * Checks if the platform is reachable (i.e., if it is connected to a network).
     *
     * This method checks whether the device is currently connected to the internet or network.
     *
     * @return `true` if the platform is reachable, otherwise `false`.
     */
    fun isReachable(): Boolean {
        return reachability.isReachable
    }

    /**
     * Adds a listener to monitor changes in the network status.
     *
     * When the network status changes, the provided listener will be notified.
     *
     * @param listener The listener to be notified about network status changes.
     */
    fun addListener(listener: PlatformReachabilityChanged) {
        listeners[listener] = reachability.addListener(reachabilityChangedListener)
    }

    /**
     * Removes a listener from monitoring network status changes.
     *
     * @param listener The listener to be removed.
     * @return `true` if the listener was successfully removed, `false` otherwise.
     */
    fun removeListener(listener: PlatformReachabilityChanged): Boolean {
        return listeners.remove(listener)?.let { id ->
            reachability.removeListener(id)
        } ?: false
    }

    private fun toLocalNetworkStatus(status: com.mapbox.common.NetworkStatus): NetworkStatus {
        return when (status) {
            NOT_REACHABLE -> NetworkStatus.NotReachable
            REACHABLE_VIA_WI_FI -> NetworkStatus.ReachableViaWiFi
            REACHABLE_VIA_ETHERNET -> NetworkStatus.ReachableViaEthernet
            REACHABLE_VIA_WWAN -> NetworkStatus.ReachableViaWWAN
        }
    }

    companion object {

        /**
         * Create new [PlatformReachability]
         *
         * @param networkHostname network hostname
         * @return new [PlatformReachability]
         */
        fun create(networkHostname: String?): PlatformReachability {
            val reachability = ReachabilityFactory.reachability(networkHostname)
            return PlatformReachability(reachability)
        }
    }
}
