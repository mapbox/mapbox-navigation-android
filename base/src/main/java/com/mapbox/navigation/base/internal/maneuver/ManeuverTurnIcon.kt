package com.mapbox.navigation.base.internal.maneuver

import androidx.annotation.DrawableRes

data class ManeuverTurnIcon(
    val degree: Float?,
    val drivingSide: String?,
    val shouldFlipIcon: Boolean,
    @DrawableRes val icon: Int?,
)
