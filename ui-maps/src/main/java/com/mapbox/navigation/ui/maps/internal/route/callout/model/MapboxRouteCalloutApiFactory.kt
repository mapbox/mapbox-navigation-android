package com.mapbox.navigation.ui.maps.internal.route.callout.model

import androidx.annotation.RestrictTo
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.ui.maps.route.callout.api.MapboxRouteCalloutApi

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
object MapboxRouteCalloutApiFactory {
    fun create(): MapboxRouteCalloutApi {
        return MapboxRouteCalloutApi()
    }
}
