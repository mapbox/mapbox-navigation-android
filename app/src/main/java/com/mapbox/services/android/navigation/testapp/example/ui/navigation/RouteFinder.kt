package com.mapbox.services.android.navigation.testapp.example.ui.navigation

import android.location.Location
import android.os.Environment
import android.preference.PreferenceManager
import androidx.lifecycle.MutableLiveData
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.extensions.coordinates
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RouteObserver
import com.mapbox.services.android.navigation.testapp.NavigationApplication
import com.mapbox.services.android.navigation.testapp.R
import com.mapbox.services.android.navigation.testapp.example.ui.ExampleViewModel
import timber.log.Timber

class RouteFinder(
    private val viewModel: ExampleViewModel,
    private val routes: MutableLiveData<List<DirectionsRoute>>,
    private val navigation: () -> MapboxNavigation,
    private val accessToken: String
) : RouteObserver {

    init {
        navigation().registerRouteObserver(this)
    }

    internal fun findRoute(location: Location, destination: Point) {
        navigation().requestRoutes(
            RouteOptions.builder()
                .accessToken(accessToken)
                .coordinates(
                    origin = Point.fromLngLat(
                        location.longitude,
                        location.latitude
                    ),
                    destination = destination
                ).build()
        )
    }

    override fun onRoutesChanged(routes: List<DirectionsRoute>) {
        updateRoutes(routes)
    }

    override fun onRoutesRequested() {
        routes.value = emptyList()
        viewModel.primaryRoute = null
    }

    override fun onRoutesRequestFailure(throwable: Throwable) {
        Timber.d(throwable)
    }

    private fun obtainOfflineDirectory(): String {
        val offline = Environment.getExternalStoragePublicDirectory("Offline")
        if (!offline.exists()) {
            Timber.d("Offline directory does not exist")
            offline.mkdirs()
        }
        return offline.absolutePath
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
