package com.mapbox.navigation.dropin

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import com.mapbox.navigation.dropin.binder.UIBinder
import com.mapbox.navigation.dropin.component.map.MapEventProducer
import com.mapbox.navigation.dropin.lifecycle.UICoordinator
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants
import com.mapbox.navigation.ui.maps.route.arrow.model.RouteArrowOptions
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineResources
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.map

/**
 * This context is a top level data object for [DropInNavigationView].
 *
 * If your data should survive orientation changes, place it inside [DropInNavigationViewModel].
 */
internal class DropInNavigationViewContext(
    val context: Context,
    val lifecycleOwner: LifecycleOwner,
    val viewModel: DropInNavigationViewModel,
    val mapEventProducer: MapEventProducer
) {
    val uiBinders = MutableStateFlow(NavigationUIBinders())
    var routeLineOptions: MapboxRouteLineOptions = MapboxRouteLineOptions.Builder(context)
        .withRouteLineResources(RouteLineResources.Builder().build())
        .withRouteLineBelowLayerId("road-label-navigation")
        .withVanishingRouteLineEnabled(true)
        .build()
    var routeArrowOptions: RouteArrowOptions = RouteArrowOptions.Builder(context)
        .withAboveLayerId(RouteLayerConstants.TOP_LEVEL_ROUTE_LINE_LAYER_ID)
        .build()
}

/**
 * Helper extension to map [UIBinder] inside a [UICoordinator].
 * Uses a distinct by class to prevent refreshing views of the same type of [UIBinder].
 */
internal fun <T : UIBinder> DropInNavigationViewContext.flowUiBinder(
    mapper: suspend (value: NavigationUIBinders) -> T
): Flow<T> = this.uiBinders.map(mapper).distinctUntilChangedBy { it.javaClass }
