package com.mapbox.navigation.examples.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.examples.R
import com.mapbox.navigation.examples.utils.extensions.ifNonNull
import com.mapbox.navigation.ui.NavigationViewOptions
import com.mapbox.navigation.ui.OnNavigationReadyCallback
import com.mapbox.navigation.ui.listeners.BannerInstructionsListener
import com.mapbox.navigation.ui.listeners.NavigationListener
import com.mapbox.navigation.ui.map.NavigationMapboxMap
import kotlinx.android.synthetic.main.activity_navigation_view.navigationView
import kotlinx.android.synthetic.main.activity_traffic_toggle.*

/**
 * This activity shows how to use [NavigationMapboxMap.updateTrafficVisibility] to traffic_toggle_activity
 * the visibility of traffic congestion map layers.
 */
class TrafficToggleActivity :
    AppCompatActivity(),
    OnNavigationReadyCallback,
    NavigationListener,
    BannerInstructionsListener {

    private lateinit var navigationMapboxMap: NavigationMapboxMap
    private lateinit var mapboxNavigation: MapboxNavigation
    private val route by lazy { getDirectionsRoute() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_traffic_toggle)

        navigationView.onCreate(savedInstanceState)
        navigationView.initialize(
            this,
            getInitialCameraPosition()
        )
    }

    override fun onLowMemory() {
        super.onLowMemory()
        navigationView.onLowMemory()
    }

    override fun onStart() {
        super.onStart()
        navigationView.onStart()
    }

    override fun onResume() {
        super.onResume()
        navigationView.onResume()
    }

    override fun onStop() {
        super.onStop()
        navigationView.onStop()
    }

    override fun onPause() {
        super.onPause()
        navigationView.onPause()
    }

    override fun onDestroy() {
        navigationView.onDestroy()
        super.onDestroy()
    }

    override fun onBackPressed() {
        // If the navigation view didn't need to do anything, call super
        if (!navigationView.onBackPressed()) {
            super.onBackPressed()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        navigationView.onSaveInstanceState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        navigationView.onRestoreInstanceState(savedInstanceState)
    }

    override fun onNavigationReady(isRunning: Boolean) {
        if (!isRunning && !::navigationMapboxMap.isInitialized) {
            ifNonNull(navigationView.retrieveNavigationMapboxMap()) { navMapboxMap ->
                this.navigationMapboxMap = navMapboxMap

                /**
                 * Set the Floating Action Button's click listener to traffic_toggle_activity the traffic
                 * congestion layers' visibility.
                 */
                toggle_traffic_button.setOnClickListener {
                    navigationMapboxMap
                        .updateTrafficVisibility(!navigationMapboxMap.isTrafficVisible)
                }

                this.navigationMapboxMap.updateLocationLayerRenderMode(RenderMode.NORMAL)
                navigationView.retrieveMapboxNavigation()?.let { this.mapboxNavigation = it }

                val optionsBuilder = NavigationViewOptions.builder(this)
                optionsBuilder.navigationListener(this)
                optionsBuilder.directionsRoute(route)
                optionsBuilder.shouldSimulateRoute(true)
                optionsBuilder.bannerInstructionsListener(this)
                navigationView.startNavigation(optionsBuilder.build())
            }
        }
    }

    override fun willDisplay(instructions: BannerInstructions?): BannerInstructions? {
        return instructions
    }

    override fun onNavigationRunning() {
    }

    override fun onNavigationFinished() {
        finish()
    }

    override fun onCancelNavigation() {
        navigationView.stopNavigation()
        finish()
    }

    private fun getInitialCameraPosition(): CameraPosition {
        val originCoordinate = route.routeOptions()?.coordinates()?.get(0)
        return CameraPosition.Builder()
            .target(LatLng(originCoordinate!!.latitude(), originCoordinate.longitude()))
            .zoom(15.0)
            .build()
    }

    private fun getDirectionsRoute(): DirectionsRoute {
        val directionsRouteAsJson = resources
            .openRawResource(R.raw.sample_route_1)
            .bufferedReader()
            .use { it.readText() }

        return DirectionsRoute.fromJson(directionsRouteAsJson)
    }
}
