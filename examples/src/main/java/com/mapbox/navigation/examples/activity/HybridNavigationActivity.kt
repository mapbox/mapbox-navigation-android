package com.mapbox.navigation.examples.activity

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import butterknife.ButterKnife
import butterknife.OnClick
import com.google.android.material.snackbar.Snackbar
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineCallback
import com.mapbox.android.core.location.LocationEngineResult
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.annotations.MarkerOptions
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.MapboxMap.OnMapClickListener
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.navigation.base.extensions.applyDefaultParams
import com.mapbox.navigation.base.extensions.coordinates
import com.mapbox.navigation.base.extensions.ifNonNull
import com.mapbox.navigation.base.logger.model.Message
import com.mapbox.navigation.base.logger.model.Tag
import com.mapbox.navigation.base.options.MapboxOnboardRouterConfig
import com.mapbox.navigation.base.route.Router
import com.mapbox.navigation.examples.R
import com.mapbox.navigation.examples.activity.notification.CustomNavigationNotification
import com.mapbox.navigation.examples.utils.Utils
import com.mapbox.navigation.logger.DEBUG
import com.mapbox.navigation.logger.ERROR
import com.mapbox.navigation.logger.INFO
import com.mapbox.navigation.logger.LogEntry
import com.mapbox.navigation.logger.LoggerObserver
import com.mapbox.navigation.logger.MapboxLogger
import com.mapbox.navigation.logger.MapboxLogger.d
import com.mapbox.navigation.logger.MapboxLogger.e
import com.mapbox.navigation.logger.MapboxLogger.logLevel
import com.mapbox.navigation.logger.MapboxLogger.setObserver
import com.mapbox.navigation.logger.VERBOSE
import com.mapbox.navigation.logger.WARN
import com.mapbox.navigation.route.hybrid.MapboxHybridRouter
import com.mapbox.navigation.route.offboard.MapboxOffboardRouter
import com.mapbox.navigation.route.onboard.MapboxOnboardRouter
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute
import com.mapbox.services.android.navigation.v5.instruction.Instruction
import com.mapbox.services.android.navigation.v5.location.replay.ReplayRouteLocationEngine
import com.mapbox.services.android.navigation.v5.milestone.Milestone
import com.mapbox.services.android.navigation.v5.milestone.MilestoneEventListener
import com.mapbox.services.android.navigation.v5.milestone.RouteMilestone
import com.mapbox.services.android.navigation.v5.milestone.Trigger.all
import com.mapbox.services.android.navigation.v5.milestone.Trigger.gt
import com.mapbox.services.android.navigation.v5.milestone.Trigger.gte
import com.mapbox.services.android.navigation.v5.milestone.Trigger.lt
import com.mapbox.services.android.navigation.v5.milestone.TriggerProperty
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigationOptions
import com.mapbox.services.android.navigation.v5.navigation.NavigationEventListener
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute.Companion.builder
import com.mapbox.services.android.navigation.v5.navigation.RefreshCallback
import com.mapbox.services.android.navigation.v5.navigation.RefreshError
import com.mapbox.services.android.navigation.v5.navigation.RouteRefresh
import com.mapbox.services.android.navigation.v5.navigation.metrics.MapboxMetricsReporter
import com.mapbox.services.android.navigation.v5.navigation.metrics.MapboxMetricsReporter.setMetricsObserver
import com.mapbox.services.android.navigation.v5.navigation.metrics.MetricEvent
import com.mapbox.services.android.navigation.v5.navigation.metrics.MetricsObserver
import com.mapbox.services.android.navigation.v5.offroute.OffRouteListener
import com.mapbox.services.android.navigation.v5.routeprogress.ProgressChangeListener
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress
import com.mapbox.turf.TurfConstants
import com.mapbox.turf.TurfMeasurement
import java.io.File
import java.lang.ref.WeakReference
import timber.log.Timber

class HybridNavigationActivity : AppCompatActivity(), OnMapReadyCallback, OnMapClickListener, ProgressChangeListener, NavigationEventListener, MilestoneEventListener, OffRouteListener, RefreshCallback, MetricsObserver, LoggerObserver {
    // Map variables
    var mapView: MapView? = null
    var startRouteButton: Button? = null
    private var mapboxMap: MapboxMap? = null
    // Navigation related variables
    private var locationEngine: LocationEngine? = null
    private var navigation: MapboxNavigation? = null
    private var route: DirectionsRoute? = null
    private var navigationMapRoute: NavigationMapRoute? = null
    private var destination: Point? = null
    private var waypoint: Point? = null
    private var routeRefresh: RouteRefresh? = null
    private var isRefreshing = false
    private val token = Mapbox.getAccessToken()!!
    private lateinit var router: Router

