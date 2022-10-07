package com.mapbox.navigation.dropin.map

import android.view.ViewGroup
import com.mapbox.maps.MapView
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
     *
     * @param viewGroup Parent view which the [MapView] will belong to.
     * @return [MapView] that will host the map.
     *  Returned view must not be attached to [viewGroup] view.
     */
    abstract fun onCreateMapView(viewGroup: ViewGroup): MapView

    /**
     * `True` if Mapbox should load map style,
     * `false` if it will be loaded by the user or handled in any other way.
     */
    open val shouldLoadMapStyle: Boolean = true

    /**
     * Triggered when this view binder instance is attached. The [viewGroup] returns a
     * [MapboxNavigationObserver] which gives this view a simple lifecycle.
     */
    override fun bind(viewGroup: ViewGroup): MapboxNavigationObserver {
        val mapView = onCreateMapView(viewGroup)
        context.mapViewOwner.updateMapView(mapView)

        val store = context.store
        val navigationState = store.select { it.navigation }
        return navigationListOf(
            CameraLayoutObserver(store, mapView, navigationViewBinding),
            LocationComponent(context.locationProvider),
            reloadOnChange(context.styles.locationPuck) { locationPuck ->
                LocationPuckComponent(mapView.location, locationPuck, context.locationProvider)
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
                context.styles.mapScalebarParams,
                context.systemBarsInsets
            )
        )
    }

    private fun routeLineComponent(lineOptions: MapboxRouteLineOptions, mapView: MapView) =
        RouteLineComponent(mapView.getMapboxMap(), mapView, lineOptions, contractProvider = {
            RouteLineComponentContractImpl(context.store, context.mapClickBehavior)
        })

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
