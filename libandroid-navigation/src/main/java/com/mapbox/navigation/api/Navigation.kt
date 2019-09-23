package com.mapbox.navigation.api

import android.location.Location
import com.mapbox.api.directions.v5.models.DirectionsRoute

interface Navigation {
  var location: Location?

  fun startNavigation(directionsRoute: DirectionsRoute,
                      listener: NavigationSession.Listener): NavigationSession
}