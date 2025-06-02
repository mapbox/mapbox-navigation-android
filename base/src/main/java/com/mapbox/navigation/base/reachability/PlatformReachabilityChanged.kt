package com.mapbox.navigation.base.reachability

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI

/**
 * Used to listen to changes in the platform's reachability API.
 *
 * TODO there is an issue with this callback or the [SharedReachability] implementation
 *   https://github.com/mapbox/mapbox-sdk-common/issues/3789
 */
@ExperimentalPreviewMapboxNavigationAPI
fun interface PlatformReachabilityChanged {

    /**
     * @param status new [NetworkStatus]
     */
    fun onChanged(status: NetworkStatus)
}
