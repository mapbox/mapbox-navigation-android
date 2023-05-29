package com.mapbox.navigation.ui.speedlimit

import com.mapbox.navigation.base.formatter.UnitType
import com.mapbox.navigation.base.speed.model.SpeedUnit
import com.mapbox.navigation.ui.speedlimit.model.SpeedData

internal class SpeedLimitProcessor {

    fun process(action: SpeedLimitAction): SpeedLimitResult {
        return when (action) {
            is SpeedLimitAction.CalculateSpeedLimitUpdate -> {
                calculateSpeedLimitUpdate(action)
            }
            is SpeedLimitAction.FindPostedAndCurrentSpeed -> {
                getPostedAndCurrentSpeed(action)
            }
        }
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

    private fun getPostedAndCurrentSpeed(
        action: SpeedLimitAction.FindPostedAndCurrentSpeed
    ): SpeedLimitResult {
        val locationMatcher = action.locationMatcherResult
        val currentSpeedMetersPerSecond: Double = locationMatcher.enhancedLocation.speed.toDouble()
        val formattedCurrentSpeed = action.formatter.format(
            SpeedData(
                currentSpeedMetersPerSecond,
                SpeedUnit.METERS_PER_SECOND,
                action.distanceFormatterOptions.unitType
            )
        )

        val formattedPostedSpeed = locationMatcher.speedLimitInfo.speed?.let { speed ->
            action.formatter.format(
                SpeedData(
                    speed.toDouble(),
                    locationMatcher.speedLimitInfo.unit,
                    action.distanceFormatterOptions.unitType
                )
            )
        }

        val postedSpeedUnit = when (action.distanceFormatterOptions.unitType) {
            UnitType.METRIC -> SpeedUnit.KILOMETERS_PER_HOUR
            UnitType.IMPERIAL -> SpeedUnit.MILES_PER_HOUR
        }

        val speedSign = locationMatcher.speedLimitInfo.sign

        return SpeedLimitResult.PostedAndCurrentSpeed(
            formattedPostedSpeed,
            formattedCurrentSpeed,
            postedSpeedUnit,
            speedSign,
        )
    }
}
