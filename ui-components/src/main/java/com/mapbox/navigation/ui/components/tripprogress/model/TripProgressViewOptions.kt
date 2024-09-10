package com.mapbox.navigation.ui.components.tripprogress.model

import android.content.res.ColorStateList
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StyleRes
import com.mapbox.navigation.ui.components.R
import com.mapbox.navigation.ui.components.tripprogress.view.DistanceRemainingView
import com.mapbox.navigation.ui.components.tripprogress.view.EstimatedArrivalTimeView
import com.mapbox.navigation.ui.components.tripprogress.view.MapboxTripProgressView
import com.mapbox.navigation.ui.components.tripprogress.view.TimeRemainingView

/**
 * Specifies options for rendering different components in [MapboxTripProgressView] .
 *
 * @param backgroundColor to style [MapboxTripProgressView] background color
 * @param distanceRemainingIcon to change distance remaining icon
 * @param estimatedArrivalTimeIcon to change estimated arrival time icon
 * @param timeRemainingTextAppearance to style [TimeRemainingView]
 * @param distanceRemainingTextAppearance to style [DistanceRemainingView]
 * @param estimatedArrivalTimeTextAppearance to style [EstimatedArrivalTimeView]
 * @param distanceRemainingIconTint to color distance remaining icon
 * @param estimatedArrivalTimeIconTint to color estimated arrival time icon
 */
