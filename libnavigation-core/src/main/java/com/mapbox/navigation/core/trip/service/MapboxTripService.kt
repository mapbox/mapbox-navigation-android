package com.mapbox.navigation.core.trip.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import com.mapbox.base.common.logger.Logger
import com.mapbox.base.common.logger.model.Message
import com.mapbox.navigation.base.trip.model.TripNotificationState
import com.mapbox.navigation.base.trip.notification.TripNotification
import com.mapbox.navigation.utils.internal.ifChannelException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Default [TripService] implementation
 *
 * @param tripNotification provide contract to communicate with notification
 * @param initializeLambda called when [TripService] has started
 * @param terminateLambda called when [TripService] has stopped
 * @param logger interface for logging any events
 */
internal class MapboxTripService(
    private val tripNotification: TripNotification,
    private val initializeLambda: () -> Unit,
    private val terminateLambda: () -> Unit,
    private val logger: Logger
) : TripService {

    companion object {

        private var notificationDataChannel = Channel<MapboxNotificationData>(1)
        internal fun getNotificationDataChannel(): ReceiveChannel<MapboxNotificationData> =
            notificationDataChannel
    }

    private constructor(
        applicationContext: Context,
        tripNotification: TripNotification,
        intent: Intent,
        logger: Logger
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
        logger
    )

    /**
     * Create [MapboxTripService] with Mapbox's [Service]
     *
     * @param applicationContext Context
     * @param tripNotification provide contract to communicate with notification
     * @param logger interface for logging any events
     */
    constructor(
        applicationContext: Context,
        tripNotification: TripNotification,
        logger: Logger
    ) : this(
        applicationContext,
        tripNotification,
        Intent(applicationContext, NavigationNotificationService::class.java),
        logger
    )

    private val serviceStarted = AtomicBoolean(false)

    private fun postDataToChannel() {
        notificationDataChannel.offer(
            MapboxNotificationData(
                tripNotification.getNotificationId(),
                tripNotification.getNotification()
            )
        )
    }

    /**
     * Start MapboxTripService
     */
    override fun startService() {
        when (serviceStarted.compareAndSet(false, true)) {
            true -> {
                tripNotification.onTripSessionStarted()
                initializeLambda()
                try {
                    postDataToChannel()
                } catch (e: Exception) {
                    e.ifChannelException {
                        notificationDataChannel = Channel(1)
                        postDataToChannel()
                    }
                }
            }
            false -> {
                logger.i(msg = Message("service already started"))
            }
        }
    }

    /**
     * Stop MapboxTripService
     */
    override fun stopService() {
        when (serviceStarted.compareAndSet(true, false)) {
            true -> {
                notificationDataChannel.cancel()
                terminateLambda()
                tripNotification.onTripSessionStopped()
            }
            false -> {
                logger.i(msg = Message("Service is not started yet"))
            }
        }
    }

    /**
     * Update the trip's information in the notification bar
     */
    override fun updateNotification(tripNotificationState: TripNotificationState) {
        tripNotification.updateNotification(tripNotificationState)
    }

    /**
     * Return *true* if service is started
     */
    override fun hasServiceStarted() = serviceStarted.get()
}
