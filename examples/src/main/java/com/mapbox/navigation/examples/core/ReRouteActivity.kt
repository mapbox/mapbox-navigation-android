package com.mapbox.navigation.examples.core

import android.annotation.SuppressLint
import android.location.Location
import android.os.Bundle
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.snackbar.Snackbar.LENGTH_LONG
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
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.directions.session.RoutesRequestCallback
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.core.trip.session.TripSessionState
import com.mapbox.navigation.core.trip.session.TripSessionStateObserver
import com.mapbox.navigation.examples.R
import com.mapbox.navigation.examples.utils.Utils
import com.mapbox.navigation.examples.utils.extensions.toPoint
import com.mapbox.navigation.ui.camera.DynamicCamera
import com.mapbox.navigation.ui.camera.NavigationCamera
import com.mapbox.navigation.ui.map.NavigationMapboxMap
import java.lang.ref.WeakReference
import kotlinx.android.synthetic.main.activity_reroute_layout.*
import kotlinx.android.synthetic.main.activity_trip_service.mapView
import timber.log.Timber

/**
 * To ensure proper functioning of this example make sure your Location is turned on.
 */
class ReRouteActivity : AppCompatActivity(), OnMapReadyCallback {

    companion object {
        const val DEFAULT_FASTEST_INTERVAL = 500L
        const val DEFAULT_ENGINE_REQUEST_INTERVAL = 1000L
    }

    private var locationComponent: LocationComponent? = null

    private lateinit var mapboxNavigation: MapboxNavigation
    private lateinit var navigationMapboxMap: NavigationMapboxMap

    private val locationListenerCallback = MyLocationEngineCallback(this)
    private val tripSessionStateObserver = object : TripSessionStateObserver {
        override fun onSessionStateChanged(tripSessionState: TripSessionState) {
            when (tripSessionState) {
                TripSessionState.STARTED -> {
                    startNavigation.visibility = GONE
                    stopLocationUpdates()
                    mapboxNavigation.registerRouteProgressObserver(routeProgressObserver)
                }
                TripSessionState.STOPPED -> {
                    startLocationUpdates()
                    mapboxNavigation.unregisterRouteProgressObserver(routeProgressObserver)
                }
            }
        }
    }
    private val locationObserver = object : LocationObserver {
        override fun onRawLocationChanged(rawLocation: Location) {
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
    private val routeObserver = object : RoutesObserver {
        override fun onRoutesChanged(routes: List<DirectionsRoute>) {
            if (routes.isNotEmpty()) {
                navigationMapboxMap.drawRoute(routes[0])
            }
        }
    }
    private val routeProgressObserver = object : RouteProgressObserver {
        override fun onRouteProgressChanged(routeProgress: RouteProgress) {
        }
    }
    private val routesReqCallback = object : RoutesRequestCallback {
        override fun onRoutesReady(routes: List<DirectionsRoute>) {
            Timber.d("route request success %s", routes.toString())
            if (routes.isNotEmpty()) {
                startNavigation.visibility = VISIBLE
            } else {
                startNavigation.visibility = GONE
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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reroute_layout)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        initListeners()
        val mapboxNavigationOptions = MapboxNavigation.defaultNavigationOptions(
            this,
            Utils.getMapboxAccessToken(this)
        )

        mapboxNavigation = MapboxNavigation(
            applicationContext,
            Utils.getMapboxAccessToken(this),
            mapboxNavigationOptions,
            locationEngine = LocationEngineProvider.getBestLocationEngine(this)
        ).also {
            it.registerRoutesObserver(routeObserver)
        }
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
        mapboxNavigation.registerLocationObserver(locationObserver)
        mapboxNavigation.registerTripSessionStateObserver(tripSessionStateObserver)
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
        mapboxNavigation.unregisterLocationObserver(locationObserver)
        mapboxNavigation.unregisterTripSessionStateObserver(tripSessionStateObserver)
        stopLocationUpdates()
        mapView.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapboxNavigation.unregisterRouteProgressObserver(routeProgressObserver)
        mapboxNavigation.unregisterRoutesObserver(routeObserver)
        mapboxNavigation.stopTripSession()
        mapboxNavigation.onDestroy()
        mapView.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onMapReady(mapboxMap: MapboxMap) {
        mapboxMap.setStyle(Style.MAPBOX_STREETS) { style ->
            locationComponent = mapboxMap.locationComponent.apply {
                activateLocationComponent(
                    LocationComponentActivationOptions.builder(this@ReRouteActivity, style)
                        .useDefaultLocationEngine(false)
                        .build()
                )
                cameraMode = CameraMode.TRACKING
                isLocationComponentEnabled = true
            }
            navigationMapboxMap = NavigationMapboxMap(mapView, mapboxMap).also {
                it.addProgressChangeListener(mapboxNavigation)
                it.setCamera(DynamicCamera(mapboxMap))
            }
        }
        mapboxMap.moveCamera(CameraUpdateFactory.zoomTo(15.0))
        mapboxMap.addOnMapLongClickListener { latLng ->
            mapboxMap.locationComponent.lastKnownLocation?.let { originLocation ->
                mapboxNavigation.requestRoutes(
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

    @SuppressLint("MissingPermission")
    private fun initListeners() {
        Snackbar.make(container, R.string.msg_long_press_for_destination, LENGTH_LONG).show()
        startNavigation.setOnClickListener {
            if (mapboxNavigation.getRoutes().isNotEmpty()) {
                navigationMapboxMap.updateLocationLayerRenderMode(RenderMode.GPS)
                navigationMapboxMap.updateCameraTrackingMode(NavigationCamera.NAVIGATION_TRACKING_MODE_GPS)
                navigationMapboxMap.startCamera(mapboxNavigation.getRoutes()[0])
            }
            mapboxNavigation.startTripSession()
        }
    }

    private fun startLocationUpdates() {
        val requestLocationUpdateRequest =
            LocationEngineRequest.Builder(DEFAULT_ENGINE_REQUEST_INTERVAL)
                .setFastestInterval(DEFAULT_FASTEST_INTERVAL)
                .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
                .build()

        mapboxNavigation.locationEngine.requestLocationUpdates(
            requestLocationUpdateRequest,
            locationListenerCallback,
            mainLooper
        )
        mapboxNavigation.locationEngine.getLastLocation(locationListenerCallback)
    }

    private fun stopLocationUpdates() {
        mapboxNavigation.locationEngine.removeLocationUpdates(locationListenerCallback)
    }

    private class MyLocationEngineCallback(activity: ReRouteActivity) :
        LocationEngineCallback<LocationEngineResult> {

        private val activityRef = WeakReference(activity)

        override fun onSuccess(result: LocationEngineResult?) {
            result?.locations?.firstOrNull()?.let {
                activityRef.get()?.locationComponent?.forceLocationUpdate(it)
            }
        }

        override fun onFailure(exception: Exception) {
        }
    }
}
