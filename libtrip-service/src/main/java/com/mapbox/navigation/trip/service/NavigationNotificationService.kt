package com.mapbox.navigation.trip.service

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.os.IBinder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@InternalCoroutinesApi
@ExperimentalCoroutinesApi
internal class NavigationNotificationService : Service() {

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundNotification()
        return START_STICKY
    }

    override fun onDestroy() {
        stopForeground(true)
        super.onDestroy()
    }

    private fun startForegroundNotification() {
        serviceScope.launch {
            while(!MapboxTripService.getNotificationDataChannel().isClosedForReceive){
                val notificationResponse = MapboxTripService.getNotificationDataChannel().receive()
                notificationResponse.notification.flags = Notification.FLAG_FOREGROUND_SERVICE
                startForeground(notificationResponse.notificationID, notificationResponse.notification)
            }
        }
    }

    companion object {
        private val job = SupervisorJob()
        val serviceScope = CoroutineScope(job + Dispatchers.IO)
    }
}
