package com.mapbox.navigation.api

import android.location.Location

interface NavigationMetricsDelegate {
  fun onReroute(newDistanceRemaining: Int, secondsSinceLastReroute: Long, locationsBefore: Array<Location>)
}