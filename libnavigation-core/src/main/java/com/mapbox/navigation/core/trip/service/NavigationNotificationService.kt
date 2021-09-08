package com.mapbox.navigation.core.trip.service

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import com.mapbox.navigation.core.telemetry.MapboxNavigationTelemetry

/**
 * Service is updating information about current trip
 */
internal class NavigationNotificationService : Service() {

    private val notificationDataObserver = NotificationDataObserver { notificationResponse ->
        notificationResponse.notification.flags = Notification.FLAG_FOREGROUND_SERVICE
        startForeground(notificationResponse.notificationId, notificationResponse.notification)
    }

    /**
     * Return the communication channel to the service (always *null*)
     */
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    /**
     * Start [Service]. Called via [Context.startForegroundService]
     *
     * @param intent Intent
     * @param flags Int
     * @param startId Int
     * @return Int
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        MapboxNavigationTelemetry.setApplicationInstance(application)
        MapboxTripService.registerOneTimeNotificationDataObserver(notificationDataObserver)
        return START_STICKY
    }

    /**
     * Destroy [Service]. Called via [Context.stopService] or [stopSelf]
     */
    override fun onDestroy() {
        super.onDestroy()
        stopForeground(true)
        MapboxTripService.unregisterOneTimeNotificationDataObserver(notificationDataObserver)
    }
}
