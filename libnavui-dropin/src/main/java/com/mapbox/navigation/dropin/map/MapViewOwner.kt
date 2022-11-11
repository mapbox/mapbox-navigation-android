package com.mapbox.navigation.dropin.map

import androidx.annotation.VisibleForTesting
import com.mapbox.maps.MapView
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.CopyOnWriteArraySet

internal class MapViewOwner {

    private var mapView: MapView? = null
        set(value) {
            field = value
            _mapViews.value = value
        }
    private val observers = CopyOnWriteArraySet<MapViewObserver>()
    private val _mapViews = MutableStateFlow(mapView)
    val mapViews = _mapViews.asStateFlow()

    fun registerObserver(observer: MapViewObserver) {
        if (observers.add(observer)) {
            mapView?.let {
                observer.onAttached(it)
            }
        }
    }

    fun unregisterObserver(observer: MapViewObserver) {
        if (observers.remove(observer)) {
            mapView?.let {
                observer.onDetached(it)
            }
        }
    }

    fun updateMapView(mapView: MapView?) {
        this.mapView?.let {
            observers.forEach { listener ->
                listener.onDetached(it)
            }
        }
        this.mapView = mapView
        mapView?.let {
            observers.forEach { listener ->
                listener.onAttached(it)
            }
        }
    }

    @VisibleForTesting
    internal fun getRegisteredObservers() = observers.toSet()
}
