package com.mapbox.navigation.dropin.navigationview

import android.content.Context
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.navigation.dropin.ViewStyleCustomization
import com.mapbox.navigation.ui.maneuver.model.ManeuverViewOptions
import com.mapbox.navigation.ui.maps.puck.LocationPuckOptions
import com.mapbox.navigation.ui.speedlimit.model.MapboxSpeedInfoOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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

    private var _compassButtonStyle: MutableStateFlow<Int> =
        MutableStateFlow(ViewStyleCustomization.defaultCompassButtonStyle())
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

    private val _speedInfoOptions: MutableStateFlow<MapboxSpeedInfoOptions> =
        MutableStateFlow(ViewStyleCustomization.defaultSpeedInfoOptions())
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
    private val _locationPuckOptions: MutableStateFlow<LocationPuckOptions> =
        MutableStateFlow(ViewStyleCustomization.defaultLocationPuckOptions(context))

    @Deprecated(message = "The parent MapboxSpeedLimitView is deprecated")
    private val _speedLimitStyle: MutableStateFlow<Int> =
        MutableStateFlow(ViewStyleCustomization.defaultSpeedLimitStyle())

    @Deprecated(message = "The parent MapboxSpeedLimitView is deprecated")
    private val _speedLimitTextAppearance: MutableStateFlow<Int> =
        MutableStateFlow(ViewStyleCustomization.defaultSpeedLimitTextAppearance())

    val infoPanelPeekHeight: StateFlow<Int> = _infoPanelPeekHeight.asStateFlow()
    val infoPanelMarginStart: StateFlow<Int> = _infoPanelMarginStart.asStateFlow()
    val infoPanelMarginEnd: StateFlow<Int> = _infoPanelMarginEnd.asStateFlow()
    val infoPanelBackground: StateFlow<Int> = _infoPanelBackground.asStateFlow()
    val poiNameTextAppearance: StateFlow<Int> = _poiNameTextAppearance.asStateFlow()
    val tripProgressStyle: StateFlow<Int> = _tripProgressStyle.asStateFlow()

    val compassButtonStyle: StateFlow<Int> = _compassButtonStyle.asStateFlow()
    val recenterButtonStyle: StateFlow<Int> = _recenterButtonStyle.asStateFlow()
    val audioGuidanceButtonStyle: StateFlow<Int> = _audioGuidanceButtonStyle.asStateFlow()
    val cameraModeButtonStyle: StateFlow<Int> = _cameraModeButtonStyle.asStateFlow()
    val routePreviewButtonStyle: StateFlow<Int> = _routePreviewButtonStyle.asStateFlow()
    val endNavigationButtonStyle: StateFlow<Int> = _endNavigationButtonStyle.asStateFlow()
    val startNavigationButtonStyle: StateFlow<Int> = _startNavigationButtonStyle.asStateFlow()

    val speedInfoOptions: StateFlow<MapboxSpeedInfoOptions> = _speedInfoOptions.asStateFlow()
    val destinationMarkerAnnotationOptions: StateFlow<PointAnnotationOptions> =
        _destinationMarkerAnnotationOptions.asStateFlow()
    val roadNameBackground: StateFlow<Int> = _roadNameBackground.asStateFlow()
    val roadNameTextAppearance: StateFlow<Int> = _roadNameTextAppearance.asStateFlow()
    val maneuverViewOptions: StateFlow<ManeuverViewOptions> = _maneuverViewOptions.asStateFlow()
    val arrivalTextAppearance: StateFlow<Int> = _arrivalTextAppearance.asStateFlow()
    val locationPuckOptions: StateFlow<LocationPuckOptions> = _locationPuckOptions.asStateFlow()

    @Deprecated(
        message = "The parent MapboxSpeedLimitView is deprecated",
        replaceWith = ReplaceWith("speedInfoStyle")
    )
    val speedLimitStyle: StateFlow<Int> = _speedLimitStyle.asStateFlow()

    @Deprecated(
        message = "The parent MapboxSpeedLimitView is deprecated",
        replaceWith = ReplaceWith("speedInfoStyle")
    )
    val speedLimitTextAppearance: StateFlow<Int> = _speedLimitTextAppearance.asStateFlow()

    fun applyCustomization(customization: ViewStyleCustomization) {
        customization.infoPanelPeekHeight?.also { _infoPanelPeekHeight.value = it }
        customization.infoPanelMarginStart?.also { _infoPanelMarginStart.value = it }
        customization.infoPanelMarginEnd?.also { _infoPanelMarginEnd.value = it }
        customization.infoPanelBackground?.also { _infoPanelBackground.value = it }
        customization.poiNameTextAppearance?.also { _poiNameTextAppearance.value = it }
        customization.tripProgressStyle?.also { _tripProgressStyle.value = it }

        customization.compassButtonStyle?.also { _compassButtonStyle.value = it }
        customization.recenterButtonStyle?.also { _recenterButtonStyle.value = it }
        customization.audioGuidanceButtonStyle?.also { _audioGuidanceButtonStyle.value = it }
        customization.cameraModeButtonStyle?.also { _cameraModeButtonStyle.value = it }
        customization.routePreviewButtonStyle?.also { _routePreviewButtonStyle.value = it }
        customization.endNavigationButtonStyle?.also { _endNavigationButtonStyle.value = it }
        customization.startNavigationButtonStyle?.also { _startNavigationButtonStyle.value = it }

        customization.speedInfoOptions?.also { _speedInfoOptions.value = it }
        customization.maneuverViewOptions?.also { _maneuverViewOptions.value = it }
        customization.destinationMarkerAnnotationOptions?.also {
            _destinationMarkerAnnotationOptions.value = it
        }
        customization.roadNameBackground?.also { _roadNameBackground.value = it }
        customization.roadNameTextAppearance?.also { _roadNameTextAppearance.value = it }
        customization.arrivalTextAppearance?.also { _arrivalTextAppearance.value = it }
        customization.locationPuckOptions?.also { _locationPuckOptions.value = it }

        customization.speedLimitStyle?.also { _speedLimitStyle.value = it }
        customization.speedLimitTextAppearance?.also { _speedLimitTextAppearance.value = it }
    }
}
