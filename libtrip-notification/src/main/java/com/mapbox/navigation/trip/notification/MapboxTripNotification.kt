package com.mapbox.navigation.trip.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.Location
import android.os.Build
import android.text.SpannableString
import android.text.TextUtils
import android.text.format.DateFormat
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.mapbox.api.directions.v5.models.LegStep
import com.mapbox.navigation.base.trip.TripNotification
import com.mapbox.navigation.model.RouteProgress
import com.mapbox.navigation.model.formatter.distance.DistanceFormatter
import com.mapbox.navigation.model.formatter.time.TimeFormatter.formatTime
import com.mapbox.navigation.utils.constants.NavigationConstants
import com.mapbox.navigation.utils.extensions.ifNonNull
import java.util.Calendar

const val NAVIGATION_NOTIFICATION_ID = 5678

internal class MapboxTripNotification(
    private val applicationContext: Context,
    private val navigationOptions: MapboxNavigationOptions,
    private val distanceFormatter: DistanceFormatter
) : TripNotification {

    private val END_NAVIGATION_ACTION = "com.mapbox.intent.action.END_NAVIGATION"
    private val SET_BACKGROUND_COLOR = "setBackgroundColor"

    private var notificationManager: NotificationManager? = null
    private lateinit var notification: Notification
    private var collapsedNotificationRemoteViews: RemoteViews? = null
    private var expandedNotificationRemoteViews: RemoteViews? = null
    private var currentDistanceText: SpannableString? = null
    private var instructionText: String? = null
    private var currentManeuverId: Int = 0
    private var isTwentyFourHourFormat: Boolean = false
    private var etaFormat: String = ""
    private var pendingOpenIntent: PendingIntent? = null
    private var pendingCloseIntent: PendingIntent? = null

    private val endNavigationBtnReceiver = object : BroadcastReceiver() {
        override fun onReceive(applicationContext: Context, intent: Intent) {
            // this@MapboxNavigationNotification.onEndNavigationBtnClick()
        }
    }

    init {
        initialize()
    }

    override fun getNotification(): Notification = notification

    override fun getNotificationId(): Int = NAVIGATION_NOTIFICATION_ID

    override fun updateRouteProgress(routeProgress: RouteProgress?) {
        routeProgress?.let { progress ->
            updateNotificationViews(progress)
            rebuildNotification()
        }
    }

    override fun updateLocation(rawLocation: Location, enhancedLocation: Location) {
    }

    override fun onTripSessionStopped() {
        unregisterReceiver(applicationContext)
    }

    // Package private (no modifier) for testing purposes
    fun generateArrivalTime(routeProgress: RouteProgress, time: Calendar): String? =
            ifNonNull(routeProgress.currentLegProgress()) { currentLegProgress ->
                val legDurationRemaining = currentLegProgress.durationRemaining() ?: 0.0
                val timeFormatType = navigationOptions.timeFormatType()
                val arrivalTime =
                        formatTime(time, legDurationRemaining, timeFormatType, isTwentyFourHourFormat)
                String.format(etaFormat, arrivalTime)
            }

    // Package private (no modifier) for testing purposes
    fun updateNotificationViews(routeProgress: RouteProgress) {
        buildRemoteViews()
        updateInstructionText(routeProgress.bannerInstruction())
        updateDistanceText(routeProgress)
        val time = Calendar.getInstance()
        generateArrivalTime(routeProgress, time)?.let { formattedTime ->
            updateViewsWithArrival(formattedTime)
            routeProgress.currentLegProgress()?.upComingStep()?.let { step ->
                routeProgress.currentLegProgress()?.upComingStep()
                updateManeuverImage(step)
            } ?: routeProgress.currentLegProgress()?.currentStep()
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

    private fun initialize() {
        etaFormat = applicationContext.getString(R.string.eta_format)
        applicationContext.getSystemService(Context.NOTIFICATION_SERVICE)
                ?.let { notificationService ->
                    notificationManager = notificationService as NotificationManager
                }
        isTwentyFourHourFormat = DateFormat.is24HourFormat(applicationContext)

        pendingOpenIntent = createPendingOpenIntent()
        pendingCloseIntent = createPendingCloseIntent()

        registerReceiver()
        createNotificationChannel()
        if (!::notification.isInitialized) {
            notification = buildNotification()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                    NavigationConstants.NAVIGATION_NOTIFICATION_CHANNEL,
                    applicationContext.getString(R.string.channel_name),
                    NotificationManager.IMPORTANCE_LOW
            )
            notificationManager?.createNotificationChannel(notificationChannel)
        }
    }

    private fun buildNotification(): Notification {
        val channelId =
                NavigationConstants.NAVIGATION_NOTIFICATION_CHANNEL
        val builder = NotificationCompat.Builder(applicationContext, channelId)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setSmallIcon(R.drawable.ic_navigation)
                .setCustomContentView(collapsedNotificationRemoteViews)
                .setCustomBigContentView(expandedNotificationRemoteViews)
                .setOngoing(true)

        pendingOpenIntent?.let { pendingOpenIntent ->
            builder.setContentIntent(pendingOpenIntent)
        }
        return builder.build()
    }

    private fun buildRemoteViews() {
        val colorResId = navigationOptions.defaultNotificationColorId()
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

    private fun createPendingOpenIntent(): PendingIntent? {
        val pm = applicationContext.packageManager
        val intent = pm.getLaunchIntentForPackage(applicationContext.packageName) ?: return null
        intent.setPackage(null)
        return PendingIntent.getActivity(applicationContext, 0, intent, 0)
    }

    private fun createPendingCloseIntent(): PendingIntent? {
        val endNavigationBtn = Intent(END_NAVIGATION_ACTION)
        return PendingIntent.getBroadcast(applicationContext, 0, endNavigationBtn, 0)
    }

    private fun registerReceiver() {
        applicationContext.registerReceiver(
                endNavigationBtnReceiver,
                IntentFilter(END_NAVIGATION_ACTION)
        )
    }

    private fun rebuildNotification() {
        notification = buildNotification()
        notificationManager?.notify(NavigationConstants.NAVIGATION_NOTIFICATION_ID, notification)
    }

    private fun unregisterReceiver(applicationContext: Context?) {
        applicationContext?.unregisterReceiver(endNavigationBtnReceiver)
        notificationManager?.cancel(NavigationConstants.NAVIGATION_NOTIFICATION_ID)
    }

    private fun updateInstructionText(bannerInstruction: com.mapbox.navigation.model.BannerInstruction?) {
        if (bannerInstruction != null && (instructionText == null || newInstructionText(
                        bannerInstruction
                ))
        ) {
            updateViewsWithInstruction(bannerInstruction.primary.text)
            instructionText = bannerInstruction.primary.text
        }
    }

    private fun updateViewsWithInstruction(text: String?) {
        collapsedNotificationRemoteViews?.setTextViewText(R.id.notificationInstructionText, text)
        expandedNotificationRemoteViews?.setTextViewText(R.id.notificationInstructionText, text)
    }

    private fun newInstructionText(bannerInstruction: com.mapbox.navigation.model.BannerInstruction): Boolean {
        return instructionText != bannerInstruction.primary.text
    }

    private fun updateDistanceText(routeProgress: RouteProgress) {
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

    private fun newDistanceText(routeProgress: RouteProgress) =
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

    private fun updateViewsWithArrival(time: String) {
        collapsedNotificationRemoteViews?.setTextViewText(R.id.notificationArrivalText, time)
        expandedNotificationRemoteViews?.setTextViewText(R.id.notificationArrivalText, time)
    }

    private fun updateManeuverImage(step: LegStep) {
        val maneuverResource = getManeuverResource(step)
        if (currentManeuverId != maneuverResource) {
            currentManeuverId = maneuverResource
            collapsedNotificationRemoteViews?.setImageViewResource(
                    R.id.maneuverImage,
                    maneuverResource
            )
            expandedNotificationRemoteViews?.setImageViewResource(
                    R.id.maneuverImage,
                    maneuverResource
            )
        }
    }

    private fun getManeuverResource(step: LegStep): Int {
        val maneuver = step.maneuver()
        val maneuverType = maneuver.type()
        val maneuverModifier = maneuver.modifier()
        if (!TextUtils.isEmpty(maneuverModifier)) {
            val drivingSide = step.drivingSide()
            return if (isLeftDrivingSideAndRoundaboutOrRotaryOrUturn(
                            maneuverType,
                            maneuverModifier,
                            drivingSide
                    )
            ) {
                obtainManeuverResourceFrom(maneuverType + maneuverModifier + drivingSide)
            } else obtainManeuverResourceFrom(maneuverType + maneuverModifier)
        }
        return obtainManeuverResourceFrom(maneuverType)
    }

    private fun obtainManeuverResourceFrom(maneuver: String?): Int {
        when (maneuver) {
            NavigationConstants.STEP_MANEUVER_TYPE_TURN + NavigationConstants.STEP_MANEUVER_MODIFIER_UTURN, NavigationConstants.STEP_MANEUVER_TYPE_CONTINUE + NavigationConstants.STEP_MANEUVER_MODIFIER_UTURN -> return R.drawable.ic_maneuver_turn_180
            NavigationConstants.STEP_MANEUVER_TYPE_TURN + NavigationConstants.STEP_MANEUVER_MODIFIER_UTURN + NavigationConstants.STEP_MANEUVER_MODIFIER_LEFT, NavigationConstants.STEP_MANEUVER_TYPE_CONTINUE + NavigationConstants.STEP_MANEUVER_MODIFIER_UTURN + NavigationConstants.STEP_MANEUVER_MODIFIER_LEFT -> return R.drawable.ic_maneuver_turn_180_left_driving_side

            NavigationConstants.STEP_MANEUVER_TYPE_ARRIVE + NavigationConstants.STEP_MANEUVER_MODIFIER_LEFT -> return R.drawable.ic_maneuver_arrive_left
            NavigationConstants.STEP_MANEUVER_TYPE_ARRIVE + NavigationConstants.STEP_MANEUVER_MODIFIER_RIGHT -> return R.drawable.ic_maneuver_arrive_right
            NavigationConstants.STEP_MANEUVER_TYPE_ARRIVE -> return R.drawable.ic_maneuver_arrive

            NavigationConstants.STEP_MANEUVER_TYPE_DEPART + NavigationConstants.STEP_MANEUVER_MODIFIER_LEFT -> return R.drawable.ic_maneuver_depart_left
            NavigationConstants.STEP_MANEUVER_TYPE_DEPART + NavigationConstants.STEP_MANEUVER_MODIFIER_RIGHT -> return R.drawable.ic_maneuver_depart_right
            NavigationConstants.STEP_MANEUVER_TYPE_DEPART -> return R.drawable.ic_maneuver_depart

            NavigationConstants.STEP_MANEUVER_TYPE_TURN + NavigationConstants.STEP_MANEUVER_MODIFIER_SHARP_RIGHT, NavigationConstants.STEP_MANEUVER_TYPE_ON_RAMP + NavigationConstants.STEP_MANEUVER_MODIFIER_SHARP_RIGHT, NavigationConstants.STEP_MANEUVER_TYPE_NOTIFICATION + NavigationConstants.STEP_MANEUVER_MODIFIER_SHARP_RIGHT -> return R.drawable.ic_maneuver_turn_75
            NavigationConstants.STEP_MANEUVER_TYPE_TURN + NavigationConstants.STEP_MANEUVER_MODIFIER_RIGHT, NavigationConstants.STEP_MANEUVER_TYPE_ON_RAMP + NavigationConstants.STEP_MANEUVER_MODIFIER_RIGHT, NavigationConstants.STEP_MANEUVER_TYPE_ROUNDABOUT_TURN + NavigationConstants.STEP_MANEUVER_MODIFIER_RIGHT, NavigationConstants.STEP_MANEUVER_TYPE_NOTIFICATION + NavigationConstants.STEP_MANEUVER_MODIFIER_RIGHT -> return R.drawable.ic_maneuver_turn_45
            NavigationConstants.STEP_MANEUVER_TYPE_TURN + NavigationConstants.STEP_MANEUVER_MODIFIER_SLIGHT_RIGHT, NavigationConstants.STEP_MANEUVER_TYPE_ON_RAMP + NavigationConstants.STEP_MANEUVER_MODIFIER_SLIGHT_RIGHT, NavigationConstants.STEP_MANEUVER_TYPE_NOTIFICATION + NavigationConstants.STEP_MANEUVER_MODIFIER_SLIGHT_RIGHT -> return R.drawable.ic_maneuver_turn_30

            NavigationConstants.STEP_MANEUVER_TYPE_TURN + NavigationConstants.STEP_MANEUVER_MODIFIER_SHARP_LEFT, NavigationConstants.STEP_MANEUVER_TYPE_ON_RAMP + NavigationConstants.STEP_MANEUVER_MODIFIER_SHARP_LEFT, NavigationConstants.STEP_MANEUVER_TYPE_NOTIFICATION + NavigationConstants.STEP_MANEUVER_MODIFIER_SHARP_LEFT -> return R.drawable.ic_maneuver_turn_75_left
            NavigationConstants.STEP_MANEUVER_TYPE_TURN + NavigationConstants.STEP_MANEUVER_MODIFIER_LEFT, NavigationConstants.STEP_MANEUVER_TYPE_ON_RAMP + NavigationConstants.STEP_MANEUVER_MODIFIER_LEFT, NavigationConstants.STEP_MANEUVER_TYPE_NOTIFICATION + NavigationConstants.STEP_MANEUVER_MODIFIER_LEFT, NavigationConstants.STEP_MANEUVER_TYPE_ROUNDABOUT_TURN + NavigationConstants.STEP_MANEUVER_MODIFIER_LEFT -> return R.drawable.ic_maneuver_turn_45_left
            NavigationConstants.STEP_MANEUVER_TYPE_TURN + NavigationConstants.STEP_MANEUVER_MODIFIER_SLIGHT_LEFT, NavigationConstants.STEP_MANEUVER_TYPE_ON_RAMP + NavigationConstants.STEP_MANEUVER_MODIFIER_SLIGHT_LEFT, NavigationConstants.STEP_MANEUVER_TYPE_NOTIFICATION + NavigationConstants.STEP_MANEUVER_MODIFIER_SLIGHT_LEFT -> return R.drawable.ic_maneuver_turn_30_left

            NavigationConstants.STEP_MANEUVER_TYPE_MERGE + NavigationConstants.STEP_MANEUVER_MODIFIER_LEFT, NavigationConstants.STEP_MANEUVER_TYPE_MERGE + NavigationConstants.STEP_MANEUVER_MODIFIER_SLIGHT_LEFT -> return R.drawable.ic_maneuver_merge_left
            NavigationConstants.STEP_MANEUVER_TYPE_MERGE + NavigationConstants.STEP_MANEUVER_MODIFIER_RIGHT, NavigationConstants.STEP_MANEUVER_TYPE_MERGE + NavigationConstants.STEP_MANEUVER_MODIFIER_SLIGHT_RIGHT -> return R.drawable.ic_maneuver_merge_right

            NavigationConstants.STEP_MANEUVER_TYPE_OFF_RAMP + NavigationConstants.STEP_MANEUVER_MODIFIER_LEFT -> return R.drawable.ic_maneuver_off_ramp_left
            NavigationConstants.STEP_MANEUVER_TYPE_OFF_RAMP + NavigationConstants.STEP_MANEUVER_MODIFIER_SLIGHT_LEFT -> return R.drawable.ic_maneuver_off_ramp_slight_left

            NavigationConstants.STEP_MANEUVER_TYPE_OFF_RAMP + NavigationConstants.STEP_MANEUVER_MODIFIER_RIGHT -> return R.drawable.ic_maneuver_off_ramp_right
            NavigationConstants.STEP_MANEUVER_TYPE_OFF_RAMP + NavigationConstants.STEP_MANEUVER_MODIFIER_SLIGHT_RIGHT -> return R.drawable.ic_maneuver_off_ramp_slight_right

            NavigationConstants.STEP_MANEUVER_TYPE_FORK + NavigationConstants.STEP_MANEUVER_MODIFIER_LEFT -> return R.drawable.ic_maneuver_fork_left
            NavigationConstants.STEP_MANEUVER_TYPE_FORK + NavigationConstants.STEP_MANEUVER_MODIFIER_SLIGHT_LEFT -> return R.drawable.ic_maneuver_fork_slight_left
            NavigationConstants.STEP_MANEUVER_TYPE_FORK + NavigationConstants.STEP_MANEUVER_MODIFIER_RIGHT -> return R.drawable.ic_maneuver_fork_right
            NavigationConstants.STEP_MANEUVER_TYPE_FORK + NavigationConstants.STEP_MANEUVER_MODIFIER_SLIGHT_RIGHT -> return R.drawable.ic_maneuver_fork_slight_right
            NavigationConstants.STEP_MANEUVER_TYPE_FORK + NavigationConstants.STEP_MANEUVER_MODIFIER_STRAIGHT -> return R.drawable.ic_maneuver_fork_straight
            NavigationConstants.STEP_MANEUVER_TYPE_FORK -> return R.drawable.ic_maneuver_fork

            NavigationConstants.STEP_MANEUVER_TYPE_END_OF_ROAD + NavigationConstants.STEP_MANEUVER_MODIFIER_LEFT -> return R.drawable.ic_maneuver_end_of_road_left
            NavigationConstants.STEP_MANEUVER_TYPE_END_OF_ROAD + NavigationConstants.STEP_MANEUVER_MODIFIER_RIGHT -> return R.drawable.ic_maneuver_end_of_road_right

            NavigationConstants.STEP_MANEUVER_TYPE_ROUNDABOUT + NavigationConstants.STEP_MANEUVER_MODIFIER_LEFT, NavigationConstants.STEP_MANEUVER_TYPE_ROTARY + NavigationConstants.STEP_MANEUVER_MODIFIER_LEFT -> return R.drawable.ic_maneuver_roundabout_left
            NavigationConstants.STEP_MANEUVER_TYPE_ROUNDABOUT + NavigationConstants.STEP_MANEUVER_MODIFIER_SHARP_LEFT, NavigationConstants.STEP_MANEUVER_TYPE_ROTARY + NavigationConstants.STEP_MANEUVER_MODIFIER_SHARP_LEFT -> return R.drawable.ic_maneuver_roundabout_sharp_left
            NavigationConstants.STEP_MANEUVER_TYPE_ROUNDABOUT + NavigationConstants.STEP_MANEUVER_MODIFIER_SLIGHT_LEFT, NavigationConstants.STEP_MANEUVER_TYPE_ROTARY + NavigationConstants.STEP_MANEUVER_MODIFIER_SLIGHT_LEFT -> return R.drawable.ic_maneuver_roundabout_slight_left
            NavigationConstants.STEP_MANEUVER_TYPE_ROUNDABOUT + NavigationConstants.STEP_MANEUVER_MODIFIER_RIGHT, NavigationConstants.STEP_MANEUVER_TYPE_ROTARY + NavigationConstants.STEP_MANEUVER_MODIFIER_RIGHT -> return R.drawable.ic_maneuver_roundabout_right
            NavigationConstants.STEP_MANEUVER_TYPE_ROUNDABOUT + NavigationConstants.STEP_MANEUVER_MODIFIER_SHARP_RIGHT, NavigationConstants.STEP_MANEUVER_TYPE_ROTARY + NavigationConstants.STEP_MANEUVER_MODIFIER_SHARP_RIGHT -> return R.drawable.ic_maneuver_roundabout_sharp_right
            NavigationConstants.STEP_MANEUVER_TYPE_ROUNDABOUT + NavigationConstants.STEP_MANEUVER_MODIFIER_SLIGHT_RIGHT, NavigationConstants.STEP_MANEUVER_TYPE_ROTARY + NavigationConstants.STEP_MANEUVER_MODIFIER_SLIGHT_RIGHT -> return R.drawable.ic_maneuver_roundabout_slight_right
            NavigationConstants.STEP_MANEUVER_TYPE_ROUNDABOUT + NavigationConstants.STEP_MANEUVER_MODIFIER_STRAIGHT, NavigationConstants.STEP_MANEUVER_TYPE_ROTARY + NavigationConstants.STEP_MANEUVER_MODIFIER_STRAIGHT -> return R.drawable.ic_maneuver_roundabout_straight
            NavigationConstants.STEP_MANEUVER_TYPE_ROUNDABOUT, NavigationConstants.STEP_MANEUVER_TYPE_ROTARY -> return R.drawable.ic_maneuver_roundabout

            NavigationConstants.STEP_MANEUVER_TYPE_ROUNDABOUT + NavigationConstants.STEP_MANEUVER_MODIFIER_LEFT + NavigationConstants.STEP_MANEUVER_MODIFIER_LEFT, NavigationConstants.STEP_MANEUVER_TYPE_ROTARY + NavigationConstants.STEP_MANEUVER_MODIFIER_LEFT + NavigationConstants.STEP_MANEUVER_MODIFIER_LEFT -> return R.drawable.ic_maneuver_roundabout_left_left_driving_side
            NavigationConstants.STEP_MANEUVER_TYPE_ROUNDABOUT + NavigationConstants.STEP_MANEUVER_MODIFIER_SHARP_LEFT + NavigationConstants.STEP_MANEUVER_MODIFIER_LEFT, NavigationConstants.STEP_MANEUVER_TYPE_ROTARY + NavigationConstants.STEP_MANEUVER_MODIFIER_SHARP_LEFT + NavigationConstants.STEP_MANEUVER_MODIFIER_LEFT -> return R.drawable.ic_maneuver_roundabout_sharp_left_left_driving_side
            NavigationConstants.STEP_MANEUVER_TYPE_ROUNDABOUT + NavigationConstants.STEP_MANEUVER_MODIFIER_SLIGHT_LEFT + NavigationConstants.STEP_MANEUVER_MODIFIER_LEFT, NavigationConstants.STEP_MANEUVER_TYPE_ROTARY + NavigationConstants.STEP_MANEUVER_MODIFIER_SLIGHT_LEFT + NavigationConstants.STEP_MANEUVER_MODIFIER_LEFT -> return R.drawable.ic_maneuver_roundabout_slight_left_left_driving_side
            NavigationConstants.STEP_MANEUVER_TYPE_ROUNDABOUT + NavigationConstants.STEP_MANEUVER_MODIFIER_RIGHT + NavigationConstants.STEP_MANEUVER_MODIFIER_LEFT, NavigationConstants.STEP_MANEUVER_TYPE_ROTARY + NavigationConstants.STEP_MANEUVER_MODIFIER_RIGHT + NavigationConstants.STEP_MANEUVER_MODIFIER_LEFT -> return R.drawable.ic_maneuver_roundabout_right_left_driving_side
            NavigationConstants.STEP_MANEUVER_TYPE_ROUNDABOUT + NavigationConstants.STEP_MANEUVER_MODIFIER_SHARP_RIGHT + NavigationConstants.STEP_MANEUVER_MODIFIER_LEFT, NavigationConstants.STEP_MANEUVER_TYPE_ROTARY + NavigationConstants.STEP_MANEUVER_MODIFIER_SHARP_RIGHT + NavigationConstants.STEP_MANEUVER_MODIFIER_LEFT -> return R.drawable.ic_maneuver_roundabout_sharp_right_left_driving_side
            NavigationConstants.STEP_MANEUVER_TYPE_ROUNDABOUT + NavigationConstants.STEP_MANEUVER_MODIFIER_SLIGHT_RIGHT + NavigationConstants.STEP_MANEUVER_MODIFIER_LEFT, NavigationConstants.STEP_MANEUVER_TYPE_ROTARY + NavigationConstants.STEP_MANEUVER_MODIFIER_SLIGHT_RIGHT + NavigationConstants.STEP_MANEUVER_MODIFIER_LEFT -> return R.drawable.ic_maneuver_roundabout_slight_right_left_driving_side
            NavigationConstants.STEP_MANEUVER_TYPE_ROUNDABOUT + NavigationConstants.STEP_MANEUVER_MODIFIER_STRAIGHT + NavigationConstants.STEP_MANEUVER_MODIFIER_LEFT, NavigationConstants.STEP_MANEUVER_TYPE_ROTARY + NavigationConstants.STEP_MANEUVER_MODIFIER_STRAIGHT + NavigationConstants.STEP_MANEUVER_MODIFIER_LEFT -> return R.drawable.ic_maneuver_roundabout_straight_left_driving_side

            NavigationConstants.STEP_MANEUVER_TYPE_MERGE + NavigationConstants.STEP_MANEUVER_MODIFIER_STRAIGHT, NavigationConstants.STEP_MANEUVER_TYPE_NOTIFICATION + NavigationConstants.STEP_MANEUVER_MODIFIER_STRAIGHT, NavigationConstants.STEP_MANEUVER_TYPE_CONTINUE + NavigationConstants.STEP_MANEUVER_MODIFIER_STRAIGHT, NavigationConstants.STEP_MANEUVER_TYPE_NEW_NAME + NavigationConstants.STEP_MANEUVER_MODIFIER_STRAIGHT -> return R.drawable.ic_maneuver_turn_0
            else -> return R.drawable.ic_maneuver_turn_0
        }
    }

    private fun isLeftDrivingSideAndRoundaboutOrRotaryOrUturn(
        maneuverType: String?,
        maneuverModifier: String?,
        drivingSide: String?
    ): Boolean {
        return NavigationConstants.STEP_MANEUVER_MODIFIER_LEFT == drivingSide && (
                NavigationConstants.STEP_MANEUVER_TYPE_ROUNDABOUT == maneuverType ||
                        NavigationConstants.STEP_MANEUVER_TYPE_ROTARY == maneuverType || NavigationConstants.STEP_MANEUVER_MODIFIER_UTURN == maneuverModifier
                )
    }

    private fun onEndNavigationBtnClick() {
        // TODO: find how to send the stop event to TripSession when stopped from notification
        /*when (::mapboxNavigation.isInitialized) {
            true -> {
                mapboxNavigation.stopNavigation()
            }
            else -> {
            }
        }*/
    }
}
