package com.mapbox.services.android.navigation.testapp.example.ui.navigation

import android.content.Context
import android.location.Location
import android.widget.Toast
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.services.android.navigation.v5.navigation.*
import timber.log.Timber

class OfflineRouteFinder(offlinePath: String,
                         version: String,
                         private val callback: OnRoutesFoundCallback) {

    private val offlineRouter: MapboxOfflineRouter
    private var ready = false

    init {
        offlineRouter = MapboxOfflineRouter(offlinePath)
        offlineRouter.configure(version, object : OnOfflineTilesConfiguredCallback {
            override fun onConfigured(numberOfTiles: Int) {
                Timber.d("Offline tiles configured: $numberOfTiles")
                ready = true
            }

            override fun onConfigurationError(error: OfflineError) {
                Timber.d("Offline tiles configuration error: {${error.message}}")
            }
        })
    }

    fun findRoute(context: Context, location: Location, destination: Point) {
        if (ready) {
            val offlineRoute = buildOfflineRoute(context, location, destination)
            offlineRouter.findRoute(offlineRoute, object : OnOfflineRouteFoundCallback {
                override fun onRouteFound(route: DirectionsRoute) {
                    callback.onRoutesFound(listOf(route))
                }

                override fun onError(error: OfflineError) {
                    callback.onError(error.message)
                }
            })
        } else {
            val errorMessage = "Offline router not configured, try again"
            Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
        }
    }

    private fun buildOfflineRoute(context: Context, location: Location, destination: Point): OfflineRoute {
        val origin = Point.fromLngLat(location.longitude, location.latitude)

        return NavigationRoute.builder(context)
                .origin(origin)
                .destination(destination)
                .accessToken(Mapbox.getAccessToken()!!)
                .let {
                    OfflineRoute.builder(it).build()
                }
    }
}
