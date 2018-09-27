package com.mapbox.services.android.navigation.testapp.example.ui.navigation

import android.arch.lifecycle.MutableLiveData
import android.location.Location
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.geojson.Point
import com.mapbox.services.android.navigation.testapp.NavigationApplication
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import timber.log.Timber

class ExampleRouteFinder(private val routes: MutableLiveData<List<DirectionsRoute>>,
                         private val accessToken: String) : Callback<DirectionsResponse> {

  companion object {
    const val BEARING_TOLERANCE = 90.0
  }

  var primaryRoute: DirectionsRoute? = null

  fun findRoute(location: Location, destination: Point) {
    find(location, destination)
  }

  override fun onResponse(call: Call<DirectionsResponse>, response: Response<DirectionsResponse>) {
    handle(response.body())
  }

  override fun onFailure(call: Call<DirectionsResponse>, throwable: Throwable) {
    Timber.e(throwable)
  }

  private fun find(location: Location, destination: Point) {
    val origin = Point.fromLngLat(location.longitude, location.latitude)
    val bearing = location.bearing.toDouble()
    NavigationRoute.builder(NavigationApplication.instance)
        .accessToken(accessToken)
        .origin(origin, bearing, BEARING_TOLERANCE)
        .destination(destination)
        .alternatives(true)
        .build()
        .getRoute(this)
  }

  private fun handle(directionsResponse: DirectionsResponse?) {
    directionsResponse?.routes()?.let {
      if (it.isNotEmpty()) {
        updateRoutes(it)
      }
    }
  }

  private fun updateRoutes(routes: List<DirectionsRoute>) {
    this.routes.value = routes
    primaryRoute = routes.first()
  }
}