    private class MyBroadcastReceiver internal constructor(navigation: MapboxNavigation) : BroadcastReceiver() {
        private val weakNavigation: WeakReference<MapboxNavigation> = WeakReference(navigation)
        override fun onReceive(context: Context, intent: Intent) {
            val navigation = weakNavigation.get()
            navigation?.stopNavigation()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mock_navigation)
        ButterKnife.bind(this)
        logLevel = VERBOSE
        setObserver(this)
        setMetricsObserver(this)
        Mapbox.getAccessToken()?.let { token ->
            routeRefresh = RouteRefresh(token, applicationContext)
        }
        mapView = findViewById(R.id.mapView)
        ifNonNull(mapView) { view ->
            view.onCreate(savedInstanceState)
            view.getMapAsync(this)
            mapView = view
        }
        val context = applicationContext
        val customNotification = CustomNavigationNotification(context)
        router = setupRouter()
        val options = MapboxNavigationOptions.Builder()
                .navigationNotification(customNotification)
                .build()
        Mapbox.getAccessToken()?.let { token ->
            navigation = MapboxNavigation(
                    this,
                    token,
                    options
            )
        }
        navigation?.let { mapboxNavigation ->
            mapboxNavigation.addMilestone(RouteMilestone.Builder()
                    .setIdentifier(BEGIN_ROUTE_MILESTONE)
                    .setInstruction(BeginRouteInstruction())
                    .setTrigger(
                            all(
                                    lt(TriggerProperty.STEP_INDEX, 3),
                                    gt(TriggerProperty.STEP_DISTANCE_TOTAL_METERS, 200),
                                    gte(TriggerProperty.STEP_DISTANCE_TRAVELED_METERS, 75)
                            )
                    ).build())
            customNotification.register(MyBroadcastReceiver(mapboxNavigation), context)
        }
    }

    fun onStartRouteClick() {
        ifNonNull(navigation, route, navigation, locationEngine, mapboxMap) { _, route, navigation, locationEngine, mapboxMap ->
            // Hide the start button
            startRouteButton?.visibility = View.INVISIBLE
            // Attach all of our navigation listeners.
            navigation.addNavigationEventListener(this)
            navigation.addProgressChangeListener(this)
            navigation.addMilestoneEventListener(this)
            navigation.addOffRouteListener(this)
            (locationEngine as ReplayRouteLocationEngine).assign(route)
            navigation.locationEngine = locationEngine
            mapboxMap.locationComponent.isLocationComponentEnabled = true
            navigation.startNavigation(route)
            mapboxMap.removeOnMapClickListener(this)
        }
    }

    @OnClick(R.id.newLocationFab)
    fun onNewLocationClick() {
        newOrigin()
    }

    private fun setupRouter(): MapboxHybridRouter {
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
        val onboardRouter = MapboxOnboardRouter(config, null)
        val offBoardRouter = MapboxOffboardRouter(token, this)
        return MapboxHybridRouter(onboardRouter, offBoardRouter, applicationContext)
    }

