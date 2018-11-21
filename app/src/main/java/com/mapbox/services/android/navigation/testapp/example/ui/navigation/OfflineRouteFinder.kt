package com.mapbox.services.android.navigation.testapp.example.ui.navigation

import android.content.Context
import android.location.Location
import android.os.Environment
import android.widget.Toast
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.services.android.navigation.v5.navigation.MapboxOfflineNavigation
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute
import com.mapbox.services.android.navigation.v5.navigation.OfflineRoute
import java.io.File

class OfflineRouteFinder(private val version: String, private val callback: RouteFoundCallback) {

    private val offlineNavigation = MapboxOfflineNavigation()
    private val offlineTileDir = Environment.getExternalStoragePublicDirectory("Offline")
    private val offlineTileVersionDir = File(offlineTileDir, version)
    private var ready = false

    init {
        offlineNavigation.initializeOfflineData(
                offlineTileVersionDir.absolutePath) { ready = true }
    }

    fun findRoute(context: Context, location: Location, destination: Point) {
        if (ready) {
            buildOfflineRoute(context, location, destination).let {
                offlineNavigation.findOfflineRoute(it) {
                    callback.routeFound(listOf(it)) }
            }
        } else {
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