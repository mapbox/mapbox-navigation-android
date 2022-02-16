package com.mapbox.navigation.dropin

import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import com.mapbox.maps.MapView
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineResources

/**
 * This context is a top level data object for [DropInNavigationView].
 *
 * If your data should survive orientation changes, place it inside [DropInNavigationViewModel].
 */
internal class DropInNavigationViewContext(
    val lifecycleOwner: LifecycleOwner,
    val viewModel: DropInNavigationViewModel,
    val mapView: MapView,
    val viewGroup: ViewGroup,
) {
    var uiBinders = NavigationUIBinders()
    var routeLineOptions: MapboxRouteLineOptions = MapboxRouteLineOptions.Builder(viewGroup.context)
        .withRouteLineResources(RouteLineResources.Builder().build())
        .withRouteLineBelowLayerId("road-label-navigation")
        .build()
}
