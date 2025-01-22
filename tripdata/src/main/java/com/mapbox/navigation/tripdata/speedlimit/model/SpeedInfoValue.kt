package com.mapbox.navigation.tripdata.speedlimit.model

import com.mapbox.navigation.base.speed.model.SpeedLimitSign
import com.mapbox.navigation.base.speed.model.SpeedUnit

/**
 * Represents a speed limit update to be rendered.
 * @property postedSpeed Int posted speed limit at user's current location
 * @property currentSpeed Int user's current speed
 * @property postedSpeedUnit SpeedUnit posted speed unit at user's current location
 * @property speedSignConvention SpeedLimitSign [SpeedLimitSign.MUTCD] or [SpeedLimitSign.VIENNA]
 * to be used to display posted speed limit and user's current speed.
 */
class SpeedInfoValue internal constructor(
    val postedSpeed: Int?,
    val currentSpeed: Int,
    val postedSpeedUnit: SpeedUnit,
    val speedSignConvention: SpeedLimitSign?,
)
