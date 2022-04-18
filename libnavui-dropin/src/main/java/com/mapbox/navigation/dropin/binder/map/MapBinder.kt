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
    private val context: NavigationViewContext,
    private val binding: MapboxNavigationViewLayoutBinding,
    private val mapView: MapView
) : UIBinder {

    init {
        mapView.compass.enabled = false
        mapView.scalebar.enabled = false
    }

    override fun bind(viewGroup: ViewGroup): MapboxNavigationObserver {
        val navigationState = context.viewModel.navigationStateViewModel.state
        return navigationListOf(
            CameraLayoutObserver(
                mapView,
                binding,
                context.viewModel.cameraViewModel,
                context.viewModel.navigationStateViewModel,
            ),
            LocationComponent(
                mapView,
                context.viewModel.locationViewModel,
            ),
            reloadOnChange(
                context.mapStyleLoader.loadedMapStyle,
                context.options.routeLineOptions
            ) { _, lineOptions ->
                RouteLineComponent(
                    mapView,
                    lineOptions,
                    context.viewModel.routesViewModel
                )
            },
            CameraComponent(
                mapView,
                context.viewModel.cameraViewModel,
                context.viewModel.locationViewModel,
                context.viewModel.navigationStateViewModel,
            ),
            reloadOnChange(
                context.styles.destinationMarker
            ) { marker ->
                MapMarkersComponent(mapView = mapView, iconImage = marker, context = context)
            },
            reloadOnChange(navigationState) {
                longPressMapComponent(it)
            },
            reloadOnChange(navigationState) {
                geocodingComponent(it)
            },
            reloadOnChange(
                context.mapStyleLoader.loadedMapStyle,
                context.options.routeArrowOptions,
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
                    context.viewModel.navigationStateViewModel,
                    context.viewModel.routesViewModel,
                    context.viewModel.destinationViewModel,
                )
            NavigationState.RoutePreview ->
                RoutePreviewLongPressMapComponent(
                    mapView,
                    context.viewModel.locationViewModel,
                    context.viewModel.routesViewModel,
                    context.viewModel.destinationViewModel,
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
                GeocodingComponent(context.viewModel.destinationViewModel)
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
