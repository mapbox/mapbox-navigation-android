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
import android.text.SpannableString
import android.text.TextUtils
import android.text.format.DateFormat
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.mapbox.annotation.navigation.module.MapboxNavigationModule
import com.mapbox.annotation.navigation.module.MapboxNavigationModuleType
import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.api.directions.v5.models.LegStep
import com.mapbox.navigation.base.formatter.DistanceFormatter
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.trip.TripNotification
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.trip.notification.utils.time.TimeFormatter.formatTime
import com.mapbox.navigation.utils.END_NAVIGATION_ACTION
import com.mapbox.navigation.utils.NAVIGATION_NOTIFICATION_CHANNEL
import com.mapbox.navigation.utils.NOTIFICATION_CHANNEL
import com.mapbox.navigation.utils.NOTIFICATION_ID
import com.mapbox.navigation.utils.SET_BACKGROUND_COLOR
import com.mapbox.navigation.utils.extensions.ifNonNull
import java.util.Calendar
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.channels.ReceiveChannel

@MapboxNavigationModule(MapboxNavigationModuleType.TripNotification, skipConfiguration = true)
class MapboxTripNotification internal constructor(
    private val applicationContext: Context,
    private val distanceFormatter: DistanceFormatter,
    navigationOptionsBuilder: NavigationOptions.Builder,
    private val navigationNotificationProvider: NavigationNotificationProvider
) : TripNotification {
    private var currentManeuverId = 0
    private var instructionText: String? = null
    private var currentDistanceText: SpannableString? = null
    private var collapsedNotificationRemoteViews: RemoteViews? = null
    private var expandedNotificationRemoteViews: RemoteViews? = null
    private var pendingOpenIntent: PendingIntent? = null
    private var pendingCloseIntent: PendingIntent? = null
    private val etaFormat: String = applicationContext.getString(R.string.eta_format)
    private val navigationOptions: NavigationOptions = navigationOptionsBuilder.build()
    private lateinit var notification: Notification
    private lateinit var notificationManager: NotificationManager

    private val notificationReceiver = object : BroadcastReceiver() {
        override fun onReceive(applicationContext: Context, intent: Intent) {
            this@MapboxTripNotification.onEndNavigationBtnClick()
        }
    }

    init {
        applicationContext.getSystemService(Context.NOTIFICATION_SERVICE)
            ?.let { notificationService ->
                notificationManager = notificationService as NotificationManager
            } ?: throw (IllegalStateException("unable to create a NotificationManager"))

        pendingOpenIntent = createPendingOpenIntent(applicationContext)
        pendingCloseIntent = createPendingCloseIntent(applicationContext)
        registerReceiver()
        createNotificationChannel()
    }

    override fun getNotification(): Notification {
        if (!::notification.isInitialized) {
            this.notification =
                navigationNotificationProvider.buildNotification(getNotificationBuilder())
        }
        return this.notification
    }

    override fun getNotificationId(): Int = NOTIFICATION_ID

    override fun updateNotification(routeProgress: RouteProgress) {
        updateNotificationViews(routeProgress)
        notification = navigationNotificationProvider.buildNotification(getNotificationBuilder())
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    override fun onTripSessionStarted() {
        registerReceiver()
    }

    override fun onTripSessionStopped() {
        unregisterReceiver()
        try {
            notificationActionButtonChannel.cancel()
        } catch (e: Exception) {
            when (e) {
                is ClosedReceiveChannelException,
                is ClosedSendChannelException -> {
                }
                else -> {
                    throw e
                }
            }
        }
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
            remoteViews.setOnClickPendingIntent(R.id.endNavigationBtn, pendingCloseIntent)
            remoteViews.setInt(expandedLayoutId, SET_BACKGROUND_COLOR, backgroundColor)
        }
    }

    private fun createPendingOpenIntent(applicationContext: Context): PendingIntent? {
        val pm = applicationContext.packageManager
        val intent = pm.getLaunchIntentForPackage(applicationContext.packageName) ?: return null
        intent.setPackage(null)
        return PendingIntent.getActivity(applicationContext, 0, intent, 0)
    }

    private fun createPendingCloseIntent(applicationContext: Context): PendingIntent? {
        val endNavigationBtn = Intent(END_NAVIGATION_ACTION)
        return PendingIntent.getBroadcast(applicationContext, 0, endNavigationBtn, 0)
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

    private fun generateArrivalTime(
        routeProgress: RouteProgress,
        time: Calendar
    ): String? =
        ifNonNull(routeProgress.currentLegProgress()) { currentLegProgress ->
            val legDurationRemaining = currentLegProgress.durationRemaining()
            val timeFormatType = navigationOptions.timeFormatType()
            val arrivalTime = formatTime(
                time,
                legDurationRemaining.toDouble(),
                timeFormatType,
                DateFormat.is24HourFormat(applicationContext)
            )
            String.format(etaFormat, arrivalTime)
        }

    private fun updateNotificationViews(routeProgress: RouteProgress) {
        buildRemoteViews()
        updateInstructionText(routeProgress.bannerInstructions())
        updateDistanceText(routeProgress)
        generateArrivalTime(routeProgress, Calendar.getInstance())?.let { formattedTime ->
            updateViewsWithArrival(formattedTime)
            routeProgress.currentLegProgress()?.upcomingStep()?.let { step ->
                updateManeuverImage(step)
            } ?: routeProgress.currentLegProgress()?.currentStepProgress()?.step()
        }
    }

    private fun updateViewsWithArrival(time: String) {
        collapsedNotificationRemoteViews?.setTextViewText(R.id.notificationArrivalText, time)
        expandedNotificationRemoteViews?.setTextViewText(R.id.notificationArrivalText, time)
    }

    private fun updateInstructionText(bannerInstruction: BannerInstructions?) {
        bannerInstruction?.let { bannerIns ->
            val primaryText = bannerIns.primary().text()
            if (instructionText.isNullOrEmpty() || instructionText != primaryText) {
                collapsedNotificationRemoteViews?.setTextViewText(
                    R.id.notificationInstructionText, primaryText
                )
                expandedNotificationRemoteViews?.setTextViewText(
                    R.id.notificationInstructionText, primaryText
                )
                instructionText = primaryText
            }
        }
    }

    private fun updateDistanceText(routeProgress: RouteProgress) {
        if (currentDistanceText == null || newDistanceText(routeProgress)) {
            currentDistanceText = ifNonNull(
                distanceFormatter,
                routeProgress.currentLegProgress()
            ) { distanceFormatter, routeLegProgress ->
                routeLegProgress.currentStepProgress()?.distanceRemaining()?.let {
                    distanceFormatter.formatDistance(it.toDouble())
                }
            }
            collapsedNotificationRemoteViews?.setTextViewText(
                R.id.notificationDistanceText,
                currentDistanceText
            )
            expandedNotificationRemoteViews?.setTextViewText(
                R.id.notificationDistanceText,
                currentDistanceText
            )
        }
    }

    private fun newDistanceText(routeProgress: RouteProgress) =
        ifNonNull(
            distanceFormatter,
            routeProgress.currentLegProgress(),
            currentDistanceText
        ) { distanceFormatter, currentLegProgress, currentDistanceText ->
            val item = currentLegProgress.currentStepProgress()?.distanceRemaining()
            // The call below can return an empty spanable string. toString() will cause a NPE and ?. will not catch it.
            val str = item?.let {
                distanceFormatter.formatDistance(it.toDouble())
            }
            if (str == null) {
                val formattedDistance = str.toString()
                currentDistanceText.toString() != formattedDistance
            } else
                false
        } ?: false

    private fun updateManeuverImage(legStep: LegStep) {
        val maneuverImageId = getManeuverResource(legStep)
        if (maneuverImageId != currentManeuverId) {
            currentManeuverId = maneuverImageId
            collapsedNotificationRemoteViews?.setImageViewResource(
                R.id.maneuverImage,
                maneuverImageId
            )
            expandedNotificationRemoteViews?.setImageViewResource(
                R.id.maneuverImage,
                maneuverImageId
            )
        }
    }

    private fun getManeuverResource(step: LegStep): Int {
        val maneuver = step.maneuver()
        val maneuverType = maneuver.type()
        maneuver.let { stepManeuver ->
            val maneuverModifier = stepManeuver.modifier()
            if (!TextUtils.isEmpty(maneuverModifier)) {
                val drivingSide = step.drivingSide()
                val isLeftSideDriving = isLeftDrivingSideAndRoundaboutOrRotaryOrUturn(
                    maneuverType,
                    maneuverModifier,
                    drivingSide
                )
                return when (isLeftSideDriving) {
                    true -> {
                        ManeuverResource.obtainManeuverResource(
                            maneuverType + maneuverModifier + drivingSide
                        )
                    }
                    else -> {
                        ManeuverResource.obtainManeuverResource(
                            maneuverType + maneuverModifier
                        )
                    }
                }
            }
        }
        return ManeuverResource.obtainManeuverResource(maneuverType)
    }

    private fun isLeftDrivingSideAndRoundaboutOrRotaryOrUturn(
        maneuverType: String?,
        maneuverModifier: String?,
        drivingSide: String?
    ): Boolean {
        return STEP_MANEUVER_MODIFIER_LEFT == drivingSide &&
            (STEP_MANEUVER_TYPE_ROUNDABOUT == maneuverType ||
                STEP_MANEUVER_TYPE_ROTARY == maneuverType ||
                STEP_MANEUVER_MODIFIER_UTURN == maneuverModifier
                )
    }

    private fun onEndNavigationBtnClick() {
        try {
            notificationActionButtonChannel.offer(NotificationAction.END_NAVIGATION)
        } catch (e: Exception) {
            when (e) {
                is ClosedReceiveChannelException,
                is ClosedSendChannelException -> {
                    notificationActionButtonChannel = Channel(1)
                    notificationActionButtonChannel.offer(NotificationAction.END_NAVIGATION)
                }
                else -> {
                    throw e
                }
            }
        }
    }

    companion object {
        private var notificationActionButtonChannel = Channel<NotificationAction>(1)
        fun getNotificationActionButtonChannel(): ReceiveChannel<NotificationAction> = notificationActionButtonChannel
    }
}
