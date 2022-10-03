package com.mapbox.navigation.dropin.map

import android.view.ViewGroup
import androidx.transition.Scene
import com.mapbox.maps.MapView
import com.mapbox.maps.plugin.compass.compass
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.dropin.R
import com.mapbox.navigation.dropin.databinding.MapboxMapviewLayoutBinding

@ExperimentalPreviewMapboxNavigationAPI
internal class MapboxMapViewBinder : MapViewBinder() {

    override fun getMapView(viewGroup: ViewGroup): MapView {
        Scene.getSceneForLayout(
            viewGroup,
            R.layout.mapbox_mapview_layout,
            viewGroup.context,
        ).enter()
        return MapboxMapviewLayoutBinding.bind(viewGroup).mapView
    }

    override fun addMapViewToLayout(mapView: MapView, viewGroup: ViewGroup) {
        // do nothing
    }

    override fun onMapViewReady(mapView: MapView) {
        mapView.compass.enabled = false
    }

    @MapStyleLoadPolicy.MapLoadStylePolicy
    override fun getMapStyleLoadPolicy(): Int = MapStyleLoadPolicy.ON_CONFIGURATION_CHANGE
}
