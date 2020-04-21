package com.mapbox.navigation.trip.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.PointF
import android.os.Build
import android.text.SpannableString
import android.text.TextUtils
import android.text.format.DateFormat
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.util.Pair
import com.mapbox.annotation.module.MapboxModule
import com.mapbox.annotation.module.MapboxModuleType
import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.api.directions.v5.models.BannerText
import com.mapbox.api.directions.v5.models.StepManeuver.StepManeuverType
import com.mapbox.navigation.base.formatter.DistanceFormatter
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.notification.NotificationAction
import com.mapbox.navigation.base.trip.notification.TripNotification
import com.mapbox.navigation.trip.notification.maneuver.ManeuverIconHelper.DEFAULT_ROUNDABOUT_ANGLE
import com.mapbox.navigation.trip.notification.maneuver.ManeuverIconHelper.MANEUVER_ICON_DRAWER_MAP
import com.mapbox.navigation.trip.notification.maneuver.ManeuverIconHelper.MANEUVER_TYPES_WITH_NULL_MODIFIERS
import com.mapbox.navigation.trip.notification.maneuver.ManeuverIconHelper.ROUNDABOUT_MANEUVER_TYPES
import com.mapbox.navigation.trip.notification.maneuver.ManeuverIconHelper.adjustRoundaboutAngle
import com.mapbox.navigation.trip.notification.maneuver.ManeuverIconHelper.isManeuverIconNeedFlip
import com.mapbox.navigation.trip.notification.maneuver.STEP_MANEUVER_MODIFIER_RIGHT
import com.mapbox.navigation.trip.notification.maneuver.STEP_MANEUVER_TYPE_ARRIVE
import com.mapbox.navigation.trip.notification.utils.time.TimeFormatter.formatTime
import com.mapbox.navigation.utils.internal.END_NAVIGATION_ACTION
import com.mapbox.navigation.utils.internal.NAVIGATION_NOTIFICATION_CHANNEL
import com.mapbox.navigation.utils.internal.NOTIFICATION_CHANNEL
import com.mapbox.navigation.utils.internal.NOTIFICATION_ID
import com.mapbox.navigation.utils.internal.SET_BACKGROUND_COLOR
import com.mapbox.navigation.utils.internal.ifChannelException
import com.mapbox.navigation.utils.internal.ifNonNull
import java.util.Calendar
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.channels.ClosedSendChannelException

/**
 * Default implementation of [TripNotification] interface
 *
 * @param applicationContext the application's [Context]
 * @param navigationOptions is [NavigationOptions] used here to format distance and time
 *
 * @property currentManeuverType This indicates the type of current maneuver. The same [BannerText.type] of primary [BannerInstructions]
 * @property currentManeuverModifier This indicates the mode of the maneuver. The same [BannerText.modifier] of primary [BannerInstructions]
 */
