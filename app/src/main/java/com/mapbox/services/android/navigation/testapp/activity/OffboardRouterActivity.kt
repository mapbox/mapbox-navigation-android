package com.mapbox.services.android.navigation.testapp.activity

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineCallback
import com.mapbox.android.core.location.LocationEngineResult
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.annotations.MarkerOptions
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.navigation.base.route.DirectionsSession
import com.mapbox.navigation.base.route.model.Route
import com.mapbox.navigation.directions.session.MapboxDirectionsSession
import com.mapbox.navigation.route.offboard.MapboxOffboardRouter
import com.mapbox.services.android.navigation.testapp.R
import com.mapbox.services.android.navigation.testapp.activity.notification.CustomNavigationNotification
import com.mapbox.services.android.navigation.testapp.utils.Utils
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute
import com.mapbox.services.android.navigation.v5.location.replay.ReplayRouteLocationEngine
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigationOptions
import com.mapbox.services.android.navigation.v5.navigation.metrics.MetricEvent
import com.mapbox.services.android.navigation.v5.navigation.metrics.MetricsObserver
import com.mapbox.turf.TurfConstants
import com.mapbox.turf.TurfMeasurement
import kotlinx.android.synthetic.main.activity_mock_navigation.*
import timber.log.Timber

class OffboardRouterActivity : AppCompatActivity(),
    OnMapReadyCallback,
    MapboxMap.OnMapClickListener,
    MetricsObserver,
    DirectionsSession.RouteObserver
{
    private var mapboxMap: MapboxMap? = null

    // Navigation related variables
    private lateinit var locationEngine: LocationEngine
    private lateinit var navigation: MapboxNavigation
    private lateinit var navigationMapRoute: NavigationMapRoute
    private var directionsSession: DirectionsSession? = null
    private var route: DirectionsRoute? = null
    private var destination: Point? = null
    private var waypoint: Point? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mock_navigation)

        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        val customNotification = CustomNavigationNotification(applicationContext)
        val options = MapboxNavigationOptions.Builder()
            .navigationNotification(customNotification)
            .defaultMilestonesEnabled(false)
            .build()

        navigation = MapboxNavigation(
            this,
            Utils.getMapboxAccessToken(this),
            options
        )

        newLocationFab.setOnClickListener { onNewLocationClick() }
    }

    private fun newOrigin() {
        mapboxMap?.let { map ->
            clearMap(map)
            val latLng = Utils.getRandomLatLng(doubleArrayOf(-77.1825, 38.7825, -76.9790, 39.0157))
            (locationEngine as ReplayRouteLocationEngine).assignLastLocation(
                Point.fromLngLat(latLng.longitude, latLng.latitude)
            )
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 12.0))
        }
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(mapboxMap: MapboxMap) {
        this.mapboxMap = mapboxMap
        this.mapboxMap?.addOnMapClickListener(this)
        mapboxMap.setStyle(Style.MAPBOX_STREETS) { style ->
            val locationComponent = mapboxMap.locationComponent
            val options = LocationComponentActivationOptions.builder(this, style).build()
            locationComponent.activateLocationComponent(options)
            locationComponent.renderMode = RenderMode.GPS
            locationComponent.isLocationComponentEnabled = false
            navigationMapRoute = NavigationMapRoute(navigation, mapView, mapboxMap)
            Snackbar.make(findViewById(R.id.container), "Tap map to place waypoint", Snackbar.LENGTH_LONG).show()
            locationEngine = ReplayRouteLocationEngine()
            newOrigin()
        }
    }

    override fun onMapClick(point: LatLng): Boolean {
        when {
            destination == null -> {
                destination = Point.fromLngLat(point.longitude, point.latitude)
                mapboxMap?.addMarker(MarkerOptions().position(point))
                calculateRoute()
            }
            waypoint == null -> {
                waypoint = Point.fromLngLat(point.longitude, point.latitude)
                mapboxMap?.addMarker(MarkerOptions().position(point))
                calculateRoute()
            }
            else -> {
                Toast.makeText(this, "Only 2 waypoints supported", Toast.LENGTH_LONG).show()
                mapboxMap?.let { clearMap(it) }
            }
        }
        return false
    }

    private fun clearMap(map: MapboxMap) {
        map.clear()
        route = null
        destination = null
        waypoint = null
        navigationMapRoute.updateRouteVisibilityTo(false)
        navigationMapRoute.updateRouteArrowVisibilityTo(false)
    }

    private fun onNewLocationClick() {
        newOrigin()
    }

    @SuppressLint("MissingPermission")
    private fun calculateRoute() {
        locationEngine.getLastLocation(object : LocationEngineCallback<LocationEngineResult> {
            override fun onSuccess(result: LocationEngineResult) {
                findRouteWith(result)
            }

            override fun onFailure(exception: Exception) {
                Timber.e(exception)
            }
        })
    }

    private fun findRouteWith(result: LocationEngineResult) {
        val userLocation = result.lastLocation
        if (userLocation == null) {
            Timber.d("calculateRoute: User location is null, therefore, origin can't be set.")
            return
        }
        val origin = Point.fromLngLat(userLocation.longitude, userLocation.latitude)
        destination?.let { destinationPoint ->
            if (TurfMeasurement.distance(origin, destinationPoint, TurfConstants.UNIT_METERS) < 50) {
                return
            }
            val waypoints = mutableListOf(waypoint).filterNotNull()
            val offboardRouter = MapboxOffboardRouter(this, Utils.getMapboxAccessToken(this))
            directionsSession = MapboxDirectionsSession(
                offboardRouter,
                origin,
                waypoints,
                destinationPoint,
                this
            )
            directionsSession?.requestRoutes()
        }
    }

    /*
     * DirectionSessions.RouteObserver
     */

    override fun onRoutesChanged(routes: List<Route>) {
        routes.firstOrNull()?.let {
            this.route = mapRouteToDirectionsRoute(it)
            navigationMapRoute.addRoute(this.route)
        }
    }

    override fun onRoutesRequested() {
        Timber.d("onRoutesRequested: navigation.getRoute()")
    }

    override fun onRoutesRequestFailure(throwable: Throwable) {
        Timber.e(throwable, "onRoutesRequestFailure: navigation.getRoute()")
    }

    /*
     * Activity lifecycle methods
     */

    public override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    public override fun onPause() {
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
        directionsSession?.cancel()
        navigation.onDestroy()
        mapboxMap?.removeOnMapClickListener(this)
        mapView.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }

    override fun onMetricUpdated(@MetricEvent.Metric metric: String, jsonStringData: String) {
        Timber.d("METRICS_LOG: $metric")
        Timber.d(jsonStringData)
    }

    private fun mapRouteToDirectionsRoute(route: Route): DirectionsRoute {
        val duration = route.duration.toDouble()
        val legs = route.legs?.legs?.let { it as List<RouteLeg> }

        return DirectionsRoute.builder()
            .distance(route.distance)
            .duration(duration)
            .geometry(route.geometry)
            .weight(route.weight)
            .weightName(route.weightName)
            .voiceLanguage(route.voiceLanguage)
            .legs(legs)
            .build()
    }
}
