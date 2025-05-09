package com.mapbox.navigation.ui.components.maps.internal.ui

import com.mapbox.annotation.MapboxExperimental
import com.mapbox.common.Cancelable
import com.mapbox.maps.CameraChangedCoalescedCallback
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
    private var onCameraChangeCallback: CameraChangedCoalescedCallback? = null
    private var cameraChangedSubscription: Cancelable? = null

    @OptIn(MapboxExperimental::class)
    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)

        if (mapboxMap != null) {
            compassButton.setOnClickListener {
                mapboxMap.flyTo(CameraOptions.Builder().bearing(.0).build())
            }
            onCameraChangeCallback = CameraChangedCoalescedCallback {
                compassButton.iconImage.rotation = -it.cameraState.bearing.toFloat()
            }.also {
                cameraChangedSubscription = mapboxMap.subscribeCameraChangedCoalesced(it)
            }
        }
    }

    @OptIn(MapboxExperimental::class)
    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        super.onDetached(mapboxNavigation)
        compassButton.setOnClickListener(null)
        cameraChangedSubscription?.cancel()
        onCameraChangeCallback = null
    }
}
