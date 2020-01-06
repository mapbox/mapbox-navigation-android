package com.mapbox.navigation.trip.service

import android.util.Log
import com.mapbox.navigation.base.trip.TripNotification
import com.mapbox.navigation.base.trip.model.MapboxNotificationData
import com.mapbox.navigation.base.trip.model.RouteProgress
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel

@InternalCoroutinesApi
@ExperimentalCoroutinesApi
class MapboxTripService(
    private val tripNotification: TripNotification,
    private val initializeLambda: () -> Unit
) : TripService {

    private val serviceStarted = AtomicBoolean(false)

    @InternalCoroutinesApi
    override fun startService() {
        if (notificationDataChannel.isClosedForSend) {
            notificationDataChannel = Channel(1)
        }
        when (serviceStarted.compareAndSet(false, true)) {
            true -> {
                tripNotification.onTripSessionStarted()
                initializeLambda()
                notificationDataChannel.offer(
                    MapboxNotificationData(
                        tripNotification.getNotificationId(),
                        tripNotification.getNotification()
                    )
                )
            }
            false -> {
                Log.i("MapboxTripService", "service already started")
            }
        }
    }

    override fun updateNotification(routeProgress: RouteProgress) {
        tripNotification.updateNotification(routeProgress)
    }

    override fun stopService() {
        notificationDataChannel.cancel()
        serviceStarted.set(false)
        tripNotification.onTripSessionStopped()
    }

    override fun hasServiceStarted() = serviceStarted.get()

    companion object {
        private var notificationDataChannel = Channel<MapboxNotificationData>(1)
        fun getNotificationDataChannel(): ReceiveChannel<MapboxNotificationData> =
            notificationDataChannel
    }
}
