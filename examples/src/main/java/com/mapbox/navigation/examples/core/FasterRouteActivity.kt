package com.mapbox.navigation.examples.core

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.snackbar.Snackbar.LENGTH_SHORT
import com.mapbox.android.core.location.LocationEngineCallback
import com.mapbox.android.core.location.LocationEngineRequest
import com.mapbox.android.core.location.LocationEngineResult
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.navigation.base.internal.extensions.applyDefaultParams
import com.mapbox.navigation.base.internal.extensions.coordinates
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.directions.session.RoutesRequestCallback
import com.mapbox.navigation.core.fasterroute.FasterRouteObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.core.trip.session.TripSessionState
import com.mapbox.navigation.core.trip.session.TripSessionStateObserver
import com.mapbox.navigation.examples.R
import com.mapbox.navigation.examples.utils.Utils
import com.mapbox.navigation.examples.utils.extensions.toPoint
import com.mapbox.navigation.ui.camera.NavigationCamera
import com.mapbox.navigation.ui.map.NavigationMapboxMap
import java.lang.ref.WeakReference
import java.util.concurrent.TimeUnit
import kotlinx.android.synthetic.main.bottom_sheet_faster_route.*
import kotlinx.android.synthetic.main.content_faster_route_layout.*
import timber.log.Timber

/**
 * To ensure proper functioning of this example make sure your Location is turned on.
 */
class FasterRouteActivity : AppCompatActivity(), OnMapReadyCallback {

    companion object {
        const val DEFAULT_FASTEST_INTERVAL = 500L
        const val DEFAULT_ENGINE_REQUEST_INTERVAL = 1000L
        const val START_TIME_MILLIS = 5000L
        const val COUNT_DOWN_INTERVAL = 10L
        const val MAX_PROGRESS = START_TIME_MILLIS / COUNT_DOWN_INTERVAL
    }

    private var mapboxMap: MapboxMap? = null
    private var fasterRoute: DirectionsRoute? = null

    private lateinit var mapboxNavigation: MapboxNavigation
    private var navigationMapboxMap: NavigationMapboxMap? = null
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>
    private val locationListenerCallback = MyLocationEngineCallback(this)

    private val routesReqCallback = object : RoutesRequestCallback {
        override fun onRoutesReady(routes: List<DirectionsRoute>) {
            if (routes.isNotEmpty()) {
                startNavigation.visibility = VISIBLE
            } else {
                startNavigation.visibility = GONE
            }
        }

        override fun onRoutesRequestFailure(throwable: Throwable, routeOptions: RouteOptions) {
        }

        override fun onRoutesRequestCanceled(routeOptions: RouteOptions) {
        }
    }

    private val routeProgressObserver = object : RouteProgressObserver {
        override fun onRouteProgressChanged(routeProgress: RouteProgress) {
        }
    }

    private val routesObserver = object : RoutesObserver {
        override fun onRoutesChanged(routes: List<DirectionsRoute>) {
            navigationMapboxMap?.drawRoutes(routes)
        }
    }

    private val tripSessionStateObserver = object : TripSessionStateObserver {
        override fun onSessionStateChanged(tripSessionState: TripSessionState) {
            when (tripSessionState) {
                TripSessionState.STARTED -> {
                    startNavigation.visibility = GONE
                    stopLocationUpdates()
                    mapboxNavigation.attachFasterRouteObserver(fasterRouteObserver)
                    mapboxNavigation.registerRouteProgressObserver(routeProgressObserver)
                }
                TripSessionState.STOPPED -> {
                    startLocationUpdates()
                    mapboxNavigation.detachFasterRouteObserver()
                    mapboxNavigation.unregisterRouteProgressObserver(routeProgressObserver)
                    navigationMapboxMap?.removeRoute()
                    updateCameraOnNavigationStateChange(false)
                }
            }
        }
    }

