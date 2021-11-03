package com.mapbox.navigation.ui.car.map

import android.graphics.Rect
import androidx.car.app.CarContext
import androidx.car.app.model.Template
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapSurface
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.ui.car.map.internal.CarMapLifecycleObserver
import com.mapbox.navigation.ui.car.map.internal.CarMapSurfaceOwner

/**
 * The [androidx.car.app.Session] has a [Lifecycle], attach it to this map
 * and then [registerObserver] your implementations to create custom experiences. Attach the
 * [androidx.car.app.Screen] lifecycle when you only want the map on a specific Screens.
 *
 * @param mapboxCarOptions provide default options for the map.
 * @param carContext ContextWrapper accessible through your CarAppService Session or Screen
 * @param lifecycle from the component that shows the map.
 */
@ExperimentalMapboxNavigationAPI
class MapboxCarMap constructor(
    mapboxCarOptions: MapboxCarOptions,
    carContext: CarContext,
    lifecycle: Lifecycle
) {
    private val carMapSurfaceSession = CarMapSurfaceOwner()
    private val carMapLifecycleObserver = CarMapLifecycleObserver(
        carContext,
        carMapSurfaceSession,
        mapboxCarOptions
    )

    /**
     * Accessor for the current [MapboxCarMapSurface]. Use [registerObserver] to observe
     * changes, and make sure to [unregisterObserver]
     */
    val mapboxCarMapSurface: MapboxCarMapSurface?
        get() = carMapSurfaceSession.mapboxCarMapSurface

    /**
     * Accessor for the current visibleArea [Rect]. This will change when [Template] views are
     * on top of the map surface. Similar to [edgeInsets] except that it represents size.
     *
     * left = min position of the width
     * right = max position of the width
     * top = min position of the height
     * bottom = max position of the height
     */
    val visibleArea: Rect?
        get() = carMapSurfaceSession.visibleArea

    /**
     * Accessor for the current edgeInsets [Rect]. This will change when [Template] views are
     * on top of the map surface. Similar to [visibleArea] except that it represents edge distance.
     */
    val edgeInsets: EdgeInsets?
        get() = carMapSurfaceSession.edgeInsets

    init {
        lifecycle.addObserver(carMapLifecycleObserver)
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                clearListeners()
            }
        })
    }

    /**
     * Register an observer to receive updates for the [MapboxCarMapSurface] and then dimensions.
     * Implement the [MapboxCarMapObserver] interface to build your own [MapSurface] editors.
     * When you call [registerObserver], the [MapboxCarMapObserver.loaded] will be called when
     * the map has already loaded, and the [MapboxCarMapObserver.visibleAreaChanged] will be called
     * when the map has visible dimensions. When these values are not ready, the calls be made
     * when they become available.
     *
     * @see unregisterObserver
     */
    fun registerObserver(mapboxCarMapObserver: MapboxCarMapObserver) = apply {
        carMapSurfaceSession.registerObserver(mapboxCarMapObserver)
    }

    /**
     * Unregister an observer to stop receiving updates for the [MapboxCarMapSurface]. When you
     * unregister the observer, the [MapboxCarMapObserver.detached] is immediately called if
     * the surface was [MapboxCarMapObserver.loaded].
     *
     * @see registerObserver
     */
    fun unregisterObserver(mapboxCarMapObserver: MapboxCarMapObserver) {
        carMapSurfaceSession.unregisterObserver(mapboxCarMapObserver)
    }

    /**
     * Unregister all observers to stop receiving updates for the [MapboxCarMapSurface]. Every
     * observer's [MapboxCarMapObserver.detached] will be called if the surface was
     * [MapboxCarMapObserver.loaded].
     *
     * @see registerObserver
     * @see unregisterObserver
     */
    fun clearListeners() {
        carMapSurfaceSession.clearObservers()
    }

    /**
     * Allows you to update the map style at any time.
     *
     * @see registerObserver
     * @see unregisterObserver
     */
    fun updateMapStyle(mapStyle: String) {
        carMapLifecycleObserver.updateMapStyle(mapStyle)
    }
}
