package com.mapbox.navigation.ui.androidauto.navigation

import com.mapbox.maps.extension.androidauto.MapboxCarMapObserver
import com.mapbox.maps.extension.androidauto.MapboxCarMapSurface
import com.mapbox.maps.plugin.delegates.listeners.OnStyleLoadedListener

class MapUserStyleObserver : MapboxCarMapObserver {
    var userId: String = ""
    var styleId: String = ""

    private val onStyleLoadedListener = OnStyleLoadedListener {
        updateState()
    }
    private var mapboxCarMapSurface: MapboxCarMapSurface? = null

    override fun onAttached(mapboxCarMapSurface: MapboxCarMapSurface) {
        super.onAttached(mapboxCarMapSurface)
        this.mapboxCarMapSurface = mapboxCarMapSurface
        updateState()
        mapboxCarMapSurface.mapSurface.getMapboxMap()
            .addOnStyleLoadedListener(onStyleLoadedListener)
    }

    private fun updateState() {
        mapboxCarMapSurface?.mapSurface?.getMapboxMap()?.getStyle()?.let { style ->
            val splits = style.styleURI.substringAfter("mapbox://styles/")
                .split("/", limit = 2)
            userId = splits[0]
            styleId = splits[1]
        }
    }

    override fun onDetached(mapboxCarMapSurface: MapboxCarMapSurface) {
        super.onDetached(mapboxCarMapSurface)
        mapboxCarMapSurface.mapSurface.getMapboxMap()
            .removeOnStyleLoadedListener(onStyleLoadedListener)
        userId = ""
        styleId = ""
        this.mapboxCarMapSurface = null
    }
}
