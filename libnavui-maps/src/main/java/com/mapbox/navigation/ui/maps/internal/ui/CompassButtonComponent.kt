package com.mapbox.navigation.ui.maps.internal.ui

import androidx.core.view.isVisible
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.plugin.animation.flyTo
import com.mapbox.maps.plugin.delegates.listeners.OnCameraChangeListener
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.ui.base.lifecycle.UIComponent
import com.mapbox.navigation.ui.base.view.MapboxExtendableButton

@ExperimentalPreviewMapboxNavigationAPI
class CompassButtonComponent(
    private val compassButton: MapboxExtendableButton,
    mapView: MapView?,
    private val enabled: Boolean
) : UIComponent() {

    private val mapboxMap: MapboxMap? = mapView?.getMapboxMap()
    private var onCameraChangeListener: OnCameraChangeListener? = null

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)

        compassButton.isVisible = enabled
        if (mapboxMap != null && enabled) {
            compassButton.setOnClickListener {
                mapboxMap.flyTo(CameraOptions.Builder().bearing(.0).build())
            }
            onCameraChangeListener = OnCameraChangeListener {
                compassButton.iconImage.rotation = -mapboxMap.cameraState.bearing.toFloat()
            }.also { mapboxMap.addOnCameraChangeListener(it) }
        }
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        super.onDetached(mapboxNavigation)
        compassButton.setOnClickListener(null)
        onCameraChangeListener?.let { mapboxMap?.removeOnCameraChangeListener(it) }
        onCameraChangeListener = null
    }
}
