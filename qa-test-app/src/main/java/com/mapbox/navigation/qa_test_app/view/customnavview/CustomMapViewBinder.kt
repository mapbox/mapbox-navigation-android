package com.mapbox.navigation.qa_test_app.view.customnavview

import android.content.Context
import android.view.ViewGroup
import com.mapbox.maps.MapView
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.dropin.map.MapViewBinder

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class CustomMapViewBinder(private val context: Context) : MapViewBinder() {

    override fun onCreateMapView(viewGroup: ViewGroup): MapView {
        return customMapViewFromCode(context).also {
            viewGroup.removeAllViews()
            viewGroup.addView(it)
        }
    }
}
