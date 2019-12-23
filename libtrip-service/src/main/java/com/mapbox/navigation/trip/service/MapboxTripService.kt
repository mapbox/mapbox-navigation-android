package com.mapbox.navigation.trip.service

import android.content.Context
import android.content.Intent
import android.os.Build
import com.mapbox.annotation.navigation.module.MapboxNavigationModule
import com.mapbox.annotation.navigation.module.MapboxNavigationModuleType
import com.mapbox.navigation.base.trip.MapboxNotificationData
import com.mapbox.navigation.base.trip.RouteProgress
import com.mapbox.navigation.base.trip.TripNotification
import com.mapbox.navigation.base.trip.TripService
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import timber.log.Timber

@InternalCoroutinesApi
@ExperimentalCoroutinesApi
@MapboxNavigationModule(MapboxNavigationModuleType.TripService, skipConfiguration = true)
internal class MapboxTripService(
    private val tripNotification: TripNotification,
    private val applicationContext: Context
) : TripService {

    private val serviceStarted = AtomicBoolean(false)

    @InternalCoroutinesApi
    override fun startService() {
        if (!notificationDataChannel.isClosedForSend) {
            notificationDataChannel.close()
            notificationDataChannel = Channel(1)
        }
        when (serviceStarted.compareAndSet(false, true)) {
            true -> {
                createService()
                notificationDataChannel.offer(
                        MapboxNotificationData(tripNotification.getNotificationId(),
                                tripNotification.getNotification())
                )
            }
            false -> {
                Timber.i("service already started")
            }
        }
    }

    override fun updateNotification(routeProgress: RouteProgress) {
        tripNotification.updateNotification(routeProgress)
    }

    override fun stopService() {
        notificationDataChannel.close()
    }

    private fun createService() {
        val intent = Intent(applicationContext, NavigationNotificationService::class.java)
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

    companion object {
        private var notificationDataChannel = Channel<MapboxNotificationData>(1)
        fun getNotificationDataChannel(): ReceiveChannel<MapboxNotificationData> = notificationDataChannel
    }
}
