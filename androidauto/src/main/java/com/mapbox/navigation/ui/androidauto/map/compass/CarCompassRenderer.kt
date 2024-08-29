package com.mapbox.navigation.ui.androidauto.map.compass

import com.mapbox.maps.CameraChangedCallback
import com.mapbox.maps.MapboxExperimental
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.extension.androidauto.MapboxCarMapObserver
import com.mapbox.maps.extension.androidauto.MapboxCarMapSurface
import com.mapbox.maps.extension.androidauto.widgets.CompassWidget

@OptIn(MapboxExperimental::class)
class CarCompassRenderer : MapboxCarMapObserver {

    private var mapboxMap: MapboxMap? = null
    private var compassWidget: CompassWidget? = null
    private val onCameraChangeListener = CameraChangedCallback { _ ->
        mapboxMap?.cameraState?.bearing?.toFloat()?.let { compassWidget?.setRotation(-it) }
    }

    override fun onAttached(mapboxCarMapSurface: MapboxCarMapSurface) {
        val compassWidget =
            CompassWidget(mapboxCarMapSurface.carContext, marginX = 26f, marginY = 120f)
        val mapboxMap = mapboxCarMapSurface.mapSurface.getMapboxMap().also { mapboxMap = it }
        this.compassWidget = compassWidget
        mapboxCarMapSurface.mapSurface.addWidget(compassWidget)
        mapboxMap.subscribeCameraChanged(onCameraChangeListener)
    }

    override fun onDetached(mapboxCarMapSurface: MapboxCarMapSurface) {
        compassWidget?.let { mapboxCarMapSurface.mapSurface.removeWidget(it) }
        compassWidget = null
        mapboxMap = null
    }
}
