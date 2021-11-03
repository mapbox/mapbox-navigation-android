package com.mapbox.androidauto.surfacelayer

import android.graphics.Rect
import androidx.annotation.CallSuper
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapboxExperimental
import com.mapbox.maps.extension.androidauto.MapboxCarMapObserver
import com.mapbox.maps.extension.androidauto.MapboxCarMapSurface

/**
 * Simplify the classes that need to extend the [MapboxCarMapObserver]
 *
 * This class is meant to have [children] so you don't
 * have to forward the calls and store surface state.
 */
@OptIn(MapboxExperimental::class)
open class CarSurfaceLayer : MapboxCarMapObserver {
    protected var mapboxCarMapSurface: MapboxCarMapSurface? = null
        private set
    var visibleArea: Rect? = null
        private set
    var edgeInsets: EdgeInsets? = null
        private set
    fun surfaceDimensions() = mapboxCarMapSurface
        ?.surfaceContainer?.run { Pair(width, height) }

    /**
     * This allows you to create children listeners.
     * Children are notified after the parent.
     */
    open fun children(): List<MapboxCarMapObserver> = emptyList()

    @CallSuper
    override fun onAttached(mapboxCarMapSurface: MapboxCarMapSurface) {
        this.mapboxCarMapSurface = mapboxCarMapSurface
        notifyChildren { onAttached(mapboxCarMapSurface) }
    }

    @CallSuper
    override fun onVisibleAreaChanged(visibleArea: Rect, edgeInsets: EdgeInsets) {
        this.visibleArea = visibleArea
        this.edgeInsets = edgeInsets
        notifyChildren { onVisibleAreaChanged(visibleArea, edgeInsets) }
    }

    @CallSuper
    override fun onDetached(mapboxCarMapSurface: MapboxCarMapSurface) {
        this.mapboxCarMapSurface = null
        this.visibleArea = null
        this.edgeInsets = null
        notifyChildren { onDetached(mapboxCarMapSurface) }
    }

    private fun notifyChildren(
        method: MapboxCarMapObserver.() -> Unit
    ) {
        children().forEach { childListener ->
            when (childListener) {
                is CarSurfaceLayer -> notifyListenerAndChildren(childListener, method)
                else -> childListener.method()
            }
        }
    }

    private fun notifyListenerAndChildren(
        layer: CarSurfaceLayer,
        method: MapboxCarMapObserver.() -> Unit
    ) {
        layer.method()
        layer.children().forEach { childListener ->
            when (childListener) {
                is CarSurfaceLayer -> notifyListenerAndChildren(childListener, method)
                else -> childListener.method()
            }
        }
    }
}
