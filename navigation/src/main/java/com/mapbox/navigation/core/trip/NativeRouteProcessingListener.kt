package com.mapbox.navigation.core.trip

import com.mapbox.navigator.RouteAlternativesObserver

/**
 * Listener to get notified when native route processing starts so that we can ignore the following
 * [RouteAlternativesObserver.onRouteAlternativesChanged] and workaround the problem of an endless loop of alternatives updates.
 */
internal fun interface NativeRouteProcessingListener {
    fun onNativeRouteProcessingStarted()
}
