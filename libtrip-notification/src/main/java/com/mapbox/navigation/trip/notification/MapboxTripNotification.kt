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
import android.text.format.DateFormat
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.mapbox.navigation.base.extension.ifNonNull
import com.mapbox.navigation.base.model.banner.BannerInstruction
import com.mapbox.navigation.base.model.route.LegStep
import com.mapbox.navigation.base.model.route.Route
import com.mapbox.navigation.base.model.route.RouteConstants.IMPERIAL
import com.mapbox.navigation.base.model.route.RouteConstants.STEP_MANEUVER_MODIFIER_LEFT
import com.mapbox.navigation.base.model.route.RouteConstants.STEP_MANEUVER_MODIFIER_RIGHT
import com.mapbox.navigation.base.model.route.RouteConstants.STEP_MANEUVER_MODIFIER_SHARP_LEFT
import com.mapbox.navigation.base.model.route.RouteConstants.STEP_MANEUVER_MODIFIER_SHARP_RIGHT
import com.mapbox.navigation.base.model.route.RouteConstants.STEP_MANEUVER_MODIFIER_SLIGHT_LEFT
import com.mapbox.navigation.base.model.route.RouteConstants.STEP_MANEUVER_MODIFIER_SLIGHT_RIGHT
import com.mapbox.navigation.base.model.route.RouteConstants.STEP_MANEUVER_MODIFIER_STRAIGHT
import com.mapbox.navigation.base.model.route.RouteConstants.STEP_MANEUVER_MODIFIER_UTURN
import com.mapbox.navigation.base.model.route.RouteConstants.STEP_MANEUVER_TYPE_ARRIVE
import com.mapbox.navigation.base.model.route.RouteConstants.STEP_MANEUVER_TYPE_CONTINUE
import com.mapbox.navigation.base.model.route.RouteConstants.STEP_MANEUVER_TYPE_DEPART
import com.mapbox.navigation.base.model.route.RouteConstants.STEP_MANEUVER_TYPE_END_OF_ROAD
import com.mapbox.navigation.base.model.route.RouteConstants.STEP_MANEUVER_TYPE_FORK
import com.mapbox.navigation.base.model.route.RouteConstants.STEP_MANEUVER_TYPE_MERGE
import com.mapbox.navigation.base.model.route.RouteConstants.STEP_MANEUVER_TYPE_NEW_NAME
import com.mapbox.navigation.base.model.route.RouteConstants.STEP_MANEUVER_TYPE_NOTIFICATION
import com.mapbox.navigation.base.model.route.RouteConstants.STEP_MANEUVER_TYPE_OFF_RAMP
import com.mapbox.navigation.base.model.route.RouteConstants.STEP_MANEUVER_TYPE_ON_RAMP
import com.mapbox.navigation.base.model.route.RouteConstants.STEP_MANEUVER_TYPE_ROTARY
import com.mapbox.navigation.base.model.route.RouteConstants.STEP_MANEUVER_TYPE_ROUNDABOUT
import com.mapbox.navigation.base.model.route.RouteConstants.STEP_MANEUVER_TYPE_ROUNDABOUT_TURN
import com.mapbox.navigation.base.model.route.RouteConstants.STEP_MANEUVER_TYPE_TURN
import com.mapbox.navigation.base.model.route.RouteProgress
import com.mapbox.navigation.base.options.TripNavigationOptions
import com.mapbox.navigation.base.trip.TripNotification
import com.mapbox.navigation.model.formatter.distance.getUnitTypeForLocale
import com.mapbox.navigation.utils.extensions.inferDeviceLanguage
import com.mapbox.navigation.utils.extensions.inferDeviceLocale
import com.mapbox.navigation.utils.formatter.distance.DistanceFormatter
import com.mapbox.navigation.utils.formatter.time.TimeFormatter.formatTime
import java.util.Calendar

