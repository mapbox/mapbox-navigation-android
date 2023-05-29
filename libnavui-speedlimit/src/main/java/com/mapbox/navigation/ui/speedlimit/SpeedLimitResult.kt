package com.mapbox.navigation.ui.speedlimit

import com.mapbox.navigation.base.speed.model.SpeedLimitSign
import com.mapbox.navigation.base.speed.model.SpeedLimitUnit
import com.mapbox.navigation.base.speed.model.SpeedUnit

internal sealed class SpeedLimitResult {
    data class SpeedLimitCalculation(
        val speedKPH: Int,
        val speedUnit: SpeedLimitUnit,
        val signFormat: SpeedLimitSign,
    ) : SpeedLimitResult()

    data class PostedAndCurrentSpeed(
        val postedSpeed: Int?,
        val currentSpeed: Int,
        val postedSpeedUnit: SpeedUnit,
        val speedSignConvention: SpeedLimitSign,
    ) : SpeedLimitResult()
}
