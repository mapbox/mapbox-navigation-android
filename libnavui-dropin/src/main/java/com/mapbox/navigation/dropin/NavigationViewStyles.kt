package com.mapbox.navigation.dropin

import android.content.Context
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.ui.maneuver.model.ManeuverViewOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@ExperimentalPreviewMapboxNavigationAPI
internal class NavigationViewStyles(context: Context) {

    private var _tripProgressStyle: MutableStateFlow<Int> =
        MutableStateFlow(ViewStyleCustomization.defaultTripProgressStyle())
    private var _audioGuidanceButtonStyle: MutableStateFlow<Int> =
        MutableStateFlow(ViewStyleCustomization.defaultAudioGuidanceButtonStyle())
    private var _recenterButtonStyle: MutableStateFlow<Int> =
        MutableStateFlow(ViewStyleCustomization.defaultRecenterButtonStyle())
    private var _cameraModeButtonStyle: MutableStateFlow<Int> =
        MutableStateFlow(ViewStyleCustomization.defaultCameraModeButtonStyle())
    private var _routePreviewButtonStyle: MutableStateFlow<Int> =
        MutableStateFlow(ViewStyleCustomization.defaultRoutePreviewButtonStyle())
    private var _endNavigationButtonStyle: MutableStateFlow<Int> =
        MutableStateFlow(ViewStyleCustomization.defaultEndNavigationButtonStyle())
    private var _startNavigationButtonStyle: MutableStateFlow<Int> =
        MutableStateFlow(ViewStyleCustomization.defaultStartNavigationButtonStyle())
    private var _speedLimitStyle: MutableStateFlow<Int> =
        MutableStateFlow(ViewStyleCustomization.defaultSpeedLimitStyle())
    private var _speedLimitTextAppearance: MutableStateFlow<Int> =
        MutableStateFlow(ViewStyleCustomization.defaultSpeedLimitTextAppearance())
    private var _destinationMarker: MutableStateFlow<Int> =
        MutableStateFlow(ViewStyleCustomization.defaultDestinationMarker())
    private var _roadNameBackground: MutableStateFlow<Int> =
        MutableStateFlow(ViewStyleCustomization.defaultRoadNameBackground())
    private var _roadNameTextAppearance: MutableStateFlow<Int> =
        MutableStateFlow(ViewStyleCustomization.defaultRoadNameTextAppearance())
    private var _maneuverViewOptions: MutableStateFlow<ManeuverViewOptions> =
        MutableStateFlow(ViewStyleCustomization.defaultManeuverViewOptions())

    val tripProgressStyle: StateFlow<Int> = _tripProgressStyle.asStateFlow()
    val recenterButtonStyle: StateFlow<Int> = _recenterButtonStyle.asStateFlow()
    val audioGuidanceButtonStyle: StateFlow<Int> = _audioGuidanceButtonStyle.asStateFlow()
    val cameraModeButtonStyle: StateFlow<Int> = _cameraModeButtonStyle.asStateFlow()
    val routePreviewButtonStyle: StateFlow<Int> = _routePreviewButtonStyle.asStateFlow()
    val endNavigationButtonStyle: StateFlow<Int> = _endNavigationButtonStyle.asStateFlow()
    val startNavigationButtonStyle: StateFlow<Int> = _startNavigationButtonStyle.asStateFlow()
    val speedLimitStyle: StateFlow<Int> = _speedLimitStyle.asStateFlow()
    val speedLimitTextAppearance: StateFlow<Int> = _speedLimitTextAppearance.asStateFlow()
    val destinationMarker: StateFlow<Int> = _destinationMarker.asStateFlow()
    val roadNameBackground: StateFlow<Int> = _roadNameBackground.asStateFlow()
    val roadNameTextAppearance: StateFlow<Int> = _roadNameTextAppearance.asStateFlow()
    val maneuverViewOptions: StateFlow<ManeuverViewOptions> = _maneuverViewOptions.asStateFlow()

    fun applyCustomization(customization: ViewStyleCustomization) {
        customization.tripProgressStyle?.also { _tripProgressStyle.tryEmit(it) }
        customization.recenterButtonStyle?.also { _recenterButtonStyle.tryEmit(it) }
        customization.cameraModeButtonStyle?.also { _cameraModeButtonStyle.tryEmit(it) }
        customization.routePreviewButtonStyle?.also { _routePreviewButtonStyle.tryEmit(it) }
        customization.audioGuidanceButtonStyle?.also { _audioGuidanceButtonStyle.tryEmit(it) }
        customization.endNavigationButtonStyle?.also { _endNavigationButtonStyle.tryEmit(it) }
        customization.startNavigationButtonStyle?.also { _startNavigationButtonStyle.tryEmit(it) }
        customization.speedLimitStyle?.also { _speedLimitStyle.tryEmit(it) }
        customization.maneuverViewOptions?.also { _maneuverViewOptions.tryEmit(it) }
        customization.speedLimitTextAppearance?.also { _speedLimitTextAppearance.tryEmit(it) }
        customization.destinationMarker?.also { _destinationMarker.tryEmit(it) }
        customization.roadNameBackground?.also { _roadNameBackground.tryEmit(it) }
        customization.roadNameTextAppearance?.also { _roadNameTextAppearance.tryEmit(it) }
    }
}
