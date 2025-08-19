package com.mapbox.navigation.ui.maps.route.callout.api

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
fun interface RouteLayerIdProvider {
    fun getLayerId(routeId: String): String?
}
