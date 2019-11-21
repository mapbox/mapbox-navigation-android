package com.mapbox.services.android.navigation.testapp.activity

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
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
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.navigation.base.route.DirectionsSession
import com.mapbox.navigation.base.route.model.Route
import com.mapbox.navigation.directions.session.MapboxDirectionsSession
import com.mapbox.navigation.route.offboard.MapboxOffboardRouter
import com.mapbox.navigation.utils.extensions.ifNonNull
import com.mapbox.services.android.navigation.testapp.R
import com.mapbox.services.android.navigation.testapp.utils.Utils
import com.mapbox.services.android.navigation.testapp.activity.notification.CustomNavigationNotification
import com.mapbox.services.android.navigation.testapp.utils.bindView
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute
import com.mapbox.services.android.navigation.v5.instruction.Instruction
import com.mapbox.services.android.navigation.v5.location.replay.ReplayRouteLocationEngine
import com.mapbox.services.android.navigation.v5.milestone.Milestone
import com.mapbox.services.android.navigation.v5.milestone.MilestoneEventListener
import com.mapbox.services.android.navigation.v5.milestone.RouteMilestone
import com.mapbox.services.android.navigation.v5.milestone.Trigger
import com.mapbox.services.android.navigation.v5.milestone.TriggerProperty
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigationOptions
import com.mapbox.services.android.navigation.v5.navigation.NavigationEventListener
import com.mapbox.services.android.navigation.v5.navigation.RefreshCallback
import com.mapbox.services.android.navigation.v5.navigation.RefreshError
import com.mapbox.services.android.navigation.v5.navigation.RouteRefresh
import com.mapbox.services.android.navigation.v5.navigation.metrics.MapboxMetricsReporter
import com.mapbox.services.android.navigation.v5.navigation.metrics.MetricEvent
import com.mapbox.services.android.navigation.v5.navigation.metrics.MetricsObserver
import com.mapbox.services.android.navigation.v5.offroute.OffRouteListener
import com.mapbox.services.android.navigation.v5.routeprogress.ProgressChangeListener
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress
import com.mapbox.turf.TurfConstants
import com.mapbox.turf.TurfMeasurement
import timber.log.Timber
import java.lang.ref.WeakReference

