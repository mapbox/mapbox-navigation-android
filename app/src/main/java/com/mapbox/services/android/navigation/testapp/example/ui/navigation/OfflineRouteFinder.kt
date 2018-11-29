package com.mapbox.services.android.navigation.testapp.example.ui.navigation

import android.content.Context
import android.location.Location
import android.widget.Toast
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.services.android.navigation.v5.navigation.*

class OfflineRouteFinder(offlinePath: String,
                         version: String,
                         private val callback: OnOfflineRouteFoundCallback) {

    private val offlineRouter: MapboxOfflineRouter
    private var ready = false

    init {
        offlineRouter = MapboxOfflineRouter(offlinePath)
        offlineRouter.configure(version) { ready = true }
    }

    fun findRoute(context: Context, location: Location, destination: Point) {
        if (ready) {
            val offlineRoute = buildOfflineRoute(context, location, destination)
            offlineRouter.findRoute(offlineRoute, callback)
        } else {
            callback.onError(OfflineData(OfflineData.Status.ROUTER_BEING_INITIALIZED))
            Toast.makeText(context, "Still initializing data, try again", Toast.LENGTH_SHORT).show()
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
