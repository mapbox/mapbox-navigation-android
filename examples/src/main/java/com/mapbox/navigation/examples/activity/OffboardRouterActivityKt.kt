package com.mapbox.navigation.examples.activity

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.mapbox.api.directions.v5.DirectionsCriteria
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
import com.mapbox.navigation.base.logger.model.Message
import com.mapbox.navigation.base.logger.model.Tag
import com.mapbox.navigation.base.route.Router
import com.mapbox.navigation.core.accounts.MapboxNavigationAccounts
import com.mapbox.navigation.examples.R
import com.mapbox.navigation.examples.utils.Utils
import com.mapbox.navigation.logger.DEBUG
import com.mapbox.navigation.logger.ERROR
import com.mapbox.navigation.logger.INFO
import com.mapbox.navigation.logger.LogEntry
import com.mapbox.navigation.logger.LoggerObserver
import com.mapbox.navigation.logger.MapboxLogger
import com.mapbox.navigation.logger.VERBOSE
import com.mapbox.navigation.logger.WARN
import com.mapbox.navigation.route.offboard.MapboxOffboardRouter
import com.mapbox.navigation.utils.extensions.ifNonNull
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute
import com.mapbox.services.android.navigation.v5.navigation.metrics.MapboxMetricsReporter
import com.mapbox.services.android.navigation.v5.navigation.metrics.MetricsObserver
import com.mapbox.turf.TurfConstants
import com.mapbox.turf.TurfMeasurement
import kotlinx.android.synthetic.main.activity_mock_navigation.*
import timber.log.Timber

class OffboardRouterActivityKt : AppCompatActivity(),
    OnMapReadyCallback,
    MapboxMap.OnMapClickListener,
    Router.Callback,
    MetricsObserver,
    LoggerObserver {

    private var mapboxMap: MapboxMap? = null

    private lateinit var navigationMapRoute: NavigationMapRoute
    private var route: DirectionsRoute? = null
    private var origin: Point? = null
    private var destination: Point? = null
    private var waypoint: Point? = null

    private var offboardRouter: MapboxOffboardRouter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mock_navigation)

        MapboxLogger.logLevel = VERBOSE
        MapboxLogger.setObserver(this)
        MapboxMetricsReporter.setMetricsObserver(this)

        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        newLocationFab.setOnClickListener { onNewLocationClick() }
    }

    private fun newOrigin() {
        mapboxMap?.let { map ->
            clearMap()
            val latLng = Utils.getRandomLatLng(doubleArrayOf(-77.1825, 38.7825, -76.9790, 39.0157))
            origin = Point.fromLngLat(latLng.longitude, latLng.latitude)
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 12.0))
        }
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(mapboxMap: MapboxMap) {
        this.mapboxMap = mapboxMap
        this.mapboxMap?.addOnMapClickListener(this)
        MapboxLogger.d(Message("Map is ready"))
        mapboxMap.setStyle(Style.MAPBOX_STREETS) {
            MapboxLogger.d(Message("Style setting finished"))
            navigationMapRoute = NavigationMapRoute(mapView, mapboxMap)
            Snackbar.make(
                findViewById(R.id.container),
                "Tap map to place waypoint",
                Snackbar.LENGTH_LONG
            ).show()
            newOrigin()
        }
    }

    override fun onMapClick(point: LatLng): Boolean {
        when {
            destination == null -> {
                destination = Point.fromLngLat(point.longitude, point.latitude)
                mapboxMap?.addMarker(MarkerOptions().position(point))
                findRoute()
            }
            waypoint == null -> {
                waypoint = Point.fromLngLat(point.longitude, point.latitude)
                mapboxMap?.addMarker(MarkerOptions().position(point))
                findRoute()
            }
            else -> {
                Toast.makeText(
                    this,
                    "Only 2 waypoints supported for this example",
                    Toast.LENGTH_LONG
                ).show()
                clearMap()
            }
        }
        return false
    }

    private fun clearMap() {
        mapboxMap?.let { map ->
            map.clear()
            route = null
            destination = null
            waypoint = null
            navigationMapRoute.updateRouteVisibilityTo(false)
            navigationMapRoute.updateRouteArrowVisibilityTo(false)
        }
    }

    private fun onNewLocationClick() {
        newOrigin()
    }

    private fun findRoute() {
        ifNonNull(origin, destination) { originPoint, destinationPoint ->
            if (offboardRouter == null) {
                offboardRouter = MapboxOffboardRouter(
                    Utils.getMapboxAccessToken(this),
                    this,
                    MapboxNavigationAccounts.getInstance(this)
                )
            } else {
                offboardRouter?.cancel()
            }

            if (TurfMeasurement.distance(
                    originPoint,
                    destinationPoint,
                    TurfConstants.UNIT_METERS
                ) < 50
            ) {
                return
            }
            val waypoints = mutableListOf(waypoint).filterNotNull()
            val options = RouteOptions.builder().applyDefaultParams().apply {
                accessToken(Utils.getMapboxAccessToken(this@OffboardRouterActivityKt))
                coordinates(originPoint, waypoints, destinationPoint)
                profile(DirectionsCriteria.PROFILE_DRIVING_TRAFFIC)
                annotations(
                    "${DirectionsCriteria.ANNOTATION_CONGESTION},${DirectionsCriteria.ANNOTATION_DISTANCE},${DirectionsCriteria.ANNOTATION_DURATION}"
                )
            }.build()

            offboardRouter?.getRoute(options, this@OffboardRouterActivityKt)
        }
    }

    /*
     * Router.Callback
     */

    override fun onResponse(routes: List<DirectionsRoute>) {
        routes.firstOrNull()?.let {
            route = it
            navigationMapRoute.addRoute(route)
        }
    }

    override fun onFailure(throwable: Throwable) {
        Toast.makeText(this, "Error: " + throwable.message, Toast.LENGTH_LONG).show()
        MapboxLogger.e(Message("Router.Callback#onFailure"), throwable)
    }

    override fun onCanceled() {
        Toast.makeText(this, "Canceled", Toast.LENGTH_LONG).show()
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
        MapboxMetricsReporter.removeObserver()
        MapboxLogger.removeObserver()
        offboardRouter?.cancel()
        mapboxMap?.removeOnMapClickListener(this)
        mapView.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }

    override fun onMetricUpdated(metricName: String, jsonStringData: String) {
        MapboxLogger.d(Tag("METRICS_LOG"), Message(metricName))
        MapboxLogger.d(Tag("METRICS_LOG"), Message(jsonStringData))
    }

    override fun log(level: Int, entry: LogEntry) {
        if (entry.tag != null) {
            Timber.tag(entry.tag)
        }
        when (level) {
            VERBOSE -> Timber.v(entry.throwable, entry.message)
            DEBUG -> Timber.d(entry.throwable, entry.message)
            INFO -> Timber.i(entry.throwable, entry.message)
            WARN -> Timber.w(entry.throwable, entry.message)
            ERROR -> Timber.e(entry.throwable, entry.message)
            else -> {
            }
        }
    }
}
