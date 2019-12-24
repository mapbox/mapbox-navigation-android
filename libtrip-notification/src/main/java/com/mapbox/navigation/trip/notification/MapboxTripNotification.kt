package com.mapbox.navigation.trip.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.mapbox.annotation.navigation.module.MapboxNavigationModule
import com.mapbox.annotation.navigation.module.MapboxNavigationModuleType
import com.mapbox.navigation.base.trip.RouteProgress
import com.mapbox.navigation.base.trip.TripNotification
import com.mapbox.navigation.utils.END_NAVIGATION_ACTION
import com.mapbox.navigation.utils.NAVIGATION_NOTIFICATION_CHANNEL
import com.mapbox.navigation.utils.NOTIFICATION_CHANNEL
import com.mapbox.navigation.utils.NOTIFICATION_ID
import com.mapbox.navigation.utils.SET_BACKGROUND_COLOR

@MapboxNavigationModule(MapboxNavigationModuleType.TripNotification, skipConfiguration = true)
class MapboxTripNotification(
    private val applicationContext: Context
) : TripNotification {
    private var collapsedNotificationRemoteViews: RemoteViews? = null
    private var expandedNotificationRemoteViews: RemoteViews? = null
    private var pendingOpenIntent: PendingIntent? = null
    private lateinit var navigationNotificationProvider: NavigationNotificationProvider
    private lateinit var notification: Notification
    private lateinit var notificationManager: NotificationManager

    private val notificationReceiver = object : BroadcastReceiver() {
        override fun onReceive(applicationContext: Context, intent: Intent) {
            // TODO: OZ/AK add code to handle when an action is performed on the notification
        }
    }

    init {
        applicationContext.getSystemService(Context.NOTIFICATION_SERVICE)
            ?.let { notificationService ->
                notificationManager = notificationService as NotificationManager
            } ?: throw (IllegalStateException("unable to create a NotificationManager"))

        pendingOpenIntent = createPendingOpenIntent(applicationContext)
        registerReceiver()
        createNotificationChannel()
    }

    constructor(
        context: Context,
        navigationNotificationProvider: NavigationNotificationProvider
    ) : this(context) {
        this.navigationNotificationProvider = navigationNotificationProvider
        this.notification =
            navigationNotificationProvider.buildNotification(getNotificationBuilder())
    }

    override fun getNotification() = notification

    override fun getNotificationId() = NOTIFICATION_ID

    override fun updateNotification(routeProgress: RouteProgress) {
        updateNotificationViews(routeProgress)
        notification = navigationNotificationProvider.buildNotification(getNotificationBuilder())
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    override fun onTripSessionStopped() {
        unregisterReceiver()
    }

    private fun registerReceiver() {
        applicationContext.registerReceiver(
            notificationReceiver,
            IntentFilter(END_NAVIGATION_ACTION)
        )
    }

    private fun unregisterReceiver() {
        applicationContext.unregisterReceiver(notificationReceiver)
        notificationManager.cancel(NOTIFICATION_ID)
    }

    private fun getNotificationBuilder(): NotificationCompat.Builder {
        val builder =
            NotificationCompat.Builder(applicationContext, NAVIGATION_NOTIFICATION_CHANNEL)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setSmallIcon(R.drawable.ic_navigation)
                .setCustomContentView(collapsedNotificationRemoteViews)
                .setCustomBigContentView(expandedNotificationRemoteViews)
                .setOngoing(true)

        pendingOpenIntent?.let { pendingOpenIntent ->
            builder.setContentIntent(pendingOpenIntent)
        }
        return builder
    }

    private fun buildRemoteViews() {
        val backgroundColor =
            ContextCompat.getColor(applicationContext, R.color.mapboxNotificationBlue)

        val collapsedLayout = R.layout.collapsed_navigation_notification_layout
        val collapsedLayoutId = R.id.navigationCollapsedNotificationLayout
        RemoteViews(applicationContext.packageName, collapsedLayout).also { remoteViews ->
            collapsedNotificationRemoteViews = remoteViews
            remoteViews.setInt(collapsedLayoutId, SET_BACKGROUND_COLOR, backgroundColor)
        }

        val expandedLayout = R.layout.expanded_navigation_notification_layout
        val expandedLayoutId = R.id.navigationExpandedNotificationLayout
        RemoteViews(applicationContext.packageName, expandedLayout).also { remoteViews ->
            expandedNotificationRemoteViews = remoteViews
            remoteViews.setInt(expandedLayoutId, SET_BACKGROUND_COLOR, backgroundColor)
        }
    }

    private fun createPendingOpenIntent(applicationContext: Context): PendingIntent? {
        val pm = applicationContext.packageManager
        val intent = pm.getLaunchIntentForPackage(applicationContext.packageName) ?: return null
        intent.setPackage(null)
        return PendingIntent.getActivity(applicationContext, 0, intent, 0)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                NAVIGATION_NOTIFICATION_CHANNEL,
                NOTIFICATION_CHANNEL,
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(notificationChannel)
        }
    }

    private fun updateNotificationViews(routeProgress: RouteProgress) {
        buildRemoteViews()
        updateData(routeProgress)
        // TODO:OZ,AK uncomment for full functionality of the NotificationService
//        updateInstructionText(routeProgress.bannerInstruction())
//        updateDistanceText(routeProgress)
//        val time = Calendar.getInstance()
//
//        generateArrivalTime(routeProgress, time)?.let { formattedTime ->
//            updateViewsWithArrival(formattedTime)
//            routeProgress.currentLegProgress()?.upComingStep()?.let { step ->
//                routeProgress.currentLegProgress()?.upComingStep()
//                updateManeuverImage(step)
//            } ?: routeProgress.currentLegProgress()?.currentStep()
//        }
    }

    private fun updateData(routeProgress: RouteProgress) {
        collapsedNotificationRemoteViews?.setTextViewText(
            R.id.notificationDistanceText,
            routeProgress.progress
        )
        expandedNotificationRemoteViews?.setTextViewText(
            R.id.notificationDistanceText,
            routeProgress.progress
        )
    }
}
