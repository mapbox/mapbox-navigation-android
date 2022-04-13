package com.mapbox.navigation.dropin

import android.content.Context
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.dropin.ViewOptionsCustomization.Companion.defaultRouteArrowOptions
import com.mapbox.navigation.dropin.ViewOptionsCustomization.Companion.defaultRouteLineOptions
import com.mapbox.navigation.ui.maps.NavigationStyles
import com.mapbox.navigation.ui.maps.route.arrow.model.RouteArrowOptions
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * A class that is a central place to hold all the configurable options for [NavigationView].
 */
@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
internal class NavigationViewOptions(context: Context) {

    private var _mapStyleUriDay: MutableStateFlow<String> =
        MutableStateFlow(NavigationStyles.NAVIGATION_DAY_STYLE)
    private var _mapStyleUriNight: MutableStateFlow<String> =
        MutableStateFlow(NavigationStyles.NAVIGATION_NIGHT_STYLE)
    private var _routeLineOptions: MutableStateFlow<MapboxRouteLineOptions> =
        MutableStateFlow(defaultRouteLineOptions(context))
    private var _routeArrowOptions: MutableStateFlow<RouteArrowOptions> =
        MutableStateFlow(defaultRouteArrowOptions(context))

    var mapStyleUriDay: StateFlow<String> = _mapStyleUriDay.asStateFlow()
    var mapStyleUriNight: StateFlow<String> = _mapStyleUriNight.asStateFlow()
    val routeLineOptions: StateFlow<MapboxRouteLineOptions> = _routeLineOptions.asStateFlow()
    val routeArrowOptions: StateFlow<RouteArrowOptions> = _routeArrowOptions.asStateFlow()

    fun applyCustomization(customization: ViewOptionsCustomization) {
        customization.mapStyleUriDay?.also { _mapStyleUriDay.tryEmit(it) }
        customization.mapStyleUriNight?.also { _mapStyleUriNight.tryEmit(it) }
        customization.routeLineOptions?.also { _routeLineOptions.tryEmit(it) }
        customization.routeArrowOptions?.also { _routeArrowOptions.tryEmit(it) }
    }
}
