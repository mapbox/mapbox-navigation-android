package com.mapbox.navigation.trip.service

import com.mapbox.annotation.navigation.module.MapboxNavigationModule
import com.mapbox.annotation.navigation.module.MapboxNavigationModuleType
import com.mapbox.navigation.base.trip.MapboxNotificationData
import com.mapbox.navigation.base.trip.RouteProgress
import com.mapbox.navigation.base.trip.TripNotification
import com.mapbox.navigation.base.trip.TripService
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import timber.log.Timber

@InternalCoroutinesApi
@ExperimentalCoroutinesApi
@MapboxNavigationModule(MapboxNavigationModuleType.TripService, skipConfiguration = true)
internal class MapboxTripService(
    private val tripNotification: TripNotification,
    private val callback: () -> Unit
) : TripService {

    private val serviceStarted = AtomicBoolean(false)

    @InternalCoroutinesApi
    override fun startService() {
        if (!notificationDataChannel.isClosedForSend) {
            notificationDataChannel.close()
        }
        notificationDataChannel = ConflatedBroadcastChannel()
        when (serviceStarted.compareAndSet(false, true)) {
            true -> {
                callback()
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

    companion object {
        private var notificationDataChannel = ConflatedBroadcastChannel<MapboxNotificationData>()
        fun getNotificationDataChannel() = notificationDataChannel.openSubscription()
    }
}
