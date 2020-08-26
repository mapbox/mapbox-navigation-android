package com.mapbox.navigation.examples.core

import android.annotation.SuppressLint
import android.location.Location
import android.os.Bundle
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.snackbar.Snackbar.LENGTH_SHORT
import com.mapbox.android.core.location.LocationEngineCallback
import com.mapbox.android.core.location.LocationEngineRequest
import com.mapbox.android.core.location.LocationEngineResult
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.navigation.base.internal.extensions.applyDefaultParams
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.directions.session.RoutesRequestCallback
import com.mapbox.navigation.core.reroute.RerouteController
import com.mapbox.navigation.core.reroute.RerouteState
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.core.trip.session.OffRouteObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.core.trip.session.TripSessionState
import com.mapbox.navigation.core.trip.session.TripSessionStateObserver
import com.mapbox.navigation.examples.R
import com.mapbox.navigation.examples.utils.Utils
import com.mapbox.navigation.examples.utils.extensions.toPoint
import com.mapbox.navigation.ui.camera.NavigationCamera
import com.mapbox.navigation.ui.map.NavigationMapboxMap
import java.lang.ref.WeakReference
//import kotlinx.android.synthetic.main.activity_reroute_layout.*
import kotlinx.android.synthetic.main.activity_reroute_layout.container
import kotlinx.android.synthetic.main.activity_reroute_layout.mapView
import kotlinx.android.synthetic.main.activity_reroute_layout.startNavigation
//import kotlinx.android.synthetic.main.activity_waypoints_reroute_layout.*
import timber.log.Timber

/**
 * This activity shows how to:
 * - observe reroute events with the Navigation SDK's [RoutesObserver];
 * - replace default [RerouteController] and handle re-route events.
 */
