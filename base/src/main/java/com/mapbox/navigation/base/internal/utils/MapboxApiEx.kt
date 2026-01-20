package com.mapbox.navigation.base.internal.utils

import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.route.ResponseOriginAPI
import com.mapbox.navigator.MapboxAPI

@ExperimentalMapboxNavigationAPI
@ResponseOriginAPI
fun MapboxAPI.mapToSDKResponseOriginAPI(): String {
    return when (this) {
        MapboxAPI.DIRECTIONS -> ResponseOriginAPI.DIRECTIONS_API
        MapboxAPI.MAP_MATCHING -> ResponseOriginAPI.MAP_MATCHING_API
    }
}
