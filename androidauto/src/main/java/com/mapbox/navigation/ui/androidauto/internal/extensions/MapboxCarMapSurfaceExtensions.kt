@file:JvmName("MapboxCarMapSurfaceEx")

package com.mapbox.navigation.ui.androidauto.internal.extensions

import com.mapbox.maps.Style
import com.mapbox.maps.extension.androidauto.MapboxCarMapSurface
import com.mapbox.maps.plugin.delegates.listeners.OnStyleLoadedListener
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

@OptIn(ExperimentalCoroutinesApi::class)
fun MapboxCarMapSurface.styleFlow(): Flow<Style> {
    return callbackFlow {
        getStyle()?.let { trySend(it) }
        val listener = OnStyleLoadedListener {
            getStyle()?.let { trySend(it) }
        }
        mapSurface.getMapboxMap().addOnStyleLoadedListener(listener)
        awaitClose { mapSurface.getMapboxMap().removeOnStyleLoadedListener(listener) }
    }
}

fun MapboxCarMapSurface.getStyle(): Style? {
    return mapSurface.getMapboxMap().getStyle()
}
