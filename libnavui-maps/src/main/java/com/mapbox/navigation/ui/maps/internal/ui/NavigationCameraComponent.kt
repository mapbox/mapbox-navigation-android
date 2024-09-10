package com.mapbox.navigation.ui.maps.internal.ui

import androidx.annotation.RestrictTo
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.internal.extensions.flowLocationMatcherResult
import com.mapbox.navigation.core.internal.extensions.flowRouteProgress
import com.mapbox.navigation.ui.base.lifecycle.UIComponent
import com.mapbox.navigation.ui.maps.camera.NavigationCamera
import com.mapbox.navigation.ui.maps.camera.data.MapboxNavigationViewportDataSource
import kotlinx.coroutines.flow.map

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
class NavigationCameraComponent(
    val viewportDataSource: MapboxNavigationViewportDataSource,
    val navigationCamera: NavigationCamera,
) : UIComponent() {

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)

        mapboxNavigation.flowRouteProgress().observe {
            viewportDataSource.onRouteProgressChanged(it)
            viewportDataSource.evaluate()
        }

        mapboxNavigation.flowLocationMatcherResult().map { it.enhancedLocation }.observe {
            viewportDataSource.onLocationChanged(it)
            viewportDataSource.evaluate()
        }
    }
}
