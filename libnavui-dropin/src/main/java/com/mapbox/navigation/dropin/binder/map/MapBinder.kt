package com.mapbox.navigation.dropin.binder.map

import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.mapbox.maps.MapView
import com.mapbox.maps.plugin.compass.compass
import com.mapbox.maps.plugin.scalebar.scalebar
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.core.internal.extensions.navigationListOf
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.dropin.NavigationViewContext
import com.mapbox.navigation.dropin.component.camera.CameraComponent
import com.mapbox.navigation.dropin.component.camera.CameraLayoutObserver
import com.mapbox.navigation.dropin.component.location.LocationComponent
import com.mapbox.navigation.dropin.component.marker.FreeDriveLongPressMapComponent
import com.mapbox.navigation.dropin.component.marker.GeocodingComponent
import com.mapbox.navigation.dropin.component.marker.MapMarkersComponent
import com.mapbox.navigation.dropin.component.marker.RoutePreviewLongPressMapComponent
import com.mapbox.navigation.dropin.component.navigation.NavigationState
import com.mapbox.navigation.dropin.component.routefetch.RoutesAction
import com.mapbox.navigation.dropin.component.routefetch.RoutesState
import com.mapbox.navigation.dropin.databinding.MapboxNavigationViewLayoutBinding
import com.mapbox.navigation.dropin.internal.extensions.reloadOnChange
import com.mapbox.navigation.dropin.model.State
import com.mapbox.navigation.dropin.model.Store
import com.mapbox.navigation.ui.base.lifecycle.UIBinder
import com.mapbox.navigation.ui.maps.internal.RouteLineComponentContract
import com.mapbox.navigation.ui.maps.internal.ui.RouteArrowComponent
import com.mapbox.navigation.ui.maps.internal.ui.RouteLineComponent
import com.mapbox.navigation.ui.maps.route.arrow.model.RouteArrowOptions
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

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

    private val store = context.viewModel.store

    override fun bind(viewGroup: ViewGroup): MapboxNavigationObserver {
        val navigationState = store.select { it.navigation }
        return navigationListOf(
            CameraLayoutObserver(store, mapView, binding),
            LocationComponent(
                mapView,
                context.viewModel.locationViewModel,
            ),
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
        RouteLineComponent(
            mapboxMap = mapView.getMapboxMap(),
            mapPlugins = mapView,
            options = lineOptions,
            contract = StoreRouteLineComponentContract(
                store,
                context.lifecycleOwner.lifecycleScope
            )
        )

    private fun longPressMapComponent(navigationState: NavigationState) =
        when (navigationState) {
            NavigationState.FreeDrive,
            NavigationState.DestinationPreview ->
                FreeDriveLongPressMapComponent(store, mapView)
            NavigationState.RoutePreview ->
                RoutePreviewLongPressMapComponent(store, mapView)
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
        RouteArrowComponent(mapView, arrowOptions)
    } else {
        null
    }
}

@ExperimentalPreviewMapboxNavigationAPI
internal class StoreRouteLineComponentContract(
    private val store: Store,
    private val coroutineScope: CoroutineScope,
) : RouteLineComponentContract {

    override val navigationRoutes: StateFlow<List<NavigationRoute>>
        get() {
            return store.state.map(::mapToRoutes).stateIn(
                coroutineScope,
                SharingStarted.WhileSubscribed(),
                mapToRoutes(store.state.value)
            )
        }

    override fun setRoutes(routes: List<NavigationRoute>) {
        store.dispatch(RoutesAction.SetRoutes(routes))
    }

    private fun mapToRoutes(state: State): List<NavigationRoute> {
        return when (state.routes) {
            is RoutesState.Ready -> state.routes.routes
            else -> emptyList()
        }
    }
}
