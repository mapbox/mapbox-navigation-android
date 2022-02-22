package com.mapbox.navigation.dropin.binder.map

import com.mapbox.maps.MapView
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.dropin.DropInNavigationViewContext
import com.mapbox.navigation.dropin.binder.Binder
import com.mapbox.navigation.dropin.binder.navigationListOf
import com.mapbox.navigation.dropin.component.camera.CameraComponent
import com.mapbox.navigation.dropin.component.location.LocationPuck
import com.mapbox.navigation.dropin.component.marker.LongPressMapComponent
import com.mapbox.navigation.dropin.component.marker.MapMarkersComponent

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
internal class FreeDriveMapBinder(
    private val navigationViewContext: DropInNavigationViewContext,
) : Binder<MapView> {

    override fun bind(mapView: MapView): MapboxNavigationObserver {
        return navigationListOf(
            LocationPuck(mapView),
            CameraComponent(
                mapView,
                navigationViewContext.viewModel.locationViewModel,
                navigationViewContext.viewModel.cameraViewModel,
            ),
            MapMarkersComponent(mapView, navigationViewContext),
            LongPressMapComponent(
                mapView,
                navigationViewContext,
            ),
        )
    }
}