@MapboxModule(MapboxModuleType.NavigationTripNotification)
class MapboxTripNotification constructor(
    private val applicationContext: Context,
    private val navigationOptions: NavigationOptions
) : TripNotification {

    companion object {
        /**
         * Broadcast of [MapboxTripNotification] actions
         */
        var notificationActionButtonChannel = Channel<NotificationAction>(1)
    }

    @StepManeuverType
    var currentManeuverType: String? = null
        private set
    var currentManeuverModifier: String? = null
        private set
    private var currentRoundaboutAngle = DEFAULT_ROUNDABOUT_ANGLE

    private var currentInstructionText: String? = null
    private var currentDistanceText: SpannableString? = null
    private var collapsedNotificationRemoteViews: RemoteViews? = null
    private var expandedNotificationRemoteViews: RemoteViews? = null
    private var pendingOpenIntent: PendingIntent? = null
    private var pendingCloseIntent: PendingIntent? = null
    private val etaFormat: String = applicationContext.getString(R.string.eta_format)
    private val navigationNotificationProvider = NavigationNotificationProvider
    private val notificationReceiver = NotificationActionReceiver()
    private val distanceFormatter: DistanceFormatter =
        navigationOptions.distanceFormatter
            ?: throw IllegalArgumentException("Distance formatter is required.")
    private lateinit var notification: Notification
    private lateinit var notificationManager: NotificationManager

    init {
        applicationContext.getSystemService(Context.NOTIFICATION_SERVICE)
            ?.let { notificationService ->
                notificationManager = notificationService as NotificationManager
            } ?: throw (IllegalStateException("unable to create a NotificationManager"))

        pendingOpenIntent = createPendingOpenIntent(applicationContext)
        pendingCloseIntent = createPendingCloseIntent(applicationContext)
        buildRemoteViews()
        createNotificationChannel()
    }

    /**
     * Provides a custom [Notification] to launch
     * with the [TripSession], specifically
     * [android.app.Service.startForeground].
     *
     * @return a custom notification
     */
    override fun getNotification(): Notification {
        if (!::notification.isInitialized) {
            this.notification =
                navigationNotificationProvider.buildNotification(getNotificationBuilder())
        }
        return this.notification
    }

    /**
     * An integer id that will be used to start this notification from [TripSession] with
     * [android.app.Service.startForeground].
     *
     * @return an int id specific to the notification
     */
    override fun getNotificationId(): Int = NOTIFICATION_ID

    /**
     * If enabled, this method will be called every time a
     * new [RouteProgress] is generated.
     *
     * This method can serve as a cue to update a [Notification]
     * with a specific notification id.
     *
     * @param routeProgress with the latest progress data
     */
    override fun updateNotification(routeProgress: RouteProgress) {
        // RemoteView has an internal mActions, which stores every change and cannot be cleared.
        // As we set new bitmaps, the mActions parcelable size will grow and eventually cause a crash.
        // buildRemoteViews() will rebuild the RemoteViews and clear the stored mActions.
        buildRemoteViews()
        updateNotificationViews(routeProgress)
        notification = navigationNotificationProvider.buildNotification(getNotificationBuilder())
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    /**
     * Called when TripSession starts via [TripSession.start]
     *
     * This callback may be used to perform any actions after the trip session is initialized.
     */
    override fun onTripSessionStarted() {
        registerReceiver()
        notificationActionButtonChannel = Channel(1)
    }

    /**
     * Called when TripSession spos via [TripSession.stop]
     *
     * This callback may be used to clean up any listeners or receivers, preventing leaks.
     */
    override fun onTripSessionStopped() {
        currentManeuverType = null
        currentManeuverModifier = null
        currentInstructionText = null
        currentDistanceText = null

        collapsedNotificationRemoteViews?.apply {
            setTextViewText(R.id.notificationDistanceText, "")
            setTextViewText(R.id.notificationArrivalText, "")
            setTextViewText(R.id.notificationInstructionText, "")
        }

        expandedNotificationRemoteViews?.apply {
            setTextViewText(R.id.notificationDistanceText, "")
            setTextViewText(R.id.notificationArrivalText, "")
            setTextViewText(R.id.notificationInstructionText, "")
            setTextViewText(R.id.endNavigationBtnText, "")
        }

        unregisterReceiver()
        try {
            notificationActionButtonChannel.cancel()
        } catch (e: Exception) {
            e.ifChannelException {
                // Do nothing
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

    /**
     * Creates live and interactive views displayed in the notification's layout.
     */
    private fun buildRemoteViews() {
        val backgroundColor =
            ContextCompat.getColor(applicationContext, R.color.mapboxNotificationBlue)

        buildCollapsedViews(backgroundColor)
        buildExpandedViews(backgroundColor)
    }

    private fun buildCollapsedViews(backgroundColor: Int) {
        val collapsedLayout = R.layout.collapsed_navigation_notification_layout
        val collapsedLayoutId = R.id.navigationCollapsedNotificationLayout

        RemoteViewsProvider.createRemoteViews(applicationContext.packageName, collapsedLayout)
            .also { remoteViews ->
                collapsedNotificationRemoteViews = remoteViews
                remoteViews.setInt(collapsedLayoutId, SET_BACKGROUND_COLOR, backgroundColor)
            }
    }

    private fun buildExpandedViews(backgroundColor: Int) {
        val expandedLayout = R.layout.expanded_navigation_notification_layout
        val expandedLayoutId = R.id.navigationExpandedNotificationLayout
        RemoteViewsProvider.createRemoteViews(applicationContext.packageName, expandedLayout)
            .also { remoteViews ->
                expandedNotificationRemoteViews = remoteViews
                remoteViews.setOnClickPendingIntent(R.id.endNavigationBtn, pendingCloseIntent)
                remoteViews.setInt(expandedLayoutId, SET_BACKGROUND_COLOR, backgroundColor)
            }
    }

    /**
     * Creates [PendingIntent] for opening application when notification view is clicked
     *
     * @param applicationContext the application's [Context]
     * @return [PendingIntent] to opening application
     */
    private fun createPendingOpenIntent(applicationContext: Context): PendingIntent? {
        val pm = applicationContext.packageManager
        val intent = pm.getLaunchIntentForPackage(applicationContext.packageName) ?: return null
        intent.setPackage(null)
        return PendingIntent.getActivity(applicationContext, 0, intent, 0)
    }

    /**
     * Creates [PendingIntent] for stopping [TripSession] when
     * proper button is clicked in the notification view
     *
     * @param applicationContext the application's [Context]
     * @return [PendingIntent] for stopping [TripSession]
     */
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

    private fun updateNotificationViews(routeProgress: RouteProgress) {
        routeProgress.route()?.let {
            updateInstructionText(routeProgress.bannerInstructions())
            updateDistanceText(routeProgress)
            generateArrivalTime(routeProgress)?.let { formattedTime ->
                updateViewsWithArrival(formattedTime)
            }
            routeProgress.bannerInstructions()?.let { bannerInstructions ->
                if (isManeuverStateChanged(bannerInstructions)) {
                    updateManeuverImage(
                        routeProgress.currentLegProgress()?.currentStepProgress()?.step()?.drivingSide()
                            ?: STEP_MANEUVER_MODIFIER_RIGHT
                    )
                }
            }
            setFreeDriveMode(false)
        } ?: setFreeDriveMode(true)
    }

    private fun setFreeDriveMode(isFreeDriveMode: Boolean) {
        updateEtaContentVisibility(isFreeDriveMode)
        updateInstructionTextVisibility(isFreeDriveMode)
        updateFreeDriveTextVisibility(isFreeDriveMode)
        updateManeuverImageResource(isFreeDriveMode)
        updateEndNavigationBtnText(isFreeDriveMode)
        updateCurrentManeuverToDefault(isFreeDriveMode)
    }

    private fun updateEtaContentVisibility(isFreeDriveMode: Boolean) {
        collapsedNotificationRemoteViews?.setViewVisibility(
            R.id.etaContent,
            if (isFreeDriveMode) GONE else VISIBLE
        )
        expandedNotificationRemoteViews?.setViewVisibility(
            R.id.etaContent,
            if (isFreeDriveMode) GONE else VISIBLE
        )
    }

    private fun updateInstructionTextVisibility(isFreeDriveMode: Boolean) {
        collapsedNotificationRemoteViews?.setViewVisibility(
            R.id.notificationInstructionText,
            if (isFreeDriveMode) GONE else VISIBLE
        )
        expandedNotificationRemoteViews?.setViewVisibility(
            R.id.notificationInstructionText,
            if (isFreeDriveMode) GONE else VISIBLE
        )
    }

    private fun updateFreeDriveTextVisibility(isFreeDriveMode: Boolean) {
        collapsedNotificationRemoteViews?.setViewVisibility(
            R.id.freeDriveText,
            if (isFreeDriveMode) VISIBLE else GONE
        )
        expandedNotificationRemoteViews?.setViewVisibility(
            R.id.freeDriveText,
            if (isFreeDriveMode) VISIBLE else GONE
        )
    }

    private fun updateEndNavigationBtnText(isFreeDriveMode: Boolean) {
        expandedNotificationRemoteViews?.setTextViewText(
            R.id.endNavigationBtnText,
            applicationContext.getString(if (isFreeDriveMode) R.string.stop_session else R.string.end_navigation)
        )
    }

    private fun updateManeuverImageResource(isFreeDriveMode: Boolean) {
        if (isFreeDriveMode) {
            collapsedNotificationRemoteViews?.setImageViewResource(
                R.id.maneuverImage,
                R.drawable.ic_navigation
            )
            expandedNotificationRemoteViews?.setImageViewResource(
                R.id.maneuverImage,
                R.drawable.ic_navigation
            )
        }
    }

    private fun updateInstructionText(bannerInstruction: BannerInstructions?) {
        bannerInstruction?.let { bannerIns ->
            val primaryText = bannerIns.primary().text()
            if (currentInstructionText.isNullOrEmpty() || currentInstructionText != primaryText) {
                collapsedNotificationRemoteViews?.setTextViewText(
                    R.id.notificationInstructionText,
                    primaryText
                )
                expandedNotificationRemoteViews?.setTextViewText(
                    R.id.notificationInstructionText,
                    primaryText
                )
                currentInstructionText = primaryText
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
                currentDistanceText.toString()
            )
            expandedNotificationRemoteViews?.setTextViewText(
                R.id.notificationDistanceText,
                currentDistanceText.toString()
            )
        }
    }

    private fun generateArrivalTime(
        routeProgress: RouteProgress,
        time: Calendar = Calendar.getInstance()
    ): String? =
        ifNonNull(routeProgress.currentLegProgress()) { currentLegProgress ->
            val legDurationRemaining = currentLegProgress.durationRemaining()
            val timeFormatType = navigationOptions.timeFormatType
            val arrivalTime = formatTime(
                time,
                legDurationRemaining.toDouble(),
                timeFormatType,
                DateFormat.is24HourFormat(applicationContext)
            )
            String.format(etaFormat, arrivalTime)
        }

    private fun updateViewsWithArrival(time: String) {
        collapsedNotificationRemoteViews?.setTextViewText(R.id.notificationArrivalText, time)
        expandedNotificationRemoteViews?.setTextViewText(R.id.notificationArrivalText, time)
    }

    private fun updateCurrentManeuverToDefault(isFreeDriveMode: Boolean) {
        if (isFreeDriveMode) {
            currentManeuverType = null
            currentManeuverModifier = null
            currentRoundaboutAngle = DEFAULT_ROUNDABOUT_ANGLE
        }
    }

    private fun newDistanceText(routeProgress: RouteProgress) =
        ifNonNull(
            distanceFormatter,
            routeProgress.currentLegProgress(),
            currentDistanceText
        ) { distanceFormatter, currentLegProgress, currentDistanceText ->
            val item = currentLegProgress.currentStepProgress()?.distanceRemaining()
            // The call below can return an empty spannable string. toString() will cause a NPE and ?. will not catch it.
            val str = item?.let {
                distanceFormatter.formatDistance(it.toDouble())
            }
            if (str != null) {
                val formattedDistance = str.toString()
                currentDistanceText.toString() != formattedDistance
            } else
                false
        } ?: false

    private fun updateManeuverImage(drivingSide: String) {
        getManeuverBitmap(
            currentManeuverType ?: "",
            currentManeuverModifier, drivingSide, currentRoundaboutAngle
        )?.let { bitmap ->
            collapsedNotificationRemoteViews?.setImageViewBitmap(R.id.maneuverImage, bitmap)
            expandedNotificationRemoteViews?.setImageViewBitmap(R.id.maneuverImage, bitmap)
        }
    }

    private fun isManeuverStateChanged(bannerInstruction: BannerInstructions): Boolean {
        val previousManeuverType = currentManeuverType
        val previousManeuverModifier = currentManeuverModifier
        val previousRoundaboutAngle = currentRoundaboutAngle

        updateManeuverState(bannerInstruction)

        return !TextUtils.equals(currentManeuverType, previousManeuverType) ||
            !TextUtils.equals(currentManeuverModifier, previousManeuverModifier) ||
            currentRoundaboutAngle != previousRoundaboutAngle
    }

    private fun updateManeuverState(bannerInstruction: BannerInstructions) {
        currentManeuverType = bannerInstruction.primary().type()
        currentManeuverModifier = bannerInstruction.primary().modifier()

        currentRoundaboutAngle = if (ROUNDABOUT_MANEUVER_TYPES.contains(currentManeuverType)) {
            adjustRoundaboutAngle(bannerInstruction.primary().degrees()?.toFloat() ?: 0f)
        } else {
            DEFAULT_ROUNDABOUT_ANGLE
        }
    }

    private fun getManeuverBitmap(
        maneuverType: String,
        maneuverModifier: String?,
        drivingSide: String,
        roundaboutAngle: Float
    ): Bitmap? {
        val maneuver = when {
            MANEUVER_TYPES_WITH_NULL_MODIFIERS.contains(maneuverType) -> Pair(maneuverType, null)
            !STEP_MANEUVER_TYPE_ARRIVE.contentEquals(maneuverType) && maneuverModifier != null -> Pair(
                null,
                maneuverModifier
            )
            else -> Pair(maneuverType, maneuverModifier)
        }

        val width =
            applicationContext.resources.getDimensionPixelSize(R.dimen.notification_maneuver_image_view_width)
        val height =
            applicationContext.resources.getDimensionPixelSize(R.dimen.notification_maneuver_image_view_height)

        val maneuverImage = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val maneuverCanvas = Canvas(maneuverImage)

        MANEUVER_ICON_DRAWER_MAP[maneuver]?.drawManeuverIcon(
            maneuverCanvas,
            ContextCompat.getColor(
                applicationContext,
                R.color.mapbox_navigation_view_color_banner_maneuver_primary
            ),
            ContextCompat.getColor(
                applicationContext,
                R.color.mapbox_navigation_view_color_banner_maneuver_secondary
            ),
            PointF(width.toFloat(), height.toFloat()),
            roundaboutAngle
        )

        maneuverCanvas.restoreToCount(maneuverCanvas.saveCount)

        return if (isManeuverIconNeedFlip(
                currentManeuverType,
                currentManeuverModifier,
                drivingSide
            )
        ) {
            Bitmap.createBitmap(
                maneuverImage,
                0,
                0,
                width,
                height,
                Matrix().apply { preScale(-1f, 1f) },
                false
            )
        } else {
            maneuverImage
        }
    }

    private fun onEndNavigationBtnClick() {
        try {
            notificationActionButtonChannel.offer(NotificationAction.END_NAVIGATION)
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

    private inner class NotificationActionReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            onEndNavigationBtnClick()
        }
    }
}
