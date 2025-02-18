package com.mapbox.navigation.mapgpt.core.reachability

/**
 * Kotlin Multiplatform implementation of the platform's reachability API.
 */
sealed class NetworkStatus {
    object NotReachable : NetworkStatus()
    object ReachableViaWiFi : NetworkStatus()
    object ReachableViaEthernet : NetworkStatus()
    object ReachableViaWWAN : NetworkStatus()
}
