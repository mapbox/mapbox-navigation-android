package com.mapbox.navigation.core.trip.service

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.mapbox.navigation.utils.thread.ThreadController
import com.mapbox.navigation.utils.thread.monitorChannelWithException
import kotlinx.coroutines.cancelChildren

class NavigationNotificationService : Service() {
    private val ioJobController by lazy {
        ThreadController.getIOScopeAndRootJob()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundNotification()
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        stopForeground(true)
        ioJobController.job.cancelChildren()
    }

    private fun startForegroundNotification() {
        ioJobController.scope.monitorChannelWithException(MapboxTripService.getNotificationDataChannel()) { notificationResponse ->
            notificationResponse.notification.flags = Notification.FLAG_FOREGROUND_SERVICE
            startForeground(notificationResponse.notificationId, notificationResponse.notification)
        }
    }
}
