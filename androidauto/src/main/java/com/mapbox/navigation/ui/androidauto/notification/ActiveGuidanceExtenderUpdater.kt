package com.mapbox.navigation.ui.androidauto.notification

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.drawable.Drawable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.TextUtils
import android.text.format.DateFormat
import androidx.car.app.notification.CarAppExtender
import androidx.core.content.ContextCompat
import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.api.directions.v5.models.ManeuverModifier
import com.mapbox.api.directions.v5.models.StepManeuver
import com.mapbox.navigation.base.TimeFormat
import com.mapbox.navigation.base.formatter.DistanceFormatter
import com.mapbox.navigation.base.internal.maneuver.TurnIconHelper
import com.mapbox.navigation.base.internal.time.TimeFormatter
import com.mapbox.navigation.base.internal.trip.notification.NotificationTurnIconResources
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.ui.androidauto.R
import java.util.Calendar

internal class ActiveGuidanceExtenderUpdater(private val context: Context) {

    @StepManeuver.StepManeuverType private var currentManeuverType: String? = null
    private var currentManeuverModifier: String? = null
    private var currentRoundaboutAngle: Float? = null
    private var currentManeuverImage: Bitmap? = null

    private var currentInstructionText: String? = null
    private var currentDistanceText: Double? = null
    private var currentFormattedDistance: SpannableString? = null
    private var currentFormattedTime: String? = null
    private val turnIconHelper = TurnIconHelper(NotificationTurnIconResources.defaultIconSet())

    fun update(
        extenderBuilder: CarAppExtender.Builder,
        routeProgress: RouteProgress,
        distanceFormatter: DistanceFormatter,
        @TimeFormat.Type timeFormatType: Int,
    ) {
        val bannerInstructions = routeProgress.bannerInstructions
        val currentLegProgress = routeProgress.currentLegProgress
        val durationRemaining = currentLegProgress?.durationRemaining
        val drivingSide = currentLegProgress?.currentStepProgress?.step?.drivingSide()

        updateDistanceText(
            currentLegProgress?.currentStepProgress?.distanceRemaining?.toDouble(),
            distanceFormatter,
        )
        updateViewsWithArrival(durationRemaining, timeFormatType)
        val titleBuilder = SpannableStringBuilder()
        currentFormattedDistance?.let { titleBuilder.append(it) }
        titleBuilder.append(" • ")
        extenderBuilder.setContentTitle(titleBuilder)
        currentFormattedTime?.let { titleBuilder.append(it) }

        updateInstructionText(bannerInstructions, extenderBuilder)
        updateManeuverImage(bannerInstructions, drivingSide, extenderBuilder)
    }

    fun updateCurrentManeuverToDefault() {
        currentManeuverType = null
        currentManeuverModifier = null
        currentRoundaboutAngle = null
    }

    private fun updateDistanceText(
        distanceRemaining: Double?,
        distanceFormatter: DistanceFormatter,
    ) {
        if (isDistanceTextChanged(distanceRemaining) && distanceRemaining != null) {
            currentDistanceText = distanceRemaining
            currentFormattedDistance = distanceFormatter.formatDistance(distanceRemaining)
        }
    }

    private fun updateViewsWithArrival(
        durationRemaining: Double?,
        @TimeFormat.Type timeFormatType: Int,
    ) {
        generateArrivalTime(durationRemaining, timeFormatType)?.let { currentFormattedTime = it }
    }

    private fun updateInstructionText(
        bannerInstructions: BannerInstructions?,
        extenderBuilder: CarAppExtender.Builder,
    ) {
        bannerInstructions?.primary()?.text()
            ?.takeIf { isInstructionTextChanged(it) }
            ?.let { currentInstructionText = it }
        currentInstructionText?.let { extenderBuilder.setContentText(it) }
    }

    private fun updateManeuverImage(
        bannerInstructions: BannerInstructions?,
        drivingSide: String?,
        extenderBuilder: CarAppExtender.Builder,
    ) {
        if (bannerInstructions != null && isManeuverStateChanged(bannerInstructions)) {
            turnIconHelper.retrieveTurnIcon(
                currentManeuverType,
                currentRoundaboutAngle,
                currentManeuverModifier,
                drivingSide = drivingSide ?: ManeuverModifier.RIGHT,
            )?.let { turnIcon ->
                turnIcon.icon
                    ?.let { ContextCompat.getDrawable(context, it) }
                    ?.let { getManeuverBitmap(it, turnIcon.shouldFlipIcon) }
                    ?.let { currentManeuverImage = it }
            }
        }
        currentManeuverImage?.let { extenderBuilder.setLargeIcon(it) }
    }

    private fun isDistanceTextChanged(distanceRemaining: Double?): Boolean {
        return currentDistanceText != distanceRemaining
    }

    private fun generateArrivalTime(
        durationRemaining: Double?,
        @TimeFormat.Type timeFormatType: Int,
    ): String? {
        val time = Calendar.getInstance()
        return durationRemaining?.let {
            val arrivalTime = TimeFormatter.formatTime(
                time,
                durationRemaining,
                timeFormatType,
                DateFormat.is24HourFormat(context),
            )
            context.getString(R.string.mapbox_eta_format, arrivalTime)
        }
    }

    private fun isInstructionTextChanged(primaryText: String): Boolean {
        return currentInstructionText.isNullOrEmpty() || currentInstructionText != primaryText
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
}
