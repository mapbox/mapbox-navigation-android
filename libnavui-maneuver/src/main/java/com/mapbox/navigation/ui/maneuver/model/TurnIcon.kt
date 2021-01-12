package com.mapbox.navigation.ui.maneuver.model

import androidx.annotation.DrawableRes

internal data class TurnIcon(
    val degree: Float?,
    val drivingSide: String?,
    val shouldFlipIcon: Boolean,
    @DrawableRes val icon: Int?
)
