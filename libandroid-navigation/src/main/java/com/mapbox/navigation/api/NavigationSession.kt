package com.mapbox.navigation.api

import android.location.Location
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress

interface NavigationSession {
  fun restart(directionsRoute: DirectionsRoute, listener: Listener)
  fun cancel()

  interface Listener {
    fun onRouteProgress(progress: RouteProgress)
    fun onOffRoute(location: Location)

    companion object {
      inline operator fun invoke(crossinline progressBlock: (RouteProgress) -> Unit,
                                 crossinline offRouteBlock: (Location) -> Unit): Listener =
        object : Listener {
        override fun onRouteProgress(progress: RouteProgress) {
          progressBlock(progress)
        }

        override fun onOffRoute(location: Location) {
          offRouteBlock(location)
        }
      }
    }
  }
}