package com.mapbox.navigation.core.trip.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import com.mapbox.navigation.base.trip.model.TripNotificationState
import com.mapbox.navigation.base.trip.notification.TripNotification
import com.mapbox.navigation.utils.internal.ThreadController
import com.mapbox.navigation.utils.internal.logE
import com.mapbox.navigation.utils.internal.logI
import com.mapbox.navigation.utils.internal.logW
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Default [TripService] implementation
 *
 * @param tripNotification provide contract to communicate with notification
 * @param initializeLambda called when [TripService] has started
 * @param terminateLambda called when [TripService] has stopped
 */
internal class MapboxTripService(
    private val tripNotification: TripNotification,
    private val initializeLambda: () -> Boolean,
    private val terminateLambda: () -> Unit,
    threadController: ThreadController,
) : TripService {

    companion object {
        private const val LOG_CATEGORY = "MapboxTripService"

        private var currentTripNotification: TripNotification? = null

        private val notificationDataObservers = CopyOnWriteArraySet<NotificationDataObserver>()

        internal fun registerOneTimeNotificationDataObserver(observer: NotificationDataObserver) {
            currentTripNotification?.let { tripNotification ->
                val notificationData = MapboxNotificationData(
                    tripNotification.getNotificationId(),
                    tripNotification.getNotification(),
                )
                observer.onNotificationUpdated(notificationData)
                return
            }
            notificationDataObservers.add(observer)
        }

        internal fun unregisterOneTimeNotificationDataObserver(observer: NotificationDataObserver) {
            notificationDataObservers.remove(observer)
        }
    }

    private constructor(
        applicationContext: Context,
        tripNotification: TripNotification,
        intent: Intent,
        threadController: ThreadController,
    ) : this(
        tripNotification,
        {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    applicationContext.startForegroundService(intent)
                } else {
                    applicationContext.startService(intent)
                }
                true
            } catch (ex: Throwable) {
                logE("Foreground service could not be started: $ex. The most common reason " +
                    "for this exception is invoking `MapboxNavigation#startTripSession` " +
                    "when the app is in background.")
                false
            }
        },
        {
            try {
                applicationContext.stopService(intent)
            } catch (ex: Throwable) {
                logE("Foreground service could not be stopped: $ex.")
            }
        },
        threadController,
    )

    /**
     * Create [MapboxTripService] with Mapbox's [Service]
     *
     * @param applicationContext Context
     * @param tripNotification provide contract to communicate with notification
     */
    constructor(
        applicationContext: Context,
        tripNotification: TripNotification,
        threadController: ThreadController,
    ) : this(
        applicationContext,
        tripNotification,
        Intent(applicationContext, NavigationNotificationService::class.java),
        threadController,
    )

    private val serviceStarted = AtomicBoolean(false)

    private val mainJobController = threadController.getMainScopeAndRootJob()
    private var allowedNotificationTime = 0L
    private var notificationJob: Job? = null

    private fun updateNotificationData() {
        val notificationData = MapboxNotificationData(
            tripNotification.getNotificationId(),
            tripNotification.getNotification(),
        )
        notificationDataObservers.forEach { it.onNotificationUpdated(notificationData) }
        notificationDataObservers.clear()
    }

    /**
     * Start MapboxTripService
     */
    override fun startService() {
        when (serviceStarted.compareAndSet(false, true)) {
            true -> {
                if (initializeLambda()) {
                    tripNotification.onTripSessionStarted()
                    currentTripNotification = tripNotification
                    updateNotificationData()
                    allowedNotificationTime = SystemClock.elapsedRealtime() + 500
                } else {
                    serviceStarted.compareAndSet(true, false)
                }
            }
            false -> {
                logI("service already started", LOG_CATEGORY)
            }
        }
    }

    /**
     * Stop MapboxTripService
     */
    override fun stopService() {
        when (serviceStarted.compareAndSet(true, false)) {
            true -> {
                currentTripNotification = null
                notificationJob?.cancel()
                tripNotification.onTripSessionStopped()
                terminateLambda()
            }
            false -> {
                logI("Service is not started yet", LOG_CATEGORY)
            }
        }
    }

    /**
     * Update the trip's information in the notification bar
     */
    override fun updateNotification(tripNotificationState: TripNotificationState) {
        if (!serviceStarted.get()) {
            logW("Cannot update notification: service has not been started yet.")
            return
        }
        notificationJob?.cancel()
        if (SystemClock.elapsedRealtime() >= allowedNotificationTime) {
            tripNotification.updateNotification(tripNotificationState)
        } else {
            notificationJob = mainJobController.scope.launch {
                delay(allowedNotificationTime - SystemClock.elapsedRealtime())
                tripNotification.updateNotification(tripNotificationState)
            }
        }
    }

    /**
     * Return *true* if service is started
     */
    override fun hasServiceStarted() = serviceStarted.get()
}
