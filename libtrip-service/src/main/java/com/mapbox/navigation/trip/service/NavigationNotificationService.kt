package com.mapbox.navigation.trip.service

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.mapbox.navigation.base.trip.RouteProgress
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.whileSelect

@InternalCoroutinesApi
@ExperimentalCoroutinesApi
internal class NavigationNotificationService : Service() {

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        startForegroundNotification()
        super.onCreate()
    }

    override fun onDestroy() {
        stopForeground(true)
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

        private val updateNotificationChannel = ConflatedBroadcastChannel<RouteProgress>()
        fun getUpdateNotificationChannel() = updateNotificationChannel.openSubscription()
    }
}
