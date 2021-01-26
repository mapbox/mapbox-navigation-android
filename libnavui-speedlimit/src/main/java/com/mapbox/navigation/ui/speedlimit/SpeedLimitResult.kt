package com.mapbox.navigation.ui.speedlimit

import com.mapbox.navigation.base.speed.model.SpeedLimitSign
import com.mapbox.navigation.base.speed.model.SpeedLimitUnit

internal sealed class SpeedLimitResult {
    data class SpeedLimitCalculation(
        val speedKPH: Int,
        val speedUnit: SpeedLimitUnit,
        val signFormat: SpeedLimitSign,
    ) : SpeedLimitResult()
}
