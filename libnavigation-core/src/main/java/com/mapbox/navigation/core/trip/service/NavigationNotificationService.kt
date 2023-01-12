package com.mapbox.navigation.core.trip.service

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.annotation.CallSuper
import androidx.core.app.ServiceCompat
import com.mapbox.navigation.core.internal.dump.MapboxDumpHandler
import com.mapbox.navigation.core.internal.dump.MapboxDumpRegistry
import java.io.FileDescriptor
import java.io.PrintWriter

/**
 * Service is updating information about current trip
 */
internal class NavigationNotificationService : Service() {

    private val notificationDataObserver = NotificationDataObserver { notificationResponse ->
        notificationResponse.notification.flags = Notification.FLAG_FOREGROUND_SERVICE
        startForeground(notificationResponse.notificationId, notificationResponse.notification)
    }

    /**
     * This will handle commands from `adb shell dumpsys activity service`.
     *
     * Use the [MapboxDumpRegistry] to add or remove dump interceptors.
     */
    private val mapboxDumpHandler = MapboxDumpHandler()

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
        MapboxTripService.registerOneTimeNotificationDataObserver(notificationDataObserver)
        return START_STICKY
    }

    /**
     * Destroy [Service]. Called via [Context.stopService] or [stopSelf]
     */
    override fun onDestroy() {
        super.onDestroy()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        MapboxTripService.unregisterOneTimeNotificationDataObserver(notificationDataObserver)
    }

    /**
     * Overrides the dump command so that state can be changed with adb.
     */
    @CallSuper
    override fun dump(fd: FileDescriptor, writer: PrintWriter, args: Array<String>?) {
        mapboxDumpHandler.handle(fd, writer, args)
    }
}
