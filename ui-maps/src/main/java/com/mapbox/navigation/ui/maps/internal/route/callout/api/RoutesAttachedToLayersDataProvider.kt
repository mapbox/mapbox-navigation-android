package com.mapbox.navigation.ui.maps.internal.route.callout.api

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
fun interface RoutesAttachedToLayersObserver {
    fun onAttached(routeIdToLayer: Map<String, String>)
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
interface RoutesAttachedToLayersDataProvider {

    fun registerRoutesAttachedToLayersObserver(observer: RoutesAttachedToLayersObserver)

    fun unregisterRoutesAttachedToLayersObserver(observer: RoutesAttachedToLayersObserver)
}
