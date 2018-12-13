package com.mapbox.services.android.navigation.testapp.example.ui.navigation

import android.arch.lifecycle.MutableLiveData
import android.location.Location
import android.os.Environment
import android.preference.PreferenceManager
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.geojson.Point
import com.mapbox.services.android.navigation.testapp.NavigationApplication
import com.mapbox.services.android.navigation.testapp.R
import com.mapbox.services.android.navigation.testapp.example.ui.ExampleViewModel
import timber.log.Timber

class RouteFinder(private val viewModel: ExampleViewModel,
                  private val routes: MutableLiveData<List<DirectionsRoute>>,
                  accessToken: String,
                  private var tileVersion: String) : OnRoutesFoundCallback {

  private val routeFinder: ExampleRouteFinder = ExampleRouteFinder(accessToken, this)
  private val offlineRouteFinder = OfflineRouteFinder(obtainOfflineDirectory(), tileVersion, this)

  internal fun findRoute(location: Location, destination: Point) {
    if (isOfflineEnabled()) {
      findOfflineRoute(location, destination)
    } else {
      findOnlineRoute(location, destination)
    }
  }

  internal fun updateOfflineVersion(tileVersion: String) {
    if (this.tileVersion != tileVersion) {
      offlineRouteFinder.configureWith(tileVersion)
      this.tileVersion = tileVersion
    }
  }

  override fun onRoutesFound(routes: List<DirectionsRoute>) {
    updateRoutes(routes)
  }

  override fun onError(error: String) {
    Timber.d(error)
  }

  private fun obtainOfflineDirectory(): String {
    val offline = Environment.getExternalStoragePublicDirectory("Offline")
    if (!offline.exists()) {
      Timber.d("Offline directory does not exist")
      offline.mkdirs()
    }
    return offline.absolutePath
  }

  private fun isOfflineEnabled(): Boolean {
    val context = NavigationApplication.instance
    val preferences = PreferenceManager.getDefaultSharedPreferences(context)
    return preferences.getBoolean(context.getString(R.string.offline_enabled), false)
  }

  private fun findOnlineRoute(location: Location, destination: Point) {
    routeFinder.findRoute(location, destination)
  }

  private fun findOfflineRoute(location: Location, destination: Point) {
    offlineRouteFinder.findRoute(location, destination)
  }

  private fun updateRoutes(routes: List<DirectionsRoute>) {
    this.routes.value = routes
    viewModel.primaryRoute = routes.first()

    // Handle off-route scenarios
    if (viewModel.isOffRoute) {
      viewModel.isOffRoute = false
      viewModel.startNavigation()
    }
  }
}
