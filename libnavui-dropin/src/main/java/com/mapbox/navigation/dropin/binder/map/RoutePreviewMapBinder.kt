package com.mapbox.navigation.dropin.binder.map

import com.mapbox.maps.MapView
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.dropin.DropInNavigationViewContext
import com.mapbox.navigation.dropin.binder.Binder
import com.mapbox.navigation.dropin.binder.navigationListOf
import com.mapbox.navigation.dropin.component.camera.DropInCameraMode
import com.mapbox.navigation.dropin.component.camera.DropInNavigationCamera
import com.mapbox.navigation.dropin.component.location.LocationPuck
import com.mapbox.navigation.dropin.component.marker.LongPressMapComponent
import com.mapbox.navigation.dropin.component.marker.MapMarkersComponent
import com.mapbox.navigation.dropin.component.routeline.RouteLineComponent

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
internal class RoutePreviewMapBinder(
    private val context: DropInNavigationViewContext,
) : Binder<MapView> {

    private val cameraState = context.viewModel.cameraState

    override fun bind(mapView: MapView): MapboxNavigationObserver {
        cameraState.setCameraMode(DropInCameraMode.OVERVIEW)
        return navigationListOf(
            LocationPuck(mapView),
            DropInNavigationCamera(
                context.viewModel.cameraState,
                mapView
            ),
            MapMarkersComponent(
                mapView,
                context.mapAnnotationFactory(),
                context.viewModel
            ),
            LongPressMapComponent(
                mapView,
                context.viewModel,
            ),
            RouteLineComponent(
                mapView,
                context.routeLineOptions
            ),
        )
    }
}