class MapboxTripNotification(
    private val applicationContext: Context,
    private val tripNavigationOptions: TripNavigationOptions,
    private var currentRoute: Route
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
    private var distanceFormatter: DistanceFormatter? = null

    private val endNavigationBtnReceiver = object : BroadcastReceiver() {
        override fun onReceive(applicationContext: Context, intent: Intent) {
            // this@MapboxNavigationNotification.onEndNavigationBtnClick()
        }
    }

    companion object {
        private const val NAVIGATION_NOTIFICATION_ID = 5678
        private const val NAVIGATION_NOTIFICATION_CHANNEL = "NAVIGATION_NOTIFICATION_CHANNEL"
    }

    init {
        etaFormat = applicationContext.getString(R.string.eta_format)
        initializeDistanceFormatter()
        notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        isTwentyFourHourFormat = DateFormat.is24HourFormat(applicationContext)

        pendingOpenIntent = createPendingOpenIntent()
        pendingCloseIntent = createPendingCloseIntent()

        registerReceiver()
        createNotificationChannel()
        if (!::notification.isInitialized) {
            notification = buildNotification()
        }
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
        TODO("not implemented")
    }

    override fun onTripSessionStopped(context: Context) {
        unregisterReceiver(context)
    }

    // Package private (no modifier) for testing purposes
    fun generateArrivalTime(routeProgress: RouteProgress, time: Calendar): String? =
            ifNonNull(routeProgress.currentLegProgress()) { currentLegProgress ->
                val legDurationRemaining = currentLegProgress.durationRemaining() ?: 0.0
                val timeFormatType = tripNavigationOptions.timeFormatType()
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

    private fun initializeDistanceFormatter() {
        val routeOptions = currentRoute.routeOptions()
        var language: String = applicationContext.inferDeviceLanguage()
        var unitType: String = applicationContext.inferDeviceLocale().getUnitTypeForLocale()
        routeOptions?.let { options ->
            language = options.language() ?: "en"
            unitType = options.voiceUnits() ?: IMPERIAL
        }
        val roundingIncrement = tripNavigationOptions.roundingIncrement()

        distanceFormatter =
                DistanceFormatter(applicationContext, language, unitType, roundingIncrement)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                    NAVIGATION_NOTIFICATION_CHANNEL,
                    applicationContext.getString(R.string.channel_name),
                    NotificationManager.IMPORTANCE_LOW
            )
            notificationManager?.createNotificationChannel(notificationChannel)
        }
    }

    private fun buildNotification(): Notification {
        val channelId = NAVIGATION_NOTIFICATION_CHANNEL
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
        val colorResId = tripNavigationOptions.defaultNotificationColorId()
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
        notificationManager?.notify(NAVIGATION_NOTIFICATION_ID, notification)
    }

    private fun unregisterReceiver(applicationContext: Context?) {
        applicationContext?.unregisterReceiver(endNavigationBtnReceiver)
        notificationManager?.cancel(NAVIGATION_NOTIFICATION_ID)
    }

    private fun updateInstructionText(bannerInstruction: BannerInstruction?) {
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

    private fun newInstructionText(bannerInstruction: BannerInstruction): Boolean {
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
        val maneuver = step.stepManeuver()
        val maneuverType = maneuver.type()
        val maneuverModifier = maneuver.modifier()
        if (!maneuverModifier.isNullOrEmpty()) {
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

    private fun isLeftDrivingSideAndRoundaboutOrRotaryOrUturn(
        maneuverType: String?,
        maneuverModifier: String?,
        drivingSide: String?
    ) = STEP_MANEUVER_MODIFIER_LEFT == drivingSide && (
            STEP_MANEUVER_TYPE_ROUNDABOUT == maneuverType ||
                    STEP_MANEUVER_TYPE_ROTARY == maneuverType || STEP_MANEUVER_MODIFIER_UTURN == maneuverModifier
            )

    private fun obtainManeuverResourceFrom(maneuver: String?): Int =
            when (maneuver) {
                STEP_MANEUVER_TYPE_TURN + STEP_MANEUVER_MODIFIER_UTURN, STEP_MANEUVER_TYPE_CONTINUE + STEP_MANEUVER_MODIFIER_UTURN -> R.drawable.ic_maneuver_turn_180
                STEP_MANEUVER_TYPE_TURN + STEP_MANEUVER_MODIFIER_UTURN + STEP_MANEUVER_MODIFIER_LEFT, STEP_MANEUVER_TYPE_CONTINUE + STEP_MANEUVER_MODIFIER_UTURN + STEP_MANEUVER_MODIFIER_LEFT -> R.drawable.ic_maneuver_turn_180_left_driving_side

                STEP_MANEUVER_TYPE_ARRIVE + STEP_MANEUVER_MODIFIER_LEFT -> R.drawable.ic_maneuver_arrive_left
                STEP_MANEUVER_TYPE_ARRIVE + STEP_MANEUVER_MODIFIER_RIGHT -> R.drawable.ic_maneuver_arrive_right
                STEP_MANEUVER_TYPE_ARRIVE -> R.drawable.ic_maneuver_arrive

                STEP_MANEUVER_TYPE_DEPART + STEP_MANEUVER_MODIFIER_LEFT -> R.drawable.ic_maneuver_depart_left
                STEP_MANEUVER_TYPE_DEPART + STEP_MANEUVER_MODIFIER_RIGHT -> R.drawable.ic_maneuver_depart_right
                STEP_MANEUVER_TYPE_DEPART -> R.drawable.ic_maneuver_depart

                STEP_MANEUVER_TYPE_TURN + STEP_MANEUVER_MODIFIER_SHARP_RIGHT, STEP_MANEUVER_TYPE_ON_RAMP + STEP_MANEUVER_MODIFIER_SHARP_RIGHT, STEP_MANEUVER_TYPE_NOTIFICATION + STEP_MANEUVER_MODIFIER_SHARP_RIGHT -> R.drawable.ic_maneuver_turn_75
                STEP_MANEUVER_TYPE_TURN + STEP_MANEUVER_MODIFIER_RIGHT, STEP_MANEUVER_TYPE_ON_RAMP + STEP_MANEUVER_MODIFIER_RIGHT, STEP_MANEUVER_TYPE_ROUNDABOUT_TURN + STEP_MANEUVER_MODIFIER_RIGHT, STEP_MANEUVER_TYPE_NOTIFICATION + STEP_MANEUVER_MODIFIER_RIGHT -> R.drawable.ic_maneuver_turn_45
                STEP_MANEUVER_TYPE_TURN + STEP_MANEUVER_MODIFIER_SLIGHT_RIGHT, STEP_MANEUVER_TYPE_ON_RAMP + STEP_MANEUVER_MODIFIER_SLIGHT_RIGHT, STEP_MANEUVER_TYPE_NOTIFICATION + STEP_MANEUVER_MODIFIER_SLIGHT_RIGHT -> R.drawable.ic_maneuver_turn_30

                STEP_MANEUVER_TYPE_TURN + STEP_MANEUVER_MODIFIER_SHARP_LEFT, STEP_MANEUVER_TYPE_ON_RAMP + STEP_MANEUVER_MODIFIER_SHARP_LEFT, STEP_MANEUVER_TYPE_NOTIFICATION + STEP_MANEUVER_MODIFIER_SHARP_LEFT -> R.drawable.ic_maneuver_turn_75_left
                STEP_MANEUVER_TYPE_TURN + STEP_MANEUVER_MODIFIER_LEFT, STEP_MANEUVER_TYPE_ON_RAMP + STEP_MANEUVER_MODIFIER_LEFT, STEP_MANEUVER_TYPE_NOTIFICATION + STEP_MANEUVER_MODIFIER_LEFT, STEP_MANEUVER_TYPE_ROUNDABOUT_TURN + STEP_MANEUVER_MODIFIER_LEFT -> R.drawable.ic_maneuver_turn_45_left
                STEP_MANEUVER_TYPE_TURN + STEP_MANEUVER_MODIFIER_SLIGHT_LEFT, STEP_MANEUVER_TYPE_ON_RAMP + STEP_MANEUVER_MODIFIER_SLIGHT_LEFT, STEP_MANEUVER_TYPE_NOTIFICATION + STEP_MANEUVER_MODIFIER_SLIGHT_LEFT -> R.drawable.ic_maneuver_turn_30_left

                STEP_MANEUVER_TYPE_MERGE + STEP_MANEUVER_MODIFIER_LEFT, STEP_MANEUVER_TYPE_MERGE + STEP_MANEUVER_MODIFIER_SLIGHT_LEFT -> R.drawable.ic_maneuver_merge_left
                STEP_MANEUVER_TYPE_MERGE + STEP_MANEUVER_MODIFIER_RIGHT, STEP_MANEUVER_TYPE_MERGE + STEP_MANEUVER_MODIFIER_SLIGHT_RIGHT -> R.drawable.ic_maneuver_merge_right

                STEP_MANEUVER_TYPE_OFF_RAMP + STEP_MANEUVER_MODIFIER_LEFT -> R.drawable.ic_maneuver_off_ramp_left
                STEP_MANEUVER_TYPE_OFF_RAMP + STEP_MANEUVER_MODIFIER_SLIGHT_LEFT -> R.drawable.ic_maneuver_off_ramp_slight_left

                STEP_MANEUVER_TYPE_OFF_RAMP + STEP_MANEUVER_MODIFIER_RIGHT -> R.drawable.ic_maneuver_off_ramp_right
                STEP_MANEUVER_TYPE_OFF_RAMP + STEP_MANEUVER_MODIFIER_SLIGHT_RIGHT -> R.drawable.ic_maneuver_off_ramp_slight_right

                STEP_MANEUVER_TYPE_FORK + STEP_MANEUVER_MODIFIER_LEFT -> R.drawable.ic_maneuver_fork_left
                STEP_MANEUVER_TYPE_FORK + STEP_MANEUVER_MODIFIER_SLIGHT_LEFT -> R.drawable.ic_maneuver_fork_slight_left
                STEP_MANEUVER_TYPE_FORK + STEP_MANEUVER_MODIFIER_RIGHT -> R.drawable.ic_maneuver_fork_right
                STEP_MANEUVER_TYPE_FORK + STEP_MANEUVER_MODIFIER_SLIGHT_RIGHT -> R.drawable.ic_maneuver_fork_slight_right
                STEP_MANEUVER_TYPE_FORK + STEP_MANEUVER_MODIFIER_STRAIGHT -> R.drawable.ic_maneuver_fork_straight
                STEP_MANEUVER_TYPE_FORK -> R.drawable.ic_maneuver_fork

                STEP_MANEUVER_TYPE_END_OF_ROAD + STEP_MANEUVER_MODIFIER_LEFT -> R.drawable.ic_maneuver_end_of_road_left
                STEP_MANEUVER_TYPE_END_OF_ROAD + STEP_MANEUVER_MODIFIER_RIGHT -> R.drawable.ic_maneuver_end_of_road_right

                STEP_MANEUVER_TYPE_ROUNDABOUT + STEP_MANEUVER_MODIFIER_LEFT, STEP_MANEUVER_TYPE_ROTARY + STEP_MANEUVER_MODIFIER_LEFT -> R.drawable.ic_maneuver_roundabout_left
                STEP_MANEUVER_TYPE_ROUNDABOUT + STEP_MANEUVER_MODIFIER_SHARP_LEFT, STEP_MANEUVER_TYPE_ROTARY + STEP_MANEUVER_MODIFIER_SHARP_LEFT -> R.drawable.ic_maneuver_roundabout_sharp_left
                STEP_MANEUVER_TYPE_ROUNDABOUT + STEP_MANEUVER_MODIFIER_SLIGHT_LEFT, STEP_MANEUVER_TYPE_ROTARY + STEP_MANEUVER_MODIFIER_SLIGHT_LEFT -> R.drawable.ic_maneuver_roundabout_slight_left
                STEP_MANEUVER_TYPE_ROUNDABOUT + STEP_MANEUVER_MODIFIER_RIGHT, STEP_MANEUVER_TYPE_ROTARY + STEP_MANEUVER_MODIFIER_RIGHT -> R.drawable.ic_maneuver_roundabout_right
                STEP_MANEUVER_TYPE_ROUNDABOUT + STEP_MANEUVER_MODIFIER_SHARP_RIGHT, STEP_MANEUVER_TYPE_ROTARY + STEP_MANEUVER_MODIFIER_SHARP_RIGHT -> R.drawable.ic_maneuver_roundabout_sharp_right
                STEP_MANEUVER_TYPE_ROUNDABOUT + STEP_MANEUVER_MODIFIER_SLIGHT_RIGHT, STEP_MANEUVER_TYPE_ROTARY + STEP_MANEUVER_MODIFIER_SLIGHT_RIGHT -> R.drawable.ic_maneuver_roundabout_slight_right
                STEP_MANEUVER_TYPE_ROUNDABOUT + STEP_MANEUVER_MODIFIER_STRAIGHT, STEP_MANEUVER_TYPE_ROTARY + STEP_MANEUVER_MODIFIER_STRAIGHT -> R.drawable.ic_maneuver_roundabout_straight
                STEP_MANEUVER_TYPE_ROUNDABOUT, STEP_MANEUVER_TYPE_ROTARY -> R.drawable.ic_maneuver_roundabout

                STEP_MANEUVER_TYPE_ROUNDABOUT + STEP_MANEUVER_MODIFIER_LEFT + STEP_MANEUVER_MODIFIER_LEFT, STEP_MANEUVER_TYPE_ROTARY + STEP_MANEUVER_MODIFIER_LEFT + STEP_MANEUVER_MODIFIER_LEFT -> R.drawable.ic_maneuver_roundabout_left_left_driving_side
                STEP_MANEUVER_TYPE_ROUNDABOUT + STEP_MANEUVER_MODIFIER_SHARP_LEFT + STEP_MANEUVER_MODIFIER_LEFT, STEP_MANEUVER_TYPE_ROTARY + STEP_MANEUVER_MODIFIER_SHARP_LEFT + STEP_MANEUVER_MODIFIER_LEFT -> R.drawable.ic_maneuver_roundabout_sharp_left_left_driving_side
                STEP_MANEUVER_TYPE_ROUNDABOUT + STEP_MANEUVER_MODIFIER_SLIGHT_LEFT + STEP_MANEUVER_MODIFIER_LEFT, STEP_MANEUVER_TYPE_ROTARY + STEP_MANEUVER_MODIFIER_SLIGHT_LEFT + STEP_MANEUVER_MODIFIER_LEFT -> R.drawable.ic_maneuver_roundabout_slight_left_left_driving_side
                STEP_MANEUVER_TYPE_ROUNDABOUT + STEP_MANEUVER_MODIFIER_RIGHT + STEP_MANEUVER_MODIFIER_LEFT, STEP_MANEUVER_TYPE_ROTARY + STEP_MANEUVER_MODIFIER_RIGHT + STEP_MANEUVER_MODIFIER_LEFT -> R.drawable.ic_maneuver_roundabout_right_left_driving_side
                STEP_MANEUVER_TYPE_ROUNDABOUT + STEP_MANEUVER_MODIFIER_SHARP_RIGHT + STEP_MANEUVER_MODIFIER_LEFT, STEP_MANEUVER_TYPE_ROTARY + STEP_MANEUVER_MODIFIER_SHARP_RIGHT + STEP_MANEUVER_MODIFIER_LEFT -> R.drawable.ic_maneuver_roundabout_sharp_right_left_driving_side
                STEP_MANEUVER_TYPE_ROUNDABOUT + STEP_MANEUVER_MODIFIER_SLIGHT_RIGHT + STEP_MANEUVER_MODIFIER_LEFT, STEP_MANEUVER_TYPE_ROTARY + STEP_MANEUVER_MODIFIER_SLIGHT_RIGHT + STEP_MANEUVER_MODIFIER_LEFT -> R.drawable.ic_maneuver_roundabout_slight_right_left_driving_side
                STEP_MANEUVER_TYPE_ROUNDABOUT + STEP_MANEUVER_MODIFIER_STRAIGHT + STEP_MANEUVER_MODIFIER_LEFT, STEP_MANEUVER_TYPE_ROTARY + STEP_MANEUVER_MODIFIER_STRAIGHT + STEP_MANEUVER_MODIFIER_LEFT -> R.drawable.ic_maneuver_roundabout_straight_left_driving_side

                STEP_MANEUVER_TYPE_MERGE + STEP_MANEUVER_MODIFIER_STRAIGHT, STEP_MANEUVER_TYPE_NOTIFICATION + STEP_MANEUVER_MODIFIER_STRAIGHT, STEP_MANEUVER_TYPE_CONTINUE + STEP_MANEUVER_MODIFIER_STRAIGHT, STEP_MANEUVER_TYPE_NEW_NAME + STEP_MANEUVER_MODIFIER_STRAIGHT -> R.drawable.ic_maneuver_turn_0
                else -> R.drawable.ic_maneuver_turn_0
            }
}
