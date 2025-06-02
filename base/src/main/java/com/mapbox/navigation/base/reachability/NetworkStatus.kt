package com.mapbox.navigation.base.reachability

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI

/**
 * Kotlin Multiplatform implementation of the platform's reachability API.
 */
@ExperimentalPreviewMapboxNavigationAPI
sealed class NetworkStatus {

    /** Represents a state where the network is not reachable. */
    object NotReachable : NetworkStatus()

    /** Represents a state where the network is reachable via Wi-Fi. */
    object ReachableViaWiFi : NetworkStatus()

    /** Represents a state where the network is reachable via an Ethernet connection. */
    object ReachableViaEthernet : NetworkStatus()

    /** Represents a state where the network is reachable via a mobile network (WWAN). */
    object ReachableViaWWAN : NetworkStatus()
}
