package com.mapbox.navigation.ui.car.map.internal

import android.graphics.Rect
import com.mapbox.base.common.logger.model.Message
import com.mapbox.base.common.logger.model.Tag
import com.mapbox.maps.EdgeInsets
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.ui.car.map.MapboxCarMap
import com.mapbox.navigation.ui.car.map.MapboxCarMapObserver
import com.mapbox.navigation.ui.car.map.MapboxCarMapSurface
import com.mapbox.navigation.utils.internal.ifNonNull
import com.mapbox.navigation.utils.internal.logI
import java.util.concurrent.CopyOnWriteArraySet

/**
 * @see MapboxCarMap to create new map experiences.
 *
 * Maintains the surface state for [MapboxCarMap].
 */
@ExperimentalMapboxNavigationAPI
internal class CarMapSurfaceOwner {

    internal var mapboxCarMapSurface: MapboxCarMapSurface? = null
        private set
    internal var visibleArea: Rect? = null
        private set
    internal var edgeInsets: EdgeInsets? = null
        private set

    private val carMapObservers = CopyOnWriteArraySet<MapboxCarMapObserver>()

    fun registerObserver(mapboxCarMapObserver: MapboxCarMapObserver) {
        carMapObservers.add(mapboxCarMapObserver)
        logI(TAG, Message("registerObserver + 1 = ${carMapObservers.size}"))

        mapboxCarMapSurface?.let { carMapSurface ->
            mapboxCarMapObserver.loaded(carMapSurface)
        }
        ifNonNull(mapboxCarMapSurface, visibleArea, edgeInsets) { _, area, edge ->
            logI(TAG, Message("registerObserver visibleAreaChanged"))
            mapboxCarMapObserver.visibleAreaChanged(area, edge)
        }
    }

    fun unregisterObserver(mapboxCarMapObserver: MapboxCarMapObserver) {
        carMapObservers.remove(mapboxCarMapObserver)
        mapboxCarMapSurface?.let { mapboxCarMapObserver.detached(it) }
        logI(TAG, Message("unregisterObserver - 1 = ${carMapObservers.size}"))
    }

    fun clearObservers() {
        val oldCarMapSurface = this.mapboxCarMapSurface
        oldCarMapSurface?.let { carMapObservers.forEach { it.detached(oldCarMapSurface) } }
        carMapObservers.clear()
    }

    fun surfaceAvailable(mapboxCarMapSurface: MapboxCarMapSurface) {
        logI(TAG, Message("surfaceAvailable"))
        val oldCarMapSurface = this.mapboxCarMapSurface
        this.mapboxCarMapSurface = mapboxCarMapSurface
        oldCarMapSurface?.let { carMapObservers.forEach { it.detached(oldCarMapSurface) } }
        carMapObservers.forEach { it.loaded(mapboxCarMapSurface) }
        notifyVisibleAreaChanged()
    }

    fun surfaceDestroyed() {
        logI(TAG, Message("surfaceDestroyed"))
        val detachSurface = this.mapboxCarMapSurface
        detachSurface?.mapSurface?.onStop()
        detachSurface?.mapSurface?.surfaceDestroyed()
        detachSurface?.mapSurface?.onDestroy()
        this.mapboxCarMapSurface = null
        detachSurface?.let { carMapObservers.forEach { it.detached(detachSurface) } }
    }

    fun surfaceVisibleAreaChanged(visibleArea: Rect) {
        logI(TAG, Message("surfaceVisibleAreaChanged"))
        this.visibleArea = visibleArea
        notifyVisibleAreaChanged()
    }

    private fun notifyVisibleAreaChanged() {
        this.edgeInsets = visibleArea?.edgeInsets()
        ifNonNull(mapboxCarMapSurface, visibleArea, edgeInsets) { _, area, edge ->
            logI(TAG, Message("notifyVisibleAreaChanged $area $edge"))
            carMapObservers.forEach {
                it.visibleAreaChanged(area, edge)
            }
        }
    }

    private fun Rect.edgeInsets(): EdgeInsets? {
        val surfaceContainer = mapboxCarMapSurface?.surfaceContainer ?: return null
        return EdgeInsets(
            top.toDouble(),
            left.toDouble(),
            (surfaceContainer.height - bottom).toDouble(),
            (surfaceContainer.width - right).toDouble()
        )
    }

    private companion object {
        private val TAG = Tag("CarMapSurfaceOwner")
    }
}
