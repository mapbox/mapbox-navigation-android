package com.mapbox.navigation.ui.maps.camera.internal

import androidx.annotation.RestrictTo
import com.mapbox.navigation.ui.maps.camera.data.MapboxNavigationViewportDataSource

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
fun MapboxNavigationViewportDataSource.internalReevaluateRoute() {
    reevaluateRoute()
}
