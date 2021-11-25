package com.mapbox.navigation.dropin.component.routearrow

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowApi
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowView
import com.mapbox.navigation.ui.maps.route.arrow.model.RouteArrowOptions

internal class RouteArrowViewModelFactory(
    private val routeArrowOptions: RouteArrowOptions
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RouteArrowViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RouteArrowViewModel(
                MapboxRouteArrowApi(),
                MapboxRouteArrowView(routeArrowOptions)
            ) as T
        }
        throw IllegalArgumentException("Unable to construct navigation view model.")
    }
}
