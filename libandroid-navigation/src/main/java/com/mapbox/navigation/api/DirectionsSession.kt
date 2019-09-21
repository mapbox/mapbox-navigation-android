package com.mapbox.navigation.api

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.geojson.Point

interface DirectionsSession {
  var route: DirectionsRoute?

  fun addWaypoint(point: Point)
  fun removeWaypoint(point: Point)
  fun reRoute(from: Point)
  fun setProfile(profile: String)
  fun setFasterRouteListener(fasterRouteListener: Listener)
  fun cancel()

  interface Listener {
    fun onDirectionsResponse(routes: Array<DirectionsRoute>)

    companion object {
      inline operator fun invoke(crossinline block: (Array<DirectionsRoute>) -> Unit): Listener =
        object : Listener {
          override fun onDirectionsResponse(route: Array<DirectionsRoute>) {
            block(route)
          }
        }
    }
  }
}