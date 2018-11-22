package com.mapbox.services.android.navigation.testapp.example.ui.navigation

import android.arch.lifecycle.MutableLiveData
import android.content.Context
import android.location.Location
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.geojson.Point
import com.mapbox.services.android.navigation.testapp.example.ui.ExampleViewModel



class RouteFinderDelegator(private val viewModel: ExampleViewModel,
                           private val routes: MutableLiveData<List<DirectionsRoute>>,
                           accessToken: String,
                           tileVersion: String): RouteFoundCallback {

    private val offlineRouteFinder = OfflineRouteFinder(tileVersion, this)
    private val routeFinder: ExampleRouteFinder = ExampleRouteFinder(accessToken, this)

    internal fun findRoute(
            context: Context, location: Location, destination: Point, isOffline: Boolean) {
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
        offlineRouteFinder.findRoute(context, location, destination)
    }

    override fun routeFound(routes: List<DirectionsRoute>) {
        updateRoutes(routes)
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
