package com.mapbox.navigation.ui.maps.camera.lifecycle

/**
 * Invoked whenever an allowed zoom level change interaction is invoked by the maps gesture
 * recognizer.
 *
 * @see NavigationScaleGestureHandler
 */
fun interface NavigationScaleGestureActionListener {

    /**
     * Invoked whenever an allowed zoom level change interaction is invoked by the maps gesture
     * recognizer.
     */
    fun onNavigationScaleGestureAction()
}
