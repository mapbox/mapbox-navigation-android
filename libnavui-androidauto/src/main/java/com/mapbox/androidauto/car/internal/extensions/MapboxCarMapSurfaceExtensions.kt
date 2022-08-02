@file:JvmName("MapboxCarMapSurfaceEx")
@file:OptIn(MapboxExperimental::class)

package com.mapbox.androidauto.car.internal.extensions

import com.mapbox.maps.MapboxExperimental
import com.mapbox.maps.Style
import com.mapbox.maps.extension.androidauto.MapboxCarMapSurface
import com.mapbox.maps.plugin.delegates.listeners.OnStyleLoadedListener
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

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

@OptIn(ExperimentalCoroutinesApi::class)
internal fun MapboxCarMapSurface.flowStyle(): Flow<Style> = callbackFlow {
    val listener = handleStyleOnAttached {
        trySend(it)
    }
    awaitClose {
        handleStyleOnDetached(listener)
    }
}

internal fun MapboxCarMapSurface.getStyle(): Style? {
    return mapSurface.getMapboxMap().getStyle()
}

internal fun MapboxCarMapSurface.getStyleAsync(block: (Style) -> Unit) {
    mapSurface.getMapboxMap().getStyle { block(it) }
}
