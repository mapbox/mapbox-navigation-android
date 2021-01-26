package com.mapbox.navigation.ui.speedlimit

internal class SpeedLimitProcessor {

    fun process(action: SpeedLimitAction): SpeedLimitResult {
        return calculateSpeedLimitUpdate(action as SpeedLimitAction.CalculateSpeedLimitUpdate)
    }

    private fun calculateSpeedLimitUpdate(
        action: SpeedLimitAction.CalculateSpeedLimitUpdate
    ): SpeedLimitResult {
        return SpeedLimitResult.SpeedLimitCalculation(
            action.speedLimit.speedKmph ?: 0,
            action.speedLimit.speedLimitUnit,
            action.speedLimit.speedLimitSign
        )
    }
}
