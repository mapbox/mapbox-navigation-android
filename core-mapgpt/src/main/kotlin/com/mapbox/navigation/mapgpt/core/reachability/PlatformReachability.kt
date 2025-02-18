package com.mapbox.navigation.mapgpt.core.reachability

import com.mapbox.common.NetworkStatus.NOT_REACHABLE
import com.mapbox.common.NetworkStatus.REACHABLE_VIA_ETHERNET
import com.mapbox.common.NetworkStatus.REACHABLE_VIA_WI_FI
import com.mapbox.common.NetworkStatus.REACHABLE_VIA_WWAN
import com.mapbox.common.ReachabilityChanged
import com.mapbox.common.ReachabilityFactory
import com.mapbox.common.ReachabilityInterface

/**
 * Interact directly with the platform's reachability API.
 */
class PlatformReachability(
    private val reachability: ReachabilityInterface,
) {

    private var listeners: MutableMap<PlatformReachabilityChanged, Long> = mutableMapOf()

    private val reachabilityChangedListener = ReachabilityChanged { status ->
        listeners.forEach { it.key.onChanged(toLocalNetworkStatus(status)) }
    }

    fun currentNetworkStatus(): NetworkStatus {
        return toLocalNetworkStatus(reachability.currentNetworkStatus())
    }

    fun isReachable(): Boolean {
        return reachability.isReachable
    }

    fun addListener(listener: PlatformReachabilityChanged) {
        listeners[listener] = reachability.addListener(reachabilityChangedListener)
    }

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
        fun create(networkHostname: String?): PlatformReachability {
            val reachability = ReachabilityFactory.reachability(networkHostname)
            return PlatformReachability(reachability)
        }
    }
}
