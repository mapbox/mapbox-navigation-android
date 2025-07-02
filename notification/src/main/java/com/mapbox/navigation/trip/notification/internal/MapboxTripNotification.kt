package com.mapbox.navigation.trip.notification.internal

import android.annotation.SuppressLint
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
import android.graphics.drawable.Drawable
import android.os.Build
import android.text.SpannableString
import android.text.TextUtils
import android.text.format.DateFormat
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.mapbox.annotation.module.MapboxModule
import com.mapbox.annotation.module.MapboxModuleType
import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.api.directions.v5.models.BannerText
import com.mapbox.api.directions.v5.models.ManeuverModifier
import com.mapbox.api.directions.v5.models.StepManeuver.StepManeuverType
import com.mapbox.navigation.base.formatter.DistanceFormatter
import com.mapbox.navigation.base.internal.maneuver.TurnIconHelper
import com.mapbox.navigation.base.internal.time.TimeFormatter.formatTime
import com.mapbox.navigation.base.internal.trip.notification.NotificationTurnIconResources
import com.mapbox.navigation.base.internal.trip.notification.TripNotificationInterceptorOwner
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.model.TripNotificationState
import com.mapbox.navigation.base.trip.notification.NotificationAction
import com.mapbox.navigation.base.trip.notification.TripNotification
import com.mapbox.navigation.trip.notification.MapboxTripNotificationView
import com.mapbox.navigation.trip.notification.R
import com.mapbox.navigation.utils.internal.DISMISS_NOTIFICATION_ACTION
import com.mapbox.navigation.utils.internal.END_NAVIGATION_ACTION
import com.mapbox.navigation.utils.internal.NAVIGATION_NOTIFICATION_CHANNEL
import com.mapbox.navigation.utils.internal.NOTIFICATION_CHANNEL
import com.mapbox.navigation.utils.internal.NOTIFICATION_ID
import com.mapbox.navigation.utils.internal.ifChannelException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.channels.ClosedSendChannelException
import java.util.Calendar

/**
 * Default implementation of [TripNotification] interface
 *
 * @property currentManeuverType This indicates the type of current maneuver. The same [BannerText.type] of primary [BannerInstructions]
 * @property currentManeuverModifier This indicates the mode of the maneuver. The same [BannerText.modifier] of primary [BannerInstructions]
 */