    private fun newOrigin() {
        ifNonNull(mapboxMap) { mapboxMap ->
            val latLng = Utils.getRandomLatLng(doubleArrayOf(-77.1825, 38.7825, -76.9790, 39.0157))
            (locationEngine as ReplayRouteLocationEngine).assignLastLocation(
                    Point.fromLngLat(latLng.longitude, latLng.latitude)
            )
            mapboxMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 12.0))
        }
    }

    private fun setupStartButton() {
        findViewById<Button>(R.id.startRouteButton)?.let { button ->
            startRouteButton = button
            button.setOnClickListener {
                onStartRouteClick()
            }
        }
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(mapboxMap: MapboxMap) {
        this.mapboxMap = mapboxMap
        ifNonNull(mapboxMap, mapView) { map, view ->
            map.addOnMapClickListener(this)
            map.setStyle(Style.MAPBOX_STREETS) { style ->
                setupStartButton()
                val locationComponent = map.locationComponent
                locationComponent.activateLocationComponent(this, style)
                locationComponent.renderMode = RenderMode.GPS
                locationComponent.isLocationComponentEnabled = false
                navigationMapRoute = NavigationMapRoute(navigation, view, map)
                Snackbar.make(findViewById(R.id.container), "Tap map to place waypoint", Snackbar.LENGTH_LONG).show()
                locationEngine = ReplayRouteLocationEngine()
                newOrigin()
            }
        }
    }

    override fun onMapClick(point: LatLng): Boolean {
        if (destination == null) {
            destination = Point.fromLngLat(point.longitude, point.latitude)
        } else if (waypoint == null) {
            waypoint = Point.fromLngLat(point.longitude, point.latitude)
        } else {
            Toast.makeText(this, "Only 2 waypoints supported", Toast.LENGTH_LONG).show()
        }
        ifNonNull(mapboxMap) { map ->
            map.addMarker(MarkerOptions().position(point))
            calculateRoute()
        }
        return false
    }

    @SuppressLint("MissingPermission")
    private fun calculateRoute() {
        ifNonNull(locationEngine) { locationEngine ->
            locationEngine.getLastLocation(object : LocationEngineCallback<LocationEngineResult> {
                override fun onSuccess(result: LocationEngineResult) {
                    findRouteWith(result)
                }

                override fun onFailure(exception: Exception) {
                    e(Message(exception.localizedMessage), exception)
                }
            })
        }
    }

    private fun findRouteWith(result: LocationEngineResult) {
        val userLocation = result.lastLocation
        if (userLocation == null) {
            d(Message("calculateRoute: User location is null, therefore, origin can't be set."))
            return
        }
        val origin = Point.fromLngLat(userLocation.longitude, userLocation.latitude)
        ifNonNull(destination, startRouteButton) { destination, startButton ->
            if (TurfMeasurement.distance(origin, destination, TurfConstants.UNIT_METERS) < 50) {
                startButton.visibility = View.GONE
                return
            }
        }
        ifNonNull(Mapbox.getAccessToken(), destination) { token, destination ->
            val navigationRouteBuilder = builder(this)
                    .accessToken(token)
            navigationRouteBuilder.origin(origin)
            navigationRouteBuilder.destination(destination)
            ifNonNull(waypoint) { waypoint ->
                navigationRouteBuilder.addWaypoint(waypoint)
            }

            val optionsBuilder =
                    RouteOptions.builder().applyDefaultParams()
                            .accessToken(token)
                            .coordinates(origin, listOf(waypoint), destination)
            router.getRoute(optionsBuilder.build(), object : Router.Callback {
                override fun onResponse(routes: List<DirectionsRoute>) {
                    if (routes.isNotEmpty()) {
                        navigationMapRoute?.addRoute(routes[0])
                        route = routes[0]
                        startRouteButton?.visibility = View.VISIBLE
                    }
                }

                override fun onFailure(throwable: Throwable) {
                    Timber.e(throwable, "onRoutesRequestFailure: navigation.getRoute()")
                }
            })
        }
    }

    /*
   * Navigation listeners
   */
    override fun onMilestoneEvent(
        routeProgress: RouteProgress,
        instruction: String,
        milestone: Milestone
    ) {
        d(Message("Milestone Event Occurred with id: " + milestone.identifier))
        d(Message("Voice instruction: $instruction"))
    }

    override fun onRunning(running: Boolean) {
        if (running) {
            d(Message("onRunning: Started"))
        } else {
            d(Message("onRunning: Stopped"))
        }
    }

    override fun userOffRoute(location: Location) {
        Toast.makeText(this, "off-route called", Toast.LENGTH_LONG).show()
    }

    override fun onProgressChange(location: Location, routeProgress: RouteProgress) {
        mapboxMap?.locationComponent?.forceLocationUpdate(location)
        if (!isRefreshing) {
            isRefreshing = true
            routeRefresh?.refresh(routeProgress, this)
        }
        d(Message("onProgressChange: fraction of route traveled: " + routeProgress.fractionTraveled()))
    }

    /*
   * Activity lifecycle methods
   */
    public override fun onResume() {
        super.onResume()
        mapView?.onResume()
    }

    public override fun onPause() {
        super.onPause()
        mapView?.onPause()
    }

    override fun onStart() {
        super.onStart()
        mapView?.onStart()
    }

    override fun onStop() {
        super.onStop()
        mapView?.onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView?.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        MapboxMetricsReporter.removeObserver()
        MapboxLogger.removeObserver()
        navigation?.onDestroy()
        if (mapboxMap != null) {
            mapboxMap?.removeOnMapClickListener(this)
        }
        mapView?.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView?.onSaveInstanceState(outState)
    }

    override fun onRefresh(directionsRoute: DirectionsRoute) {
        navigation?.startNavigation(directionsRoute)
        isRefreshing = false
    }

    override fun onError(error: RefreshError) {
        isRefreshing = false
    }

    override fun onMetricUpdated(@MetricEvent.Metric metricName: String, jsonStringData: String) {
        d(Tag("METRICS_LOG"), Message(metricName))
        d(Tag("METRICS_LOG"), Message(jsonStringData))
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

    private class BeginRouteInstruction : Instruction() {
        override fun buildInstruction(routeProgress: RouteProgress): String {
            return "Have a safe trip!"
        }
    }

    companion object {
        private const val BEGIN_ROUTE_MILESTONE = 1001
        private const val TWENTY_FIVE_METERS = 25.0
    }
}
