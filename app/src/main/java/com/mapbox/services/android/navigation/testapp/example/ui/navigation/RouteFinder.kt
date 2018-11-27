package com.mapbox.services.android.navigation.testapp.example.ui.navigation

import android.arch.lifecycle.MutableLiveData
import android.content.Context
import android.location.Location
import android.widget.Toast
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.geojson.Point
import com.mapbox.services.android.navigation.testapp.example.ui.ExampleViewModel
import com.mapbox.services.android.navigation.v5.navigation.OfflineData
import com.mapbox.services.android.navigation.v5.navigation.RouteFoundCallback
import timber.log.Timber


class RouteFinder(private val viewModel: ExampleViewModel,
                  private val routes: MutableLiveData<List<DirectionsRoute>>,
                  accessToken: String,
                  tileVersion: String): RouteFoundCallback {

    private val offlineRouteFinder: OfflineRouteFinder?
    private val routeFinder: ExampleRouteFinder = ExampleRouteFinder(accessToken, this)
    private lateinit var toast:Toast

    init {
        if (tileVersion == "Offline Disabled") {
            offlineRouteFinder = null
        } else {
            offlineRouteFinder = OfflineRouteFinder(tileVersion, this)
        }

    }

    internal fun findRoute(
            context: Context, location: Location, destination: Point, isOffline: Boolean) {
        toast = Toast.makeText(context, "There was an error retrieving the route", Toast
                .LENGTH_LONG)

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

    override fun routesFound(routes: MutableList<DirectionsRoute>) {
        if (routes == null || routes.isEmpty()) {
            toast.show()
            return
        }

        updateRoutes(routes)
    }

    override fun onError(offlineData: OfflineData) {
    }

    private fun updateRoutes(routes: List<DirectionsRoute>) {
        this.routes.value = routes
        Timber.d("dfs " + routes.size)
        viewModel.primaryRoute = routes.first()

        // Handle off-route scenarios
        if (viewModel.isOffRoute) {
            viewModel.isOffRoute = false
            viewModel.startNavigation()
        }
    }
}
