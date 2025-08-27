package com.mapbox.navigation.ui.maps.internal.route.callout.model

import androidx.annotation.RestrictTo
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.ui.maps.internal.route.callout.api.MapboxRouteCalloutsApi

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
object MapboxRouteCalloutApiFactory {
    @OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
    fun create(): MapboxRouteCalloutsApi {
        return MapboxRouteCalloutsApi()
    }
}
