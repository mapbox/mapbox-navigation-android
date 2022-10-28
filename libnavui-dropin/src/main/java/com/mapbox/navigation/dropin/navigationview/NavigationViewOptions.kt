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

    private var _showManeuver: MutableStateFlow<Boolean> = MutableStateFlow(true)
    private var _showSpeedLimit: MutableStateFlow<Boolean> = MutableStateFlow(true)
    private var _showRoadName: MutableStateFlow<Boolean> = MutableStateFlow(true)
    private var _showActionButtons: MutableStateFlow<Boolean> = MutableStateFlow(true)
    private var _showCompassActionButton: MutableStateFlow<Boolean> = MutableStateFlow(false)
    private var _showCameraModeActionButton: MutableStateFlow<Boolean> = MutableStateFlow(true)
    private var _showToggleAudioActionButton: MutableStateFlow<Boolean> = MutableStateFlow(true)
    private var _showRecenterActionButton: MutableStateFlow<Boolean> = MutableStateFlow(true)
    private var _showMapScalebar: MutableStateFlow<Boolean> = MutableStateFlow(false)
    private var _showTripProgress: MutableStateFlow<Boolean> = MutableStateFlow(true)
    private var _showRoutePreviewButton: MutableStateFlow<Boolean> = MutableStateFlow(true)
    private var _showEndNavigationButton: MutableStateFlow<Boolean> = MutableStateFlow(true)
    private var _showStartNavigationButton: MutableStateFlow<Boolean> = MutableStateFlow(true)

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

    val showManeuver: StateFlow<Boolean> = _showManeuver.asStateFlow()
    val showSpeedLimit: StateFlow<Boolean> = _showSpeedLimit.asStateFlow()
    val showRoadName: StateFlow<Boolean> = _showRoadName.asStateFlow()
    val showActionButtons: StateFlow<Boolean> = _showActionButtons.asStateFlow()
    val showCompassActionButton: StateFlow<Boolean> = _showCompassActionButton.asStateFlow()
    val showCameraModeActionButton: StateFlow<Boolean> = _showCameraModeActionButton.asStateFlow()
    val showToggleAudioActionButton: StateFlow<Boolean> = _showToggleAudioActionButton.asStateFlow()
    val showRecenterActionButton: StateFlow<Boolean> = _showRecenterActionButton.asStateFlow()
    val showMapScalebar: StateFlow<Boolean> = _showMapScalebar.asStateFlow()
    val showTripProgress: StateFlow<Boolean> = _showTripProgress.asStateFlow()
    val showRoutePreviewButton: StateFlow<Boolean> = _showRoutePreviewButton.asStateFlow()
    val showEndNavigationButton: StateFlow<Boolean> = _showEndNavigationButton.asStateFlow()
    val showStartNavigationButton: StateFlow<Boolean> = _showStartNavigationButton.asStateFlow()

    fun applyCustomization(customization: ViewOptionsCustomization) {
        customization.mapStyleUriDay?.also { _mapStyleUriDay.value = it }
        customization.mapStyleUriNight?.also { _mapStyleUriNight.value = it }
        customization.routeLineOptions?.also { _routeLineOptions.value = it }
        customization.routeArrowOptions?.also { _routeArrowOptions.value = it }
        customization.showInfoPanelInFreeDrive?.also { _showInfoPanelInFreeDrive.value = it }
        customization.enableMapLongClickIntercept?.also { _enableMapLongClickIntercept.value = it }
        customization.isInfoPanelHideable?.also { _isInfoPanelHideable.value = it }
        customization.infoPanelForcedState?.also { _infoPanelForcedState.value = it }
        customization.distanceFormatterOptions?.also { _distanceFormatterOptions.value = it }
        customization.showCameraDebugInfo?.also { _showCameraDebugInfo.value = it }
        customization.showManeuver?.also { _showManeuver.value = it }
        customization.showSpeedLimit?.also { _showSpeedLimit.value = it }
        customization.showRoadName?.also { _showRoadName.value = it }
        customization.showActionButtons?.also { _showActionButtons.value = it }
        customization.showCompassActionButton?.also { _showCompassActionButton.value = it }
        customization.showCameraModeActionButton?.also { _showCameraModeActionButton.value = it }
        customization.showToggleAudioActionButton?.also {
            _showToggleAudioActionButton.value = it
        }
        customization.showRecenterActionButton?.also { _showRecenterActionButton.value = it }
        customization.showTripProgress?.also { _showTripProgress.value = it }
        customization.showMapScalebar?.also { _showMapScalebar.value = it }
        customization.showRoutePreviewButton?.also { _showRoutePreviewButton.value = it }
        customization.showEndNavigationButton?.also { _showEndNavigationButton.value = it }
        customization.showStartNavigationButton?.also { _showStartNavigationButton.value = it }
    }
}
