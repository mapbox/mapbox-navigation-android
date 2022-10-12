package com.mapbox.navigation.qa_test_app.view.customnavview

import android.content.Context
import com.mapbox.maps.MapView
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.dropin.map.MapViewBinder

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class CustomMapViewBinder : MapViewBinder() {

    override fun getMapView(context: Context): MapView {
        return customMapViewFromCode(context)
    }

    override val shouldLoadMapStyle: Boolean = false
}
