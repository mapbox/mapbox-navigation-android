package com.mapbox.navigation.ui.maps.camera

/**
 * Callback that provides the current camera state
 *
 * @see [MapboxCameraState]
 */
interface MapboxCameraStateChangeObserver {
    fun onMapboxCameraStateChange(mapboxCameraState: MapboxCameraState)
}
