package com.mapbox.navigation.dropin.map

import com.mapbox.maps.MapView
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import java.util.concurrent.CopyOnWriteArraySet

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
internal class MapViewOwner {

    private var mapView: MapView? = null
    private val listeners = CopyOnWriteArraySet<MapViewObserver>()

    fun registerObserver(observer: MapViewObserver) {
        if (listeners.add(observer)) {
            mapView?.let {
                observer.onAttached(it)
            }
        }
    }

    fun unregisterObserver(observer: MapViewObserver) {
        if (listeners.remove(observer)) {
            mapView?.let {
                observer.onDetached(it)
            }
        }
    }

    fun updateMapView(mapView: MapView?) {
        this.mapView?.let {
            listeners.forEach { listener ->
                listener.onDetached(it)
            }
        }
        this.mapView = mapView
        mapView?.let {
            listeners.forEach { listener ->
                listener.onAttached(it)
            }
        }
    }
}
