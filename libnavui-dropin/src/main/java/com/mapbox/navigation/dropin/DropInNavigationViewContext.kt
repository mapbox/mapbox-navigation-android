package com.mapbox.navigation.dropin

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineResources

/**
 * This context is a top level data object for [DropInNavigationView].
 *
 * If your data should survive orientation changes, place it inside [DropInNavigationViewModel].
 */
internal class DropInNavigationViewContext(
    val context: Context,
    val lifecycleOwner: LifecycleOwner,
    val viewModel: DropInNavigationViewModel,
) {
    var uiBinders = NavigationUIBinders()
    var routeLineOptions: MapboxRouteLineOptions = MapboxRouteLineOptions.Builder(context)
        .withRouteLineResources(RouteLineResources.Builder().build())
        .withRouteLineBelowLayerId("road-label-navigation")
        .build()
}
