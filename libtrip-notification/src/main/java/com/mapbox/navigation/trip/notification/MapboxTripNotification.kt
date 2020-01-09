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
import com.mapbox.navigation.base.banner.BannerInstruction
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.route.model.LegStepNavigation
import com.mapbox.navigation.base.route.model.RouteProgressNavigation
import com.mapbox.navigation.base.trip.TripNotification
import com.mapbox.navigation.trip.notification.utils.distance.DistanceFormatter
import com.mapbox.navigation.trip.notification.utils.time.TimeFormatter.formatTime
import com.mapbox.navigation.utils.END_NAVIGATION_ACTION
import com.mapbox.navigation.utils.NAVIGATION_NOTIFICATION_CHANNEL
import com.mapbox.navigation.utils.NOTIFICATION_CHANNEL
import com.mapbox.navigation.utils.NOTIFICATION_ID
import com.mapbox.navigation.utils.SET_BACKGROUND_COLOR
import com.mapbox.navigation.utils.extensions.ifNonNull
import java.util.Calendar

@MapboxNavigationModule(MapboxNavigationModuleType.TripNotification, skipConfiguration = true)
class MapboxTripNotification(
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

    override fun updateNotification(routeProgress: RouteProgressNavigation) {
        updateNotificationViews(routeProgress)
        notification = navigationNotificationProvider.buildNotification(getNotificationBuilder())
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    override fun onTripSessionStarted() {
        registerReceiver()
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

    private fun generateArrivalTime(
        routeProgress: RouteProgressNavigation,
        time: Calendar
    ): String? =
        ifNonNull(routeProgress.currentLegProgress()) { currentLegProgress ->
            val legDurationRemaining = currentLegProgress.durationRemaining()
            val timeFormatType = navigationOptions.timeFormatType()
            val arrivalTime = formatTime(
                time,
                legDurationRemaining,
                timeFormatType,
                DateFormat.is24HourFormat(applicationContext)
            )
            String.format(etaFormat, arrivalTime)
        }

    private fun updateNotificationViews(routeProgress: RouteProgressNavigation) {
        buildRemoteViews()
        updateInstructionText(routeProgress.bannerInstruction())
        updateDistanceText(routeProgress)
        generateArrivalTime(routeProgress, Calendar.getInstance())?.let { formattedTime ->
            updateViewsWithArrival(formattedTime)
            routeProgress.currentLegProgress()?.upComingStep()?.let { step ->
                updateManeuverImage(step)
            } ?: routeProgress.currentLegProgress()?.currentStep()
        }
    }

    private fun updateViewsWithArrival(time: String) {
        collapsedNotificationRemoteViews?.setTextViewText(R.id.notificationArrivalText, time)
        expandedNotificationRemoteViews?.setTextViewText(R.id.notificationArrivalText, time)
    }

    private fun updateInstructionText(bannerInstruction: BannerInstruction?) {
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

    private fun updateDistanceText(routeProgress: RouteProgressNavigation) {
        if (currentDistanceText == null || newDistanceText(routeProgress)) {
            currentDistanceText = ifNonNull(
                distanceFormatter,
                routeProgress.currentLegProgress()
            ) { distanceFormatter, routeLegProgress ->
                routeLegProgress.currentStepProgress()?.distanceRemaining()?.let {
                    distanceFormatter.formatDistance(it)
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

    private fun newDistanceText(routeProgress: RouteProgressNavigation) =
        ifNonNull(
            distanceFormatter,
            routeProgress.currentLegProgress(),
            currentDistanceText
        ) { distanceFormatter, currentLegProgress, currentDistanceText ->
            val item = currentLegProgress.currentStepProgress()?.distanceRemaining()
            // The call below can return an empty spanable string. toString() will cause a NPE and ?. will not catch it.
            val str = item?.let {
                distanceFormatter.formatDistance(it)
            }
            if (str == null) {
                val formattedDistance = str.toString()
                currentDistanceText.toString() != formattedDistance
            } else
                false
        } ?: false

    private fun updateManeuverImage(legStep: LegStepNavigation) {
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

    private fun getManeuverResource(step: LegStepNavigation): Int {
        val maneuver = step.stepManeuver()
        val maneuverType = maneuver?.type()
        maneuver?.let { stepManeuver ->
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
        // TODO: communicate back to MapboxTripSession to stop navigation.
    }
}
