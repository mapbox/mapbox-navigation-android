package com.mapbox.navigation.examples.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory.zoomTo
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.navigation.base.trip.model.RouteLegProgress
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.arrival.ArrivalObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.examples.R
import com.mapbox.navigation.ui.NavigationViewOptions
import com.mapbox.navigation.ui.OnNavigationReadyCallback
import com.mapbox.navigation.ui.listeners.NavigationListener
import com.mapbox.navigation.ui.map.NavigationMapboxMap
import com.mapbox.navigation.ui.map.building.BuildingFootprintHighlightLayer
import com.mapbox.navigation.utils.internal.ifNonNull
import kotlinx.android.synthetic.main.activity_final_destination_arrival_building_highlight.*

/**
 * This activity shows how to use the Navigation UI SDK's
 * [BuildingFootprintHighlightLayer] to show and customize a building's
 * footprint on the [com.mapbox.navigation.ui.NavigationView]'s map.
 */
class BuildingFootprintHighlightActivity :
    AppCompatActivity(),
    OnNavigationReadyCallback,
    NavigationListener,
    ArrivalObserver,
    RouteProgressObserver {

    private lateinit var mapboxMap: MapboxMap
    private lateinit var navigationMapboxMap: NavigationMapboxMap
    private val route by lazy { getDirectionsRoute() }
    private val highlightQueryLatLng = LatLng(37.79115, -122.41376)
    private var higlightedFootprintAlreadyShown = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_final_destination_arrival_building_highlight)

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
        super.onDestroy()
        navigationView.onDestroy()
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
                this.navigationMapboxMap.updateLocationLayerRenderMode(RenderMode.NORMAL)
                this.mapboxMap = navMapboxMap.retrieveMap()
                val optionsBuilder = NavigationViewOptions.builder(this)
                optionsBuilder.navigationListener(this)

                // Pass the ArrivalObserver interface (this activity)
                optionsBuilder.arrivalObserver(this)

                // Pass the RouteProgressObserver interface (this activity)
                optionsBuilder.routeProgressObserver(this)

                optionsBuilder.directionsRoute(route)
                optionsBuilder.shouldSimulateRoute(true)
                navigationView.startNavigation(optionsBuilder.build())
            }
        }
    }

    override fun onNextRouteLegStart(routeLegProgress: RouteLegProgress) {
        // Not needed in this example
    }

    override fun onFinalDestinationArrival(routeProgress: RouteProgress) {
        mapboxMap.easeCamera(zoomTo(18.0), 1800)
    }

    override fun onRouteProgressChanged(routeProgress: RouteProgress) {
        /**
         * Show the building footprint when the device is a certain number of meters away
         * from the end of the [DirectionsRoute] and if the highlighted footprint hasn't already been shown.
         */
        if (routeProgress.distanceRemaining < METERS_DISTANCE_AWAY_TO_TRIGGER_HIGHLIGHTING &&
            !higlightedFootprintAlreadyShown
        ) {
            mapboxMap.getStyle {
                /**
                 * Set the [LatLng] to be used by the [BuildingFootprintHighlightLayer].
                 *
                 * The LatLng passed through below is different than the coordinate used as the
                 * final destination coordinate in this example's [DirectionsRoute].
                 *
                 * This LatLng should be set before the visibility of the footprint is set to true.
                 */

                // Initialize the Nav UI SDK's BuildingFootprintHighlightLayer class.
                val buildingFootprintHighlightLayer = BuildingFootprintHighlightLayer(mapboxMap)

                buildingFootprintHighlightLayer.queryLatLng = highlightQueryLatLng

                buildingFootprintHighlightLayer.updateVisibility(true)

                /**
                 * Set to true so that the code doesn't try to show highlighted footprint
                 * again as the device remains less than a certain number of meters away
                 * from the end of the [DirectionsRoute].
                 */
                higlightedFootprintAlreadyShown = true

                // Click on a building footprint to move the highlighted footprint
                mapboxMap.addOnMapClickListener {
                    buildingFootprintHighlightLayer.queryLatLng = it
                    true
                }
            }
        }
    }

    override fun onNavigationRunning() {
        // Not needed in this example
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

    companion object {
        private const val METERS_DISTANCE_AWAY_TO_TRIGGER_HIGHLIGHTING = 250f
    }
}
