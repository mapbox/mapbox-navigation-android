package com.mapbox.services.android.navigation.v5.navigation

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
import com.mapbox.api.directions.v5.DirectionsCriteria.IMPERIAL
import com.mapbox.api.directions.v5.models.LegStep
import com.mapbox.navigator.BannerInstruction
import com.mapbox.services.android.navigation.R
import com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.*
import com.mapbox.services.android.navigation.v5.navigation.notification.NavigationNotification
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress
import com.mapbox.services.android.navigation.v5.utils.DistanceFormatter
import com.mapbox.services.android.navigation.v5.utils.LocaleUtils
import com.mapbox.services.android.navigation.v5.utils.time.TimeFormatter.formatTime
import java.util.*

/**
 * This is in charge of creating the persistent navigation session notification and updating it.
 */
internal class MapboxNavigationNotification : NavigationNotification {

    private val END_NAVIGATION_ACTION = "com.mapbox.intent.action.END_NAVIGATION"
    private val SET_BACKGROUND_COLOR = "setBackgroundColor"

    private var notificationManager: NotificationManager? = null
    private var notification: Notification? = null
    private var collapsedNotificationRemoteViews: RemoteViews? = null
    private var expandedNotificationRemoteViews: RemoteViews? = null
    private lateinit var mapboxNavigation: MapboxNavigation
    private var currentDistanceText: SpannableString? = null
    private var distanceFormatter: DistanceFormatter? = null
    private var instructionText: String? = null
    private var currentManeuverId: Int = 0
    private var isTwentyFourHourFormat: Boolean = false
    private var etaFormat: String = ""
    private val applicationContext: Context
    private var pendingOpenIntent: PendingIntent? = null
    private var pendingCloseIntent: PendingIntent? = null

    private val endNavigationBtnReceiver = object : BroadcastReceiver() {
        override fun onReceive(applicationContext: Context, intent: Intent) {
            this@MapboxNavigationNotification.onEndNavigationBtnClick()
        }
    }

    constructor(applicationContext: Context, mapboxNavigation: MapboxNavigation) {
        this.applicationContext = applicationContext
        initialize(applicationContext, mapboxNavigation)
    }

    // For testing only
    constructor(
        applicationContext: Context,
        mapboxNavigation: MapboxNavigation,
        notification: Notification
    ) {
        this.applicationContext = applicationContext
        this.notification = notification
        initialize(applicationContext, mapboxNavigation)
    }

    override fun getNotification() = notification

    override fun getNotificationId(): Int {
        return NAVIGATION_NOTIFICATION_ID
    }

    override fun updateNotification(routeProgress: RouteProgress) {
        updateNotificationViews(routeProgress)
        rebuildNotification()
    }

    override fun onNavigationStopped(applicationContext: Context) {
        unregisterReceiver(applicationContext)
    }

    // Package private (no modifier) for testing purposes
    fun generateArrivalTime(routeProgress: RouteProgress, time: Calendar): String? =
        mapboxNavigation.let { mapboxNavigation ->
            val options = mapboxNavigation.options()
            val legDurationRemaining = routeProgress.currentLegProgress().durationRemaining()
            val timeFormatType = options.timeFormatType()
            val arrivalTime = formatTime(time, legDurationRemaining, timeFormatType, isTwentyFourHourFormat)
            String.format(etaFormat, arrivalTime)
        }

    // Package private (no modifier) for testing purposes
    fun updateNotificationViews(routeProgress: RouteProgress) {
        buildRemoteViews()
        updateInstructionText(routeProgress.bannerInstruction())
        updateDistanceText(routeProgress)
        val time = Calendar.getInstance()

        /* Original code
          LegStep step = routeProgress.currentLegProgress().upComingStep() != null
          ? routeProgress.currentLegProgress().upComingStep()
          : routeProgress.currentLegProgress().currentStep();
          updateManeuverImage(step)
        * */

        // *** Converted code
        generateArrivalTime(routeProgress, time)?.let { formattedTime ->
            updateViewsWithArrival(formattedTime)
            routeProgress.currentLegProgress().upComingStep()?.let { step ->
                routeProgress.currentLegProgress().upComingStep()
                updateManeuverImage(step)
            } ?: routeProgress.currentLegProgress().currentStep()
        }
    }

