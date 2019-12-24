package com.mapbox.services.android.navigation.testapp.activity

import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.annotations.MarkerOptions
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.navigation.base.route.Router
import com.mapbox.navigation.base.route.model.RouteOptionsNavigation
import com.mapbox.navigation.route.onboard.MapboxOnboardRouter
import com.mapbox.navigation.route.onboard.model.Config
import com.mapbox.navigation.utils.extensions.ifNonNull
import com.mapbox.services.android.navigation.testapp.R
import com.mapbox.services.android.navigation.testapp.utils.Utils
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute
import com.mapbox.turf.TurfConstants
import com.mapbox.turf.TurfMeasurement
import java.io.File
import kotlinx.android.synthetic.main.activity_mock_navigation.*

class OnboardRouterActivityKt : AppCompatActivity(),
    OnMapReadyCallback,
    MapboxMap.OnMapClickListener {
// DirectionsSession.RouteObserver {

    private lateinit var onboardRouter: Router
    private lateinit var mapboxMap: MapboxMap

    private var route: DirectionsRoute? = null
    private lateinit var navigationMapRoute: NavigationMapRoute
    // private lateinit var directionsSession: DirectionsSession
    private var origin: Point? = null
    private var destination: Point? = null
    private var waypoint: Point? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mock_navigation)
        setupRouter()
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)
        newLocationFab?.setOnClickListener { newOrigin() }
    }

    private fun setupRouter() {
        val file = File(
            Environment.getExternalStoragePublicDirectory("Offline").absolutePath,
            "2019_04_13-00_00_11"
        )
        val fileTiles = File(file, "tiles")
        val config = Config(
            fileTiles.absolutePath,
            null,
            null,
            null,
            null // working with pre-fetched tiles only
        )
        onboardRouter = MapboxOnboardRouter(config)
    }

    private fun newOrigin() {
        clearMap()
        val latLng = LatLng(47.05991, 9.49183)
        origin = Point.fromLngLat(latLng.longitude, latLng.latitude)
        mapboxMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 12.0))
    }

    override fun onMapReady(mapboxMap: MapboxMap) {
        this.mapboxMap = mapboxMap
        this.mapboxMap.addOnMapClickListener(this)
        mapboxMap.setStyle(
            Style.MAPBOX_STREETS
        ) {
            navigationMapRoute = NavigationMapRoute(mapView, mapboxMap)
            Snackbar.make(
                findViewById(R.id.container),
                "Tap map to place waypoint",
                Snackbar.LENGTH_LONG
            ).show()
            newOrigin()
        }
    }

    private fun clearMap() {
        mapboxMap.clear()
        route = null
        destination = null
        waypoint = null
        navigationMapRoute.updateRouteVisibilityTo(false)
        navigationMapRoute.updateRouteArrowVisibilityTo(false)
    }

    private fun findRoute() {
        // directionsSession = MapboxDirectionsSession(
        //     onboardRouter,
        //     this
        // )
        ifNonNull(origin, destination) { origin, destination ->
            if (TurfMeasurement.distance(origin, destination, TurfConstants.UNIT_METERS) > 50) {

                val optionsBuilder =
                    RouteOptionsNavigation.Builder()
                        .accessToken(Utils.getMapboxAccessToken(this))
                        .origin(origin)
                        .destination(destination)
                waypoint?.let { optionsBuilder.addWaypoint(it) }

                // directionsSession.requestRoutes(optionsBuilder.build())
            }
        }
    }

    override fun onMapClick(point: LatLng): Boolean {
        when {
            destination == null -> {
                destination = Point.fromLngLat(point.longitude, point.latitude)
                mapboxMap.addMarker(MarkerOptions().position(point))
                findRoute()
            }
            waypoint == null -> {
                waypoint = Point.fromLngLat(point.longitude, point.latitude)
                mapboxMap.addMarker(MarkerOptions().position(point))
                findRoute()
            }
            else -> {
                Toast.makeText(
                    this,
                    "Only 2 waypoints supported for this example",
                    Toast.LENGTH_LONG
                )
                    .show()
                clearMap()
            }
        }
        return false
    }

    /*
     * DirectionSessions.RouteObserver
     */
    // override fun onRoutesChanged(routes: List<Route>) {
    //     if (routes.isNotEmpty()) {
    //         route = routes[0].mapToDirectionsRoute()
    //         navigationMapRoute.addRoute(route)
    //     }
    // }

    // override fun onRoutesRequested() {
    //     Timber.d("onRoutesRequested: navigation.getRoute()")
    // }
    //
    // override fun onRoutesRequestFailure(throwable: Throwable) {
    //     Timber.e(throwable, "onRoutesRequestFailure: navigation.getRoute()")
    // }

    /*
     * Activity lifecycle methods
     */
    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        // directionsSession.cancel()
        mapboxMap.removeOnMapClickListener(this)
        mapView.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        outState?.let { mapView.onSaveInstanceState(it) }
    }
}
