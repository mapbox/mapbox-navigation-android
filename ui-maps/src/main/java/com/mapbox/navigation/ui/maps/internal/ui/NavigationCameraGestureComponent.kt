package com.mapbox.navigation.ui.maps.internal.ui

import com.mapbox.maps.MapView
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.ui.base.lifecycle.UIComponent
import com.mapbox.navigation.ui.maps.camera.NavigationCamera
import com.mapbox.navigation.ui.maps.camera.lifecycle.NavigationBasicGesturesHandler

internal class NavigationCameraGestureComponent(
    private val mapView: MapView,
    navigationCamera: NavigationCamera,
) : UIComponent() {

    private val gesturesHandler = NavigationBasicGesturesHandler(navigationCamera)

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)
        mapView.camera.addCameraAnimationsLifecycleListener(gesturesHandler)
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        super.onDetached(mapboxNavigation)
        mapView.camera.removeCameraAnimationsLifecycleListener(gesturesHandler)
    }
}
