package com.mapbox.navigation.examples.core

import android.annotation.SuppressLint
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.View
import androidx.appcompat.app.AppCompatActivity
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
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.navigation.base.internal.extensions.applyDefaultParams
import com.mapbox.navigation.base.internal.extensions.coordinates
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesRequestCallback
import com.mapbox.navigation.core.replay.MapboxReplayer
import com.mapbox.navigation.core.replay.ReplayLocationEngine
import com.mapbox.navigation.core.replay.route2.ReplayProgressObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.core.trip.session.TripSessionState
import com.mapbox.navigation.core.trip.session.TripSessionStateObserver
import com.mapbox.navigation.examples.R
import com.mapbox.navigation.examples.utils.Utils
import com.mapbox.navigation.examples.utils.extensions.toPoint
import com.mapbox.navigation.ui.camera.NavigationCamera
import com.mapbox.navigation.ui.map.NavigationMapboxMap
import com.mapbox.navigation.ui.map.NavigationMapboxMapInstanceState
import java.lang.ref.WeakReference
import kotlinx.android.synthetic.main.activity_basic_navigation_layout.*
import timber.log.Timber

/**
 * This activity shows how to set up a basic turn-by-turn
 * navigation experience with the Navigation SDK and
 * Navigation UI SDK.
 */
class BasicNavigationActivity : AppCompatActivity(), OnMapReadyCallback {

    companion object {
        const val MAP_INSTANCE_STATE_KEY = "navgation_mapbox_map_instance_state"
        const val DEFAULT_INTERVAL_IN_MILLISECONDS = 1000L
        const val DEFAULT_MAX_WAIT_TIME = DEFAULT_INTERVAL_IN_MILLISECONDS * 5
    }

    private var mapboxNavigation: MapboxNavigation? = null
    private var navigationMapboxMap: NavigationMapboxMap? = null
    private var mapInstanceState: NavigationMapboxMapInstanceState? = null
    private val mapboxReplayer = MapboxReplayer()