class TripProgressViewOptions private constructor(
    @ColorRes val backgroundColor: Int,
    @DrawableRes val distanceRemainingIcon: Int,
    @DrawableRes val estimatedArrivalTimeIcon: Int,
    @StyleRes val timeRemainingTextAppearance: Int,
    @StyleRes val distanceRemainingTextAppearance: Int,
    @StyleRes val estimatedArrivalTimeTextAppearance: Int,
    val distanceRemainingIconTint: ColorStateList?,
    val estimatedArrivalTimeIconTint: ColorStateList?,
) {

    /**
     * @return the [Builder] that created the [TripProgressViewOptions]
     */
    fun toBuilder(): Builder = Builder()
        .backgroundColor(backgroundColor)
        .distanceRemainingIcon(distanceRemainingIcon)
        .estimatedArrivalTimeIcon(estimatedArrivalTimeIcon)
        .timeRemainingTextAppearance(timeRemainingTextAppearance)
        .distanceRemainingTextAppearance(distanceRemainingTextAppearance)
        .estimatedArrivalTimeTextAppearance(estimatedArrivalTimeTextAppearance)
        .distanceRemainingIconTint(distanceRemainingIconTint)
        .estimatedArrivalTimeIconTint(estimatedArrivalTimeIconTint)

    /**
     * Regenerate whenever a change is made
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TripProgressViewOptions

        if (backgroundColor != other.backgroundColor) return false
        if (distanceRemainingIcon != other.distanceRemainingIcon) return false
        if (estimatedArrivalTimeIcon != other.estimatedArrivalTimeIcon) return false
        if (timeRemainingTextAppearance != other.timeRemainingTextAppearance) return false
        if (distanceRemainingTextAppearance != other.distanceRemainingTextAppearance) return false
        if (estimatedArrivalTimeTextAppearance != other.estimatedArrivalTimeTextAppearance) {
            return false
        }
        if (distanceRemainingIconTint != other.distanceRemainingIconTint) return false
        if (estimatedArrivalTimeIconTint != other.estimatedArrivalTimeIconTint) return false

        return true
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun hashCode(): Int {
        var result = backgroundColor
        result = 31 * result + distanceRemainingIcon
        result = 31 * result + estimatedArrivalTimeIcon
        result = 31 * result + timeRemainingTextAppearance
        result = 31 * result + distanceRemainingTextAppearance
        result = 31 * result + estimatedArrivalTimeTextAppearance
        result = 31 * result + (distanceRemainingIconTint?.hashCode() ?: 0)
        result = 31 * result + (estimatedArrivalTimeIconTint?.hashCode() ?: 0)
        return result
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun toString(): String {
        return "TripProgressViewOptions(" +
            "backgroundColor=$backgroundColor, " +
            "distanceRemainingIcon=$distanceRemainingIcon, " +
            "estimatedArrivalTimeIcon=$estimatedArrivalTimeIcon, " +
            "timeRemainingTextAppearance=$timeRemainingTextAppearance, " +
            "distanceRemainingTextAppearance=$distanceRemainingTextAppearance, " +
            "estimatedArrivalTimeTextAppearance=$estimatedArrivalTimeTextAppearance, " +
            "distanceRemainingIconTint=$distanceRemainingIconTint, " +
            "estimatedArrivalTimeIconTint=$estimatedArrivalTimeIconTint" +
            ")"
    }

    /**
     * Builder of [TripProgressViewOptions]
     */
    class Builder {
        @ColorRes
        private var backgroundColor: Int =
            R.color.mapbox_trip_progress_view_background_color

        @DrawableRes
        private var distanceRemainingIcon: Int = R.drawable.mapbox_ic_pin

        @DrawableRes
        private var estimatedArrivalTimeIcon: Int = R.drawable.mapbox_ic_time

        @StyleRes
        private var timeRemainingTextAppearance: Int = R.style.MapboxStyleTimeRemaining

        @StyleRes
        private var distanceRemainingTextAppearance: Int =
            R.style.MapboxStyleDistanceRemaining

        @StyleRes
        private var estimatedArrivalTimeTextAppearance: Int =
            R.style.MapboxStyleEstimatedArrivalTime
        private var distanceRemainingIconTint: ColorStateList? = null
        private var estimatedArrivalTimeIconTint: ColorStateList? = null

        /**
         * Allows you to style the background color for [MapboxTripProgressView].
         *
         * @param backgroundColor background color settings
         * @return Builder
         */
        fun backgroundColor(@ColorRes backgroundColor: Int): Builder = apply {
            this.backgroundColor = backgroundColor
        }

        /**
         * Allows you to define the icon for distance remaining.
         *
         * @param distanceRemainingIcon icon settings
         * @return Builder
         */
        fun distanceRemainingIcon(@DrawableRes distanceRemainingIcon: Int): Builder = apply {
            this.distanceRemainingIcon = distanceRemainingIcon
        }

        /**
         * Allows you to define the icon for estimated arrival time.
         *
         * @param estimatedArrivalTimeIcon icon settings
         * @return Builder
         */
        fun estimatedArrivalTimeIcon(@DrawableRes estimatedArrivalTimeIcon: Int): Builder = apply {
            this.estimatedArrivalTimeIcon = estimatedArrivalTimeIcon
        }

        /**
         * Allows you to style [TimeRemainingView].
         *
         * @param timeRemainingTextAppearance text settings
         * @return Builder
         */
        fun timeRemainingTextAppearance(@StyleRes timeRemainingTextAppearance: Int): Builder =
            apply {
                this.timeRemainingTextAppearance = timeRemainingTextAppearance
            }

        /**
         * Allows you to style [DistanceRemainingView].
         *
         * @param distanceRemainingTextAppearance text settings
         * @return Builder
         */
        fun distanceRemainingTextAppearance(
            @StyleRes distanceRemainingTextAppearance: Int,
        ): Builder = apply {
            this.distanceRemainingTextAppearance = distanceRemainingTextAppearance
        }

        /**
         * Allows you to style [EstimatedArrivalTimeView].
         *
         * @param estimatedArrivalTimeTextAppearance text settings
         * @return Builder
         */
        fun estimatedArrivalTimeTextAppearance(
            @StyleRes estimatedArrivalTimeTextAppearance: Int,
        ): Builder = apply {
            this.estimatedArrivalTimeTextAppearance = estimatedArrivalTimeTextAppearance
        }

        /**
         * Allows you to style the icon tint for distance remaining.
         *
         * @param distanceRemainingIconTint icon tint settings
         * @return Builder
         */
        fun distanceRemainingIconTint(distanceRemainingIconTint: ColorStateList?): Builder = apply {
            this.distanceRemainingIconTint = distanceRemainingIconTint
        }

        /**
         * Allows you to style the icon tint for estimated arrival time.
         *
         * @param estimatedArrivalTimeIconTint icon tint settings
         * @return Builder
         */
        fun estimatedArrivalTimeIconTint(estimatedArrivalTimeIconTint: ColorStateList?): Builder =
            apply { this.estimatedArrivalTimeIconTint = estimatedArrivalTimeIconTint }

        /**
         * Build a new instance of [TripProgressViewOptions]
         *
         * @return TripProgressViewOptions
         */
        fun build(): TripProgressViewOptions {
            return TripProgressViewOptions(
                backgroundColor = backgroundColor,
                distanceRemainingIcon = distanceRemainingIcon,
                estimatedArrivalTimeIcon = estimatedArrivalTimeIcon,
                timeRemainingTextAppearance = timeRemainingTextAppearance,
                distanceRemainingTextAppearance = distanceRemainingTextAppearance,
                estimatedArrivalTimeTextAppearance = estimatedArrivalTimeTextAppearance,
                distanceRemainingIconTint = distanceRemainingIconTint,
                estimatedArrivalTimeIconTint = estimatedArrivalTimeIconTint,
            )
        }
    }
}
