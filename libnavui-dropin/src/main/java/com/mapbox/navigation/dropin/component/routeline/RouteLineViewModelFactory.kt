package com.mapbox.navigation.dropin.component.routeline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineApi
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineView
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions

internal class RouteLineViewModelFactory(
    private val mapboxRouteLineOptions: MapboxRouteLineOptions
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RouteLineViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RouteLineViewModel(
                MapboxRouteLineApi(mapboxRouteLineOptions),
                MapboxRouteLineView(mapboxRouteLineOptions)
            ) as T
        }
        throw IllegalArgumentException("Unable to construct navigation view model.")
    }
}
