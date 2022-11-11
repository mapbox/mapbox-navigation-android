package com.mapbox.navigation.dropin.map

import com.mapbox.maps.MapView

/**
 * Defines an object that needs to interact with or observe [MapView].
 */
abstract class MapViewObserver {

    /**
     * Signals that [mapView] is attached and ready to use. Use this function to register [mapView]
     * observers or perform operations.
     */
    open fun onAttached(mapView: MapView) = Unit

    /**
     * Signals that [mapView] instance is being detached. Use this function to unregister [mapView]
     * observers that were registered in [onAttached]
     */
    open fun onDetached(mapView: MapView) = Unit
}
