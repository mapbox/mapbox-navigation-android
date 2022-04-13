package com.mapbox.navigation.dropin.component.tripsession

import android.annotation.SuppressLint
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.replay.route.ReplayProgressObserver

@ExperimentalPreviewMapboxNavigationAPI
@SuppressLint("MissingPermission")
internal class ReplayRouteTripSession {
    private var replayProgressObserver: ReplayProgressObserver? = null
    private var routesObserver: RoutesObserver? = null

    fun start(mapboxNavigation: MapboxNavigation) {
        mapboxNavigation.stopTripSession()
        mapboxNavigation.startReplayTripSession()
        val context = mapboxNavigation.navigationOptions.applicationContext
        val mapboxReplayer = mapboxNavigation.mapboxReplayer

        routesObserver = RoutesObserver { result ->
            if (result.navigationRoutes.isEmpty()) {
                mapboxReplayer.clearEvents()
                mapboxNavigation.resetTripSession()
                mapboxReplayer.pushRealLocation(context, 0.0)
                mapboxReplayer.play()
            }
        }.also { mapboxNavigation.registerRoutesObserver(it) }

        replayProgressObserver = ReplayProgressObserver(mapboxNavigation.mapboxReplayer)
            .also { mapboxNavigation.registerRouteProgressObserver(it) }

        mapboxReplayer.pushRealLocation(context, 0.0)
        mapboxReplayer.playbackSpeed(1.5)
        mapboxReplayer.play()
    }

    fun stop(mapboxNavigation: MapboxNavigation) {
        replayProgressObserver?.let { mapboxNavigation.unregisterRouteProgressObserver(it) }
        routesObserver?.let { mapboxNavigation.unregisterRoutesObserver(it) }
        mapboxNavigation.mapboxReplayer.stop()
        mapboxNavigation.mapboxReplayer.clearEvents()
        mapboxNavigation.stopTripSession()
    }
}
