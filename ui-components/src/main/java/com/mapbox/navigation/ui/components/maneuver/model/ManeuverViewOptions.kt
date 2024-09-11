package com.mapbox.navigation.ui.components.maneuver.model

import androidx.annotation.ColorRes
import androidx.annotation.StyleRes
import com.mapbox.navigation.ui.components.R
import com.mapbox.navigation.ui.components.maneuver.view.MapboxLaneGuidance
import com.mapbox.navigation.ui.components.maneuver.view.MapboxPrimaryManeuver
import com.mapbox.navigation.ui.components.maneuver.view.MapboxSecondaryManeuver
import com.mapbox.navigation.ui.components.maneuver.view.MapboxStepDistance
import com.mapbox.navigation.ui.components.maneuver.view.MapboxSubManeuver
import com.mapbox.navigation.ui.components.maneuver.view.MapboxTurnIconManeuver

/**
 * Specifies options for rendering different components in a [Maneuver].
 *
 * @param maneuverBackgroundColor to style main maneuver background color
 * @param subManeuverBackgroundColor to style sub maneuver background color
 * @param upcomingManeuverBackgroundColor to style upcoming maneuver background color
 * @param turnIconManeuver to style the [MapboxTurnIconManeuver] turn icon colors
 * @param stepDistanceTextAppearance to style [MapboxStepDistance]
 * @param laneGuidanceTurnIconManeuver to style the [MapboxLaneGuidance] turn icon colors
 * @param primaryManeuverOptions to style [MapboxPrimaryManeuver]
 * @param secondaryManeuverOptions to style [MapboxSecondaryManeuver]
 * @param subManeuverOptions to style [MapboxSubManeuver]
 */
