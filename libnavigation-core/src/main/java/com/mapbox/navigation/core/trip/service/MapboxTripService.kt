package com.mapbox.navigation.core.trip.service

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.mapbox.navigation.base.trip.TripNotification
import com.mapbox.navigation.base.trip.model.MapboxNotificationData
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.utils.thread.ifChannelException
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel

// todo make internal
class MapboxTripService(
    private val tripNotification: TripNotification,
    private val initializeLambda: () -> Unit,
    private val terminateLambda: () -> Unit
) : TripService {

    companion object {
        private var notificationDataChannel = Channel<MapboxNotificationData>(1)
        internal fun getNotificationDataChannel(): ReceiveChannel<MapboxNotificationData> = notificationDataChannel
    }

    private constructor(
        applicationContext: Context,
        tripNotification: TripNotification,
        intent: Intent
    ) : this(
        tripNotification, {
            try {
                applicationContext.startService(intent)
            } catch (e: IllegalStateException) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    applicationContext.startForegroundService(intent)
                } else {
                    throw e
                }
            }
        }, {
            applicationContext.stopService(intent)
        })

    constructor(applicationContext: Context, tripNotification: TripNotification) :
        this(
            applicationContext,
            tripNotification,
            Intent(applicationContext, NavigationNotificationService::class.java)
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
                Log.i("MapboxTripService", "service already started")
            }
        }
    }

    override fun stopService() {
        when (serviceStarted.compareAndSet(true, false)) {
            true -> {
                notificationDataChannel.cancel()
                terminateLambda()
                tripNotification.onTripSessionStopped()
            }
            false -> {
                Log.i("MapboxTripService", "service is not started yet")
            }
        }
    }

    override fun updateNotification(routeProgress: RouteProgress) {
        tripNotification.updateNotification(routeProgress)
    }

    override fun hasServiceStarted() = serviceStarted.get()
}
