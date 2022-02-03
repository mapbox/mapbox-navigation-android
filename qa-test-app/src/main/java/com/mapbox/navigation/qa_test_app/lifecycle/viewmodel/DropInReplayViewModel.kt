package com.mapbox.navigation.qa_test_app.lifecycle.viewmodel

import android.annotation.SuppressLint
import androidx.lifecycle.ViewModel
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.core.replay.route.ReplayProgressObserver

@SuppressLint("MissingPermission")
@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class DropInReplayViewModel : ViewModel() {

    private val navigationObserver = object : MapboxNavigationObserver {
        private lateinit var replayProgressObserver: ReplayProgressObserver
        private lateinit var routesObserver: RoutesObserver

        override fun onAttached(mapboxNavigation: MapboxNavigation) {
            val context = mapboxNavigation.navigationOptions.applicationContext
            val mapboxReplayer = mapboxNavigation.mapboxReplayer
            routesObserver = RoutesObserver { result ->
                if (result.routes.isEmpty()) {
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
            mapboxNavigation.unregisterRouteProgressObserver(replayProgressObserver)
            mapboxNavigation.unregisterRoutesObserver(routesObserver)
            mapboxNavigation.mapboxReplayer.finish()
        }
    }

    fun startSimulation() {
        with(MapboxNavigationApp.current() ?: return) {
            mapboxReplayer.clearEvents()
            resetTripSession()
            mapboxReplayer.pushRealLocation(navigationOptions.applicationContext, 0.0)
            mapboxReplayer.play()
        }
    }

    init {
        MapboxNavigationApp.registerObserver(navigationObserver)
    }

    override fun onCleared() {
        MapboxNavigationApp.unregisterObserver(navigationObserver)
    }
}