class MockNavigationNewRoutingActivity : AppCompatActivity(),
    OnMapReadyCallback,
    MapboxMap.OnMapClickListener,
    ProgressChangeListener,
    NavigationEventListener,
    MilestoneEventListener,
    OffRouteListener,
    RefreshCallback,
    MetricsObserver,
    DirectionsSession.RouteObserver {

    companion object {
        private const val BEGIN_ROUTE_MILESTONE = 1001
        private const val TWENTY_FIVE_METERS = 25.0
    }

    // Map variables
    private val mapView by bindView<MapView>(R.id.mapView)
    private val newLocationFab by bindView<FloatingActionButton>(R.id.newLocationFab)
    private val startRouteButton by bindView<Button>(R.id.startRouteButton)

    private var mapboxMap: MapboxMap? = null

    // Navigation related variables
    private lateinit var locationEngine: LocationEngine
    private lateinit var navigation: MapboxNavigation
    private lateinit var routeRefresh: RouteRefresh
    private lateinit var navigationMapRoute: NavigationMapRoute
    private var directionsSession: DirectionsSession? = null
    private var route: DirectionsRoute? = null
    private var destination: Point? = null
    private var waypoint: Point? = null
    private var isRefreshing = false

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
        routeRefresh = RouteRefresh(Utils.getMapboxAccessToken(this))

        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        val context = applicationContext
        val customNotification = CustomNavigationNotification(context)
        val options = MapboxNavigationOptions.Builder()
            .navigationNotification(customNotification)
            .defaultMilestonesEnabled(false)
            .build()

        navigation = MapboxNavigation(
            this,
            Utils.getMapboxAccessToken(this),
            options
        )
        MapboxMetricsReporter.setMetricsObserver(this)

        navigation.addMilestone(
            RouteMilestone.Builder()
                .setIdentifier(BEGIN_ROUTE_MILESTONE)
                .setInstruction(BeginRouteInstruction())
                .setTrigger(
                    Trigger.all(
                        Trigger.lt(TriggerProperty.STEP_INDEX, 3),
                        Trigger.gt(TriggerProperty.STEP_DISTANCE_TOTAL_METERS, 200),
                        Trigger.gte(TriggerProperty.STEP_DISTANCE_TRAVELED_METERS, 75)
                    )
                ).build()
        )
        customNotification.register(MyBroadcastReceiver(navigation), context)

        startRouteButton.setOnClickListener { onStartRouteClick() }
        newLocationFab.setOnClickListener { onNewLocationClick() }
    }

    private fun newOrigin() {
        mapboxMap?.let { map ->
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
            destination == null -> destination = Point.fromLngLat(point.longitude, point.latitude)
            waypoint == null -> waypoint = Point.fromLngLat(point.longitude, point.latitude)
            else -> Toast.makeText(this, "Only 2 waypoints supported", Toast.LENGTH_LONG).show()
        }
        mapboxMap?.addMarker(MarkerOptions().position(point))
        calculateRoute()
        return false
    }

    private fun onStartRouteClick() {
        ifNonNull(route, locationEngine) { route, locationEngine ->
            val isValidRoute = route.distance()?.let { it > TWENTY_FIVE_METERS } ?: false
            if (isValidRoute) {
                // Hide the start button
                startRouteButton.visibility = View.INVISIBLE

                // Attach all of our navigation listeners.
                navigation.addNavigationEventListener(this)
                navigation.addProgressChangeListener(this)
                navigation.addMilestoneEventListener(this)
                navigation.addOffRouteListener(this)

                (locationEngine as ReplayRouteLocationEngine).assign(route)
                navigation.locationEngine = locationEngine
                mapboxMap?.let { it.locationComponent.isLocationComponentEnabled = true }
                navigation.startNavigation(route)
                mapboxMap?.removeOnMapClickListener(this)
            }
        }
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
                startRouteButton.visibility = View.GONE
                return
            }

            var waypoints: MutableList<Point>? = null
            waypoint?.let {
                waypoints = mutableListOf(it)
            }
            val offboardRouter = MapboxOffboardRouter(this, Utils.getMapboxAccessToken(this))
            directionsSession = MapboxDirectionsSession(
                offboardRouter,
                origin,
                waypoints,
                destinationPoint,
                this
            )
        }
    }

    /*
     * DirectionSessions.RouteObserver
     */

    override fun onRouteChanged(route: Route?) {
        if (route != null) {
            this.route = mapRouteToDirectionsRoute(route)
            navigationMapRoute.addRoute(this.route)
            startRouteButton.visibility = View.VISIBLE
        }
    }

    override fun onFailure(throwable: Throwable) {
        Timber.e(throwable, "onFailure: navigation.getRoute()")
    }

    /*
     * Navigation listeners
     */

    override fun onMilestoneEvent(routeProgress: RouteProgress, instruction: String, milestone: Milestone) {
        Timber.d("Milestone Event Occurred with id: %d", milestone.identifier)
        Timber.d("Voice instruction: %s", instruction)
    }

    override fun onRunning(running: Boolean) {
        Timber.d("onRunning: ${if (running) "Started" else "Stopped"}")
    }

    override fun userOffRoute(location: Location) {
        Toast.makeText(this, "off-route called", Toast.LENGTH_LONG).show()
    }

    override fun onProgressChange(location: Location, routeProgress: RouteProgress) {
        mapboxMap?.locationComponent?.forceLocationUpdate(location)
        if (!isRefreshing) {
            isRefreshing = true
            routeRefresh.refresh(routeProgress, this)
        }
        Timber.d("onProgressChange: fraction of route traveled: ${routeProgress.fractionTraveled()}")
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

    override fun onRefresh(directionsRoute: DirectionsRoute) {
        navigation.startNavigation(directionsRoute)
        isRefreshing = false
    }

    override fun onError(error: RefreshError) {
        isRefreshing = false
    }

    override fun onMetricUpdated(@MetricEvent.Metric metric: String, jsonStringData: String) {
        Timber.d("METRICS_LOG: $metric")
        Timber.d(jsonStringData)
    }

    private fun mapRouteToDirectionsRoute(route: Route): DirectionsRoute {
        val duration = route.duration?.toDouble() ?: 0.0
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

    private class BeginRouteInstruction : Instruction() {

        override fun buildInstruction(routeProgress: RouteProgress): String {
            return "Have a safe trip!"
        }
    }
}
