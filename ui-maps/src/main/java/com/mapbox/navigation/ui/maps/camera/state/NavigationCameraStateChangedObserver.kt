package com.mapbox.navigation.ui.maps.camera.state

import androidx.annotation.UiThread

/**
 * Observer that gets notified whenever [NavigationCameraState] changes.
 */
fun interface NavigationCameraStateChangedObserver {

    /**
     * Called whenever [NavigationCameraState] changes.
     * @param navigationCameraState current states
     */
    @UiThread
    fun onNavigationCameraStateChanged(navigationCameraState: NavigationCameraState)
}