class WaypointsRerouteActivity : AppCompatActivity(), OnMapReadyCallback, OffRouteObserver,
        RerouteController.RerouteStateObserver {

    companion object {
        private const val DEFAULT_FASTEST_INTERVAL = 500L
        private const val DEFAULT_ENGINE_REQUEST_INTERVAL = 1000L
    }

    private var mapboxNavigation: MapboxNavigation? = null
    private var navigationMapboxMap: NavigationMapboxMap? = null
    private var directionRoute: DirectionsRoute? = null
    private val routeSettings = CoordinatesHolder()
    private val stopsController = StopsControllerr()

    private val locationListenerCallback = MyLocationEngineCallback(this)
    private val tripSessionStateObserver = object : TripSessionStateObserver {
        override fun onSessionStateChanged(tripSessionState: TripSessionState) {
            when (tripSessionState) {
                TripSessionState.STARTED -> {
                    startNavigation.visibility = GONE
                    stopLocationUpdates()
                    mapboxNavigation?.registerRouteProgressObserver(routeProgressObserver)
                }
                TripSessionState.STOPPED -> {
                    stopsController.clear()
                    startLocationUpdates()
                    navigationMapboxMap?.hideRoute()
                    updateCameraOnNavigationStateChange(false)
                }
            }
        }
    }

    private val routeObserver = object : RoutesObserver {
        override fun onRoutesChanged(routes: List<DirectionsRoute>) {
            startNavigation.visibility = if (routes.isNotEmpty()) {
                VISIBLE
            } else {
                GONE
            }
            directionRoute = routes.firstOrNull()
            navigationMapboxMap?.drawRoutes(routes)
        }
    }
    private val routeProgressObserver = object : RouteProgressObserver {
        override fun onRouteProgressChanged(routeProgress: RouteProgress) {
        }
    }
    private val routesReqCallback = object : RoutesRequestCallback {
        override fun onRoutesReady(routes: List<DirectionsRoute>) {
            Timber.d("route request success %s", routes.toString())
        }

        override fun onRoutesRequestFailure(throwable: Throwable, routeOptions: RouteOptions) {
            Timber.e("route request failure %s", throwable.toString())
        }

        override fun onRoutesRequestCanceled(routeOptions: RouteOptions) {
            Timber.d("route request canceled")
        }
    }

    private val locationObserver = object : LocationObserver {
        override fun onRawLocationChanged(rawLocation: Location) {
            routeSettings.currentLocation = rawLocation
        }

        override fun onEnhancedLocationChanged(
                enhancedLocation: Location,
                keyPoints: List<Location>
        ) = Unit
    }

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_waypoints_reroute_layout)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        val mapboxNavigationOptions = MapboxNavigation
                .defaultNavigationOptionsBuilder(this, Utils.getMapboxAccessToken(this))
                .build()

        mapboxNavigation = MapboxNavigation(mapboxNavigationOptions)
        initListeners()
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()

        mapboxNavigation?.run {
            registerLocationObserver(locationObserver)
            registerTripSessionStateObserver(tripSessionStateObserver)
            registerRoutesObserver(routeObserver)
            registerRouteProgressObserver(routeProgressObserver)
            registerOffRouteObserver(this@WaypointsRerouteActivity)
            getRerouteController()?.registerRerouteStateObserver(this@WaypointsRerouteActivity)
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
        mapboxNavigation?.run {
            unregisterLocationObserver(locationObserver)
            unregisterTripSessionStateObserver(tripSessionStateObserver)
            unregisterRouteProgressObserver(routeProgressObserver)
            unregisterRoutesObserver(routeObserver)
            unregisterOffRouteObserver(this@WaypointsRerouteActivity)
            getRerouteController()?.unregisterRerouteStateObserver(this@WaypointsRerouteActivity)
        }
        stopLocationUpdates()
        mapView.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapboxNavigation?.stopTripSession()
        mapboxNavigation?.onDestroy()
        mapView.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(mapboxMap: MapboxMap) {
        mapboxMap.setStyle(Style.MAPBOX_STREETS) { style ->
            mapboxMap.moveCamera(CameraUpdateFactory.zoomTo(15.0))
            navigationMapboxMap = NavigationMapboxMap(mapView, mapboxMap, this, true)

            mapboxNavigation?.navigationOptions?.locationEngine?.getLastLocation(
                    locationListenerCallback
            )

            directionRoute?.let {
                navigationMapboxMap?.drawRoute(it)
                mapboxNavigation?.setRoutes(listOf(it))
                startNavigation.visibility = View.VISIBLE
            }
        }

        mapboxMap.addOnMapLongClickListener { latLng ->
            stopsController.add(latLng)
            routeSettings.destination = latLng.toPoint()
            mapboxMap.locationComponent.lastKnownLocation?.let { originLocation ->
                mapboxNavigation?.requestRoutes(
                        RouteOptions.builder().applyDefaultParams()
                                .accessToken(Utils.getMapboxAccessToken(applicationContext))
//                        .coordinates(originLocation.toPoint(), null, latLng.toPoint())
                                .coordinates(stopsController.coordinates(originLocation))
                                .waypointIndices("0;${stopsController.stops.size}")
                                .alternatives(true)
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
        startNavigation.setOnClickListener {
            updateCameraOnNavigationStateChange(true)
            navigationMapboxMap?.addProgressChangeListener(mapboxNavigation!!)
            if (mapboxNavigation?.getRoutes()?.isNotEmpty() == true) {
                navigationMapboxMap?.startCamera(mapboxNavigation?.getRoutes()!![0])
            }
            mapboxNavigation?.startTripSession()
            startNavigation.visibility = View.GONE
//            btnReroute.visibility = View.VISIBLE
            stopLocationUpdates()
        }
//        btnReroute.setOnClickListener {
//            mapboxNavigation?.reroute()
//        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        val requestLocationUpdateRequest =
                LocationEngineRequest.Builder(DEFAULT_ENGINE_REQUEST_INTERVAL)
                        .setFastestInterval(DEFAULT_FASTEST_INTERVAL)
                        .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
                        .build()

        mapboxNavigation?.navigationOptions?.locationEngine?.requestLocationUpdates(
                requestLocationUpdateRequest,
                locationListenerCallback,
                mainLooper
        )
    }

    private fun stopLocationUpdates() {
        mapboxNavigation?.navigationOptions?.locationEngine?.removeLocationUpdates(
                locationListenerCallback
        )
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
        super.onRestoreInstanceState(savedInstanceState)
        directionRoute = Utils.getRouteFromBundle(savedInstanceState)
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

    private class MyLocationEngineCallback(activity: WaypointsRerouteActivity) :
            LocationEngineCallback<LocationEngineResult> {

        private val activityRef = WeakReference(activity)

        override fun onSuccess(result: LocationEngineResult?) {
            result?.locations?.firstOrNull()?.let {
                val activity = activityRef.get() ?: return@let
                activity.navigationMapboxMap?.updateLocation(result.lastLocation)
            }
        }

        override fun onFailure(exception: Exception) {
        }
    }

    private class CoordinatesHolder {
        var currentLocation: Location = Location("InternalProvider")
        var destination: Point = Point.fromLngLat(0.0, 0.0)
    }
}

private class StopsControllerr {
    val stops = mutableListOf<Point>()

    fun add(latLng: LatLng) {
        stops.add(latLng.toPoint())
    }

    fun clear() {
        stops.clear()
    }

    fun coordinates(originLocation: Location): List<Point> {
        val coordinates = mutableListOf<Point>()
        coordinates.add(originLocation.toPoint())
        coordinates.addAll(stops)
        return coordinates
    }
}