class ManeuverViewOptions private constructor(
    @ColorRes val maneuverBackgroundColor: Int,
    @ColorRes val subManeuverBackgroundColor: Int,
    @ColorRes val upcomingManeuverBackgroundColor: Int,
    @StyleRes val turnIconManeuver: Int,
    @StyleRes val stepDistanceTextAppearance: Int,
    @StyleRes val laneGuidanceTurnIconManeuver: Int,
    val primaryManeuverOptions: ManeuverPrimaryOptions,
    val secondaryManeuverOptions: ManeuverSecondaryOptions,
    val subManeuverOptions: ManeuverSubOptions,
) {

    /**
     * @return the [Builder] that created the [ManeuverViewOptions]
     */
    fun toBuilder(): Builder = Builder()
        .maneuverBackgroundColor(maneuverBackgroundColor)
        .subManeuverBackgroundColor(subManeuverBackgroundColor)
        .upcomingManeuverBackgroundColor(upcomingManeuverBackgroundColor)
        .turnIconManeuver(turnIconManeuver)
        .stepDistanceTextAppearance(stepDistanceTextAppearance)
        .laneGuidanceTurnIconManeuver(laneGuidanceTurnIconManeuver)
        .primaryManeuverOptions(primaryManeuverOptions)
        .secondaryManeuverOptions(secondaryManeuverOptions)
        .subManeuverOptions(subManeuverOptions)

    /**
     * Regenerate whenever a change is made
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ManeuverViewOptions

        if (maneuverBackgroundColor != other.maneuverBackgroundColor) return false
        if (subManeuverBackgroundColor != other.subManeuverBackgroundColor) return false
        if (upcomingManeuverBackgroundColor != other.upcomingManeuverBackgroundColor) return false
        if (turnIconManeuver != other.turnIconManeuver) return false
        if (stepDistanceTextAppearance != other.stepDistanceTextAppearance) return false
        if (laneGuidanceTurnIconManeuver != other.laneGuidanceTurnIconManeuver) return false
        if (primaryManeuverOptions != other.primaryManeuverOptions) return false
        if (secondaryManeuverOptions != other.secondaryManeuverOptions) return false
        if (subManeuverOptions != other.subManeuverOptions) return false

        return true
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun hashCode(): Int {
        var result = maneuverBackgroundColor
        result = 31 * result + subManeuverBackgroundColor.hashCode()
        result = 31 * result + upcomingManeuverBackgroundColor.hashCode()
        result = 31 * result + turnIconManeuver.hashCode()
        result = 31 * result + stepDistanceTextAppearance.hashCode()
        result = 31 * result + laneGuidanceTurnIconManeuver.hashCode()
        result = 31 * result + primaryManeuverOptions.hashCode()
        result = 31 * result + secondaryManeuverOptions.hashCode()
        result = 31 * result + subManeuverOptions.hashCode()
        return result
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun toString(): String {
        return "ManeuverViewOptions(" +
            "maneuverBackgroundColor=$maneuverBackgroundColor, " +
            "subManeuverBackgroundColor=$subManeuverBackgroundColor, " +
            "upcomingManeuverBackgroundColor=$upcomingManeuverBackgroundColor, " +
            "turnIconManeuver=$turnIconManeuver, " +
            "stepDistanceTextAppearance=$stepDistanceTextAppearance, " +
            "laneGuidanceTurnIconManeuver=$laneGuidanceTurnIconManeuver, " +
            "primaryManeuverOptions=$primaryManeuverOptions, " +
            "secondaryManeuverOptions=$secondaryManeuverOptions, " +
            "subManeuverOptions=$subManeuverOptions" +
            ")"
    }

    /**
     * Builder of [ManeuverViewOptions]
     */
    class Builder {

        @ColorRes private var maneuverBackgroundColor =
            R.color.mapbox_main_maneuver_background_color

        @ColorRes private var subManeuverBackgroundColor =
            R.color.mapbox_sub_maneuver_background_color

        @ColorRes private var upcomingManeuverBackgroundColor =
            R.color.mapbox_upcoming_maneuver_background_color

        @StyleRes private var turnIconManeuver = R.style.MapboxStyleTurnIconManeuver

        @StyleRes private var stepDistanceTextAppearance = R.style.MapboxStyleStepDistance

        @StyleRes private var laneGuidanceTurnIconManeuver = R.style.MapboxStyleTurnIconManeuver
        private var primaryManeuverOptions = ManeuverPrimaryOptions.Builder().build()
        private var secondaryManeuverOptions = ManeuverSecondaryOptions.Builder().build()
        private var subManeuverOptions = ManeuverSubOptions.Builder().build()

        /**
         * Allows you to style the background color for main maneuver.
         *
         * @param maneuverBackgroundColor background color settings
         * @return Builder
         */
        fun maneuverBackgroundColor(@ColorRes maneuverBackgroundColor: Int): Builder = apply {
            this.maneuverBackgroundColor = maneuverBackgroundColor
        }

        /**
         * Allows you to style the background color for sub maneuver.
         *
         * @param subManeuverBackgroundColor background color settings
         * @return Builder
         */
        fun subManeuverBackgroundColor(@ColorRes subManeuverBackgroundColor: Int): Builder = apply {
            this.subManeuverBackgroundColor = subManeuverBackgroundColor
        }

        /**
         * Allows you to style the background color for upcoming maneuvers.
         *
         * @param upcomingManeuverBackgroundColor background color settings
         * @return Builder
         */
        fun upcomingManeuverBackgroundColor(
            @ColorRes upcomingManeuverBackgroundColor: Int,
        ): Builder = apply {
            this.upcomingManeuverBackgroundColor = upcomingManeuverBackgroundColor
        }

        /**
         * Allows you to style the turn icon colors.
         *
         * @param turnIconManeuver turn icon color settings
         * @return Builder
         */
        fun turnIconManeuver(@StyleRes turnIconManeuver: Int): Builder = apply {
            this.turnIconManeuver = turnIconManeuver
        }

        /**
         * Allows you to style [MapboxStepDistance].
         *
         * @param stepDistance text settings
         * @return Builder
         */
        fun stepDistanceTextAppearance(@StyleRes stepDistanceTextAppearance: Int): Builder = apply {
            this.stepDistanceTextAppearance = stepDistanceTextAppearance
        }

        /**
         * Allows you to style the lane guidance turn icon colors.
         *
         * @param laneGuidanceTurnIconManeuver turn lane guidance icon color settings
         * @return Builder
         */
        fun laneGuidanceTurnIconManeuver(
            @StyleRes laneGuidanceTurnIconManeuver: Int,
        ): Builder = apply {
            this.laneGuidanceTurnIconManeuver = laneGuidanceTurnIconManeuver
        }

        /**
         * Allows you to style [MapboxPrimaryManeuver].
         *
         * @param primaryManeuverOptions text settings
         * @return Builder
         */
        fun primaryManeuverOptions(
            primaryManeuverOptions: ManeuverPrimaryOptions,
        ): Builder = apply {
            this.primaryManeuverOptions = primaryManeuverOptions
        }

        /**
         * Allows you to style [MapboxSecondaryManeuver].
         *
         * @param secondaryManeuverOptions text settings
         * @return Builder
         */
        fun secondaryManeuverOptions(
            secondaryManeuverOptions: ManeuverSecondaryOptions,
        ): Builder = apply {
            this.secondaryManeuverOptions = secondaryManeuverOptions
        }

        /**
         * Allows you to style [MapboxSubManeuver].
         *
         * @param subManeuverOptions text settings
         * @return Builder
         */
        fun subManeuverOptions(subManeuverOptions: ManeuverSubOptions): Builder = apply {
            this.subManeuverOptions = subManeuverOptions
        }

        /**
         * Build a new instance of [ManeuverViewOptions]
         *
         * @return ManeuverOptions
         */
        fun build() = ManeuverViewOptions(
            maneuverBackgroundColor = maneuverBackgroundColor,
            subManeuverBackgroundColor = subManeuverBackgroundColor,
            upcomingManeuverBackgroundColor = upcomingManeuverBackgroundColor,
            turnIconManeuver = turnIconManeuver,
            stepDistanceTextAppearance = stepDistanceTextAppearance,
            laneGuidanceTurnIconManeuver = laneGuidanceTurnIconManeuver,
            primaryManeuverOptions = primaryManeuverOptions,
            secondaryManeuverOptions = secondaryManeuverOptions,
            subManeuverOptions = subManeuverOptions,
        )
    }
}
