package com.mapbox.navigation.examples.core

import android.content.Context
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.snackbar.Snackbar.LENGTH_SHORT
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.base.common.logger.model.Message
import com.mapbox.base.common.logger.model.Tag
import com.mapbox.common.logger.DEBUG
import com.mapbox.common.logger.ERROR
import com.mapbox.common.logger.INFO
import com.mapbox.common.logger.LogEntry
import com.mapbox.common.logger.LoggerObserver
import com.mapbox.common.logger.MapboxLogger
import com.mapbox.common.logger.VERBOSE
import com.mapbox.common.logger.WARN
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.plugins.annotation.SymbolManager
import com.mapbox.mapboxsdk.plugins.annotation.SymbolOptions
import com.mapbox.navigation.base.internal.extensions.applyDefaultParams
import com.mapbox.navigation.base.internal.extensions.coordinates
import com.mapbox.navigation.base.metrics.MetricsObserver
import com.mapbox.navigation.base.options.MapboxOnboardRouterConfig
import com.mapbox.navigation.base.route.Router
import com.mapbox.navigation.base.route.internal.RouteUrl.Companion.PROFILE_DRIVING_TRAFFIC
import com.mapbox.navigation.core.internal.accounts.MapboxNavigationAccounts
import com.mapbox.navigation.examples.R
import com.mapbox.navigation.examples.utils.Utils
import com.mapbox.navigation.metrics.MapboxMetricsReporter
import com.mapbox.navigation.navigator.internal.MapboxNativeNavigatorImpl
import com.mapbox.navigation.route.hybrid.MapboxHybridRouter
import com.mapbox.navigation.route.offboard.MapboxOffboardRouter
import com.mapbox.navigation.route.onboard.MapboxOnboardRouter
import com.mapbox.navigation.ui.route.NavigationMapRoute
import com.mapbox.navigation.utils.internal.NetworkStatusService
import com.mapbox.navigation.utils.internal.ifNonNull
import com.mapbox.turf.TurfConstants
import com.mapbox.turf.TurfMeasurement
import java.io.File
import kotlinx.android.synthetic.main.activity_mock_navigation.mapView
import kotlinx.android.synthetic.main.activity_mock_navigation.newLocationFab
import timber.log.Timber

abstract class BaseRouterActivityKt :
    AppCompatActivity(),
    OnMapReadyCallback,
    MapboxMap.OnMapClickListener,
    MetricsObserver,
    LoggerObserver {

    private val router: Router by lazy { setupRouter() }
    private var origin: Point? = null
    private var destination: Point? = null
    private var waypoint: Point? = null
    private var mapboxMap: MapboxMap? = null
    private var navigationMapRoute: NavigationMapRoute? = null
    private var symbolManager: SymbolManager? = null

    abstract fun setupRouter(): Router

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mock_navigation)

        MapboxLogger.logLevel = VERBOSE
        MapboxLogger.setObserver(this)
        MapboxMetricsReporter.setMetricsObserver(this)

        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)
        newLocationFab?.setOnClickListener { newOrigin() }
    }

    private fun newOrigin() {
        clearMap()
        val latLng = Utils.getRandomLatLng(doubleArrayOf(-77.1825, 38.7825, -76.9790, 39.0157))
        origin = Point.fromLngLat(latLng.longitude, latLng.latitude)
        mapboxMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 12.0))
    }

    override fun onMapReady(mapboxMap: MapboxMap) {
        this.mapboxMap = mapboxMap
        MapboxLogger.d(Message("Map is ready"))
        mapboxMap.setStyle(Style.MAPBOX_STREETS) { style ->
            MapboxLogger.d(Message("Style setting finished"))
            style.addImage(MARKER_ROUTE, R.drawable.mapbox_marker_icon_default)
            navigationMapRoute = NavigationMapRoute(mapView, mapboxMap)
            symbolManager = SymbolManager(mapView, mapboxMap, style)
            Snackbar.make(findViewById(R.id.container), R.string.msg_tap_map_to_place_waypoint,
                LENGTH_SHORT).show()
            newOrigin()
            mapboxMap.addOnMapClickListener(this)
        }
    }

    private fun clearMap() {
        symbolManager?.deleteAll()
        destination = null
        waypoint = null
        navigationMapRoute?.run {
            updateRouteVisibilityTo(false)
            updateRouteArrowVisibilityTo(false)
        }
    }

    private fun findRoute() {
        ifNonNull(origin, destination) { origin, destination ->
            router.cancel()

            if (TurfMeasurement.distance(origin, destination, TurfConstants.UNIT_METERS) > 50) {

                val optionsBuilder =
                    RouteOptions.builder().applyDefaultParams()
                        .accessToken(Utils.getMapboxAccessToken(this))
                        .coordinates(origin, listOf(waypoint), destination)
                        .profile(PROFILE_DRIVING_TRAFFIC)
                        .annotations(
                            "${DirectionsCriteria.ANNOTATION_CONGESTION},${DirectionsCriteria.ANNOTATION_DISTANCE},${DirectionsCriteria.ANNOTATION_DURATION}"
                        )

                router.getRoute(optionsBuilder.build(), object : Router.Callback {
                    override fun onResponse(routes: List<DirectionsRoute>) {
                        MapboxLogger.d(Message("Router.Callback#onResponse"))
                        if (routes.isNotEmpty()) {
                            navigationMapRoute?.addRoute(routes[0])
                        }
                    }

                    override fun onFailure(throwable: Throwable) {
                        MapboxLogger.e(Message("Router.Callback#onFailure"), throwable)
                    }

                    override fun onCanceled() {
                        MapboxLogger.d(Message("Router.Callback#onCanceled"))
                    }
                })
            }
        }
    }

    override fun onMapClick(point: LatLng): Boolean {
        when {
            destination == null -> {
                destination = Point.fromLngLat(point.longitude, point.latitude).also {
                    addMarker(it)
                }
                findRoute()
            }
            waypoint == null -> {
                waypoint = Point.fromLngLat(point.longitude, point.latitude).also {
                    addMarker(it)
                }
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

    private fun addMarker(point: Point) {
        symbolManager?.create(
            SymbolOptions()
                .withIconImage(MARKER_ROUTE)
                .withGeometry(point)
        )
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
        router.shutdown()
        mapboxMap?.removeOnMapClickListener(this)
        mapView.onDestroy()
        MapboxLogger.removeObserver()
        MapboxMetricsReporter.removeObserver()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }

    private fun Style.addImage(imageName: String, @DrawableRes drawableRes: Int) {
        ContextCompat.getDrawable(this@BaseRouterActivityKt, drawableRes)?.let {
            addImage(imageName, it)
        }
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

    companion object {
        private const val MARKER_ROUTE = "marker.route"

        fun setupOffboardRouter(context: Context): Router {
            return MapboxOffboardRouter(
                Utils.getMapboxAccessToken(context),
                context,
                MapboxNavigationAccounts.getInstance(context)
            )
        }

        fun setupOnboardRouter(): Router {
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
            return MapboxOnboardRouter(MapboxNativeNavigatorImpl, config, MapboxLogger)
        }

        fun setupHybridRouter(applicationContext: Context): Router {
            return MapboxHybridRouter(
                setupOnboardRouter(),
                setupOffboardRouter(applicationContext),
                NetworkStatusService(applicationContext)
            )
        }
    }
}
