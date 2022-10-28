package com.mapbox.navigation.qa_test_app.view.customnavview

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class CustomizedViewModel : ViewModel() {
    val useCustomViews = MutableLiveData(false)
    val useCustomStyles = MutableLiveData(false)
    val showCustomMapView = MutableLiveData(false)
    val customInfoPanelLayout = MutableLiveData("--")
    val useCustomInfoPanelStyles = MutableLiveData(false)
    val customInfoPanelContent = MutableLiveData("--")
    val customInfoPanelPeekHeight = MutableLiveData("--")
    val showCustomInfoPanelEndNavButton = MutableLiveData(false)
    val showBottomSheetInFreeDrive = MutableLiveData(false)
    val enableDestinationPreview = MutableLiveData(true)
    val isInfoPanelHideable = MutableLiveData(false)
    val infoPanelStateOverride = MutableLiveData("--")
    val fullScreen = MutableLiveData(false)
    val distanceFormatterMetric = MutableLiveData(false)
    val showCameraDebugInfo = MutableLiveData(false)
    val enableScalebar = MutableLiveData(false)
    val routingProfile = MutableLiveData("--")

    val actionsShowCompassButton = MutableLiveData(false)
    val actionsAdditionalButtons = MutableLiveData(false)
    val actionsCustomCompassButton = MutableLiveData(false)
    val actionsCustomCameraButton = MutableLiveData(false)
    val actionsCustomAudioButton = MutableLiveData(false)
    val actionsCustomRecenterButton = MutableLiveData(false)
    val actionsCustomLayout = MutableLiveData(false)
}
