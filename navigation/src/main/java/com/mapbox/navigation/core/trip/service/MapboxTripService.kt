package com.mapbox.navigation.core.trip.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import com.mapbox.navigation.base.trip.model.TripNotificationState
import com.mapbox.navigation.base.trip.notification.TripNotification
import com.mapbox.navigation.utils.internal.ThreadController
import com.mapbox.navigation.utils.internal.logI
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
    private val initializeLambda: () -> Unit,
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
                applicationContext.startService(intent)
            } catch (e: IllegalStateException) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    applicationContext.startForegroundService(intent)
                } else {
                    throw e
                }
            }
        },
        {
            applicationContext.stopService(intent)
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
                tripNotification.onTripSessionStarted()
                initializeLambda()
                currentTripNotification = tripNotification
                updateNotificationData()
                allowedNotificationTime = SystemClock.elapsedRealtime() + 500
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
                terminateLambda()
                tripNotification.onTripSessionStopped()
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
