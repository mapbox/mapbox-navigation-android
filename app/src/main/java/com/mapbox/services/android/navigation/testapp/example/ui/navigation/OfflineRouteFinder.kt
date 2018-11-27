package com.mapbox.services.android.navigation.testapp.example.ui.navigation

import android.content.Context
import android.location.Location
import android.os.Environment
import android.widget.Toast
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.services.android.navigation.v5.navigation.*
import java.io.File

class OfflineRouteFinder(version: String, private val callback: RouteFoundCallback) {

    private val offlineRouter = MapboxOfflineRouter()
    private val offlineDir = Environment.getExternalStoragePublicDirectory("Offline")
    private val offlineTileDir = File(offlineDir, "tiles")
    private val offlineTileVersionDir = File(offlineTileDir, version)
    private var ready = false

    init {
        initDirectory(offlineTileVersionDir)

        offlineRouter.initializeOfflineData(
                offlineTileVersionDir.absolutePath) { ready = true }
    }

    fun initDirectory(directory: File) {
        directory.let {
            if (!it.exists()) {
                it.mkdir()
            }
        }
    }

    fun findRoute(context: Context, location: Location, destination: Point) {
        if (ready) {
            val offlineRoute = buildOfflineRoute(context, location, destination)
            offlineRouter.findOfflineRoute(offlineRoute, callback)
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
