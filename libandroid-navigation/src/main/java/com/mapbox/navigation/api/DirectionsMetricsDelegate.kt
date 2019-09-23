package com.mapbox.navigation.api

interface DirectionsMetricsDelegate {
  fun onFasterRoute(newDistanceRemaining: Int)
}