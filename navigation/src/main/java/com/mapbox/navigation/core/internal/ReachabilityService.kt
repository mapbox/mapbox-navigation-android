package com.mapbox.navigation.core.internal

import com.mapbox.common.ReachabilityFactory
import com.mapbox.common.ReachabilityInterface
import com.mapbox.navigation.utils.internal.ConnectivityHandler

object ReachabilityService {
    private val reachabilityInterface: ReachabilityInterface =
        ReachabilityFactory.reachability(null)

    fun addReachabilityObserver(connectivityHandler: ConnectivityHandler): Long {
        return reachabilityInterface.addListener(connectivityHandler)
    }

    fun removeReachabilityObserver(reachabilityObserverId: Long) {
        reachabilityInterface.removeListener(reachabilityObserverId)
    }
}
