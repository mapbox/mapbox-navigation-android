package com.mapbox.navigation.ui.components.speedlimit.model

import androidx.annotation.DrawableRes
import androidx.annotation.StyleRes
import com.mapbox.navigation.ui.components.R
import com.mapbox.navigation.ui.components.speedlimit.view.MapboxSpeedInfoView

/**
 * A utility class that can be used to apply background and text styling to [MapboxSpeedInfoView]
 * components.
 */
class SpeedInfoStyle {

    /**
     * Background resource applied to outer view for MUTCD convention.
     */
    @DrawableRes
    var mutcdLayoutBackground: Int = R.drawable.background_mutcd_outer_layout

    /**
     * Background resource applied to inner view for MUTCD convention.
     */
    @DrawableRes
    var postedSpeedMutcdLayoutBackground: Int = R.drawable.background_mutcd_posted_speed_limit

    /**
     * Text styling applied to posted speed legend for MUTCD convention.
     */
    @StyleRes
    var postedSpeedLegendTextAppearance: Int = R.style.MapboxSpeedInfoMutcdLegendStyle

    /**
     * Text styling applied to posted speed for MUTCD convention.
     */
    @StyleRes
    var postedSpeedMutcdTextAppearance: Int = R.style.MapboxSpeedInfoPostedSpeedMutcdStyle

    /**
     * Text styling applied to posted speed unit for MUTCD convention.
     */
    @StyleRes
    var postedSpeedUnitTextAppearance: Int = R.style.MapboxSpeedInfoMutcdUnitStyle

    /**
     * Text styling applied to current speed for MUTCD convention.
     */
    @StyleRes
    var currentSpeedMutcdTextAppearance: Int = R.style.MapboxSpeedInfoCurrentSpeedMutcdStyle

    /**
     * Background resource applied to outer view for VIENNA convention.
     */
    @DrawableRes
    var viennaLayoutBackground: Int = R.drawable.background_vienna_outer_layout

    /**
     * Background resource applied to inner view for VIENNA convention.
     */
    @DrawableRes
    var postedSpeedViennaLayoutBackground: Int = R.drawable.background_vienna_posted_speed_limit

    /**
     * Text styling applied to posted speed for VIENNA convention.
     */
    @StyleRes
    var postedSpeedViennaTextAppearance: Int = R.style.MapboxSpeedInfoPostedSpeedViennaStyle

    /**
     * Text styling applied to current speed for VIENNA convention.
     */
    @StyleRes
    var currentSpeedViennaTextAppearance: Int = R.style.MapboxSpeedInfoCurrentSpeedViennaStyle
}
