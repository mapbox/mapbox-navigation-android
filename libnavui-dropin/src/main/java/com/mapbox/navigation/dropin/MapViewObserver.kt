package com.mapbox.navigation.dropin

import com.mapbox.maps.MapView
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI

/**
 * Defines an object that needs to interact with or observe [MapView].
 */
@ExperimentalPreviewMapboxNavigationAPI
abstract class MapViewObserver {

    /**
     * Signals that [mapView] is ready to be used. Use this function to register [mapView] observers
     * or perform operations.
     */
    open fun onAttached(mapView: MapView) = Unit

    /**
     * Signals that [mapView] instance is being detached. Use this function to unregister [mapView]
     * observers that were registered in [onAttached]
     */
    open fun onDetached(mapView: MapView) = Unit
}
