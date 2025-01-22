package com.mapbox.navigation.tripdata.speedlimit

import com.mapbox.navigation.base.speed.model.SpeedLimitSign
import com.mapbox.navigation.base.speed.model.SpeedUnit

internal sealed class SpeedLimitResult {

    data class PostedAndCurrentSpeed(
        val postedSpeed: Int?,
        val currentSpeed: Int?,
        val postedSpeedUnit: SpeedUnit,
        val speedSignConvention: SpeedLimitSign,
    ) : SpeedLimitResult()
}
