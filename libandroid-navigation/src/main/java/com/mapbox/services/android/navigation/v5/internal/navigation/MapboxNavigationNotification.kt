package com.mapbox.services.android.navigation.v5.internal.navigation

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
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.util.Pair
import com.mapbox.api.directions.v5.DirectionsCriteria.IMPERIAL
import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.services.android.navigation.R
import com.mapbox.services.android.navigation.v5.internal.navigation.maneuver.ManeuverIconHelper.DEFAULT_ROUNDABOUT_ANGLE
import com.mapbox.services.android.navigation.v5.internal.navigation.maneuver.ManeuverIconHelper.MANEUVER_ICON_DRAWER_MAP
import com.mapbox.services.android.navigation.v5.internal.navigation.maneuver.ManeuverIconHelper.MANEUVER_TYPES_WITH_NULL_MODIFIERS
import com.mapbox.services.android.navigation.v5.internal.navigation.maneuver.ManeuverIconHelper.ROUNDABOUT_MANEUVER_TYPES
import com.mapbox.services.android.navigation.v5.internal.navigation.maneuver.ManeuverIconHelper.adjustRoundaboutAngle
import com.mapbox.services.android.navigation.v5.internal.navigation.maneuver.ManeuverIconHelper.isManeuverIconNeedFlip
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation
import com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.NAVIGATION_NOTIFICATION_CHANNEL
import com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.NAVIGATION_NOTIFICATION_ID
import com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_MODIFIER_RIGHT
import com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_TYPE_ARRIVE
import com.mapbox.services.android.navigation.v5.navigation.notification.NavigationNotification
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress
import com.mapbox.services.android.navigation.v5.utils.DistanceFormatter
import com.mapbox.services.android.navigation.v5.utils.extensions.getUnitTypeForLocale
import com.mapbox.services.android.navigation.v5.utils.extensions.ifNonNull
import com.mapbox.services.android.navigation.v5.utils.extensions.inferDeviceLanguage
import com.mapbox.services.android.navigation.v5.utils.extensions.inferDeviceLocale
import com.mapbox.services.android.navigation.v5.utils.time.TimeFormatter.formatTime
import java.util.Calendar

/**
 * This is in charge of creating the persistent navigation session notification and updating it.
 */
internal class MapboxNavigationNotification : NavigationNotification {

    var currentManeuverType: String? = null
        private set
    var currentManeuverModifier: String? = null
        private set
    private var currentRoundaboutAngle = DEFAULT_ROUNDABOUT_ANGLE

    private var notificationManager: NotificationManager? = null
    private lateinit var notification: Notification
    private var collapsedNotificationRemoteViews: RemoteViews? = null
    private var expandedNotificationRemoteViews: RemoteViews? = null
    private lateinit var mapboxNavigation: MapboxNavigation
    private var currentDistanceText: SpannableString? = null
    private var distanceFormatter: DistanceFormatter? = null
    private var instructionText: String? = null
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

    override fun onNavigationStopped(context: Context) {
        unregisterReceiver(context)
    }

    // Package private (no modifier) for testing purposes
    fun generateArrivalTime(
        routeProgress: RouteProgress,
        time: Calendar = Calendar.getInstance()
    ): String? =
        ifNonNull(
            mapboxNavigation,
            routeProgress.currentLegProgress()
        ) { mapboxNavigation, currentLegProgress ->
            val options = mapboxNavigation.options()
            val legDurationRemaining = currentLegProgress.durationRemaining() ?: 0.0
            val timeFormatType = options.timeFormatType()
            val arrivalTime =
                formatTime(time, legDurationRemaining, timeFormatType, isTwentyFourHourFormat)
            String.format(etaFormat, arrivalTime)
        }

    // Package private (no modifier) for testing purposes
    fun updateNotificationViews(routeProgress: RouteProgress) {
        buildRemoteViews()
        updateInstructionText(routeProgress.bannerInstruction())
        updateDistanceText(routeProgress)
        generateArrivalTime(routeProgress)?.let { formattedTime ->
            updateViewsWithArrival(formattedTime)
        }

        routeProgress.bannerInstruction()?.let { bannerInstructions ->
            if (updateManeuverState(bannerInstructions)) {
                updateManeuverImage(
                    routeProgress.currentLegProgress?.currentStep?.drivingSide()
                        ?: STEP_MANEUVER_MODIFIER_RIGHT
                )
            }
        }
    }

    // Package private (no modifier) for testing purposes
    fun retrieveInstructionText(): String? {
        return instructionText
    }

    private fun initialize(applicationContext: Context, mapboxNavigation: MapboxNavigation) {
        this.mapboxNavigation = mapboxNavigation
        etaFormat = applicationContext.getString(R.string.eta_format)
        initializeDistanceFormatter(applicationContext, mapboxNavigation)
        applicationContext.getSystemService(Context.NOTIFICATION_SERVICE)
            ?.let { notificationService ->
                notificationManager = notificationService as NotificationManager
            }
        isTwentyFourHourFormat = DateFormat.is24HourFormat(applicationContext)

        pendingOpenIntent = createPendingOpenIntent(applicationContext)
        pendingCloseIntent = createPendingCloseIntent(applicationContext)

        registerReceiver(applicationContext)
        createNotificationChannel(applicationContext)
        if (!::notification.isInitialized) {
            notification = buildNotification(applicationContext)
        }
    }

