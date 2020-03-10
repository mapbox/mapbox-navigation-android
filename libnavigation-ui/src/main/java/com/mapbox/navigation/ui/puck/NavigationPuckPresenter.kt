package com.mapbox.navigation.ui.puck

import androidx.annotation.DrawableRes
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.mapbox.mapboxsdk.location.LocationComponentOptions
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.trip.session.RouteProgressObserver

class NavigationPuckPresenter(private val mapboxMap: MapboxMap, puckDrawableSupplier: PuckDrawableSupplier) : LifecycleObserver {

    private var mapboxNavigation: MapboxNavigation? = null
    private var observerRegistered = false

    private val routeProgressObserver = object : RouteProgressObserver {
        override fun onRouteProgressChanged(routeProgress: RouteProgress) {
            routeProgress.currentState()?.let {
                val drawable = puckDrawableSupplier.getPuckDrawable(it)
                updateCurrentLocationDrawable(drawable)
            }
        }
    }

    fun addProgressChangeListener(mapboxNavigation: MapboxNavigation) {
        this.mapboxNavigation = mapboxNavigation
        if (!observerRegistered) {
            mapboxNavigation.registerRouteProgressObserver(routeProgressObserver)
            observerRegistered = true
        }
    }

    /**
     * Call in {@link FragmentActivity#onStart()} to properly add the {@link RouteProgressObserver}
     * for the puck updating and prevent any leaks or further updates.
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onStart() {
        if (!observerRegistered) {
            mapboxNavigation?.registerRouteProgressObserver(routeProgressObserver)
            observerRegistered = true
        }
    }

    /**
     * Call in {@link FragmentActivity#onStop()} to properly remove the
     * {@link RouteProgressObserver}
     * for the puck updating and prevent any leaks or further updates.
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onStop() {
        mapboxNavigation?.unregisterRouteProgressObserver(routeProgressObserver)
        observerRegistered = false
    }

    private fun updateCurrentLocationDrawable(@DrawableRes gpsDrawable: Int) {
        val options: LocationComponentOptions = mapboxMap.locationComponent.locationComponentOptions
        if (options.gpsDrawable() != gpsDrawable) {
            val newOptions = options.toBuilder()
                    .gpsDrawable(gpsDrawable)
                    .padding(mapboxMap.padding)
                    .build()
            mapboxMap.locationComponent.applyStyle(newOptions)
        }
    }
}