    private val fasterRouteObserver = object : FasterRouteObserver {
        // [Optional] Override the interval to check for faster routes.
        override fun restartAfterMillis(): Long = TimeUnit.MINUTES.toMillis(1)

        override fun onFasterRoute(currentRoute: DirectionsRoute, alternativeRoute: DirectionsRoute, isAlternativeFaster: Boolean) {
            if (isAlternativeFaster) {
                this@FasterRouteActivity.fasterRoute = alternativeRoute
                fasterRouteSelectionTimer.start()
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }
    }

    private val fasterRouteSelectionTimer: CountDownTimer =
        object : CountDownTimer(START_TIME_MILLIS, COUNT_DOWN_INTERVAL) {
            override fun onTick(millisUntilFinished: Long) {
                Timber.d("FASTER_ROUTE: millisUntilFinished $millisUntilFinished")
                fasterRouteAcceptProgress.progress =
                        (MAX_PROGRESS - millisUntilFinished / COUNT_DOWN_INTERVAL).toInt()
            }

            override fun onFinish() {
                Timber.d("FASTER_ROUTE: finished")
                this@FasterRouteActivity.fasterRoute = null
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_faster_route)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        val mapboxNavigationOptions = MapboxNavigation.defaultNavigationOptions(
                this,
                Mapbox.getAccessToken()
        )
        mapboxNavigationOptions.onboardRouterConfig?.toBuilder()?.tilePath("")

        mapboxNavigation = MapboxNavigation(applicationContext, mapboxNavigationOptions).also {
            it.registerRoutesObserver(routesObserver)
        }

        initListeners()
        Snackbar.make(container, R.string.msg_long_press_map_to_place_waypoint, LENGTH_SHORT)
                .show()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
        navigationMapboxMap?.onStart()
        mapboxNavigation.registerTripSessionStateObserver(tripSessionStateObserver)
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onStop() {
        super.onStop()
        mapboxNavigation.unregisterTripSessionStateObserver(tripSessionStateObserver)
        navigationMapboxMap?.onStop()
        mapView.onStop()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapboxNavigation.unregisterRouteProgressObserver(routeProgressObserver)
        mapboxNavigation.unregisterRoutesObserver(routesObserver)
        mapboxNavigation.stopTripSession()
        mapboxNavigation.onDestroy()
        mapView.onDestroy()
    }

    override fun onMapReady(mapboxMap: MapboxMap) {
        this.mapboxMap = mapboxMap
        mapboxMap.setStyle(Style.MAPBOX_STREETS) { style ->
            mapboxMap.moveCamera(CameraUpdateFactory.zoomTo(15.0))
            navigationMapboxMap = NavigationMapboxMap(mapView, mapboxMap, true)
        }
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
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheetFasterRoute)
        bottomSheetBehavior.peekHeight = 0
        fasterRouteAcceptProgress.max = MAX_PROGRESS.toInt()
        startNavigation.setOnClickListener {
            updateCameraOnNavigationStateChange(true)
            navigationMapboxMap?.addProgressChangeListener(mapboxNavigation)
            if (mapboxNavigation.getRoutes().isNotEmpty()) {
                navigationMapboxMap?.startCamera(mapboxNavigation.getRoutes()[0])
            }
            mapboxNavigation.startTripSession()
            stopLocationUpdates()
        }
        dismissLayout.setOnClickListener {
            fasterRouteSelectionTimer.onFinish()
        }
        acceptLayout.setOnClickListener {
            fasterRoute?.let {
                mapboxNavigation.setRoutes(mapboxNavigation.getRoutes().toMutableList().apply {
                    removeAt(0)
                    add(0, it)
                })
                fasterRouteSelectionTimer.onFinish()
            }
        }
    }

    private fun stopLocationUpdates() {
        mapboxNavigation.locationEngine.removeLocationUpdates(locationListenerCallback)
    }

    @SuppressLint("RestrictedApi")
    private fun startLocationUpdates() {
        val requestLocationUpdateRequest =
                LocationEngineRequest.Builder(DEFAULT_ENGINE_REQUEST_INTERVAL)
                        .setFastestInterval(DEFAULT_FASTEST_INTERVAL)
                        .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
                        .build()
        try {
            mapboxNavigation.locationEngine.requestLocationUpdates(
                    requestLocationUpdateRequest,
                    locationListenerCallback,
                    mainLooper
            )
            mapboxNavigation.locationEngine.getLastLocation(locationListenerCallback)
        } catch (exception: SecurityException) {
            Timber.e(exception)
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

    private class MyLocationEngineCallback(activity: FasterRouteActivity) :
            LocationEngineCallback<LocationEngineResult> {

        private val activityRef = WeakReference(activity)

        override fun onSuccess(result: LocationEngineResult?) {
            result?.locations?.firstOrNull()?.let {
                activityRef.get()?.mapboxMap?.locationComponent?.forceLocationUpdate(it)
            }
        }

        override fun onFailure(exception: Exception) {
        }
    }
}
