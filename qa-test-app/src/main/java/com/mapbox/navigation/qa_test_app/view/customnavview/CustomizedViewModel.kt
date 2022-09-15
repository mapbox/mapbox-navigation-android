package com.mapbox.navigation.qa_test_app.view.customnavview

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class CustomizedViewModel : ViewModel() {
    val useCustomViews = MutableLiveData(false)
    val useCustomStyles = MutableLiveData(false)
    val showCustomMapView = MutableLiveData(false)
    val useCustomInfoPanelLayout = MutableLiveData(false)
    val useCustomInfoPanelStyles = MutableLiveData(false)
    val showCustomInfoPanelContent = MutableLiveData(false)
    val showCustomInfoPanelEndNavButton = MutableLiveData(false)
    val showBottomSheetInFreeDrive = MutableLiveData(false)
    val enableDestinationPreview = MutableLiveData(true)
    val isInfoPanelHideable = MutableLiveData(false)
    val infoPanelStateOverride = MutableLiveData("--")
    val fullScreen = MutableLiveData(false)
    val distanceFormatterMetric = MutableLiveData(false)
}
