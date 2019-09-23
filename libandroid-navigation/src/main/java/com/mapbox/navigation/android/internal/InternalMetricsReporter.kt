package com.mapbox.navigation.android.internal

import android.location.Location
import com.mapbox.navigation.api.DirectionsMetricsDelegate
import com.mapbox.navigation.api.NavigationMetricsDelegate

internal object InternalMetricsReporter: DirectionsMetricsDelegate, NavigationMetricsDelegate {
  override fun onFasterRoute(newDistanceRemaining: Int) {
    TODO("not implemented")
  }

  override fun onReroute(newDistanceRemaining: Int, secondsSinceLastReroute: Long, locationsBefore: Array<Location>) {
    TODO("not implemented")
  }
}