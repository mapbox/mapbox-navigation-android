package com.mapbox.services.android.navigation.testapp.example.ui.navigation

import android.arch.lifecycle.MutableLiveData
import android.content.Context
import android.location.Location
import android.os.Environment
import android.widget.Toast
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.geojson.Point
import com.mapbox.services.android.navigation.testapp.example.ui.ExampleViewModel
import timber.log.Timber

class RouteFinder(private val viewModel: ExampleViewModel,
                  private val routes: MutableLiveData<List<DirectionsRoute>>,
                  accessToken: String,
                  tileVersion: String): OnRoutesFoundCallback {
    private var isOffline = tileVersion != "Offline Disabled"
    private val routeFinder: ExampleRouteFinder = ExampleRouteFinder(accessToken, this)
    private lateinit var toast:Toast
    private var offlineRouteFinder =
            if (isOffline) {
                OfflineRouteFinder(obtainOfflineDirectory(), tileVersion, this)
            } else {
                null
            }

    private fun obtainOfflineDirectory(): String {
        val offline = Environment.getExternalStoragePublicDirectory("Offline")
        if (!offline.exists()) {
            Timber.d("Offline directory does not exist")
            offline.mkdirs()
        }
        return offline.absolutePath
    }

    internal fun findRoute(context: Context, location: Location,
                           destination: Point, isOffline: Boolean) {

        if (isOffline) {
            findOfflineRoute(context, location, destination)
        } else {
            findOnlineRoute(location, destination)
        }
    }

    private fun findOnlineRoute(location: Location, destination: Point) {
        routeFinder.findRoute(location, destination)
    }

    private fun findOfflineRoute(context: Context, location: Location, destination: Point) {
        offlineRouteFinder?.findRoute(context, location, destination)
    }

    override fun onRoutesFound(routes: List<DirectionsRoute>) {
        updateRoutes(routes)
    }

    override fun onError(error: String) {
        Timber.d(error)
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
