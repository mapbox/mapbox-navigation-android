package com.mapbox.navigation.dropin.binder.map

import com.mapbox.maps.MapView
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.dropin.DropInNavigationViewContext
import com.mapbox.navigation.dropin.binder.Binder
import com.mapbox.navigation.dropin.binder.navigationListOf
import com.mapbox.navigation.dropin.component.camera.CameraComponent
import com.mapbox.navigation.dropin.component.location.LocationComponent
import com.mapbox.navigation.dropin.component.marker.GeocodingComponent
import com.mapbox.navigation.dropin.component.marker.LongPressMapComponent
import com.mapbox.navigation.dropin.component.marker.MapMarkersComponent
import com.mapbox.navigation.dropin.component.routeline.RouteLineComponent
import com.mapbox.navigation.dropin.lifecycle.reloadOnChange

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
internal class FreeDriveMapBinder(
    private val navigationViewContext: DropInNavigationViewContext,
) : Binder<MapView> {

    override fun bind(value: MapView): MapboxNavigationObserver {
        return navigationListOf(
            LocationComponent(
                value,
                navigationViewContext.viewModel.locationViewModel,
            ),
            reloadOnChange(navigationViewContext.options.routeLineOptions) { lineOptions ->
                RouteLineComponent(
                    value,
                    lineOptions,
                    navigationViewContext.viewModel.routesViewModel
                )
            },
            CameraComponent(
                value,
                navigationViewContext.viewModel.cameraViewModel,
                navigationViewContext.viewModel.locationViewModel,
                navigationViewContext.viewModel.navigationStateViewModel,
            ),
            MapMarkersComponent(value, navigationViewContext),
            LongPressMapComponent(
                value,
                navigationViewContext,
            ),
            GeocodingComponent(
                navigationViewContext.viewModel.destinationViewModel
            )
        )
    }
}
