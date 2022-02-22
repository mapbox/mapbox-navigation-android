package com.mapbox.navigation.dropin

import android.content.Context
import androidx.activity.OnBackPressedCallback
import androidx.lifecycle.LifecycleOwner
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.extensions.applyLanguageAndVoiceUnitOptions
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.dropin.binder.UIBinder
import com.mapbox.navigation.dropin.component.marker.MapMarkerFactory
import com.mapbox.navigation.dropin.lifecycle.UICoordinator
import com.mapbox.navigation.dropin.usecase.guidance.StartActiveGuidanceUseCase
import com.mapbox.navigation.dropin.usecase.guidance.StopActiveGuidanceUseCase
import com.mapbox.navigation.dropin.usecase.location.GetCurrentLocationUseCase
import com.mapbox.navigation.dropin.usecase.route.FetchAndSetRouteUseCase
import com.mapbox.navigation.dropin.usecase.route.FetchRouteUseCase
import com.mapbox.navigation.dropin.util.BitmapMemoryCache
import com.mapbox.navigation.dropin.util.BitmapMemoryCache.Companion.MB_IN_BYTES
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants
import com.mapbox.navigation.ui.maps.route.arrow.model.RouteArrowOptions
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineResources
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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

    val navigation: MapboxNavigation
        get() = checkNotNull(MapboxNavigationApp.current()) {
            "MapboxNavigationApp not initialized. You must call MapboxNavigationApp.setup() first."
        }

    //region Builders & Factories

    fun routeOptionsBuilder() = RouteOptions.builder()
        .applyDefaultNavigationOptions()
        .applyLanguageAndVoiceUnitOptions(context)

    fun mapAnnotationFactory() = MapMarkerFactory(
        context,
        BitmapMemoryCache(4 * MB_IN_BYTES)
    )

    //endregion

    //region Use Cases

    fun fetchRoutesUseCase() = FetchRouteUseCase(
        navigation,
        this::routeOptionsBuilder,
        getCurrentLocationUseCase(),
        Dispatchers.IO
    )

    fun fetchAndSetRouteUseCase() = FetchAndSetRouteUseCase(
        navigation,
        fetchRoutesUseCase(),
        Dispatchers.Main
    )

    fun getCurrentLocationUseCase() = GetCurrentLocationUseCase(
        navigation,
        viewModel.locationBehavior,
        Dispatchers.Default
    )

    fun startActiveGuidanceUseCase() = StartActiveGuidanceUseCase(
        navigation,
        viewModel._activeNavigationStarted,
        Dispatchers.Main
    )

    fun stopActiveGuidanceUseCase() = StopActiveGuidanceUseCase(
        navigation,
        viewModel._activeNavigationStarted,
        Dispatchers.Main
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
