package com.mapbox.navigation.dropin

import android.content.Context
import com.mapbox.maps.plugin.LocationPuck
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.ui.maneuver.model.ManeuverViewOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@ExperimentalPreviewMapboxNavigationAPI
internal class NavigationViewStyles(context: Context) {

    private val _infoPanelPeekHeight: MutableStateFlow<Int> =
        MutableStateFlow(ViewStyleCustomization.defaultInfoPanelPeekHeight(context))
    private val _infoPanelMarginStart: MutableStateFlow<Int> =
        MutableStateFlow(ViewStyleCustomization.defaultInfoPanelMarginStart())
    private val _infoPanelMarginEnd: MutableStateFlow<Int> =
        MutableStateFlow(ViewStyleCustomization.defaultInfoPanelMarginEnd())
    private val _infoPanelBackground: MutableStateFlow<Int> =
        MutableStateFlow(ViewStyleCustomization.defaultInfoPanelBackground())
    private val _poiNameTextAppearance: MutableStateFlow<Int> =
        MutableStateFlow(ViewStyleCustomization.defaultPoiNameTextAppearance())
    private val _tripProgressStyle: MutableStateFlow<Int> =
        MutableStateFlow(ViewStyleCustomization.defaultTripProgressStyle())
    private val _audioGuidanceButtonParams: MutableStateFlow<MapboxExtendableButtonParams> =
        MutableStateFlow(ViewStyleCustomization.defaultAudioGuidanceButtonParams(context))
    private val _recenterButtonParams: MutableStateFlow<MapboxExtendableButtonParams> =
        MutableStateFlow(ViewStyleCustomization.defaultRecenterButtonParams(context))
    private val _cameraModeButtonParams: MutableStateFlow<MapboxExtendableButtonParams> =
        MutableStateFlow(ViewStyleCustomization.defaultCameraModeButtonParams(context))
    private val _routePreviewButtonParams: MutableStateFlow<MapboxExtendableButtonParams> =
        MutableStateFlow(ViewStyleCustomization.defaultRoutePreviewButtonParams(context))
    private val _endNavigationButtonParams: MutableStateFlow<MapboxExtendableButtonParams> =
        MutableStateFlow(ViewStyleCustomization.defaultEndNavigationButtonParams(context))
    private val _startNavigationButtonParams: MutableStateFlow<MapboxExtendableButtonParams> =
        MutableStateFlow(ViewStyleCustomization.defaultStartNavigationButtonParams(context))
    private val _speedLimitStyle: MutableStateFlow<Int> =
        MutableStateFlow(ViewStyleCustomization.defaultSpeedLimitStyle())
    private val _speedLimitTextAppearance: MutableStateFlow<Int> =
        MutableStateFlow(ViewStyleCustomization.defaultSpeedLimitTextAppearance())
    private val _roadNameBackground: MutableStateFlow<Int> =
        MutableStateFlow(ViewStyleCustomization.defaultRoadNameBackground())
    private val _roadNameTextAppearance: MutableStateFlow<Int> =
        MutableStateFlow(ViewStyleCustomization.defaultRoadNameTextAppearance())
    private val _maneuverViewOptions: MutableStateFlow<ManeuverViewOptions> =
        MutableStateFlow(ViewStyleCustomization.defaultManeuverViewOptions())
    private val _destinationMarkerAnnotationOptions: MutableStateFlow<PointAnnotationOptions> =
        MutableStateFlow(ViewStyleCustomization.defaultDestinationMarkerAnnotationOptions(context))
    private val _arrivalTextAppearance: MutableStateFlow<Int> =
        MutableStateFlow(ViewStyleCustomization.defaultArrivalTextAppearance())
    private val _locationPuck: MutableStateFlow<LocationPuck> =
        MutableStateFlow(ViewStyleCustomization.defaultLocationPuck(context))
    private val _mapScalebarParams: MutableStateFlow<MapboxMapScalebarParams> = MutableStateFlow(
        ViewStyleCustomization.defaultMapScalebarParams(context)
    )