    // Package private (no modifier) for testing purposes
    fun retrieveInstructionText(): String? {
        return instructionText
    }

    // Package private (no modifier) for testing purposes
    fun retrieveCurrentManeuverId(): Int {
        return currentManeuverId
    }

    private fun initialize(applicationContext: Context, mapboxNavigation: MapboxNavigation) {
        this.mapboxNavigation = mapboxNavigation
        etaFormat = applicationContext.getString(R.string.eta_format)
        initializeDistanceFormatter(applicationContext, mapboxNavigation)
        applicationContext.getSystemService(Context.NOTIFICATION_SERVICE)?.let { notificationService ->
            notificationManager = notificationService as NotificationManager
        }
        isTwentyFourHourFormat = DateFormat.is24HourFormat(applicationContext)

        pendingOpenIntent = createPendingOpenIntent(applicationContext)
        pendingCloseIntent = createPendingCloseIntent(applicationContext)

        registerReceiver(applicationContext)
        createNotificationChannel(applicationContext)
        if (notification == null) {
            notification = buildNotification(applicationContext)
        }
    }

    private fun initializeDistanceFormatter(applicationContext: Context, mapboxNavigation: MapboxNavigation) {
        val routeOptions = mapboxNavigation.route.routeOptions()
        val localeUtils = LocaleUtils()
        var language: String = localeUtils.inferDeviceLanguage(applicationContext)
        var unitType: String = localeUtils.getUnitTypeForDeviceLocale(applicationContext)
        routeOptions?.let { options ->
            language = options.language() ?: "en"
            unitType = options.voiceUnits() ?: IMPERIAL
        }

        val mapboxNavigationOptions = mapboxNavigation.options()
        val roundingIncrement = mapboxNavigationOptions.roundingIncrement()

        distanceFormatter = DistanceFormatter(applicationContext, language, unitType, roundingIncrement)
    }

