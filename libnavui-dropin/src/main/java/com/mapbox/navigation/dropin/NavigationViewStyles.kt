package com.mapbox.navigation.dropin

import android.content.Context
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.ui.maneuver.model.ManeuverViewOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@ExperimentalPreviewMapboxNavigationAPI
internal class NavigationViewStyles(context: Context) {

    private var _infoPanelPeekHeight: MutableStateFlow<Int> =
        MutableStateFlow(ViewStyleCustomization.defaultInfoPanelPeekHeight(context))
    private var _infoPanelMarginStart: MutableStateFlow<Int> =
        MutableStateFlow(ViewStyleCustomization.defaultInfoPanelMarginStart())
    private var _infoPanelMarginEnd: MutableStateFlow<Int> =
        MutableStateFlow(ViewStyleCustomization.defaultInfoPanelMarginEnd())
    private var _infoPanelBackground: MutableStateFlow<Int> =
        MutableStateFlow(ViewStyleCustomization.defaultInfoPanelBackground())
    private var _poiNameTextAppearance: MutableStateFlow<Int> =
        MutableStateFlow(ViewStyleCustomization.defaultPoiNameTextAppearance())
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
    private var _roadNameBackground: MutableStateFlow<Int> =
        MutableStateFlow(ViewStyleCustomization.defaultRoadNameBackground())
    private var _roadNameTextAppearance: MutableStateFlow<Int> =
        MutableStateFlow(ViewStyleCustomization.defaultRoadNameTextAppearance())
    private var _maneuverViewOptions: MutableStateFlow<ManeuverViewOptions> =
        MutableStateFlow(ViewStyleCustomization.defaultManeuverViewOptions())
    private var _destinationMarkerAnnotationOptions: MutableStateFlow<PointAnnotationOptions> =
        MutableStateFlow(ViewStyleCustomization.defaultMarkerAnnotationOptions(context))
    private var _arrivalTextAppearance: MutableStateFlow<Int> =
        MutableStateFlow(ViewStyleCustomization.defaultArrivalTextAppearance())

    val infoPanelPeekHeight: StateFlow<Int> = _infoPanelPeekHeight.asStateFlow()
    val infoPanelMarginStart: StateFlow<Int> = _infoPanelMarginStart.asStateFlow()
    val infoPanelMarginEnd: StateFlow<Int> = _infoPanelMarginEnd.asStateFlow()
    val infoPanelBackground: StateFlow<Int> = _infoPanelBackground.asStateFlow()
    val poiNameTextAppearance: StateFlow<Int> = _poiNameTextAppearance.asStateFlow()
    val tripProgressStyle: StateFlow<Int> = _tripProgressStyle.asStateFlow()
    val recenterButtonStyle: StateFlow<Int> = _recenterButtonStyle.asStateFlow()
    val audioGuidanceButtonStyle: StateFlow<Int> = _audioGuidanceButtonStyle.asStateFlow()
    val cameraModeButtonStyle: StateFlow<Int> = _cameraModeButtonStyle.asStateFlow()
    val routePreviewButtonStyle: StateFlow<Int> = _routePreviewButtonStyle.asStateFlow()
    val endNavigationButtonStyle: StateFlow<Int> = _endNavigationButtonStyle.asStateFlow()
    val startNavigationButtonStyle: StateFlow<Int> = _startNavigationButtonStyle.asStateFlow()
    val speedLimitStyle: StateFlow<Int> = _speedLimitStyle.asStateFlow()
    val speedLimitTextAppearance: StateFlow<Int> = _speedLimitTextAppearance.asStateFlow()
    val destinationMarkerAnnotationOptions: StateFlow<PointAnnotationOptions> =
        _destinationMarkerAnnotationOptions.asStateFlow()
    val roadNameBackground: StateFlow<Int> = _roadNameBackground.asStateFlow()
    val roadNameTextAppearance: StateFlow<Int> = _roadNameTextAppearance.asStateFlow()
    val maneuverViewOptions: StateFlow<ManeuverViewOptions> = _maneuverViewOptions.asStateFlow()
    val arrivalTextAppearance: StateFlow<Int> = _arrivalTextAppearance.asStateFlow()

    fun applyCustomization(customization: ViewStyleCustomization) {
        customization.infoPanelPeekHeight?.also { _infoPanelPeekHeight.tryEmit(it) }
        customization.infoPanelMarginStart?.also { _infoPanelMarginStart.tryEmit(it) }
        customization.infoPanelMarginEnd?.also { _infoPanelMarginEnd.tryEmit(it) }
        customization.infoPanelBackground?.also { _infoPanelBackground.tryEmit(it) }
        customization.poiNameTextAppearance?.also { _poiNameTextAppearance.tryEmit(it) }
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
        customization.destinationMarkerAnnotationOptions?.also {
            _destinationMarkerAnnotationOptions.tryEmit(it)
        }
        customization.roadNameBackground?.also { _roadNameBackground.tryEmit(it) }
        customization.roadNameTextAppearance?.also { _roadNameTextAppearance.tryEmit(it) }
        customization.arrivalTextAppearance?.also { _arrivalTextAppearance.tryEmit(it) }
    }
}