    val infoPanelPeekHeight: StateFlow<Int> = _infoPanelPeekHeight.asStateFlow()
    val infoPanelMarginStart: StateFlow<Int> = _infoPanelMarginStart.asStateFlow()
    val infoPanelMarginEnd: StateFlow<Int> = _infoPanelMarginEnd.asStateFlow()
    val infoPanelBackground: StateFlow<Int> = _infoPanelBackground.asStateFlow()
    val poiNameTextAppearance: StateFlow<Int> = _poiNameTextAppearance.asStateFlow()
    val tripProgressStyle: StateFlow<Int> = _tripProgressStyle.asStateFlow()
    val recenterButtonParams: StateFlow<MapboxExtendableButtonParams> =
        _recenterButtonParams.asStateFlow()
    val audioGuidanceButtonParams: StateFlow<MapboxExtendableButtonParams> =
        _audioGuidanceButtonParams.asStateFlow()
    val cameraModeButtonParams: StateFlow<MapboxExtendableButtonParams> =
        _cameraModeButtonParams.asStateFlow()
    val routePreviewButtonParams: StateFlow<MapboxExtendableButtonParams> =
        _routePreviewButtonParams.asStateFlow()
    val endNavigationButtonParams: StateFlow<MapboxExtendableButtonParams> =
        _endNavigationButtonParams.asStateFlow()
    val startNavigationButtonParams: StateFlow<MapboxExtendableButtonParams> =
        _startNavigationButtonParams.asStateFlow()
    val speedLimitStyle: StateFlow<Int> = _speedLimitStyle.asStateFlow()
    val speedLimitTextAppearance: StateFlow<Int> = _speedLimitTextAppearance.asStateFlow()
    val destinationMarkerAnnotationOptions: StateFlow<PointAnnotationOptions> =
        _destinationMarkerAnnotationOptions.asStateFlow()
    val roadNameBackground: StateFlow<Int> = _roadNameBackground.asStateFlow()
    val roadNameTextAppearance: StateFlow<Int> = _roadNameTextAppearance.asStateFlow()
    val maneuverViewOptions: StateFlow<ManeuverViewOptions> = _maneuverViewOptions.asStateFlow()
    val arrivalTextAppearance: StateFlow<Int> = _arrivalTextAppearance.asStateFlow()
    val locationPuck: StateFlow<LocationPuck> = _locationPuck.asStateFlow()
    val mapScalebarParams: StateFlow<MapboxMapScalebarParams> = _mapScalebarParams.asStateFlow()

    fun applyCustomization(customization: ViewStyleCustomization) {
        customization.infoPanelPeekHeight?.also { _infoPanelPeekHeight.value = it }
        customization.infoPanelMarginStart?.also { _infoPanelMarginStart.value = it }
        customization.infoPanelMarginEnd?.also { _infoPanelMarginEnd.value = it }
        customization.infoPanelBackground?.also { _infoPanelBackground.value = it }
        customization.poiNameTextAppearance?.also { _poiNameTextAppearance.value = it }
        customization.tripProgressStyle?.also { _tripProgressStyle.value = it }
        customization.recenterButtonParams?.also { _recenterButtonParams.value = it }
        customization.cameraModeButtonParams?.also { _cameraModeButtonParams.value = it }
        customization.routePreviewButtonParams?.also { _routePreviewButtonParams.value = it }
        customization.audioGuidanceButtonParams?.also { _audioGuidanceButtonParams.value = it }
        customization.endNavigationButtonParams?.also { _endNavigationButtonParams.value = it }
        customization.startNavigationButtonParams?.also { _startNavigationButtonParams.value = it }
        customization.speedLimitStyle?.also { _speedLimitStyle.value = it }
        customization.maneuverViewOptions?.also { _maneuverViewOptions.value = it }
        customization.speedLimitTextAppearance?.also { _speedLimitTextAppearance.value = it }
        customization.destinationMarkerAnnotationOptions?.also {
            _destinationMarkerAnnotationOptions.value = it
        }
        customization.roadNameBackground?.also { _roadNameBackground.value = it }
        customization.roadNameTextAppearance?.also { _roadNameTextAppearance.value = it }
        customization.arrivalTextAppearance?.also { _arrivalTextAppearance.value = it }
        customization.locationPuck?.also { _locationPuck.value = it }
        customization.mapScalebarParams?.also { _mapScalebarParams.value = it }
    }
}
