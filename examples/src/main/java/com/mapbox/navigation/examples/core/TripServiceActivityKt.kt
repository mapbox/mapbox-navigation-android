package com.mapbox.navigation.examples.core

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.SystemClock
import androidx.appcompat.app.AppCompatActivity
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.trip.model.RouteLegProgress
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.model.RouteStepProgress
import com.mapbox.navigation.base.typedef.METRIC
import com.mapbox.navigation.base.typedef.ROUNDING_INCREMENT_FIFTY
import com.mapbox.navigation.base.typedef.TWENTY_FOUR_HOURS
import com.mapbox.navigation.core.MapboxDistanceFormatter
import com.mapbox.navigation.core.trip.service.MapboxTripService
import com.mapbox.navigation.examples.R
import com.mapbox.navigation.trip.notification.MapboxTripNotification
import com.mapbox.navigation.trip.notification.NotificationAction
import com.mapbox.navigation.utils.thread.ThreadController
import com.mapbox.navigation.utils.thread.monitorChannelWithException
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute
import kotlinx.android.synthetic.main.activity_trip_service.mapView
import kotlinx.android.synthetic.main.activity_trip_service.notifyTextView
import kotlinx.android.synthetic.main.activity_trip_service.toggleNotification
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber

class TripServiceActivityKt : AppCompatActivity(), OnMapReadyCallback {

    private var mainJobController = ThreadController.getMainScopeAndRootJob()
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
                        stopService()
                    }
                    false -> {
                        mapboxTripService.startService()
                        changeText()
                        toggleNotification.text = "Stop"
                        monitorNotificationActionButton(MapboxTripNotification.notificationActionButtonChannel)
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

        mapboxTripNotification = MapboxTripNotification(
            applicationContext,
            NavigationOptions.Builder(
                distanceFormatter = MapboxDistanceFormatter(
                    applicationContext,
                    "en",
                    METRIC,
                    ROUNDING_INCREMENT_FIFTY
                )
            ).timeFormatType(TWENTY_FOUR_HOURS).build()
        )

        // If you want to use Mapbox provided Service do this
        mapboxTripService = MapboxTripService(applicationContext, mapboxTripNotification)

        /*
        // else do this
        val intent = Intent(applicationContext, <Your_own_service>::class.java)
        mapboxTripService = MapboxTripService(mapboxTripNotification, {
            try {
                applicationContext.startService(intent)
            } catch (e: IllegalStateException) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    applicationContext.startForegroundService(intent)
                } else {
                    throw e
                }
            }
        }, {
            stopService(intent)
        })*/
    }

    private fun monitorNotificationActionButton(channel: ReceiveChannel<NotificationAction>) {
        mainJobController.scope.monitorChannelWithException(channel, { notificationAction ->
            when (notificationAction) {
                NotificationAction.END_NAVIGATION -> stopService()
            }
        })
    }

    private fun stopService() {
        textUpdateJob.cancel()
        mapboxTripService.stopService()
        toggleNotification.text = "Start"
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
        ThreadController.cancelAllNonUICoroutines()
        ThreadController.cancelAllUICoroutines()
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
        textUpdateJob = mainJobController.scope.launch {
            while (isActive) {
                val text = "Time elapsed: + ${SystemClock.elapsedRealtime()}"
                notifyTextView.text = text
                mapboxTripService.updateNotification(
                    RouteProgress.Builder()
                        .currentLegProgress(
                            RouteLegProgress.Builder()
                                .currentStepProgress(
                                    RouteStepProgress.Builder()
                                        .distanceRemaining(100f)
                                        .build()
                                ).build()
                        ).build()
                )
                Timber.i(text)
                delay(1000L)
            }
        }
    }
}
