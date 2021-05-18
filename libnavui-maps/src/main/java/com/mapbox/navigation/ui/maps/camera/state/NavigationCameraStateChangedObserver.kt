package com.mapbox.navigation.ui.maps.camera.state

/**
 * Observer that gets notified whenever [NavigationCameraState] changes.
 */
fun interface NavigationCameraStateChangedObserver {

    /**
     * Called whenever [NavigationCameraState] changes.
     * @param navigationCameraState current states
     */
    fun onNavigationCameraStateChanged(navigationCameraState: NavigationCameraState)
}
