package com.mapbox.navigation.examples.activity

import android.annotation.SuppressLint
import android.location.Location
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.mapbox.android.core.location.LocationEngineProvider
import com.mapbox.android.core.location.LocationEngineRequest
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.navigation.examples.R
import com.mapbox.navigation.navigator.MapboxNativeNavigatorImpl
import com.mapbox.navigation.trip.notification.MapboxTripNotification
import com.mapbox.navigation.trip.notification.NavigationNotificationProvider
import com.mapbox.navigation.trip.service.MapboxTripService
import com.mapbox.navigation.trip.session.MapboxTripSession
import com.mapbox.navigation.trip.session.TripSession
import kotlinx.android.synthetic.main.activity_trip_service.mapView
import kotlinx.android.synthetic.main.activity_trip_session.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi

const val UPDATE_INTERVAL_IN_MILLISECONDS: Long = 1000
const val FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS: Long = 500

@ExperimentalCoroutinesApi
@InternalCoroutinesApi
class TripSessionActivityKt : AppCompatActivity(), OnMapReadyCallback {

    private var mapboxMap: MapboxMap? = null
    private lateinit var tripSession: MapboxTripSession
    private var isActive: Boolean = false
    private val locationObserver = object : TripSession.LocationObserver {
        override fun onRawLocationChanged(rawLocation: Location) {
            mapboxMap?.locationComponent?.forceLocationUpdate(rawLocation)
        }

        override fun onEnhancedLocationChanged(enhancedLocation: Location) {
            TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
        }
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(mapboxMap: MapboxMap) {
        this.mapboxMap = mapboxMap
        mapboxMap.setStyle(Style.MAPBOX_STREETS) { style ->
            toggleSession.setOnClickListener {
                when (isActive) {
                    true -> {
                        mapboxMap.locationComponent.isLocationComponentEnabled = false
                        tripSession.unregisterLocationObserver(locationObserver)
                        tripSession.stopLocationUpdates()
                        toggleSession.text = "Start"
                        isActive = false
                    }
                    false -> {
                        mapboxMap.locationComponent.activateLocationComponent(
                            LocationComponentActivationOptions.Builder(
                                applicationContext,
                                style
                            ).useDefaultLocationEngine(false).build()
                        )
                        mapboxMap.locationComponent.cameraMode = CameraMode.TRACKING_GPS
                        mapboxMap.locationComponent.renderMode = RenderMode.GPS
                        tripSession.registerLocationObserver(locationObserver)
                        tripSession.startLocationUpdates()
                        mapboxMap.locationComponent.isLocationComponentEnabled = true
                        toggleSession.text = "Stop"
                        isActive = true
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trip_session)

        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        tripSession = MapboxTripSession(
            MapboxTripService(
                MapboxTripNotification(
                    applicationContext,
                    NavigationNotificationProvider()
                )
            ) {},
            LocationEngineProvider.getBestLocationEngine(applicationContext),
            LocationEngineRequest.Builder(UPDATE_INTERVAL_IN_MILLISECONDS)
                .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
                .setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS)
                .build(),
            MapboxNativeNavigatorImpl
        )
    }

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
        mapView.onDestroy()
        if (::tripSession.isInitialized) {
            tripSession.unregisterLocationObserver(locationObserver)
            tripSession.stopLocationUpdates()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }
}
