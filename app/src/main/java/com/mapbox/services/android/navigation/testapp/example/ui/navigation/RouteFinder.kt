package com.mapbox.services.android.navigation.testapp.example.ui.navigation

import android.location.Location
import android.os.Environment
import androidx.lifecycle.MutableLiveData
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.extensions.applyDefaultParams
import com.mapbox.navigation.base.extensions.coordinates
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.services.android.navigation.testapp.example.ui.ExampleViewModel
import timber.log.Timber

class RouteFinder(
    private val viewModel: ExampleViewModel,
    private val routes: MutableLiveData<List<DirectionsRoute>>,
    private val navigation: () -> MapboxNavigation,
    private val accessToken: String
) : RoutesObserver {

    init {
        navigation().registerRoutesObserver(this)
    }

    internal fun findRoute(location: Location, destination: Point) {
        navigation().requestRoutes(
            RouteOptions.builder()
                .applyDefaultParams()
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
