package com.mapbox.navigation.ui.speedlimit

import com.mapbox.navigation.base.formatter.DistanceFormatterOptions
import com.mapbox.navigation.base.speed.model.SpeedLimit
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.ui.base.formatter.ValueFormatter
import com.mapbox.navigation.ui.speedlimit.model.SpeedData

internal sealed class SpeedLimitAction {
    data class CalculateSpeedLimitUpdate(val speedLimit: SpeedLimit) : SpeedLimitAction()
    data class FindPostedAndCurrentSpeed(
        val formatter: ValueFormatter<SpeedData, Int>,
        val locationMatcherResult: LocationMatcherResult,
        val distanceFormatterOptions: DistanceFormatterOptions,
    ) : SpeedLimitAction()
}
