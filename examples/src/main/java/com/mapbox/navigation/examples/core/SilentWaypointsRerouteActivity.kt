package com.mapbox.navigation.examples.core

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Looper
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.snackbar.Snackbar.LENGTH_SHORT
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineCallback
import com.mapbox.android.core.location.LocationEngineProvider
import com.mapbox.android.core.location.LocationEngineRequest
import com.mapbox.android.core.location.LocationEngineResult
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.location.LocationComponent
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.location.LocationUpdate
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.navigation.base.internal.extensions.applyDefaultParams
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.directions.session.RoutesRequestCallback
import com.mapbox.navigation.core.replay.MapboxReplayer
import com.mapbox.navigation.core.replay.ReplayLocationEngine
import com.mapbox.navigation.core.replay.route.ReplayProgressObserver
import com.mapbox.navigation.core.reroute.RerouteController
import com.mapbox.navigation.core.reroute.RerouteState
import com.mapbox.navigation.core.trip.session.OffRouteObserver
import com.mapbox.navigation.core.trip.session.TripSessionState
import com.mapbox.navigation.core.trip.session.TripSessionStateObserver
import com.mapbox.navigation.examples.R
import com.mapbox.navigation.examples.core.utils.WaypointsController
import com.mapbox.navigation.examples.utils.Utils
import com.mapbox.navigation.ui.camera.NavigationCamera
import com.mapbox.navigation.ui.map.NavigationMapboxMap
import kotlinx.android.synthetic.main.activity_silent_waypoints_reroute_layout.btnReroute
import kotlinx.android.synthetic.main.activity_silent_waypoints_reroute_layout.btnStartNavigation
import kotlinx.android.synthetic.main.activity_silent_waypoints_reroute_layout.container
import kotlinx.android.synthetic.main.activity_silent_waypoints_reroute_layout.mapView
import kotlinx.android.synthetic.main.activity_silent_waypoints_reroute_layout.seekBar
import kotlinx.android.synthetic.main.activity_silent_waypoints_reroute_layout.seekBarLayout
import kotlinx.android.synthetic.main.activity_silent_waypoints_reroute_layout.seekBarText
import kotlinx.android.synthetic.main.content_simple_mapbox_navigation.*
import timber.log.Timber
import java.lang.ref.WeakReference

/**
 * This activity shows how to:
 * - add silent waypoints to the route;
 * - manually invoke reroute;
 * - observe reroute events with the Navigation SDK's [RoutesObserver].
 * - the Navigation SDK's route replay infrastructure
 */
