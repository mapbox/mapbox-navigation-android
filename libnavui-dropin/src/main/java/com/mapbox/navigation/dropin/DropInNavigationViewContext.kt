package com.mapbox.navigation.dropin

import android.content.Context
import androidx.activity.OnBackPressedCallback
import androidx.lifecycle.LifecycleOwner
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.dropin.binder.UIBinder
import com.mapbox.navigation.dropin.component.marker.MapMarkerFactory
import com.mapbox.navigation.dropin.component.navigationstate.NavigationState
import com.mapbox.navigation.dropin.component.routefetch.RoutesAction
import com.mapbox.navigation.dropin.component.routefetch.RoutesState
import com.mapbox.navigation.dropin.lifecycle.UICoordinator
import com.mapbox.navigation.dropin.util.BitmapMemoryCache
import com.mapbox.navigation.dropin.util.BitmapMemoryCache.Companion.MB_IN_BYTES
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants
import com.mapbox.navigation.ui.maps.route.arrow.model.RouteArrowOptions
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineResources
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.map

/**
 * This context is a top level data object for [DropInNavigationView].
 *
 * If your data should survive orientation changes, place it inside [DropInNavigationViewModel].
 */
@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
internal class DropInNavigationViewContext(
    val context: Context,
    val lifecycleOwner: LifecycleOwner,
    val viewModel: DropInNavigationViewModel,
) {
    val uiBinders = MutableStateFlow(NavigationUIBinders())
    var routeLineOptions: MapboxRouteLineOptions = MapboxRouteLineOptions.Builder(context)
        .withRouteLineResources(RouteLineResources.Builder().build())
        .withRouteLineBelowLayerId("road-label-navigation")
        .build()
    var routeArrowOptions: RouteArrowOptions = RouteArrowOptions.Builder(context)
        .withAboveLayerId(RouteLayerConstants.TOP_LEVEL_ROUTE_LINE_LAYER_ID)
        .build()

    val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() = viewModel.onBackPressed()
    }

    val dispatch: (action: Any) -> Unit = { action ->
        when (action) {
            is RoutesAction -> viewModel.routesViewModel.invoke(action)
        }
    }

    val navigationState: StateFlow<NavigationState> get() = viewModel.navigationStateViewModel.state
    val routesState: StateFlow<RoutesState> get() = viewModel.routesViewModel.state

    //region Builders & Factories

    fun mapAnnotationFactory() = MapMarkerFactory(
        context,
        BitmapMemoryCache(4 * MB_IN_BYTES)
    )

    //endregion
}

/**
 * Helper extension to map [UIBinder] inside a [UICoordinator].
 * Uses a distinct by class to prevent refreshing views of the same type of [UIBinder].
 */
internal fun <T : UIBinder> DropInNavigationViewContext.flowUiBinder(
    mapper: suspend (value: NavigationUIBinders) -> T
): Flow<T> = this.uiBinders.map(mapper).distinctUntilChangedBy { it.javaClass }
