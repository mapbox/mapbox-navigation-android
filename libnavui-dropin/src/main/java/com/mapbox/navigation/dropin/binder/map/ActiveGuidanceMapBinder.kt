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
import com.mapbox.navigation.dropin.component.marker.MapMarkersComponent
import com.mapbox.navigation.dropin.component.routeline.RouteLineComponent

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
internal class ActiveGuidanceMapBinder(
    private val context: DropInNavigationViewContext,
) : Binder<MapView> {

    private val viewModel = context.viewModel

    override fun bind(mapView: MapView): MapboxNavigationObserver {
        viewModel.cameraState.setCameraMode(DropInCameraMode.FOLLOWING)
        return navigationListOf(
            LocationPuck(mapView),
            DropInNavigationCamera(
                viewModel.cameraState,
                mapView
            ),
            MapMarkersComponent(
                mapView,
                context.mapAnnotationFactory(),
                viewModel
            ),
            RouteLineComponent(
                mapView,
                context.routeLineOptions
            ),
        )
    }
}
