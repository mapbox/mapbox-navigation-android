package com.mapbox.navigation.ui.maps.camera.lifecycle

import com.mapbox.navigation.ui.maps.camera.NavigationCamera
import com.mapbox.navigation.ui.maps.internal.camera.NavigationCameraStateChangedObserverInternal
import com.mapbox.navigation.ui.maps.internal.camera.NavigationCameraStateInternal
import com.mapbox.navigation.ui.maps.internal.camera.lifecycle.CameraStateManager

internal class NavigationCameraStateManager(
    private val navigationCamera: NavigationCamera,
) : CameraStateManager {

    override fun registerStateChangeObserver(
        observer: NavigationCameraStateChangedObserverInternal,
    ) {
        navigationCamera.registerNavigationCameraStateChangeObserverInternal(observer)
    }

    override fun unregisterStateChangeObserver(
        observer: NavigationCameraStateChangedObserverInternal,
    ) {
        navigationCamera.unregisterNavigationCameraStateChangeObserverInternal(observer)
    }

    override fun getCurrentState(): NavigationCameraStateInternal {
        return navigationCamera.stateInternal
    }

    override fun disable() {
        navigationCamera.requestNavigationCameraToIdle()
    }
}
