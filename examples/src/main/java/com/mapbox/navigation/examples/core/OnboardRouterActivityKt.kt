package com.mapbox.navigation.examples.core

import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.annotations.MarkerOptions
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.navigation.base.extensions.applyDefaultParams
import com.mapbox.navigation.base.extensions.coordinates
import com.mapbox.navigation.base.options.MapboxOnboardRouterConfig
import com.mapbox.navigation.base.route.Router
import com.mapbox.navigation.examples.R
import com.mapbox.navigation.examples.utils.Utils
import com.mapbox.navigation.navigator.MapboxNativeNavigatorImpl
import com.mapbox.navigation.route.onboard.MapboxOnboardRouter
import com.mapbox.navigation.utils.extensions.ifNonNull
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute
import com.mapbox.turf.TurfConstants
import com.mapbox.turf.TurfMeasurement
import java.io.File
import kotlinx.android.synthetic.main.activity_mock_navigation.*
import timber.log.Timber

class OnboardRouterActivityKt : AppCompatActivity(), OnMapReadyCallback,
    MapboxMap.OnMapClickListener {

    private lateinit var onboardRouter: Router
    private lateinit var mapboxMap: MapboxMap

    private var route: DirectionsRoute? = null
    private lateinit var navigationMapRoute: NavigationMapRoute
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
        val config = MapboxOnboardRouterConfig(
            fileTiles.absolutePath,
            null,
            null,
            null,
            null // working with pre-fetched tiles only
        )
        onboardRouter = MapboxOnboardRouter(MapboxNativeNavigatorImpl, config)
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
        ifNonNull(origin, destination) { origin, destination ->
            if (TurfMeasurement.distance(origin, destination, TurfConstants.UNIT_METERS) > 50) {

                val optionsBuilder =
                    RouteOptions.builder().applyDefaultParams()
                        .accessToken(Utils.getMapboxAccessToken(this))
                        .coordinates(origin, listOf(waypoint), destination)

                onboardRouter.getRoute(optionsBuilder.build(), object : Router.Callback {

                    override fun onResponse(routes: List<DirectionsRoute>) {
                        if (routes.isNotEmpty()) {
                            navigationMapRoute.addRoute(routes[0])
                        }
                    }

                    override fun onFailure(throwable: Throwable) {
                        Timber.e(throwable, "onRoutesRequestFailure: navigation.getRoute()")
                    }

                    override fun onCanceled() {
                        Timber.e("onRoutesRequestCanceled")
                    }
                })
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
        onboardRouter.cancel()
        mapboxMap.removeOnMapClickListener(this)
        mapView.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }
}
