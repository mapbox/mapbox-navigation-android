package com.mapbox.navigation.ui.components.maps.internal.ui

import com.mapbox.common.Cancelable
import com.mapbox.maps.CameraChangedCallback
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.plugin.animation.flyTo
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.ui.base.lifecycle.UIComponent
import com.mapbox.navigation.ui.components.MapboxExtendableButton

class CompassButtonComponent(
    private val compassButton: MapboxExtendableButton,
    mapView: MapView?,
) : UIComponent() {

    private val mapboxMap: MapboxMap? = mapView?.getMapboxMap()
    private var onCameraChangeCallback: CameraChangedCallback? = null
    private var cameraChangedSubscription: Cancelable? = null

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)

        if (mapboxMap != null) {
            compassButton.setOnClickListener {
                mapboxMap.flyTo(CameraOptions.Builder().bearing(.0).build())
            }
            onCameraChangeCallback = CameraChangedCallback {
                compassButton.iconImage.rotation = -mapboxMap.cameraState.bearing.toFloat()
            }.also {
                cameraChangedSubscription = mapboxMap.subscribeCameraChanged(it)
            }
        }
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        super.onDetached(mapboxNavigation)
        compassButton.setOnClickListener(null)
        cameraChangedSubscription?.cancel()
        onCameraChangeCallback = null
    }
}
