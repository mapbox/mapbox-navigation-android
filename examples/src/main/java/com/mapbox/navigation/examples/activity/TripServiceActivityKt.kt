package com.mapbox.navigation.examples.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import androidx.appcompat.app.AppCompatActivity
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.navigation.base.logger.model.Message
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.examples.R
import com.mapbox.navigation.logger.MapboxLogger
import com.mapbox.navigation.trip.notification.MapboxTripNotification
import com.mapbox.navigation.trip.notification.NavigationNotificationProvider
import com.mapbox.navigation.trip.service.MapboxTripService
import com.mapbox.navigation.trip.service.NavigationNotificationService
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute
import kotlinx.android.synthetic.main.activity_trip_service.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable.isActive
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@ExperimentalCoroutinesApi
@InternalCoroutinesApi
class TripServiceActivityKt : AppCompatActivity(), OnMapReadyCallback {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + Dispatchers.Main)
    private var mapboxMap: MapboxMap? = null
    private lateinit var mapboxTripNotification: MapboxTripNotification
    private lateinit var navigationMapRoute: NavigationMapRoute
    private lateinit var mapboxTripService: MapboxTripService
    private var textUpdateJob: Job = Job()

    @SuppressLint("MissingPermission")
    override fun onMapReady(mapboxMap: MapboxMap) {
        this.mapboxMap = mapboxMap
        mapboxMap.setStyle(Style.MAPBOX_STREETS) {
            navigationMapRoute = NavigationMapRoute(mapView, mapboxMap)
            newOrigin()
            toggleNotification.setOnClickListener {
                when (mapboxTripService.hasServiceStarted()) {
                    true -> {
                        textUpdateJob.cancel()
                        mapboxTripService.stopService()
                        toggleNotification.text = "Start"
                    }
                    false -> {
                        mapboxTripService.startService()
                        changeText()
                        toggleNotification.text = "Stop"
                    }
                }
            }
        }
    }

    /*
     * Activity lifecycle methods
     */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trip_service)

        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        mapboxTripNotification =
            MapboxTripNotification(applicationContext, NavigationNotificationProvider())
        mapboxTripService =
            MapboxTripService(mapboxTripNotification) {
                val intent =
                    Intent(applicationContext, NavigationNotificationService::class.java)
                try {
                    applicationContext.startService(intent)
                } catch (e: IllegalStateException) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        applicationContext.startForegroundService(intent)
                    } else {
                        throw e
                    }
                }
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
        mapboxTripService.stopService()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }

    private fun newOrigin() {
        mapboxMap?.let { map ->
            val latLng = LatLng(37.791674, -122.396469)
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 12.0))
        }
    }

    private fun changeText() {
        textUpdateJob = scope.launch {
            while (isActive) {
                val text = "Time elapsed: + ${SystemClock.elapsedRealtime()}"
                notifyTextView.text = text
                mapboxTripService.updateNotification(RouteProgress(text))
                MapboxLogger.i(Message(text))
                delay(1000L)
            }
        }
    }
}
