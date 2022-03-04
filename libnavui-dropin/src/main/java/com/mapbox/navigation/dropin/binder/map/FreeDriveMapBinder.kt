package com.mapbox.navigation.dropin.binder.map

import com.mapbox.maps.MapView
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.dropin.DropInNavigationViewContext
import com.mapbox.navigation.dropin.binder.Binder
import com.mapbox.navigation.dropin.binder.navigationListOf
import com.mapbox.navigation.dropin.component.camera.CameraComponent
import com.mapbox.navigation.dropin.component.location.LocationPuck

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
internal class FreeDriveMapBinder(
    private val navigationViewContext: DropInNavigationViewContext,
) : Binder<MapView> {

    override fun bind(mapView: MapView): MapboxNavigationObserver {
        return navigationListOf(
            LocationPuck(mapView),
            CameraComponent(mapView, navigationViewContext.viewModel.cameraViewModel)
            /*DropInNavigationCamera(
                navigationViewContext.viewModel.cameraState,
                mapView
            ),*/
        )
    }
}
