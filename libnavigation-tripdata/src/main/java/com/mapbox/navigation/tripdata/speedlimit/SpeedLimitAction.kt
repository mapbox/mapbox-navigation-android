package com.mapbox.navigation.tripdata.speedlimit

import com.mapbox.navigation.base.formatter.DistanceFormatterOptions
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.tripdata.speedlimit.model.SpeedData
import com.mapbox.navigation.ui.base.formatter.ValueFormatter

internal sealed class SpeedLimitAction {
    data class FindPostedAndCurrentSpeed(
        val formatter: ValueFormatter<SpeedData, Int>,
        val locationMatcherResult: LocationMatcherResult,
        val distanceFormatterOptions: DistanceFormatterOptions,
    ) : SpeedLimitAction()
}
