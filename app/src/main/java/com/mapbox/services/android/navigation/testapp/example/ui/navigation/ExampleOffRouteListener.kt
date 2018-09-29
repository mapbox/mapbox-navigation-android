package com.mapbox.services.android.navigation.testapp.example.ui.navigation

import android.location.Location
import com.mapbox.services.android.navigation.testapp.example.ui.ExampleViewModel
import com.mapbox.services.android.navigation.v5.offroute.OffRouteListener

class ExampleOffRouteListener(private val viewModel: ExampleViewModel) : OffRouteListener {

  override fun userOffRoute(location: Location?) {
    viewModel.isOffRoute = true
    viewModel.findRouteToDestination()
  }
}