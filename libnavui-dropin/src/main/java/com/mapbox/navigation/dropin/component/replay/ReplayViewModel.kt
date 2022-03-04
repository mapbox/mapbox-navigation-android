package com.mapbox.navigation.dropin.component.replay

import android.annotation.SuppressLint
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.replay.route.ReplayProgressObserver
import com.mapbox.navigation.dropin.lifecycle.UIViewModel

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
@SuppressLint("MissingPermission")
class ReplayViewModel : UIViewModel<Unit, Unit>(Unit) {
    private lateinit var replayProgressObserver: ReplayProgressObserver
    private lateinit var routesObserver: RoutesObserver

    override fun process(mapboxNavigation: MapboxNavigation, state: Unit, action: Unit) {
        // No op
    }

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)
        val context = mapboxNavigation.navigationOptions.applicationContext
        val mapboxReplayer = mapboxNavigation.mapboxReplayer
        routesObserver = RoutesObserver { result ->
            if (result.navigationRoutes.isEmpty()) {
                mapboxReplayer.clearEvents()
                mapboxNavigation.resetTripSession()
                mapboxReplayer.pushRealLocation(context, 0.0)
                mapboxReplayer.play()
            }
        }
        replayProgressObserver = ReplayProgressObserver(mapboxNavigation.mapboxReplayer)

        mapboxNavigation.registerRouteProgressObserver(replayProgressObserver)
        mapboxNavigation.registerRoutesObserver(routesObserver)

        mapboxReplayer.pushRealLocation(context, 0.0)
        mapboxReplayer.playbackSpeed(1.5)
        mapboxReplayer.play()
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        super.onDetached(mapboxNavigation)
        mapboxNavigation.unregisterRouteProgressObserver(replayProgressObserver)
        mapboxNavigation.unregisterRoutesObserver(routesObserver)
        mapboxNavigation.mapboxReplayer.finish()
    }
}
