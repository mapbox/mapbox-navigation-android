package com.mapbox.navigation.trip.service

import com.mapbox.annotation.navigation.module.MapboxNavigationModule
import com.mapbox.annotation.navigation.module.MapboxNavigationModuleType
import com.mapbox.navigation.base.trip.MapboxNotificationData
import com.mapbox.navigation.base.trip.TripNotification
import com.mapbox.navigation.base.trip.TripService
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 *TripServiceImpl(val callback:() ->Unit, private val context: Context)
 * The callback() must contain at least the following code:

val intent: Intent = Intent(context, NavigationNotificationService::class.java)
try {
context.startService(intent)
} catch (e: IllegalStateException) {
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
context.startForegroundService(intent)
} else {
throw e
}
}
private val navigator: MapboxNativeNavigator,

 */

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
                NavigationNotificationService.serviceScope.launch {
                    monitorRouteProgress()
                }
                callback()
                notificationDataChannel.offer(MapboxNotificationData(tripNotification.getNotificationId(), tripNotification.getNotification()))
            }
            false -> {
                Timber.i("service already started")
            }
        }
    }

    private suspend fun monitorRouteProgress() {
        val channel = NavigationNotificationService.getUpdateNotificationChannel()
        while (when (!channel.isClosedForReceive) {
                    true -> {
                        val routeData = channel.receive()
                        tripNotification.updateNotification(routeData)
                        true
                    }
                    false -> {
                        false
                    }
                }
        ) {
        }
    }

    override fun stopService() {
        notificationDataChannel.close()
    }

    companion object {
        private var notificationDataChannel = ConflatedBroadcastChannel<MapboxNotificationData>()
        fun getNotificationDataChannel() = notificationDataChannel.openSubscription()
    }
}
