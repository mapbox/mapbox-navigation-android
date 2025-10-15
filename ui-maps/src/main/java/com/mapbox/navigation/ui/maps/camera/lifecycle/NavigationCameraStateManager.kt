package com.mapbox.navigation.ui.maps.camera.lifecycle

import com.mapbox.navigation.ui.maps.camera.NavigationCamera
import com.mapbox.navigation.ui.maps.camera.state.NavigationCameraState
import com.mapbox.navigation.ui.maps.camera.state.NavigationCameraStateChangedObserver
import com.mapbox.navigation.ui.maps.internal.camera.lifecycle.CameraStateManager

internal class NavigationCameraStateManager(
    private val navigationCamera: NavigationCamera,
) : CameraStateManager {

    override fun registerStateChangeObserver(observer: NavigationCameraStateChangedObserver) {
        navigationCamera.registerNavigationCameraStateChangeObserver(observer)
    }

    override fun unregisterStateChangeObserver(observer: NavigationCameraStateChangedObserver) {
        navigationCamera.unregisterNavigationCameraStateChangeObserver(observer)
    }

    override fun getCurrentState(): NavigationCameraState {
        return navigationCamera.state
    }

    override fun deactivate() {
        navigationCamera.requestNavigationCameraToIdle()
    }
}