@MapboxModule(MapboxModuleType.NavigationTripNotification)
class MapboxTripNotification constructor(
    navigationOptions: NavigationOptions,
    private val interceptorOwner: TripNotificationInterceptorOwner,
    private val distanceFormatter: DistanceFormatter,
) : TripNotification {

    companion object {
        /**
         * Broadcast of [MapboxTripNotification] actions
         */
        var notificationActionButtonChannel = Channel<NotificationAction>(1)
    }

    private enum class State {
        NOT_STARTED, // before session is started or after session is stopped
        STARTED, // after session is started before notification is dismissed
        DISMISSED, // after notification is dismissed before session is stopped
    }

    private val applicationContext = navigationOptions.applicationContext
    private val timeFormatType = navigationOptions.timeFormatType

    @StepManeuverType
    var currentManeuverType: String? = null
        private set
    var currentManeuverModifier: String? = null
        private set
    private var currentRoundaboutAngle: Float? = null
    private var currentManeuverImage: Bitmap? = null

    private var currentInstructionText: String? = null
    private var currentDistanceText: Double? = null
    private var currentFormattedDistance: SpannableString? = null
    private var currentFormattedTime: String? = null
    private var pendingOpenIntent: PendingIntent? = null
    private var pendingDismissalIntent: PendingIntent? = null
    private var pendingCloseIntent: PendingIntent? = null
    private val etaFormat: String = applicationContext.getString(R.string.mapbox_eta_format)
    private val notificationEndReceiver = NotificationEndReceiver()
    private val notificationDismissedReceiver = NotificationDismissedReceiver()
    private lateinit var notification: Notification
    private lateinit var notificationManager: NotificationManager
    private var state: State = State.NOT_STARTED
    private val turnIconHelper = TurnIconHelper(NotificationTurnIconResources.defaultIconSet())

    private var notificationView: MapboxTripNotificationView
    private val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    } else {
        PendingIntent.FLAG_UPDATE_CURRENT
    }

    init {
        applicationContext.getSystemService(Context.NOTIFICATION_SERVICE)
            ?.let { notificationService ->
                notificationManager = notificationService as NotificationManager
            } ?: throw (IllegalStateException("Unable to create a NotificationManager"))

        pendingOpenIntent = createPendingOpenIntent(applicationContext)
        pendingCloseIntent = createPendingCloseIntent(applicationContext)
        pendingDismissalIntent = createPendingDismissalIntent(applicationContext)
        notificationView = MapboxTripNotificationView(applicationContext)

        notificationView.buildRemoteViews(pendingCloseIntent)
        createNotificationChannel()
    }

    /**
     * Provides a custom [Notification] to launch
     * with the trip session, specifically
     * [android.app.Service.startForeground].
     *
     * @return a custom notification
     */
    override fun getNotification(): Notification {
        if (!::notification.isInitialized) {
            this.notification = getNotificationBuilder().build()
        }
        return this.notification
    }

    /**
     * An integer id that will be used to start this notification from trip session with
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
     * @param state with the latest progress data
     */
    override fun updateNotification(state: TripNotificationState) {
        if (this.state == State.STARTED) {
            // RemoteView has an internal mActions, which stores every change and cannot be cleared.
            // As we set new bitmaps, the mActions parcelable size will grow and eventually cause a crash.
            // buildRemoteViews() will rebuild the RemoteViews and clear the stored mActions.
            notificationView.buildRemoteViews(pendingCloseIntent)
            updateNotificationViews(state)
            notification = getNotificationBuilder().build()
            notificationManager.notify(NOTIFICATION_ID, notification)
        }
    }

    /**
     * Called when TripSession starts
     *
     * This callback may be used to perform any actions after the trip session is initialized.
     */
    override fun onTripSessionStarted() {
        registerReceivers()
        notificationActionButtonChannel = Channel(1)
        notificationView.setVisibility(VISIBLE)
        notificationView.setEndNavigationButtonText(R.string.mapbox_stop_session)
        state = State.STARTED
    }

    /**
     * Called when TripSession stops
     *
     * This callback may be used to clean up any listeners or receivers, preventing leaks.
     */
    override fun onTripSessionStopped() {
        cleanUp()
        state = State.NOT_STARTED
    }

    private fun cleanUp() {
        if (state == State.STARTED) {
            currentManeuverType = null
            currentManeuverModifier = null
            currentInstructionText = null
            currentDistanceText = null
            notificationView.resetView()
            unregisterReceivers()
            try {
                notificationActionButtonChannel.cancel()
            } catch (e: Exception) {
                e.ifChannelException {
                    // Do nothing
                }
            }
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private fun registerReceivers() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            applicationContext.registerReceiver(
                notificationEndReceiver,
                IntentFilter(END_NAVIGATION_ACTION),
                Context.RECEIVER_NOT_EXPORTED,
            )
            applicationContext.registerReceiver(
                notificationDismissedReceiver,
                IntentFilter(DISMISS_NOTIFICATION_ACTION),
                Context.RECEIVER_NOT_EXPORTED,
            )
        } else {
            applicationContext.registerReceiver(
                notificationEndReceiver,
                IntentFilter(END_NAVIGATION_ACTION),
            )
            applicationContext.registerReceiver(
                notificationDismissedReceiver,
                IntentFilter(DISMISS_NOTIFICATION_ACTION),
            )
        }
    }

    private fun unregisterReceivers() {
        applicationContext.unregisterReceiver(notificationEndReceiver)
        applicationContext.unregisterReceiver(notificationDismissedReceiver)
        notificationManager.cancel(NOTIFICATION_ID)
    }

    private fun getNotificationBuilder(): NotificationCompat.Builder {
        val builder = NotificationBuilderProvider
            .create(applicationContext, NAVIGATION_NOTIFICATION_CHANNEL)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setSmallIcon(R.drawable.mapbox_ic_navigation)
            .setCustomContentView(notificationView.collapsedView)
            .setCustomBigContentView(notificationView.expandedView)
            .setOngoing(true)

        if (Build.VERSION.SDK_INT >= 31) {
            val color = ContextCompat.getColor(applicationContext, R.color.mapbox_notification_blue)
            builder.setColor(color).setColorized(true)
        }

        pendingOpenIntent?.let { pendingOpenIntent ->
            builder.setContentIntent(pendingOpenIntent)
        }
        pendingDismissalIntent?.let { pendingDismissalIntent ->
            builder.setDeleteIntent(pendingDismissalIntent)
        }

        return interceptorOwner.interceptor?.intercept(builder) ?: builder
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
        return PendingIntent.getActivity(applicationContext, 0, intent, flags)
    }

    /**
     * Creates [PendingIntent] for stopping trip session when
     * proper button is clicked in the notification view
     *
     * @param applicationContext the application's [Context]
     * @return [PendingIntent] for stopping trip session
     */
    private fun createPendingCloseIntent(applicationContext: Context): PendingIntent? {
        val endNavigationBtn = Intent(END_NAVIGATION_ACTION).also {
            it.setPackage(applicationContext.packageName)
        }
        return PendingIntent.getBroadcast(applicationContext, 0, endNavigationBtn, flags)
    }

    private fun createPendingDismissalIntent(applicationContext: Context): PendingIntent? {
        val intent = Intent(DISMISS_NOTIFICATION_ACTION).also {
            it.setPackage(applicationContext.packageName)
        }
        return PendingIntent.getBroadcast(applicationContext, 0, intent, flags)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                NAVIGATION_NOTIFICATION_CHANNEL,
                NOTIFICATION_CHANNEL,
                NotificationManager.IMPORTANCE_LOW,
            )
            notificationManager.createNotificationChannel(notificationChannel)
        }
    }

    private fun updateNotificationViews(state: TripNotificationState) {
        notificationView.setVisibility(GONE)

        when (state) {
            is TripNotificationState.TripNotificationFreeState -> setFreeDriveMode(true)
            is TripNotificationState.TripNotificationData -> {
                updateDistanceText(state.distanceRemaining)
                updateViewsWithArrival(state.durationRemaining)
                updateInstructionText(state.bannerInstructions)
                updateManeuverImage(state.bannerInstructions, state.drivingSide)
                setFreeDriveMode(false)
            }
        }
    }

    private fun setFreeDriveMode(isFreeDriveMode: Boolean) {
        notificationView.setFreeDriveMode(isFreeDriveMode)
        updateCurrentManeuverToDefault(isFreeDriveMode)
    }

    private fun updateCurrentManeuverToDefault(isFreeDriveMode: Boolean) {
        if (isFreeDriveMode) {
            currentManeuverType = null
            currentManeuverModifier = null
            currentRoundaboutAngle = null
        }
    }

    private fun isDistanceTextChanged(distanceRemaining: Double?): Boolean {
        return currentDistanceText != distanceRemaining
    }

    private fun updateDistanceText(distanceRemaining: Double?) {
        if (isDistanceTextChanged(distanceRemaining) && distanceRemaining != null) {
            currentDistanceText = distanceRemaining
            currentFormattedDistance = distanceFormatter.formatDistance(distanceRemaining)
        }
        currentFormattedDistance?.let { notificationView.updateDistanceText(it) }
    }

    private fun generateArrivalTime(
        durationRemaining: Double?,
        time: Calendar = Calendar.getInstance(),
    ): String? {
        return durationRemaining?.let {
            val timeFormatType = timeFormatType
            val arrivalTime = formatTime(
                time,
                durationRemaining,
                timeFormatType,
                DateFormat.is24HourFormat(applicationContext),
            )
            String.format(etaFormat, arrivalTime)
        }
    }

    private fun updateViewsWithArrival(durationRemaining: Double?) {
        generateArrivalTime(durationRemaining)?.let { currentFormattedTime = it }
        currentFormattedTime?.let { notificationView.updateArrivalTime(it) }
    }

    private fun isInstructionTextChanged(primaryText: String): Boolean {
        return currentInstructionText.isNullOrEmpty() || currentInstructionText != primaryText
    }

    private fun updateInstructionText(bannerInstructions: BannerInstructions?) {
        bannerInstructions?.primary()?.text()
            ?.takeIf { isInstructionTextChanged(it) }
            ?.let { currentInstructionText = it }
        currentInstructionText?.let { notificationView.updateInstructionText(it) }
    }

    private fun isManeuverStateChanged(bannerInstruction: BannerInstructions): Boolean {
        val previousManeuverType = currentManeuverType
        val previousManeuverModifier = currentManeuverModifier
        val previousRoundaboutAngle = currentRoundaboutAngle

        currentManeuverType = bannerInstruction.primary().type()
        currentManeuverModifier = bannerInstruction.primary().modifier()
        currentRoundaboutAngle = bannerInstruction.primary().degrees()?.toFloat()

        return !TextUtils.equals(currentManeuverType, previousManeuverType) ||
            !TextUtils.equals(currentManeuverModifier, previousManeuverModifier) ||
            currentRoundaboutAngle != previousRoundaboutAngle
    }

    private fun updateManeuverImage(bannerInstructions: BannerInstructions?, drivingSide: String?) {
        if (bannerInstructions != null && isManeuverStateChanged(bannerInstructions)) {
            turnIconHelper.retrieveTurnIcon(
                currentManeuverType,
                currentRoundaboutAngle,
                currentManeuverModifier,
                drivingSide = drivingSide ?: ManeuverModifier.RIGHT,
            )?.let { turnIcon ->
                turnIcon.icon
                    ?.let { notificationView.getImageDrawable(it) }
                    ?.let { getManeuverBitmap(it, turnIcon.shouldFlipIcon) }
                    ?.let { currentManeuverImage = it }
            }
        }
        currentManeuverImage?.let { notificationView.updateImage(it) }
    }

    private fun getManeuverBitmap(drawable: Drawable, shouldFlipIcon: Boolean): Bitmap? {
        val maneuverImageBitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth,
            drawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888,
        )
        val maneuverCanvas = Canvas(maneuverImageBitmap)
        drawable.setBounds(0, 0, maneuverCanvas.width, maneuverCanvas.height)
        drawable.draw(maneuverCanvas)
        maneuverCanvas.restoreToCount(maneuverCanvas.saveCount)
        return if (shouldFlipIcon) {
            Bitmap.createBitmap(
                maneuverImageBitmap,
                0,
                0,
                drawable.intrinsicWidth,
                drawable.intrinsicHeight,
                Matrix().apply { preScale(-1f, 1f) },
                false,
            )
        } else {
            maneuverImageBitmap
        }
    }

    private fun onEndNavigationBtnClick() {
        try {
            notificationActionButtonChannel.trySend(NotificationAction.END_NAVIGATION)
        } catch (e: Exception) {
            when (e) {
                is ClosedReceiveChannelException,
                is ClosedSendChannelException,
                -> {
                }
                else -> {
                    throw e
                }
            }
        }
    }

    private fun onNotificationDismissed() {
        cleanUp()
        state = State.DISMISSED
    }

    private inner class NotificationEndReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            onEndNavigationBtnClick()
        }
    }

    private inner class NotificationDismissedReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            onNotificationDismissed()
        }
    }
}