    private fun initializeDistanceFormatter(
        applicationContext: Context,
        mapboxNavigation: MapboxNavigation
    ) {
        val routeOptions = mapboxNavigation.route.routeOptions()
        var language: String = applicationContext.inferDeviceLanguage()
        var unitType: String = applicationContext.inferDeviceLocale().getUnitTypeForLocale()
        routeOptions?.let { options ->
            language = options.language() ?: "en"
            unitType = options.voiceUnits() ?: IMPERIAL
        }

        val mapboxNavigationOptions = mapboxNavigation.options()
        val roundingIncrement = mapboxNavigationOptions.roundingIncrement()

        distanceFormatter =
            DistanceFormatter(applicationContext, language, unitType, roundingIncrement)
    }

    private fun createNotificationChannel(applicationContext: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                NAVIGATION_NOTIFICATION_CHANNEL,
                applicationContext.getString(R.string.channel_name),
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager?.createNotificationChannel(notificationChannel)
        }
    }

    private fun buildNotification(applicationContext: Context): Notification {
        val channelId =
            NAVIGATION_NOTIFICATION_CHANNEL
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
        applicationContext?.registerReceiver(
            endNavigationBtnReceiver,
            IntentFilter(END_NAVIGATION_ACTION)
        )
    }

    private fun rebuildNotification() {
        notification = buildNotification(applicationContext)
        notificationManager?.notify(NAVIGATION_NOTIFICATION_ID, notification)
    }

    private fun unregisterReceiver(applicationContext: Context?) {
        applicationContext?.unregisterReceiver(endNavigationBtnReceiver)
        notificationManager?.cancel(NAVIGATION_NOTIFICATION_ID)
    }

    private fun updateInstructionText(bannerInstruction: BannerInstructions?) {
        if (bannerInstruction != null && (instructionText == null || newInstructionText(
                bannerInstruction
            ))
        ) {
            updateViewsWithInstruction(bannerInstruction.primary().text())
            instructionText = bannerInstruction.primary().text()
        }
    }

    private fun updateViewsWithInstruction(text: String?) {
        collapsedNotificationRemoteViews?.setTextViewText(R.id.notificationInstructionText, text)
        expandedNotificationRemoteViews?.setTextViewText(R.id.notificationInstructionText, text)
    }

    private fun newInstructionText(bannerInstruction: BannerInstructions): Boolean {
        return instructionText != bannerInstruction.primary().text()
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
                currentDistanceText.toString()
            )
            expandedNotificationRemoteViews?.setTextViewText(
                R.id.notificationDistanceText,
                currentDistanceText.toString()
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
            if (str != null) {
                val formattedDistance = str.toString()
                currentDistanceText.toString() != formattedDistance
            } else
                false
        } ?: false

    private fun updateViewsWithArrival(time: String) {
        collapsedNotificationRemoteViews?.setTextViewText(R.id.notificationArrivalText, time)
        expandedNotificationRemoteViews?.setTextViewText(R.id.notificationArrivalText, time)
    }

    private fun updateManeuverImage(drivingSide: String) {
        getManeuverBitmap(
            currentManeuverType ?: "",
            currentManeuverModifier, drivingSide, currentRoundaboutAngle
        ).let { bitmap ->
            collapsedNotificationRemoteViews?.setImageViewBitmap(R.id.maneuverImage, bitmap)
            expandedNotificationRemoteViews?.setImageViewBitmap(R.id.maneuverImage, bitmap)
        }
    }

    private fun updateManeuverState(bannerInstruction: BannerInstructions): Boolean {
        val previousManeuverType = currentManeuverType
        val previousManeuverModifier = currentManeuverModifier
        val previousRoundaboutAngle = currentRoundaboutAngle

        currentManeuverType = bannerInstruction.primary().type()
        currentManeuverModifier = bannerInstruction.primary().modifier()
        currentRoundaboutAngle = if (ROUNDABOUT_MANEUVER_TYPES.contains(currentManeuverType))
            adjustRoundaboutAngle(bannerInstruction.primary().degrees()?.toFloat() ?: 0f)
        else
            DEFAULT_ROUNDABOUT_ANGLE

        return !TextUtils.equals(currentManeuverType, previousManeuverType) ||
            !TextUtils.equals(currentManeuverModifier, previousManeuverModifier) ||
            currentRoundaboutAngle != previousRoundaboutAngle
    }

    fun getManeuverBitmap(
        maneuverType: String,
        maneuverModifier: String?,
        drivingSide: String,
        roundaboutAngle: Float
    ): Bitmap {
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
        when (::mapboxNavigation.isInitialized) {
            true -> {
                mapboxNavigation.stopNavigation()
            }
            else -> {
            }
        }
    }

    companion object {
        private const val END_NAVIGATION_ACTION = "com.mapbox.intent.action.END_NAVIGATION"
        private const val SET_BACKGROUND_COLOR = "setBackgroundColor"
    }
}
