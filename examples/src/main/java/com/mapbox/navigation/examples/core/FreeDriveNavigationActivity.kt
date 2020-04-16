package com.mapbox.navigation.examples.core

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.mapbox.android.core.location.LocationEngineCallback
import com.mapbox.android.core.location.LocationEngineProvider
import com.mapbox.android.core.location.LocationEngineRequest
import com.mapbox.android.core.location.LocationEngineResult
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.navigation.base.options.Endpoint
import com.mapbox.navigation.base.options.MapboxOnboardRouterConfig
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.trip.session.TripSessionState
import com.mapbox.navigation.core.trip.session.TripSessionStateObserver
import com.mapbox.navigation.examples.R
import com.mapbox.navigation.examples.utils.Utils
import com.mapbox.navigation.ui.camera.NavigationCamera
import com.mapbox.navigation.ui.map.NavigationMapboxMap
import com.mapbox.navigation.ui.map.NavigationMapboxMapInstanceState
import java.io.File
import java.lang.ref.WeakReference
import java.net.URI
import kotlinx.android.synthetic.main.free_drive_navigation_layout.mapView
import kotlinx.android.synthetic.main.free_drive_navigation_layout.startNavigation
import timber.log.Timber

/**
 * To ensure proper functioning of this example make sure your Location is turned on.
 */
class FreeDriveNavigationActivity : AppCompatActivity(), OnMapReadyCallback {

    companion object {
        const val MAP_INSTANCE_STATE_KEY = "navgation_mapbox_map_instance_state"
        const val DEFAULT_INTERVAL_IN_MILLISECONDS = 1000L
        const val DEFAULT_MAX_WAIT_TIME = DEFAULT_INTERVAL_IN_MILLISECONDS * 5
    }

    private var mapboxNavigation: MapboxNavigation? = null
    private var navigationMapboxMap: NavigationMapboxMap? = null
    private var mapInstanceState: NavigationMapboxMapInstanceState? = null

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.free_drive_navigation_layout)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        val mapboxNavigationOptions = MapboxNavigation.defaultNavigationOptions(
                this,
                Utils.getMapboxAccessToken(this)
        )
        val updatedOptions = mapboxNavigationOptions.toBuilder().onboardRouterConfig(getOnBoardRouterConfig()).build()

        mapboxNavigation = MapboxNavigation(
                applicationContext,
                Utils.getMapboxAccessToken(this),
                updatedOptions
        )
        initListeners()
    }

    override fun onMapReady(mapboxMap: MapboxMap) {
        mapboxMap.setStyle(Style.MAPBOX_STREETS) { style ->
            mapboxMap.moveCamera(CameraUpdateFactory.zoomTo(15.0))
            navigationMapboxMap = NavigationMapboxMap(mapView, mapboxMap)
            mapInstanceState?.let { state ->
                navigationMapboxMap?.restoreFrom(state)
            }
            // center the map at current location
            LocationEngineProvider.getBestLocationEngine(this).getLastLocation(locationListenerCallback)
        }
    }

    fun startLocationUpdates() {
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

    @SuppressLint("MissingPermission")
    fun initListeners() {
        startNavigation.setOnClickListener {
            navigationMapboxMap?.updateCameraTrackingMode(NavigationCamera.NAVIGATION_TRACKING_MODE_GPS)
            navigationMapboxMap?.updateLocationLayerRenderMode(RenderMode.GPS)
            navigationMapboxMap?.addProgressChangeListener(mapboxNavigation!!)
            if (mapboxNavigation?.getRoutes()?.isNotEmpty() == true) {
                navigationMapboxMap?.startCamera(mapboxNavigation?.getRoutes()!![0])
            }
            mapboxNavigation?.startTripSession()
            stopLocationUpdates()
            startNavigation.visibility = View.GONE
        }
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
        navigationMapboxMap?.onStart()
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
        stopLocationUpdates()
        mapboxNavigation?.unregisterTripSessionStateObserver(tripSessionStateObserver)
        navigationMapboxMap?.onStop()
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

    private class MyLocationEngineCallback(activity: FreeDriveNavigationActivity) : LocationEngineCallback<LocationEngineResult> {

        private val activityRef = WeakReference(activity)

        override fun onSuccess(result: LocationEngineResult) {
            activityRef.get()?.navigationMapboxMap?.updateLocation(result.lastLocation)
        }

        override fun onFailure(exception: java.lang.Exception) {
            Timber.i(exception)
        }
    }

    private fun stopLocationUpdates() {
        mapboxNavigation?.locationEngine?.removeLocationUpdates(locationListenerCallback)
    }

    private fun getOnBoardRouterConfig(): MapboxOnboardRouterConfig {
        val tilesUri = URI("https://api-routing-tiles-staging.tilestream.net")
        val tilesVersion = "2020_02_02-03_00_00"
        val tilesDir = if (tilesUri.toString().isNotEmpty() && tilesVersion.isNotEmpty()) {
            File(
                    this.filesDir,
                    "Offline/${tilesUri.host}/$tilesVersion"
            ).absolutePath
        } else ""

        return MapboxOnboardRouterConfig(
                tilesDir,
                null,
                null,
                2,
                Endpoint(
                        tilesUri.toString(),
                        tilesVersion,
                        Utils.getMapboxAccessToken(this) ?: "",
                        "MapboxNavigationNative"
                )
        )
    }

    private val tripSessionStateObserver = object : TripSessionStateObserver {
        override fun onSessionStateChanged(tripSessionState: TripSessionState) {
            when (tripSessionState) {
                TripSessionState.STARTED -> {
                    startNavigation.visibility = View.GONE
                    stopLocationUpdates()
                }
                TripSessionState.STOPPED -> {
                    startLocationUpdates()
                }
            }
        }
    }
}
