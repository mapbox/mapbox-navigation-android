package com.mapbox.navigation.trip.service

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineCallback
import com.mapbox.android.core.location.LocationEngineRequest
import com.mapbox.android.core.location.LocationEngineResult
import com.mapbox.navigation.base.trip.NavigationTripDescriptor
import com.mapbox.navigation.navigator.MapboxNativeNavigatorImpl
import com.mapbox.navigation.utils.extensions.ifNonNull
import java.util.Date
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.NonCancellable.isActive
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.whileSelect
const val UPDATE_INTERVAL_IN_MILLISECONDS: Long = 1000
const val FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS: Long = 500

@InternalCoroutinesApi
@ExperimentalCoroutinesApi
internal class NavigationNotificationService : Service() {

    private var locationEngineCallback = object : LocationEngineCallback<LocationEngineResult> {
        override fun onSuccess(result: LocationEngineResult?) {
        }

        override fun onFailure(exception: Exception) {
        }
    }
    private var locationEngine: LocationEngine? = null
    private val locationEngineRequest: LocationEngineRequest = LocationEngineRequest.Builder(UPDATE_INTERVAL_IN_MILLISECONDS)
            .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
            .setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS)
            .build()

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        startForegroundNotification()
        notificationChannel.close()
        notificationChannel = ConflatedBroadcastChannel()
        serviceScope.launch {
            while (isActive) {
                val status = MapboxNativeNavigatorImpl.getStatus(Date()) // Request that will drive the Polling functionality
                locationEngine?.getLastLocation(object : LocationEngineCallback<LocationEngineResult> {
                    override fun onSuccess(result: LocationEngineResult?) {
                        ifNonNull(result, result?.lastLocation,
                                { _, rawLocation ->
                                    notificationChannel.offer(NavigationTripDescriptor(
                                            status.routeProgress, rawLocation, status.enhancedLocation))
                                })
                    }

                    override fun onFailure(exception: Exception) {
                    }
                })
                delay(UPDATE_INTERVAL_IN_MILLISECONDS) // Poll every second
            }
        }
        locationEngine?.requestLocationUpdates(locationEngineRequest, locationEngineCallback, null)
        super.onCreate()
    }

    override fun onDestroy() {
        stopForeground(true)
        notificationChannel.close()
        super.onDestroy()
    }

    private fun startForegroundNotification() {
        serviceScope.launch {
            whileSelect {
                MapboxTripService.getNotificationDataChannel().onReceiveOrClosed { notificationResponse ->
                    when (notificationResponse.isClosed) {
                        true -> {
                            false
                        }
                        false -> {
                            val notification = notificationResponse.value.notification
                            notification.flags = Notification.FLAG_FOREGROUND_SERVICE
                            startForeground(notificationResponse.value.notificationID, notification)
                            true
                        }
                    }
                }
            }
        }
    }

    companion object {
        private val job = SupervisorJob()
        val serviceScope = CoroutineScope(job + Dispatchers.IO)
        private var notificationChannel = ConflatedBroadcastChannel<NavigationTripDescriptor>()
        fun getNotificationChannel() = notificationChannel.openSubscription()
    }
}