    private fun createNotificationChannel(applicationContext: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                NAVIGATION_NOTIFICATION_CHANNEL, applicationContext.getString(R.string.channel_name),
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager?.createNotificationChannel(notificationChannel)
        }
    }

    private fun buildNotification(applicationContext: Context): Notification {
        val channelId = NAVIGATION_NOTIFICATION_CHANNEL
        val builder = NotificationCompat.Builder(applicationContext, channelId)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setSmallIcon(R.drawable.ic_navigation)
            .setCustomContentView(collapsedNotificationRemoteViews)
            .setCustomBigContentView(expandedNotificationRemoteViews)
            .setOngoing(true)

        if (pendingOpenIntent != null) {
            builder.setContentIntent(pendingOpenIntent)
        }
        return builder.build()
    }

    private fun buildRemoteViews() {
        val colorResId = mapboxNavigation.options().defaultNotificationColorId()
        val backgroundColor = ContextCompat.getColor(applicationContext, colorResId)

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

    private fun registerReceiver(applicationContext: Context?) {
        applicationContext?.registerReceiver(endNavigationBtnReceiver, IntentFilter(END_NAVIGATION_ACTION))
    }

    private fun rebuildNotification() {
        notification = buildNotification(applicationContext)
        notificationManager?.notify(NAVIGATION_NOTIFICATION_ID, notification)
    }

    private fun unregisterReceiver(applicationContext: Context?) {
        applicationContext?.unregisterReceiver(endNavigationBtnReceiver)
        notificationManager?.cancel(NAVIGATION_NOTIFICATION_ID)
    }

    private fun updateInstructionText(bannerInstruction: BannerInstruction?) {
        if (bannerInstruction != null && (instructionText == null || newInstructionText(bannerInstruction))) {
            updateViewsWithInstruction(bannerInstruction.primary.text)
            instructionText = bannerInstruction.primary.text
        }
    }

    private fun updateViewsWithInstruction(text: String?) {
        collapsedNotificationRemoteViews?.setTextViewText(R.id.notificationInstructionText, text)
        expandedNotificationRemoteViews?.setTextViewText(R.id.notificationInstructionText, text)
    }

    private fun newInstructionText(bannerInstruction: BannerInstruction): Boolean {
        return instructionText != bannerInstruction.primary.text
    }

    private fun updateDistanceText(routeProgress: RouteProgress) {
        if (currentDistanceText == null || newDistanceText(routeProgress)) {
            currentDistanceText = distanceFormatter?.formatDistance(
                routeProgress.currentLegProgress().currentStepProgress().distanceRemaining()
            )
            collapsedNotificationRemoteViews?.setTextViewText(R.id.notificationDistanceText, currentDistanceText)
            expandedNotificationRemoteViews?.setTextViewText(R.id.notificationDistanceText, currentDistanceText)
        }
    }

    private fun newDistanceText(routeProgress: RouteProgress): Boolean {
        val formattedDistance = distanceFormatter?.formatDistance(routeProgress.currentLegProgress().currentStepProgress().distanceRemaining()).toString()
        return currentDistanceText?.let { currentDistance ->
            currentDistance.toString() != formattedDistance
        } ?: false
    }

    private fun updateViewsWithArrival(time: String) {
        collapsedNotificationRemoteViews?.setTextViewText(R.id.notificationArrivalText, time)
        expandedNotificationRemoteViews?.setTextViewText(R.id.notificationArrivalText, time)
    }

    private fun updateManeuverImage(step: LegStep) {
        val maneuverResource = getManeuverResource(step)
        if (currentManeuverId != maneuverResource) {
            currentManeuverId = maneuverResource
            collapsedNotificationRemoteViews?.setImageViewResource(R.id.maneuverImage, maneuverResource)
            expandedNotificationRemoteViews?.setImageViewResource(R.id.maneuverImage, maneuverResource)
        }
    }

    private fun getManeuverResource(step: LegStep): Int {
        val maneuver = step.maneuver()
        val maneuverType = maneuver.type()
        val maneuverModifier = maneuver.modifier()
        if (!TextUtils.isEmpty(maneuverModifier)) {
            val drivingSide = step.drivingSide()
            return if (isLeftDrivingSideAndRoundaboutOrRotaryOrUturn(maneuverType, maneuverModifier, drivingSide)) {
                obtainManeuverResourceFrom(maneuverType + maneuverModifier + drivingSide)
            } else obtainManeuverResourceFrom(maneuverType + maneuverModifier)
        }
        return obtainManeuverResourceFrom(maneuverType)
    }

    private fun obtainManeuverResourceFrom(maneuver: String?): Int {
        when (maneuver) {
            STEP_MANEUVER_TYPE_TURN + STEP_MANEUVER_MODIFIER_UTURN, STEP_MANEUVER_TYPE_CONTINUE + STEP_MANEUVER_MODIFIER_UTURN -> return R.drawable.ic_maneuver_turn_180
            STEP_MANEUVER_TYPE_TURN + STEP_MANEUVER_MODIFIER_UTURN + STEP_MANEUVER_MODIFIER_LEFT, STEP_MANEUVER_TYPE_CONTINUE + STEP_MANEUVER_MODIFIER_UTURN + STEP_MANEUVER_MODIFIER_LEFT -> return R.drawable.ic_maneuver_turn_180_left_driving_side

            STEP_MANEUVER_TYPE_ARRIVE + STEP_MANEUVER_MODIFIER_LEFT -> return R.drawable.ic_maneuver_arrive_left
            STEP_MANEUVER_TYPE_ARRIVE + STEP_MANEUVER_MODIFIER_RIGHT -> return R.drawable.ic_maneuver_arrive_right
            STEP_MANEUVER_TYPE_ARRIVE -> return R.drawable.ic_maneuver_arrive

            STEP_MANEUVER_TYPE_DEPART + STEP_MANEUVER_MODIFIER_LEFT -> return R.drawable.ic_maneuver_depart_left
            STEP_MANEUVER_TYPE_DEPART + STEP_MANEUVER_MODIFIER_RIGHT -> return R.drawable.ic_maneuver_depart_right
            STEP_MANEUVER_TYPE_DEPART -> return R.drawable.ic_maneuver_depart

            STEP_MANEUVER_TYPE_TURN + STEP_MANEUVER_MODIFIER_SHARP_RIGHT, STEP_MANEUVER_TYPE_ON_RAMP + STEP_MANEUVER_MODIFIER_SHARP_RIGHT, STEP_MANEUVER_TYPE_NOTIFICATION + STEP_MANEUVER_MODIFIER_SHARP_RIGHT -> return R.drawable.ic_maneuver_turn_75
            STEP_MANEUVER_TYPE_TURN + STEP_MANEUVER_MODIFIER_RIGHT, STEP_MANEUVER_TYPE_ON_RAMP + STEP_MANEUVER_MODIFIER_RIGHT, STEP_MANEUVER_TYPE_ROUNDABOUT_TURN + STEP_MANEUVER_MODIFIER_RIGHT, STEP_MANEUVER_TYPE_NOTIFICATION + STEP_MANEUVER_MODIFIER_RIGHT -> return R.drawable.ic_maneuver_turn_45
            STEP_MANEUVER_TYPE_TURN + STEP_MANEUVER_MODIFIER_SLIGHT_RIGHT, STEP_MANEUVER_TYPE_ON_RAMP + STEP_MANEUVER_MODIFIER_SLIGHT_RIGHT, STEP_MANEUVER_TYPE_NOTIFICATION + STEP_MANEUVER_MODIFIER_SLIGHT_RIGHT -> return R.drawable.ic_maneuver_turn_30

            STEP_MANEUVER_TYPE_TURN + STEP_MANEUVER_MODIFIER_SHARP_LEFT, STEP_MANEUVER_TYPE_ON_RAMP + STEP_MANEUVER_MODIFIER_SHARP_LEFT, STEP_MANEUVER_TYPE_NOTIFICATION + STEP_MANEUVER_MODIFIER_SHARP_LEFT -> return R.drawable.ic_maneuver_turn_75_left
            STEP_MANEUVER_TYPE_TURN + STEP_MANEUVER_MODIFIER_LEFT, STEP_MANEUVER_TYPE_ON_RAMP + STEP_MANEUVER_MODIFIER_LEFT, STEP_MANEUVER_TYPE_NOTIFICATION + STEP_MANEUVER_MODIFIER_LEFT, STEP_MANEUVER_TYPE_ROUNDABOUT_TURN + STEP_MANEUVER_MODIFIER_LEFT -> return R.drawable.ic_maneuver_turn_45_left
            STEP_MANEUVER_TYPE_TURN + STEP_MANEUVER_MODIFIER_SLIGHT_LEFT, STEP_MANEUVER_TYPE_ON_RAMP + STEP_MANEUVER_MODIFIER_SLIGHT_LEFT, STEP_MANEUVER_TYPE_NOTIFICATION + STEP_MANEUVER_MODIFIER_SLIGHT_LEFT -> return R.drawable.ic_maneuver_turn_30_left

            STEP_MANEUVER_TYPE_MERGE + STEP_MANEUVER_MODIFIER_LEFT, STEP_MANEUVER_TYPE_MERGE + STEP_MANEUVER_MODIFIER_SLIGHT_LEFT -> return R.drawable.ic_maneuver_merge_left
            STEP_MANEUVER_TYPE_MERGE + STEP_MANEUVER_MODIFIER_RIGHT, STEP_MANEUVER_TYPE_MERGE + STEP_MANEUVER_MODIFIER_SLIGHT_RIGHT -> return R.drawable.ic_maneuver_merge_right

            STEP_MANEUVER_TYPE_OFF_RAMP + STEP_MANEUVER_MODIFIER_LEFT -> return R.drawable.ic_maneuver_off_ramp_left
            STEP_MANEUVER_TYPE_OFF_RAMP + STEP_MANEUVER_MODIFIER_SLIGHT_LEFT -> return R.drawable.ic_maneuver_off_ramp_slight_left

            STEP_MANEUVER_TYPE_OFF_RAMP + STEP_MANEUVER_MODIFIER_RIGHT -> return R.drawable.ic_maneuver_off_ramp_right
            STEP_MANEUVER_TYPE_OFF_RAMP + STEP_MANEUVER_MODIFIER_SLIGHT_RIGHT -> return R.drawable.ic_maneuver_off_ramp_slight_right

            STEP_MANEUVER_TYPE_FORK + STEP_MANEUVER_MODIFIER_LEFT -> return R.drawable.ic_maneuver_fork_left
            STEP_MANEUVER_TYPE_FORK + STEP_MANEUVER_MODIFIER_SLIGHT_LEFT -> return R.drawable.ic_maneuver_fork_slight_left
            STEP_MANEUVER_TYPE_FORK + STEP_MANEUVER_MODIFIER_RIGHT -> return R.drawable.ic_maneuver_fork_right
            STEP_MANEUVER_TYPE_FORK + STEP_MANEUVER_MODIFIER_SLIGHT_RIGHT -> return R.drawable.ic_maneuver_fork_slight_right
            STEP_MANEUVER_TYPE_FORK + STEP_MANEUVER_MODIFIER_STRAIGHT -> return R.drawable.ic_maneuver_fork_straight
            STEP_MANEUVER_TYPE_FORK -> return R.drawable.ic_maneuver_fork

            STEP_MANEUVER_TYPE_END_OF_ROAD + STEP_MANEUVER_MODIFIER_LEFT -> return R.drawable.ic_maneuver_end_of_road_left
            STEP_MANEUVER_TYPE_END_OF_ROAD + STEP_MANEUVER_MODIFIER_RIGHT -> return R.drawable.ic_maneuver_end_of_road_right

            STEP_MANEUVER_TYPE_ROUNDABOUT + STEP_MANEUVER_MODIFIER_LEFT, STEP_MANEUVER_TYPE_ROTARY + STEP_MANEUVER_MODIFIER_LEFT -> return R.drawable.ic_maneuver_roundabout_left
            STEP_MANEUVER_TYPE_ROUNDABOUT + STEP_MANEUVER_MODIFIER_SHARP_LEFT, STEP_MANEUVER_TYPE_ROTARY + STEP_MANEUVER_MODIFIER_SHARP_LEFT -> return R.drawable.ic_maneuver_roundabout_sharp_left
            STEP_MANEUVER_TYPE_ROUNDABOUT + STEP_MANEUVER_MODIFIER_SLIGHT_LEFT, STEP_MANEUVER_TYPE_ROTARY + STEP_MANEUVER_MODIFIER_SLIGHT_LEFT -> return R.drawable.ic_maneuver_roundabout_slight_left
            STEP_MANEUVER_TYPE_ROUNDABOUT + STEP_MANEUVER_MODIFIER_RIGHT, STEP_MANEUVER_TYPE_ROTARY + STEP_MANEUVER_MODIFIER_RIGHT -> return R.drawable.ic_maneuver_roundabout_right
            STEP_MANEUVER_TYPE_ROUNDABOUT + STEP_MANEUVER_MODIFIER_SHARP_RIGHT, STEP_MANEUVER_TYPE_ROTARY + STEP_MANEUVER_MODIFIER_SHARP_RIGHT -> return R.drawable.ic_maneuver_roundabout_sharp_right
            STEP_MANEUVER_TYPE_ROUNDABOUT + STEP_MANEUVER_MODIFIER_SLIGHT_RIGHT, STEP_MANEUVER_TYPE_ROTARY + STEP_MANEUVER_MODIFIER_SLIGHT_RIGHT -> return R.drawable.ic_maneuver_roundabout_slight_right
            STEP_MANEUVER_TYPE_ROUNDABOUT + STEP_MANEUVER_MODIFIER_STRAIGHT, STEP_MANEUVER_TYPE_ROTARY + STEP_MANEUVER_MODIFIER_STRAIGHT -> return R.drawable.ic_maneuver_roundabout_straight
            STEP_MANEUVER_TYPE_ROUNDABOUT, STEP_MANEUVER_TYPE_ROTARY -> return R.drawable.ic_maneuver_roundabout

            STEP_MANEUVER_TYPE_ROUNDABOUT + STEP_MANEUVER_MODIFIER_LEFT + STEP_MANEUVER_MODIFIER_LEFT, STEP_MANEUVER_TYPE_ROTARY + STEP_MANEUVER_MODIFIER_LEFT + STEP_MANEUVER_MODIFIER_LEFT -> return R.drawable.ic_maneuver_roundabout_left_left_driving_side
            STEP_MANEUVER_TYPE_ROUNDABOUT + STEP_MANEUVER_MODIFIER_SHARP_LEFT + STEP_MANEUVER_MODIFIER_LEFT, STEP_MANEUVER_TYPE_ROTARY + STEP_MANEUVER_MODIFIER_SHARP_LEFT + STEP_MANEUVER_MODIFIER_LEFT -> return R.drawable.ic_maneuver_roundabout_sharp_left_left_driving_side
            STEP_MANEUVER_TYPE_ROUNDABOUT + STEP_MANEUVER_MODIFIER_SLIGHT_LEFT + STEP_MANEUVER_MODIFIER_LEFT, STEP_MANEUVER_TYPE_ROTARY + STEP_MANEUVER_MODIFIER_SLIGHT_LEFT + STEP_MANEUVER_MODIFIER_LEFT -> return R.drawable.ic_maneuver_roundabout_slight_left_left_driving_side
            STEP_MANEUVER_TYPE_ROUNDABOUT + STEP_MANEUVER_MODIFIER_RIGHT + STEP_MANEUVER_MODIFIER_LEFT, STEP_MANEUVER_TYPE_ROTARY + STEP_MANEUVER_MODIFIER_RIGHT + STEP_MANEUVER_MODIFIER_LEFT -> return R.drawable.ic_maneuver_roundabout_right_left_driving_side
            STEP_MANEUVER_TYPE_ROUNDABOUT + STEP_MANEUVER_MODIFIER_SHARP_RIGHT + STEP_MANEUVER_MODIFIER_LEFT, STEP_MANEUVER_TYPE_ROTARY + STEP_MANEUVER_MODIFIER_SHARP_RIGHT + STEP_MANEUVER_MODIFIER_LEFT -> return R.drawable.ic_maneuver_roundabout_sharp_right_left_driving_side
            STEP_MANEUVER_TYPE_ROUNDABOUT + STEP_MANEUVER_MODIFIER_SLIGHT_RIGHT + STEP_MANEUVER_MODIFIER_LEFT, STEP_MANEUVER_TYPE_ROTARY + STEP_MANEUVER_MODIFIER_SLIGHT_RIGHT + STEP_MANEUVER_MODIFIER_LEFT -> return R.drawable.ic_maneuver_roundabout_slight_right_left_driving_side
            STEP_MANEUVER_TYPE_ROUNDABOUT + STEP_MANEUVER_MODIFIER_STRAIGHT + STEP_MANEUVER_MODIFIER_LEFT, STEP_MANEUVER_TYPE_ROTARY + STEP_MANEUVER_MODIFIER_STRAIGHT + STEP_MANEUVER_MODIFIER_LEFT -> return R.drawable.ic_maneuver_roundabout_straight_left_driving_side

            STEP_MANEUVER_TYPE_MERGE + STEP_MANEUVER_MODIFIER_STRAIGHT, STEP_MANEUVER_TYPE_NOTIFICATION + STEP_MANEUVER_MODIFIER_STRAIGHT, STEP_MANEUVER_TYPE_CONTINUE + STEP_MANEUVER_MODIFIER_STRAIGHT, STEP_MANEUVER_TYPE_NEW_NAME + STEP_MANEUVER_MODIFIER_STRAIGHT -> return R.drawable.ic_maneuver_turn_0
            else -> return R.drawable.ic_maneuver_turn_0
        }
    }

    private fun isLeftDrivingSideAndRoundaboutOrRotaryOrUturn(
        maneuverType: String?,
        maneuverModifier: String?,
        drivingSide: String?
    ): Boolean {
        return STEP_MANEUVER_MODIFIER_LEFT == drivingSide && (
            STEP_MANEUVER_TYPE_ROUNDABOUT == maneuverType ||
                STEP_MANEUVER_TYPE_ROTARY == maneuverType || STEP_MANEUVER_MODIFIER_UTURN == maneuverModifier
            )
    }

    private fun onEndNavigationBtnClick() {
        when (::mapboxNavigation.isInitialized) {
            true -> {
                mapboxNavigation.stopNavigation()
            }
        }
    }
}