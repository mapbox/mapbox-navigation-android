package com.mapbox.navigation.qa_test_app.view.customnavview

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class CustomizedViewModel : ViewModel() {
    val useCustomViews = MutableLiveData(false)
    val useCustomStyles = MutableLiveData(false)
    val showCustomMapView = MutableLiveData(false)
    val showBottomSheetInFreeDrive = MutableLiveData(false)
}
