@file:JvmName("MapboxCarMapSurfaceEx")
@file:OptIn(MapboxExperimental::class)

package com.mapbox.androidauto.car.internal.extensions

import com.mapbox.maps.MapboxExperimental
import com.mapbox.maps.Style
import com.mapbox.maps.extension.androidauto.MapboxCarMapSurface
import com.mapbox.maps.plugin.delegates.listeners.OnStyleLoadedListener

internal fun MapboxCarMapSurface.handleStyleOnAttached(
    block: (Style) -> Unit
): OnStyleLoadedListener {
    getStyle()?.let { block(it) }

    return OnStyleLoadedListener {
        getStyle()?.let { block(it) }
    }.also {
        mapSurface.getMapboxMap().addOnStyleLoadedListener(it)
    }
}

internal fun MapboxCarMapSurface.handleStyleOnDetached(
    listener: OnStyleLoadedListener?,
): Style? {
    listener?.let { mapSurface.getMapboxMap().removeOnStyleLoadedListener(it) }
    return getStyle()
}

internal fun MapboxCarMapSurface.getStyle(): Style? {
    return mapSurface.getMapboxMap().getStyle()
}

internal fun MapboxCarMapSurface.getStyleAsync(block: (Style) -> Unit) {
    mapSurface.getMapboxMap().getStyle { block(it) }
}
