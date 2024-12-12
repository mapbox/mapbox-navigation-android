package com.mapbox.navigation.core.trip.service

import android.annotation.SuppressLint
import android.app.ForegroundServiceStartNotAllowedException
import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.annotation.CallSuper
import androidx.core.app.ServiceCompat
import com.mapbox.navigation.core.internal.dump.MapboxDumpHandler
import com.mapbox.navigation.core.internal.dump.MapboxDumpRegistry
import com.mapbox.navigation.utils.internal.logE
import java.io.FileDescriptor
import java.io.PrintWriter

/**
 * Service is updating information about current trip
 */
internal class NavigationNotificationService : Service() {

    // Unknown types in catch blocks work fine on platforms where they are not defined.
    @SuppressLint("NewApi")
    private val notificationDataObserver = NotificationDataObserver { notificationResponse ->
        try {
            notificationResponse.notification.flags = Notification.FLAG_FOREGROUND_SERVICE
            startForeground(
                notificationResponse.notificationId,
                notificationResponse.notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION,
            )
        } catch (e: ForegroundServiceStartNotAllowedException) {
            // Even if startForeground is called in Activity.onResume callback there is a chance
            // that the application will be moved to background when this RPC call reaches
            // the ActivityManager on the OS side.
            // There is a simple way to reproduce it:
            // 1. Put a breakpoint on the line 26 with startForeground call above.
            // 2. Start the app with the debugger attached.
            // 3. When the app is suspended on that breakpoint, move the app to background
            // 4. Wait for a few seconds
            // 5. Continue execution
            logE("ForegroundServiceStartNotAllowedException: ${e.message}")
        }
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
