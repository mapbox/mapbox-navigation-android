package com.mapbox.navigation.dropin.map

import android.content.Context
import com.mapbox.maps.MapView
import com.mapbox.maps.plugin.compass.compass
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI

@ExperimentalPreviewMapboxNavigationAPI
internal class MapboxMapViewBinder : MapViewBinder() {

    override val shouldLoadMapStyle: Boolean = true

    override fun getMapView(context: Context): MapView {
        return MapView(context).apply {
            compass.enabled = false
        }
    }
}