class SilentWaypointsRerouteActivity :
    AppCompatActivity(),
    OnMapReadyCallback,
    OffRouteObserver,
    RerouteController.RerouteStateObserver {

    private var directionRoute: DirectionsRoute? = null
    private val waypointsController = WaypointsController()
    private val mapboxReplayer = MapboxReplayer()
    private var locationComponent: LocationComponent? = null

    private lateinit var localLocationEngine: LocationEngine
    private lateinit var mapboxNavigation: MapboxNavigation
    private lateinit var navigationMapboxMap: NavigationMapboxMap

    private val locationEngineCallback = MyLocationEngineCallback(this)
    private val tripSessionStateObserver = object : TripSessionStateObserver {
        override fun onSessionStateChanged(tripSessionState: TripSessionState) {
            when (tripSessionState) {
                TripSessionState.STARTED -> {
                    stopLocationUpdates()
                    btnStartNavigation.visibility = GONE
                }
                TripSessionState.STOPPED -> {
                    startLocationUpdates()
                    waypointsController.clear()
                    updateCameraOnNavigationStateChange(false)
                }
            }
        }
    }

    private val routeObserver = object : RoutesObserver {
        override fun onRoutesChanged(routes: List<DirectionsRoute>) {
            directionRoute = routes.firstOrNull()
            navigationMapboxMap.drawRoutes(routes)
        }
    }

    private val routesReqCallback = object : RoutesRequestCallback {
        override fun onRoutesReady(routes: List<DirectionsRoute>) {
            Timber.d("route request success $routes")
            btnStartNavigation.visibility = if (routes.isNotEmpty()) {
                VISIBLE
            } else {
                GONE
            }
        }

        override fun onRoutesRequestFailure(throwable: Throwable, routeOptions: RouteOptions) {
            Timber.e("route request failure $throwable")
        }

        override fun onRoutesRequestCanceled(routeOptions: RouteOptions) {
            Timber.d("route request canceled")
        }
    }

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_silent_waypoints_reroute_layout)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)
        localLocationEngine = LocationEngineProvider.getBestLocationEngine(applicationContext)

        val mapboxNavigationOptions = MapboxNavigation
            .defaultNavigationOptionsBuilder(this, Utils.getMapboxAccessToken(this))
        mapboxNavigation = getMapboxNavigation(mapboxNavigationOptions)

        initListeners()
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()

        mapboxNavigation.run {
            registerTripSessionStateObserver(tripSessionStateObserver)
            registerRoutesObserver(routeObserver)
            registerOffRouteObserver(this@SilentWaypointsRerouteActivity)
            getRerouteController()?.registerRerouteStateObserver(
                this@SilentWaypointsRerouteActivity
            )
        }
    }

    public override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    public override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onStop() {
        super.onStop()
        stopLocationUpdates()
        mapboxNavigation.run {
            unregisterTripSessionStateObserver(tripSessionStateObserver)
            unregisterRoutesObserver(routeObserver)
            unregisterOffRouteObserver(this@SilentWaypointsRerouteActivity)
            getRerouteController()?.unregisterRerouteStateObserver(
                this@SilentWaypointsRerouteActivity
            )
        }
        mapView.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapboxReplayer.finish()
        mapboxNavigation.stopTripSession()
        mapboxNavigation.onDestroy()
        mapView.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(mapboxMap: MapboxMap) {
        mapboxMap.moveCamera(CameraUpdateFactory.zoomTo(15.0))
        mapboxMap.setStyle(Style.MAPBOX_STREETS) { style ->
            locationComponent = mapboxMap.locationComponent.apply {
                activateLocationComponent(
                    LocationComponentActivationOptions.builder(
                        this@SilentWaypointsRerouteActivity,
                        style
                    )
                        .useDefaultLocationEngine(false)
                        .build()
                )
                cameraMode = CameraMode.TRACKING
                isLocationComponentEnabled = true
            }
            navigationMapboxMap = NavigationMapboxMap.Builder(mapView, mapboxMap, this)
                .vanishRouteLineEnabled(true)
                .build()
            navigationMapboxMap.addProgressChangeListener(mapboxNavigation)

            when (directionRoute) {
                null -> {
                    if (shouldSimulateRoute()) {
                        mapboxNavigation
                            .registerRouteProgressObserver(ReplayProgressObserver(mapboxReplayer))
                        mapboxReplayer.pushRealLocation(this, 0.0)
                        mapboxReplayer.play()
                    }
                    Snackbar
                        .make(
                            container,
                            R.string.msg_long_press_map_to_place_waypoint,
                            LENGTH_SHORT
                        )
                        .show()
                }
                else -> restoreNavigation()
            }
        }

        mapboxMap.addOnMapLongClickListener { latLng ->
            waypointsController.add(latLng)
            locationComponent?.lastKnownLocation?.let { originLocation ->
                mapboxNavigation.requestRoutes(
                    RouteOptions.builder().applyDefaultParams()
                        .accessToken(Utils.getMapboxAccessToken(applicationContext))
                        .coordinates(waypointsController.coordinates(originLocation))
                        .waypointIndices("0;${waypointsController.waypoints.size}")
                        .profile(DirectionsCriteria.PROFILE_DRIVING_TRAFFIC)
                        .build(),
                    routesReqCallback
                )
            }
            true
        }

        if (directionRoute == null) {
            Snackbar.make(container, R.string.msg_long_press_map_to_place_waypoint, LENGTH_SHORT)
                .show()
        }
    }

    @SuppressLint("MissingPermission")
    private fun initListeners() {
        setupReplayControls()
        btnStartNavigation.setOnClickListener {
            updateCameraOnNavigationStateChange(true)
            navigationMapboxMap.addProgressChangeListener(mapboxNavigation)
            if (mapboxNavigation.getRoutes().isNotEmpty()) {
                navigationMapboxMap.startCamera(mapboxNavigation.getRoutes()[0])
            }
            mapboxNavigation.startTripSession()
            btnReroute.visibility = VISIBLE
            btnStartNavigation.visibility = GONE
            mapboxReplayer.play()
        }
        btnReroute.setOnClickListener {
            mapboxNavigation.getRerouteController()?.reroute(
                object : RerouteController.RoutesCallback {
                    override fun onNewRoutes(routes: List<DirectionsRoute>) {
                        //  default implementation of the RerouteController automatically sets the route, no action needed
                        // if you're implementing a custom RerouteController, you should invoke `MapboxNavigation#setRoute` here
                        Timber.d("Reroute successful")
                    }
                }
            )
        }
    }

    private fun setupReplayControls() {
        if (shouldSimulateRoute()) {
            seekBar.max = 4
            seekBar.progress = 1
            seekBarText.text = getString(R.string.replay_playback_speed_seekbar, seekBar.progress)
            seekBar.setOnSeekBarChangeListener(
                object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(
                        seekBar: SeekBar,
                        progress: Int,
                        fromUser: Boolean
                    ) {
                        mapboxReplayer.playbackSpeed(progress.toDouble())
                        seekBarText.text =
                            getString(R.string.replay_playback_speed_seekbar, progress)
                    }

                    override fun onStartTrackingTouch(seekBar: SeekBar) {}
                    override fun onStopTrackingTouch(seekBar: SeekBar) {}
                }
            )
        } else {
            seekBarLayout.visibility = GONE
        }
    }

    private fun startLocationUpdates() {
        val request = LocationEngineRequest.Builder(1000L)
            .setFastestInterval(500L)
            .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
            .build()
        try {
            localLocationEngine.requestLocationUpdates(
                request,
                locationEngineCallback,
                Looper.getMainLooper()
            )
            if (directionRoute == null) {
                localLocationEngine.getLastLocation(locationEngineCallback)
            }
        } catch (exception: SecurityException) {
            Timber.e(exception)
        }
    }

    private fun stopLocationUpdates() {
        localLocationEngine.removeLocationUpdates(locationEngineCallback)
    }

    private fun updateCameraOnNavigationStateChange(navigationStarted: Boolean) {
        if (::navigationMapboxMap.isInitialized) {
            navigationMapboxMap.apply {
                if (navigationStarted) {
                    updateCameraTrackingMode(NavigationCamera.NAVIGATION_TRACKING_MODE_GPS)
                    updateLocationLayerRenderMode(RenderMode.GPS)
                } else {
                    updateCameraTrackingMode(NavigationCamera.NAVIGATION_TRACKING_MODE_NONE)
                    updateLocationLayerRenderMode(RenderMode.COMPASS)
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        // This is not the most efficient way to preserve the route on a device rotation.
        // This is here to demonstrate that this event needs to be handled in order to
        // redraw the route line after a rotation.
        directionRoute?.let {
            outState.putString(Utils.PRIMARY_ROUTE_BUNDLE_KEY, it.toJson())
        }
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        savedInstanceState?.let {
            super.onRestoreInstanceState(savedInstanceState)
            Utils.getRouteFromBundle(savedInstanceState).let { route ->
                directionRoute = route
                navigationMapboxMap.drawRoute(route)
                mapboxNavigation.setRoutes(listOf(route))
                btnStartNavigation.visibility = VISIBLE
            }
        }
    }

    override fun onOffRouteStateChanged(offRoute: Boolean) {
        if (offRoute) {
            Toast.makeText(this, "You're off-route", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRerouteStateChanged(rerouteState: RerouteState) {
        val message: String = when (rerouteState) {
            RerouteState.Idle -> "Reroute is idle"
            RerouteState.Interrupted -> "Reroute request interrupted"
            is RerouteState.Failed -> "Reroute request failed: ${rerouteState.message}"
            RerouteState.FetchingRoute -> "Reroute request is in progress"
            RerouteState.RouteFetched -> "Reroute request is finished successful"
        }
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun getMapboxNavigation(optionsBuilder: NavigationOptions.Builder): MapboxNavigation {
        return if (shouldSimulateRoute()) {
            optionsBuilder.locationEngine(ReplayLocationEngine(mapboxReplayer))
            MapboxNavigation(optionsBuilder.build()).apply {
                registerRouteProgressObserver(ReplayProgressObserver(mapboxReplayer))
                if (directionRoute == null) {
                    mapboxReplayer.pushRealLocation(applicationContext, 0.0)
                    mapboxReplayer.play()
                }
            }
        } else {
            MapboxNavigation(optionsBuilder.build())
        }
    }

    private fun shouldSimulateRoute(): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(this.applicationContext)
            .getBoolean(this.getString(R.string.simulate_route_key), false)
    }

    @SuppressLint("MissingPermission")
    private fun restoreNavigation() {
        directionRoute?.let {
            btnStartNavigation.visibility = VISIBLE
            mapboxNavigation.setRoutes(listOf(it))
            navigationMapboxMap.drawRoute(it)
            navigationMapboxMap.addProgressChangeListener(mapboxNavigation)
            navigationMapboxMap.startCamera(mapboxNavigation.getRoutes()[0])
            updateCameraOnNavigationStateChange(true)
            mapboxNavigation.startTripSession()
        }
    }

    private class MyLocationEngineCallback(activity: SilentWaypointsRerouteActivity) :
        LocationEngineCallback<LocationEngineResult> {

        private val activityRef = WeakReference(activity)

        override fun onSuccess(result: LocationEngineResult?) {
            result?.locations?.firstOrNull()?.let {
                activityRef.get()?.locationComponent?.forceLocationUpdate(
                    LocationUpdate.Builder()
                        .location(result.lastLocation)
                        .build()
                )
            }
        }

        override fun onFailure(exception: Exception) {
        }
    }
}
