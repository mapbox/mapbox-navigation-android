package com.mapbox.navigation.dropin.binder.map

import android.view.ViewGroup
import com.mapbox.maps.MapView
import com.mapbox.maps.plugin.compass.compass
import com.mapbox.maps.plugin.scalebar.scalebar
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.internal.extensions.navigationListOf
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.dropin.NavigationViewContext
import com.mapbox.navigation.dropin.component.camera.CameraComponent
import com.mapbox.navigation.dropin.component.camera.CameraLayoutObserver
import com.mapbox.navigation.dropin.component.location.LocationComponent
import com.mapbox.navigation.dropin.component.logo.LogoAttributionComponent
import com.mapbox.navigation.dropin.component.marker.FreeDriveLongPressMapComponent
import com.mapbox.navigation.dropin.component.marker.GeocodingComponent
import com.mapbox.navigation.dropin.component.marker.MapMarkersComponent
import com.mapbox.navigation.dropin.component.marker.RoutePreviewLongPressMapComponent
import com.mapbox.navigation.dropin.databinding.MapboxNavigationViewLayoutBinding
import com.mapbox.navigation.dropin.internal.extensions.reloadOnChange
import com.mapbox.navigation.ui.app.internal.SharedApp
import com.mapbox.navigation.ui.app.internal.Store
import com.mapbox.navigation.ui.app.internal.navigation.NavigationState
import com.mapbox.navigation.ui.app.internal.routefetch.RoutePreviewAction
import com.mapbox.navigation.ui.app.internal.routefetch.RoutePreviewState
import com.mapbox.navigation.ui.app.internal.routefetch.RoutesAction
import com.mapbox.navigation.ui.base.lifecycle.UIBinder
import com.mapbox.navigation.ui.maps.internal.ui.RouteArrowComponent
import com.mapbox.navigation.ui.maps.internal.ui.RouteLineComponent
import com.mapbox.navigation.ui.maps.internal.ui.RouteLineComponentContract
import com.mapbox.navigation.ui.maps.route.arrow.model.RouteArrowOptions
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

@ExperimentalPreviewMapboxNavigationAPI
@FlowPreview
internal class MapBinder(
    private val context: NavigationViewContext,
    private val binding: MapboxNavigationViewLayoutBinding,
    private val mapView: MapView
) : UIBinder {

    init {
        mapView.compass.enabled = false
        mapView.scalebar.enabled = false
    }

    private val store = context.store

    override fun bind(viewGroup: ViewGroup): MapboxNavigationObserver {
        val navigationState = store.select { it.navigation }
        return navigationListOf(
            CameraLayoutObserver(store, mapView, binding),
            LocationComponent(mapView, SharedApp.locationStateController),
            LogoAttributionComponent(mapView, context.systemBarsInsets),
            reloadOnChange(
                context.mapStyleLoader.loadedMapStyle,
                context.options.routeLineOptions
            ) { _, lineOptions ->
                routeLineComponent(lineOptions)
            },
            CameraComponent(store, mapView),
            reloadOnChange(
                context.styles.destinationMarker
            ) { marker ->
                MapMarkersComponent(store, mapView, marker)
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

    private fun routeLineComponent(lineOptions: MapboxRouteLineOptions) =
        RouteLineComponent(mapView.getMapboxMap(), mapView, lineOptions, contractProvider = {
            RouteLineComponentContractImpl(store)
        })

    private fun longPressMapComponent(navigationState: NavigationState) =
        when (navigationState) {
            NavigationState.FreeDrive,
            NavigationState.DestinationPreview ->
                FreeDriveLongPressMapComponent(store, mapView, context)
            NavigationState.RoutePreview ->
                RoutePreviewLongPressMapComponent(store, mapView, context)
            NavigationState.ActiveNavigation,
            NavigationState.Arrival ->
                null
        }

    private fun geocodingComponent(navigationState: NavigationState) =
        when (navigationState) {
            NavigationState.FreeDrive,
            NavigationState.DestinationPreview,
            NavigationState.RoutePreview ->
                GeocodingComponent(store)
            NavigationState.ActiveNavigation,
            NavigationState.Arrival ->
                null
        }

    private fun routeArrowComponent(
        navigationState: NavigationState,
        arrowOptions: RouteArrowOptions
    ) = if (navigationState == NavigationState.ActiveNavigation) {
        RouteArrowComponent(mapView.getMapboxMap(), arrowOptions)
    } else {
        null
    }
}

@ExperimentalPreviewMapboxNavigationAPI
internal class RouteLineComponentContractImpl(
    private val store: Store
) : RouteLineComponentContract {
    override fun setRoutes(mapboxNavigation: MapboxNavigation, routes: List<NavigationRoute>) {
        when (store.state.value.navigation) {
            is NavigationState.RoutePreview -> {
                store.dispatch(RoutePreviewAction.Ready(routes))
            }
            is NavigationState.ActiveNavigation -> {
                store.dispatch(RoutesAction.SetRoutes(routes))
            }
            else -> {
                // no op
            }
        }
    }

    override fun getRouteInPreview(): Flow<List<NavigationRoute>?> {
        return combine(
            store.select { it.navigation },
            store.select { it.previewRoutes },
        ) { navigationState, routePreviewState ->
            if (routePreviewState is RoutePreviewState.Ready) {
                if (navigationState == NavigationState.RoutePreview) {
                    routePreviewState.routes
                } else {
                    null
                }
            } else {
                emptyList()
            }
        }
    }
}
