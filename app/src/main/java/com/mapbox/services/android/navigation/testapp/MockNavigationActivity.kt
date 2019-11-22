package com.mapbox.services.android.navigation.testapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.annotations.MarkerOptions
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.LocationComponent
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.services.android.navigation.v5.instruction.Instruction
import com.mapbox.services.android.navigation.v5.location.replay.ReplayRouteLocationEngine
import com.mapbox.services.android.navigation.v5.milestone.*
import com.mapbox.services.android.navigation.v5.navigation.*
import com.mapbox.services.android.navigation.v5.offroute.OffRouteListener
import com.mapbox.services.android.navigation.v5.routeprogress.ProgressChangeListener
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress
import com.mapbox.turf.TurfConstants
import com.mapbox.turf.TurfMeasurement
import kotlinx.android.synthetic.main.activity_mock_navigation.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import timber.log.Timber
import java.lang.ref.WeakReference

class MockNavigationActivity : AppCompatActivity(), OnMapReadyCallback,
        MapboxMap.OnMapClickListener, ProgressChangeListener, NavigationEventListener,
        MilestoneEventListener, OffRouteListener {
    private val BEGIN_ROUTE_MILESTONE = 1001
    private lateinit var mapboxMap: MapboxMap

    // Navigation related variables
    private var locationEngine: ReplayRouteLocationEngine = ReplayRouteLocationEngine()
    private lateinit var navigation: MapboxNavigation
    private var route: DirectionsRoute? = null
    private var navigationMapRoute: NavigationMapRoute? = null
    private var destination: Point? = null
    private var waypoint: Point? = null
    private var locationComponent: LocationComponent? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mock_navigation)
        mapView.apply {
            onCreate(savedInstanceState)
            getMapAsync(this@MockNavigationActivity)
        }

        val context = applicationContext
        val customNotification = CustomNavigationNotification(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            customNotification.createNotificationChannel(this)
        }
        val options = MapboxNavigationOptions.builder()
                .navigationNotification(customNotification)
                .build()

        Mapbox.getAccessToken()?.let {
            navigation = MapboxNavigation(this, it, options)

            navigation.addMilestone(RouteMilestone.Builder()
                    .setIdentifier(BEGIN_ROUTE_MILESTONE)
                    .setInstruction(BeginRouteInstruction())
                    .setTrigger(
                            Trigger.all(
                                    Trigger.lt(TriggerProperty.STEP_INDEX, 3),
                                    Trigger.gt(TriggerProperty.STEP_DISTANCE_TOTAL_METERS, 200),
                                    Trigger.gte(TriggerProperty.STEP_DISTANCE_TRAVELED_METERS, 75)
                            )
                    ).build())
            customNotification.register(MyBroadcastReceiver(navigation), context)
        }

        startRouteButton.setOnClickListener {
            route?.let { route ->
                startRouteButton.visibility = View.INVISIBLE

                // Attach all of our navigation listeners.
                navigation.apply {
                    addNavigationEventListener(this@MockNavigationActivity)
                    addProgressChangeListener(this@MockNavigationActivity)
                    addMilestoneEventListener(this@MockNavigationActivity)
                    addOffRouteListener(this@MockNavigationActivity)
                }

                locationEngine.also {
                    it.assign(route)
                    navigation.locationEngine = it
                    navigation.startNavigation(route)
                    if (::mapboxMap.isInitialized)
                        mapboxMap.removeOnMapClickListener(this)
                }
            }
        }

        newLocationFab.setOnClickListener {
            newOrigin()
        }

        clearPoints.setOnClickListener {
            if (::mapboxMap.isInitialized)
                mapboxMap.markers.forEach {
                    mapboxMap.removeMarker(it)
                }
            destination = null
            waypoint = null
            it.visibility = View.GONE

            navigationMapRoute?.removeRoute()
        }
    }

    override fun onMapReady(mapboxMap: MapboxMap) {
        this.mapboxMap = mapboxMap
        mapboxMap.setStyle(Style.Builder().fromUri(STYLE_URL)) { style ->
            enableLocationComponent(style)
        }

        navigationMapRoute = NavigationMapRoute(navigation, mapView, mapboxMap)

        mapboxMap.addOnMapClickListener(this)
        Snackbar.make(findViewById(R.id.container), "Tap map to place waypoint", BaseTransientBottomBar.LENGTH_LONG).show()

        newOrigin()

    }

    @SuppressWarnings("MissingPermission")
    private fun enableLocationComponent(style: Style) {
        // Get an instance of the component
        locationComponent = mapboxMap.locationComponent

        locationComponent?.let {
            // Activate with a built LocationComponentActivationOptions object
            it.activateLocationComponent(LocationComponentActivationOptions.builder(this, style).build())

            // Enable to make component visible
            it.isLocationComponentEnabled = true

            // Set the component's camera mode
            it.cameraMode = CameraMode.TRACKING

            // Set the component's render mode
            it.renderMode = RenderMode.GPS

            it.locationEngine = locationEngine
        }
    }

    override fun onMapClick(point: LatLng): Boolean {
        var addMarker = true
        when {
            destination == null -> destination = Point.fromLngLat(point.longitude, point.latitude)
            waypoint == null -> waypoint = Point.fromLngLat(point.longitude, point.latitude)
            else -> {
                Toast.makeText(this, "Only 2 waypoints supported", Toast.LENGTH_LONG).show()
                addMarker = false
            }
        }

        if (addMarker)
            mapboxMap.addMarker(MarkerOptions().position(point))
        clearPoints.visibility = View.VISIBLE

        startRouteButton.visibility = View.VISIBLE
        calculateRoute()
        return true
    }

    private fun calculateRoute() {
        val userLocation = locationEngine.lastLocation
        val accesstoken = Mapbox.getAccessToken()
        val destination = destination
        if (userLocation == null) {
            Timber.d("calculateRoute: User location is null, therefore, origin can't be set.")
            return
        }

        if (destination == null || accesstoken == null) {
            return
        }

        val origin = Point.fromLngLat(userLocation.longitude, userLocation.latitude)
        if (TurfMeasurement.distance(origin, destination, TurfConstants.UNIT_METERS) < 50) {
            startRouteButton.visibility = View.GONE
            return
        }

        val navigationRouteBuilder = NavigationRoute.builder(this).apply {
            this.accessToken(accesstoken)
            this.origin(origin)
            this.destination(destination)
            this.voiceUnits(DirectionsCriteria.METRIC)
            this.alternatives(true)
            this.baseUrl(BASE_URL)
        }

        navigationRouteBuilder.build().getRoute(object : Callback<DirectionsResponse> {
            override fun onResponse(call: Call<DirectionsResponse>, response: Response<DirectionsResponse>) {
                Timber.d("Url: %s", call.request().url().toString())
                response.body()?.let { response ->
                    if (response.routes().isNotEmpty()) {
                        val directionsRoute = response.routes().first()
                        this@MockNavigationActivity.route = directionsRoute
                        navigationMapRoute?.addRoutes(response.routes())
                    }
                }
            }

            override fun onFailure(call: Call<DirectionsResponse>, throwable: Throwable) {
                Timber.e(throwable, "onFailure: navigation.getRoute()")
            }
        })
    }

    override fun onProgressChange(location: Location?, routeProgress: RouteProgress?) {

    }

    override fun onRunning(running: Boolean) {

    }

    override fun onMilestoneEvent(routeProgress: RouteProgress?, instruction: String?, milestone: Milestone?) {

    }

    override fun userOffRoute(location: Location?) {

    }

    private class BeginRouteInstruction : Instruction() {

        override fun buildInstruction(routeProgress: RouteProgress): String {
            return "Have a safe trip!"
        }
    }

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
        navigation.onDestroy()
        if (::mapboxMap.isInitialized) {
            mapboxMap.removeOnMapClickListener(this)
        }
        mapView.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }


    private class MyBroadcastReceiver internal constructor(navigation: MapboxNavigation) : BroadcastReceiver() {
        private val weakNavigation: WeakReference<MapboxNavigation> = WeakReference(navigation)

        override fun onReceive(context: Context, intent: Intent) {
            weakNavigation.get()?.stopNavigation()
        }
    }

    private fun newOrigin() {
        mapboxMap.let {
            val latLng = LatLng(52.039176, 5.550339)
            locationEngine.assignLastLocation(
                    Point.fromLngLat(latLng.longitude, latLng.latitude)
            )
            it.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 12.0))
        }
    }

    companion object{
        private const val STYLE_URL = "YOUR STYLE URL HERE"
        private const val BASE_URL = "YOUR BASE URL HERE"
    }
}
