package com.mapbox.navigation.qa_test_app.view.customnavview

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class CustomizedViewModel : ViewModel() {
    // Debug
    val showCameraDebugInfo = MutableLiveData(false)

    // Activity
    val fullScreen = MutableLiveData(false)

    // General
    val showManeuver = MutableLiveData(true)
    val showSpeedLimit = MutableLiveData(true)
    val showRoadName = MutableLiveData(true)
    val showActionButtons = MutableLiveData(true)
    val useCustomStyles = MutableLiveData(false)
    val useCustomViews = MutableLiveData(false)
    val distanceFormatterMetric = MutableLiveData(false)
    val routingProfile = MutableLiveData("--")

    // Map
    val showCustomMapView = MutableLiveData(false)
    val enableScalebar = MutableLiveData(false)

    // Action Buttons
    val actionsShowCompassButton = MutableLiveData(false)
    val actionsShowCameraButton = MutableLiveData(true)
    val actionsShowAudioButton = MutableLiveData(true)
    val actionsShowRecenterButton = MutableLiveData(true)
    val actionsAdditionalButtons = MutableLiveData(false)
    val actionsCustomCompassButton = MutableLiveData(false)
    val actionsCustomCameraButton = MutableLiveData(false)
    val actionsCustomAudioButton = MutableLiveData(false)
    val actionsCustomRecenterButton = MutableLiveData(false)
    val actionsCustomLayout = MutableLiveData(false)

    // Info Panel
    val infoPanelShowTripProgress = MutableLiveData(true)
    val infoPanelShowRoutePreviewButton = MutableLiveData(true)
    val infoPanelShowStartNavigationButton = MutableLiveData(true)
    val infoPanelShowEndNavigationButton = MutableLiveData(true)
    val customInfoPanelLayout = MutableLiveData("--")
    val useCustomInfoPanelStyles = MutableLiveData(false)
    val customInfoPanelContent = MutableLiveData("--")
    val customInfoPanelPeekHeight = MutableLiveData("--")
    val showCustomInfoPanelEndNavButton = MutableLiveData(false)
    val showBottomSheetInFreeDrive = MutableLiveData(false)
    val enableDestinationPreview = MutableLiveData(true)
    val isInfoPanelHideable = MutableLiveData(false)
    val infoPanelStateOverride = MutableLiveData("--")
    val infoPanelShowPoiName = MutableLiveData(true)
    val infoPanelShowArrivalText = MutableLiveData(true)
}