    private val mapStyles = listOf(
        Style.MAPBOX_STREETS,
        Style.OUTDOORS,
        Style.LIGHT,
        Style.DARK,
        Style.SATELLITE_STREETS
    )

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_basic_navigation_layout)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        val mapboxNavigationOptions = MapboxNavigation.defaultNavigationOptions(
            this,
            Utils.getMapboxAccessToken(this)
        )

        mapboxNavigation = MapboxNavigation(
            applicationContext,
            mapboxNavigationOptions,
            locationEngine = getLocationEngine()
        ).apply {
            registerTripSessionStateObserver(tripSessionStateObserver)
            registerRouteProgressObserver(routeProgressObserver)
        }

        initListeners()
        Snackbar.make(container, R.string.msg_long_press_map_to_place_waypoint, LENGTH_SHORT)
            .show()
    }

    override fun onMapReady(mapboxMap: MapboxMap) {
        mapboxMap.setStyle(Style.MAPBOX_STREETS) {
            mapboxMap.moveCamera(CameraUpdateFactory.zoomTo(15.0))
            navigationMapboxMap = NavigationMapboxMap(mapView, mapboxMap, this, true)
            mapInstanceState?.let { state ->
                navigationMapboxMap?.restoreFrom(state)
            }
            // center the map at current location
            if (shouldSimulateRoute()) {
                mapboxNavigation?.registerRouteProgressObserver(ReplayProgressObserver(mapboxReplayer))
                mapboxReplayer.pushRealLocation(this, 0.0)
                mapboxReplayer.play()
            }
            mapboxNavigation?.locationEngine?.getLastLocation(locationListenerCallback)
        }
        mapboxMap.addOnMapLongClickListener { latLng ->
            mapboxMap.locationComponent.lastKnownLocation?.let { originLocation ->
                mapboxNavigation?.requestRoutes(
                    RouteOptions.builder().applyDefaultParams()
                        .accessToken(Utils.getMapboxAccessToken(applicationContext))
                        .coordinates(originLocation.toPoint(), null, latLng.toPoint())
                        .alternatives(true)
                        .profile(DirectionsCriteria.PROFILE_DRIVING_TRAFFIC)
                        .build(),
                    routesReqCallback
                )
            }
            true
        }
    }

    private val routeProgressObserver = object : RouteProgressObserver {
        override fun onRouteProgressChanged(routeProgress: RouteProgress) {
            // do something with the route progress
            Timber.i("route progress: ${routeProgress.currentState}")
        }
    }

    fun startLocationUpdates() {
        if (!shouldSimulateRoute()) {
            val requestLocationUpdateRequest =
                LocationEngineRequest.Builder(DEFAULT_INTERVAL_IN_MILLISECONDS)
                    .setPriority(LocationEngineRequest.PRIORITY_NO_POWER)
                    .setMaxWaitTime(DEFAULT_MAX_WAIT_TIME)
                    .build()

            mapboxNavigation?.locationEngine?.requestLocationUpdates(
                requestLocationUpdateRequest,
                locationListenerCallback,
                mainLooper
            )
        }
    }

    private val routesReqCallback = object : RoutesRequestCallback {
        override fun onRoutesReady(routes: List<DirectionsRoute>) {
            if (routes.isNotEmpty()) {
                navigationMapboxMap?.drawRoute(routes[0])
                startNavigation.visibility = View.VISIBLE
            } else {
                startNavigation.visibility = View.GONE
            }
        }

        override fun onRoutesRequestFailure(throwable: Throwable, routeOptions: RouteOptions) {
            Timber.e("route request failure %s", throwable.toString())
        }

        override fun onRoutesRequestCanceled(routeOptions: RouteOptions) {
            Timber.d("route request canceled")
        }
    }

    @SuppressLint("MissingPermission")
    fun initListeners() {
        startNavigation.setOnClickListener {
            updateCameraOnNavigationStateChange(true)
            navigationMapboxMap?.addProgressChangeListener(mapboxNavigation!!)
            if (mapboxNavigation?.getRoutes()?.isNotEmpty() == true) {
                navigationMapboxMap?.startCamera(mapboxNavigation?.getRoutes()!![0])
            }
            mapboxNavigation?.startTripSession()
            startNavigation.visibility = View.GONE
            stopLocationUpdates()
        }

        fabToggleStyle.setOnClickListener {
            navigationMapboxMap?.retrieveMap()?.setStyle(mapStyles.shuffled().first())
        }
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
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
        mapView.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapboxNavigation?.unregisterTripSessionStateObserver(tripSessionStateObserver)
        mapboxNavigation?.unregisterRouteProgressObserver(routeProgressObserver)
        mapboxNavigation?.stopTripSession()
        mapboxNavigation?.onDestroy()
        mapView.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        navigationMapboxMap?.saveStateWith(MAP_INSTANCE_STATE_KEY, outState)
        mapView.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        super.onRestoreInstanceState(savedInstanceState)
        mapInstanceState = savedInstanceState?.getParcelable(MAP_INSTANCE_STATE_KEY)
    }

    private val locationListenerCallback = MyLocationEngineCallback(this)

    private fun stopLocationUpdates() {
        if (!shouldSimulateRoute()) {
            mapboxNavigation?.locationEngine?.removeLocationUpdates(locationListenerCallback)
        }
    }

    private val tripSessionStateObserver = object : TripSessionStateObserver {
        override fun onSessionStateChanged(tripSessionState: TripSessionState) {
            when (tripSessionState) {
                TripSessionState.STARTED -> {
                    stopLocationUpdates()
                }
                TripSessionState.STOPPED -> {
                    startLocationUpdates()
                    navigationMapboxMap?.removeRoute()
                    updateCameraOnNavigationStateChange(false)
                }
            }
        }
    }

    // Used to determine if the ReplayRouteLocationEngine should be used to simulate the routing.
    // This is used for testing purposes.
    private fun shouldSimulateRoute(): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(this.applicationContext)
            .getBoolean(this.getString(R.string.simulate_route_key), false)
    }

    // If shouldSimulateRoute is true a ReplayRouteLocationEngine will be used which is intended
    // for testing else a real location engine is used.
    private fun getLocationEngine(): LocationEngine {
        return if (shouldSimulateRoute()) {
            ReplayLocationEngine(mapboxReplayer)
        } else {
            LocationEngineProvider.getBestLocationEngine(this)
        }
    }

    private fun updateCameraOnNavigationStateChange(
        navigationStarted: Boolean
    ) {
        navigationMapboxMap?.apply {
            if (navigationStarted) {
                updateCameraTrackingMode(NavigationCamera.NAVIGATION_TRACKING_MODE_GPS)
                updateLocationLayerRenderMode(RenderMode.GPS)
            } else {
                updateCameraTrackingMode(NavigationCamera.NAVIGATION_TRACKING_MODE_NONE)
                updateLocationLayerRenderMode(RenderMode.COMPASS)
            }
        }
    }

    private class MyLocationEngineCallback(activity: BasicNavigationActivity) :
        LocationEngineCallback<LocationEngineResult> {

        private val activityRef = WeakReference(activity)

        override fun onSuccess(result: LocationEngineResult) {
            activityRef.get()?.navigationMapboxMap?.updateLocation(result.lastLocation)
        }

        override fun onFailure(exception: java.lang.Exception) {
            Timber.i(exception)
        }
    }
}
