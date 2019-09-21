package com.mapbox.navigation.api

import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute

interface Directions {
  fun getDirections(routeRequest: NavigationRoute,
                    listener: DirectionsSession.Listener): DirectionsSession
}