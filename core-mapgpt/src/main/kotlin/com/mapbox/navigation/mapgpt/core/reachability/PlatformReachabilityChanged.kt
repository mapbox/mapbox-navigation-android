package com.mapbox.navigation.mapgpt.core.reachability

/**
 * Used to listen to changes in the platform's reachability API.
 *
 * TODO there is an issue with this callback or the [SharedReachability] implementation
 *   https://github.com/mapbox/mapbox-sdk-common/issues/3789
 */
fun interface PlatformReachabilityChanged {
    fun onChanged(status: NetworkStatus)
}
