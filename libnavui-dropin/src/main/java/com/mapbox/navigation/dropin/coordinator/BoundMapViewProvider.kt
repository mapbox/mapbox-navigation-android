package com.mapbox.navigation.dropin.coordinator

import android.view.ViewGroup
import com.mapbox.maps.MapView
import com.mapbox.navigation.dropin.databinding.MapboxMapviewLayoutBinding

internal object BoundMapViewProvider {

    fun bindLayoutAndGet(viewGroup: ViewGroup): MapView =
        MapboxMapviewLayoutBinding.bind(viewGroup).mapView
}
