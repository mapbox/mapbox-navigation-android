package com.mapbox.navigation.dropin.map

import android.content.Context
import android.view.ViewGroup
import com.mapbox.maps.MapView
import com.mapbox.maps.plugin.locationcomponent.LocationComponentPlugin
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.internal.extensions.navigationListOf
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.dropin.camera.CameraComponent
import com.mapbox.navigation.dropin.camera.CameraLayoutObserver
import com.mapbox.navigation.dropin.databinding.MapboxNavigationViewLayoutBinding
import com.mapbox.navigation.dropin.internal.extensions.reloadOnChange
import com.mapbox.navigation.dropin.map.geocoding.GeocodingComponent
import com.mapbox.navigation.dropin.map.logo.LogoAttributionComponent
import com.mapbox.navigation.dropin.map.longpress.FreeDriveLongPressMapComponent
import com.mapbox.navigation.dropin.map.longpress.RoutePreviewLongPressMapComponent
import com.mapbox.navigation.dropin.map.marker.MapMarkersComponent
import com.mapbox.navigation.dropin.map.scalebar.ScalebarComponent
import com.mapbox.navigation.dropin.navigationview.NavigationViewContext
import com.mapbox.navigation.ui.app.internal.navigation.NavigationState
import com.mapbox.navigation.ui.base.lifecycle.UIBinder
import com.mapbox.navigation.ui.maps.internal.ui.LocationComponent
import com.mapbox.navigation.ui.maps.internal.ui.LocationPuckComponent
import com.mapbox.navigation.ui.maps.internal.ui.RouteArrowComponent
import com.mapbox.navigation.ui.maps.internal.ui.RouteLineComponent
import com.mapbox.navigation.ui.maps.location.NavigationLocationProvider
import com.mapbox.navigation.ui.maps.puck.LocationPuckOptions
import com.mapbox.navigation.ui.maps.route.arrow.model.RouteArrowOptions
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions

/**
 * Base Binder class used for inflating and binding Map View.
 * Use [MapViewBinder.defaultBinder] to access default implementation.
 */
@ExperimentalPreviewMapboxNavigationAPI
abstract class MapViewBinder : UIBinder {

    internal lateinit var context: NavigationViewContext
    internal lateinit var navigationViewBinding: MapboxNavigationViewLayoutBinding

    /**
     * Create [MapView].
     * NOTE: you shouldn't attach [MapView] to any parent:
     * Navigation SDK will do that under the hood.
     *
     * @return [MapView] that will host the map.
     */
    abstract fun getMapView(context: Context): MapView

    /**
     * `True` if Mapbox should load map style,
     * `false` if it will be loaded by the user or handled in any other way.
     */
    open val shouldLoadMapStyle: Boolean = false

    /**
     * Triggered when this view binder instance is attached. The [viewGroup] returns a
     * [MapboxNavigationObserver] which gives this view a simple lifecycle.
     */
    override fun bind(viewGroup: ViewGroup): MapboxNavigationObserver {
        val mapView = getMapView(viewGroup.context)
        viewGroup.removeAllViews()
        viewGroup.addView(
            mapView,
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        context.mapViewOwner.updateMapView(mapView)

        val store = context.store
        val navigationState = store.select { it.navigation }
        return navigationListOf(
            CameraLayoutObserver(store, mapView, navigationViewBinding),
            LocationComponent(context.locationProvider),
            reloadOnChange(
                navigationState,
                context.styles.locationPuckOptions
            ) { navState, _ ->
                locationPuckComponent(
                    navState,
                    mapView.location,
                    context.locationProvider,
                    context.styles.locationPuckOptions.value
                )
            },
            LogoAttributionComponent(mapView, context.systemBarsInsets),
            reloadOnChange(
                context.mapStyleLoader.loadedMapStyle,
                context.options.routeLineOptions
            ) { _, lineOptions ->
                routeLineComponent(lineOptions, mapView)
            },
            CameraComponent(context, mapView),
            reloadOnChange(
                context.styles.destinationMarkerAnnotationOptions
            ) { markerAnnotationOptions ->
                MapMarkersComponent(store, mapView, markerAnnotationOptions)
            },
            reloadOnChange(navigationState) {
                longPressMapComponent(it, mapView)
            },
            reloadOnChange(navigationState) {
                geocodingComponent(it)
            },
            reloadOnChange(
                context.mapStyleLoader.loadedMapStyle,
                context.options.routeArrowOptions,
                navigationState
            ) { _, arrowOptions, navState ->
                routeArrowComponent(mapView, navState, arrowOptions)
            },
            ScalebarComponent(
                mapView,
                context.options.showMapScalebar,
                context.systemBarsInsets,
                context.options.distanceFormatterOptions,
            )
        )
    }

    private fun routeLineComponent(lineOptions: MapboxRouteLineOptions, mapView: MapView) =
        RouteLineComponent(mapView.getMapboxMap(), mapView, lineOptions, contractProvider = {
            RouteLineComponentContractImpl(context.store, context.mapClickBehavior)
        })

    private fun locationPuckComponent(
        navigationState: NavigationState,
        location: LocationComponentPlugin,
        provider: NavigationLocationProvider,
        options: LocationPuckOptions
    ): LocationPuckComponent {
        return when (navigationState) {
            NavigationState.FreeDrive -> {
                LocationPuckComponent(
                    location,
                    options.freeDrivePuck,
                    provider,
                )
            }
            NavigationState.DestinationPreview -> {
                LocationPuckComponent(
                    location,
                    options.destinationPreviewPuck,
                    provider,
                )
            }
            NavigationState.RoutePreview -> {
                LocationPuckComponent(
                    location,
                    options.routePreviewPuck,
                    provider,
                )
            }
            NavigationState.ActiveNavigation -> {
                LocationPuckComponent(
                    location,
                    options.activeNavigationPuck,
                    provider,
                )
            }
            NavigationState.Arrival -> {
                LocationPuckComponent(
                    location,
                    options.arrivalPuck,
                    provider,
                )
            }
        }
    }

    private fun longPressMapComponent(navigationState: NavigationState, mapView: MapView) =
        when (navigationState) {
            NavigationState.FreeDrive,
            NavigationState.DestinationPreview ->
                FreeDriveLongPressMapComponent(context.store, mapView, context)
            NavigationState.RoutePreview ->
                RoutePreviewLongPressMapComponent(context.store, mapView, context)
            NavigationState.ActiveNavigation,
            NavigationState.Arrival ->
                null
        }

    private fun geocodingComponent(navigationState: NavigationState) =
        when (navigationState) {
            NavigationState.FreeDrive,
            NavigationState.DestinationPreview,
            NavigationState.RoutePreview ->
                GeocodingComponent(context.store)
            NavigationState.ActiveNavigation,
            NavigationState.Arrival ->
                null
        }

    private fun routeArrowComponent(
        mapView: MapView,
        navigationState: NavigationState,
        arrowOptions: RouteArrowOptions
    ) = if (navigationState == NavigationState.ActiveNavigation) {
        RouteArrowComponent(mapView.getMapboxMap(), arrowOptions)
    } else {
        null
    }

    companion object {
        /**
         * Default Info Panel Binder.
         */
        fun defaultBinder(): MapViewBinder = MapboxMapViewBinder()
    }
}
