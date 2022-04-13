package com.mapbox.navigation.dropin.binder.map

import android.view.ViewGroup
import com.mapbox.maps.MapView
import com.mapbox.maps.plugin.compass.compass
import com.mapbox.maps.plugin.scalebar.scalebar
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.dropin.NavigationViewContext
import com.mapbox.navigation.dropin.binder.UIBinder
import com.mapbox.navigation.dropin.component.camera.CameraComponent
import com.mapbox.navigation.dropin.component.camera.CameraLayoutObserver
import com.mapbox.navigation.dropin.component.location.LocationComponent
import com.mapbox.navigation.dropin.component.marker.FreeDriveLongPressMapComponent
import com.mapbox.navigation.dropin.component.marker.GeocodingComponent
import com.mapbox.navigation.dropin.component.marker.MapMarkersComponent
import com.mapbox.navigation.dropin.component.marker.RoutePreviewLongPressMapComponent
import com.mapbox.navigation.dropin.component.navigation.NavigationState
import com.mapbox.navigation.dropin.component.routearrow.RouteArrowComponent
import com.mapbox.navigation.dropin.component.routeline.RouteLineComponent
import com.mapbox.navigation.dropin.databinding.MapboxNavigationViewLayoutBinding
import com.mapbox.navigation.dropin.internal.extensions.navigationListOf
import com.mapbox.navigation.dropin.internal.extensions.reloadOnChange
import com.mapbox.navigation.ui.maps.route.arrow.model.RouteArrowOptions

@ExperimentalPreviewMapboxNavigationAPI
internal class MapBinder(
    private val navigationViewContext: NavigationViewContext,
    private val binding: MapboxNavigationViewLayoutBinding,
    private val mapView: MapView
) : UIBinder {

    init {
        mapView.compass.enabled = false
        mapView.scalebar.enabled = false
    }

    override fun bind(viewGroup: ViewGroup): MapboxNavigationObserver {
        val navigationState = navigationViewContext.viewModel.navigationStateViewModel.state
        return navigationListOf(
            CameraLayoutObserver(
                mapView,
                binding,
                navigationViewContext.viewModel.cameraViewModel,
                navigationViewContext.viewModel.navigationStateViewModel,
            ),
            LocationComponent(
                mapView,
                navigationViewContext.viewModel.locationViewModel,
            ),
            reloadOnChange(
                navigationViewContext.mapStyleLoader.loadedMapStyle,
                navigationViewContext.options.routeLineOptions
            ) { _, lineOptions ->
                RouteLineComponent(
                    mapView,
                    lineOptions,
                    navigationViewContext.viewModel.routesViewModel
                )
            },
            CameraComponent(
                mapView,
                navigationViewContext.viewModel.cameraViewModel,
                navigationViewContext.viewModel.locationViewModel,
                navigationViewContext.viewModel.navigationStateViewModel,
            ),
            MapMarkersComponent(mapView, navigationViewContext),
            reloadOnChange(navigationState) {
                longPressMapComponent(it)
            },
            reloadOnChange(navigationState) {
                geocodingComponent(it)
            },
            reloadOnChange(
                navigationViewContext.mapStyleLoader.loadedMapStyle,
                navigationViewContext.options.routeArrowOptions,
                navigationState
            ) { _, arrowOptions, navState ->
                routeArrowComponent(navState, arrowOptions)
            }
        )
    }

    private fun longPressMapComponent(navigationState: NavigationState) =
        when (navigationState) {
            NavigationState.FreeDrive,
            NavigationState.DestinationPreview ->
                FreeDriveLongPressMapComponent(
                    mapView,
                    navigationViewContext.viewModel.navigationStateViewModel,
                    navigationViewContext.viewModel.routesViewModel,
                    navigationViewContext.viewModel.destinationViewModel,
                )
            NavigationState.RoutePreview ->
                RoutePreviewLongPressMapComponent(
                    mapView,
                    navigationViewContext.viewModel.locationViewModel,
                    navigationViewContext.viewModel.routesViewModel,
                    navigationViewContext.viewModel.destinationViewModel,
                )
            NavigationState.ActiveNavigation,
            NavigationState.Arrival ->
                null
        }

    private fun geocodingComponent(navigationState: NavigationState) =
        when (navigationState) {
            NavigationState.FreeDrive,
            NavigationState.DestinationPreview,
            NavigationState.RoutePreview ->
                GeocodingComponent(navigationViewContext.viewModel.destinationViewModel)
            NavigationState.ActiveNavigation,
            NavigationState.Arrival ->
                null
        }

    private fun routeArrowComponent(
        navigationState: NavigationState,
        arrowOptions: RouteArrowOptions
    ) = if (navigationState == NavigationState.ActiveNavigation) {
        RouteArrowComponent(mapView, arrowOptions)
    } else {
        null
    }
}
