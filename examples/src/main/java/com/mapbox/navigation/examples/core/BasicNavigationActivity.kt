package com.mapbox.navigation.examples.core

import android.annotation.SuppressLint
import android.location.Location
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
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
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.navigation.base.extensions.applyDefaultParams
import com.mapbox.navigation.base.extensions.coordinates
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesRequestCallback
import com.mapbox.navigation.core.replay.route.ReplayRouteLocationEngine
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.core.trip.session.TripSessionState
import com.mapbox.navigation.core.trip.session.TripSessionStateObserver
import com.mapbox.navigation.examples.R
import com.mapbox.navigation.examples.utils.Utils
import com.mapbox.navigation.examples.utils.extensions.toPoint
import com.mapbox.navigation.ui.camera.NavigationCamera
import com.mapbox.navigation.ui.map.NavigationMapboxMap
import com.mapbox.navigation.ui.map.NavigationMapboxMapInstanceState
import kotlinx.android.synthetic.main.activity_basic_navigation_layout.*
import kotlinx.android.synthetic.main.activity_basic_navigation_layout.container
import kotlinx.android.synthetic.main.activity_trip_service.mapView
import timber.log.Timber

/**
 * To ensure proper functioning of this example make sure your Location is turned on.
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
    private var locationComponent: LocationComponent? = null
    private lateinit var locationEngine: LocationEngine

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_basic_navigation_layout)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        locationEngine = LocationEngineProvider.getBestLocationEngine(this)

        val mapboxNavigationOptions = MapboxNavigation.defaultNavigationOptions(
            this,
            Utils.getMapboxAccessToken(this)
        )

        mapboxNavigation = MapboxNavigation(
            applicationContext,
            Utils.getMapboxAccessToken(this),
            mapboxNavigationOptions,
            locationEngine = getLocationEngine()
        )

        initListeners()
        Snackbar.make(container, R.string.msg_long_press_map_for_destination, Snackbar.LENGTH_LONG)
            .show()
    }

    override fun onMapReady(mapboxMap: MapboxMap) {
        mapboxMap.setStyle(Style.MAPBOX_STREETS) { style ->
            initLocationComponent(style, mapboxMap)
            navigationMapboxMap = NavigationMapboxMap(mapView, mapboxMap).also {
                it.addProgressChangeListener(mapboxNavigation!!)
                mapInstanceState?.let { state ->
                    it.restoreFrom(state)
                }
            }
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
        locationComponent = mapboxMap.locationComponent
        Snackbar.make(
                findViewById(R.id.container),
                getString(R.string.msg_long_press_map_for_destination),
                Snackbar.LENGTH_SHORT
        ).show()
    }

    @SuppressLint("RestrictedApi")
    fun initLocationComponent(loadedMapStyle: Style, mapboxMap: MapboxMap) {
        mapboxMap.moveCamera(CameraUpdateFactory.zoomTo(15.0))
        mapboxMap.locationComponent.let { locationComponent ->
            val locationComponentActivationOptions =
                LocationComponentActivationOptions.builder(this, loadedMapStyle)
                    .build()

            locationComponent.activateLocationComponent(locationComponentActivationOptions)
            locationComponent.isLocationComponentEnabled = true
            locationComponent.cameraMode = CameraMode.TRACKING
            locationComponent.renderMode = RenderMode.COMPASS
        }
    }

    private val routesReqCallback = object : RoutesRequestCallback {
        override fun onRoutesReady(routes: List<DirectionsRoute>) {
            if (routes.isNotEmpty()) {
                navigationMapboxMap?.drawRoute(routes[0])
                if (shouldSimulateRoute()) {
                    (mapboxNavigation?.locationEngine as ReplayRouteLocationEngine).assign(routes[0])
                }
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
            if (mapboxNavigation?.getRoutes()?.isNotEmpty() == true) {
                navigationMapboxMap?.updateLocationLayerRenderMode(RenderMode.GPS)
                navigationMapboxMap?.updateCameraTrackingMode(NavigationCamera.NAVIGATION_TRACKING_MODE_GPS)
                navigationMapboxMap?.startCamera(mapboxNavigation?.getRoutes()!![0])
            }
            mapboxNavigation?.startTripSession()
            startNavigation.visibility = View.GONE
        }
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
        mapboxNavigation?.registerLocationObserver(locationObserver)
        mapboxNavigation?.registerTripSessionStateObserver(tripSessionStateObserver)
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
        mapboxNavigation?.unregisterLocationObserver(locationObserver)
        stopLocationUpdates()
        mapView.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()

        mapboxNavigation?.unregisterTripSessionStateObserver(tripSessionStateObserver)
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

    private val locationListenerCallback: LocationEngineCallback<LocationEngineResult> =
        object : LocationEngineCallback<LocationEngineResult> {
            override fun onSuccess(result: LocationEngineResult) {
                // todo
            }

            override fun onFailure(exception: Exception) {
                Timber.i(exception)
            }
        }

    private fun startLocationUpdates() {
        val requestLocationUpdateRequest =
            LocationEngineRequest.Builder(DEFAULT_INTERVAL_IN_MILLISECONDS)
                .setPriority(LocationEngineRequest.PRIORITY_NO_POWER)
                .setMaxWaitTime(DEFAULT_MAX_WAIT_TIME)
                .build()

        locationEngine.requestLocationUpdates(
            requestLocationUpdateRequest,
            locationListenerCallback,
            mainLooper
        )
        locationEngine.getLastLocation(locationListenerCallback)
    }

    private fun stopLocationUpdates() {
        locationEngine.removeLocationUpdates(locationListenerCallback)
    }

    private val locationObserver = object : LocationObserver {
        override fun onRawLocationChanged(rawLocation: Location) {
            Timber.d("raw location %s", rawLocation.toString())
        }

        override fun onEnhancedLocationChanged(
            enhancedLocation: Location,
            keyPoints: List<Location>
        ) {
            if (keyPoints.isNotEmpty()) {
                locationComponent?.forceLocationUpdate(keyPoints, true)
            } else {
                locationComponent?.forceLocationUpdate(enhancedLocation)
            }
        }
    }

    private val tripSessionStateObserver = object : TripSessionStateObserver {
        override fun onSessionStateChanged(tripSessionState: TripSessionState) {
            when (tripSessionState) {
                TripSessionState.STARTED -> {
                    stopLocationUpdates()
                }
                TripSessionState.STOPPED -> {
                    navigationMapboxMap?.removeRoute()
                    startLocationUpdates()
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
            ReplayRouteLocationEngine()
        } else {
            LocationEngineProvider.getBestLocationEngine(this)
        }
    }
}
