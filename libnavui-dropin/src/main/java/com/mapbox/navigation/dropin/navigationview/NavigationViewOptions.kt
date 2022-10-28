package com.mapbox.navigation.dropin.navigationview

import android.content.Context
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.formatter.DistanceFormatterOptions
import com.mapbox.navigation.dropin.ViewOptionsCustomization
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
    private var _showInfoPanelInFreeDrive: MutableStateFlow<Boolean> =
        MutableStateFlow(false)
    private var _enableMapLongClickIntercept: MutableStateFlow<Boolean> =
        MutableStateFlow(true)
    private var _isInfoPanelHideable: MutableStateFlow<Boolean> =
        MutableStateFlow(false)
    private var _infoPanelForcedState: MutableStateFlow<Int> =
        MutableStateFlow(0)
    private var _distanceFormatterOptions: MutableStateFlow<DistanceFormatterOptions> =
        MutableStateFlow(DistanceFormatterOptions.Builder(context).build())
    private var _showCameraDebugInfo: MutableStateFlow<Boolean> =
        MutableStateFlow(false)

    private var _showCompassActionButton: MutableStateFlow<Boolean> =
        MutableStateFlow(false)

    var mapStyleUriDay: StateFlow<String> = _mapStyleUriDay.asStateFlow()
    var mapStyleUriNight: StateFlow<String> = _mapStyleUriNight.asStateFlow()
    val routeLineOptions: StateFlow<MapboxRouteLineOptions> = _routeLineOptions.asStateFlow()
    val routeArrowOptions: StateFlow<RouteArrowOptions> = _routeArrowOptions.asStateFlow()
    val showInfoPanelInFreeDrive: StateFlow<Boolean> = _showInfoPanelInFreeDrive.asStateFlow()
    val enableMapLongClickIntercept: StateFlow<Boolean> = _enableMapLongClickIntercept.asStateFlow()
    val isInfoPanelHideable: StateFlow<Boolean> = _isInfoPanelHideable.asStateFlow()
    val infoPanelForcedState: StateFlow<Int> = _infoPanelForcedState.asStateFlow()
    val distanceFormatterOptions: StateFlow<DistanceFormatterOptions> =
        _distanceFormatterOptions.asStateFlow()
    val showCameraDebugInfo: StateFlow<Boolean> = _showCameraDebugInfo.asStateFlow()

    val showCompassActionButton: StateFlow<Boolean> = _showCompassActionButton.asStateFlow()

    fun applyCustomization(customization: ViewOptionsCustomization) {
        customization.mapStyleUriDay?.also { _mapStyleUriDay.tryEmit(it) }
        customization.mapStyleUriNight?.also { _mapStyleUriNight.tryEmit(it) }
        customization.routeLineOptions?.also { _routeLineOptions.tryEmit(it) }
        customization.routeArrowOptions?.also { _routeArrowOptions.tryEmit(it) }
        customization.showInfoPanelInFreeDrive?.also { _showInfoPanelInFreeDrive.tryEmit(it) }
        customization.enableMapLongClickIntercept?.also { _enableMapLongClickIntercept.tryEmit(it) }
        customization.isInfoPanelHideable?.also { _isInfoPanelHideable.tryEmit(it) }
        customization.infoPanelForcedState?.also { _infoPanelForcedState.tryEmit(it) }
        customization.distanceFormatterOptions?.also { _distanceFormatterOptions.tryEmit(it) }
        customization.showCameraDebugInfo?.also { _showCameraDebugInfo.tryEmit(it) }

        customization.showCompassActionButton?.also { _showCompassActionButton.tryEmit(it) }
    }
}
