package com.mapbox.navigation.ui.maps.internal.camera

import androidx.annotation.RestrictTo
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.ui.maps.camera.data.MapboxNavigationViewportDataSource

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
fun MapboxNavigationViewportDataSource.internalReevaluateRoute() {
    reevaluateRoute()
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
fun MapboxNavigationViewportDataSource.onRoutesChanged(routes: List<NavigationRoute>) {
    onRoutesChanged(routes)
}

var MapboxNavigationViewportDataSource.internalOptions
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    get() = internalOptions

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    set(value) { internalOptions = value }
