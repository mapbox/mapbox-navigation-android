package com.mapbox.services.android.navigation.testapp.example.ui.navigation

import com.mapbox.navigation.core.trip.session.OffRouteObserver
import com.mapbox.services.android.navigation.testapp.example.ui.ExampleViewModel

class ExampleOffRouteListener(private val viewModel: ExampleViewModel) : OffRouteObserver {

    override fun onOffRouteStateChanged(offRoute: Boolean) {
        viewModel.isOffRoute = true
        viewModel.findRouteToDestination()
    }
}
