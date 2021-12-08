package com.mapbox.navigation.qa_test_app.lifecycle

import androidx.lifecycle.ViewModel
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.core.replay.route.ReplayProgressObserver
import com.mapbox.navigation.core.replay.route.ReplayRouteMapper

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class DropInReplayViewModel : ViewModel() {

    private val navigationObserver = object : MapboxNavigationObserver {
        private lateinit var replayProgressObserver: ReplayProgressObserver
        private lateinit var routesObserver: RoutesObserver

        override fun onAttached(mapboxNavigation: MapboxNavigation) {
            val context = mapboxNavigation.navigationOptions.applicationContext
            routesObserver = RoutesObserver { result ->
                if (result.routes.isEmpty()) {
                    mapboxNavigation.mapboxReplayer.clearEvents()
                    mapboxNavigation.resetTripSession()
                    mapboxNavigation.mapboxReplayer.pushRealLocation(context, 0.0)
                    mapboxNavigation.mapboxReplayer.play()
                }
            }
            replayProgressObserver = ReplayProgressObserver(mapboxNavigation.mapboxReplayer)

            mapboxNavigation.registerRouteProgressObserver(replayProgressObserver)
            mapboxNavigation.registerRoutesObserver(routesObserver)

            mapboxNavigation.mapboxReplayer.pushRealLocation(context, 0.0)
            mapboxNavigation.mapboxReplayer.playbackSpeed(1.5)
            mapboxNavigation.mapboxReplayer.play()
        }

        override fun onDetached(mapboxNavigation: MapboxNavigation) {
            mapboxNavigation.unregisterRouteProgressObserver(replayProgressObserver)
            mapboxNavigation.unregisterRoutesObserver(routesObserver)
            mapboxNavigation.mapboxReplayer.finish()
        }
    }

    fun startSimulation() = MapboxNavigationApp.current()?.apply {
        val route = MapboxNavigationApp.current()?.getRoutes()?.get(0)
        checkNotNull(route) { "Current route should not be null" }
        mapboxReplayer.stop()
        mapboxReplayer.clearEvents()
        val replayData = ReplayRouteMapper().mapDirectionsRouteGeometry(route)
        mapboxReplayer.pushEvents(replayData)
        mapboxReplayer.seekTo(replayData[0])
        mapboxReplayer.play()
        startReplayTripSession()
    }

    init {
        MapboxNavigationApp.registerObserver(navigationObserver)
    }

    override fun onCleared() {
        MapboxNavigationApp.unregisterObserver(navigationObserver)
    }
}
