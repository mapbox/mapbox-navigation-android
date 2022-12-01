package com.mapbox.navigation.ui.speedlimit.api

import com.mapbox.navigation.base.formatter.DistanceFormatterOptions
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.ui.base.formatter.ValueFormatter
import com.mapbox.navigation.ui.speedlimit.SpeedLimitAction
import com.mapbox.navigation.ui.speedlimit.SpeedLimitProcessor
import com.mapbox.navigation.ui.speedlimit.SpeedLimitResult
import com.mapbox.navigation.ui.speedlimit.model.PostedAndCurrentSpeedFormatter
import com.mapbox.navigation.ui.speedlimit.model.SpeedData
import com.mapbox.navigation.ui.speedlimit.model.SpeedInfoValue

/**
 * Mapbox implementation of for deriving the posted speed limit and current speed at users current
 * location.
 *
 * @param processor an instance of a [SpeedLimitProcessor]
 */
class MapboxSpeedInfoApi internal constructor(private val processor: SpeedLimitProcessor) {

    /**
     * Mapbox implementation for deriving the posted and current speed at users current location.
     */
    constructor() : this(SpeedLimitProcessor())

    /**
     * Evaluates [LocationMatcherResult] data to calculate the posted and user's current speed.
     *
     * @param formatter a [ValueFormatter] instance
     * @param locationMatcherResult [LocationMatcherResult]
     * @param distanceFormatterOptions [DistanceFormatterOptions]
     *
     * @return an updated state for rendering in the view
     */
    fun updatePostedAndCurrentSpeed(
        locationMatcherResult: LocationMatcherResult,
        distanceFormatterOptions: DistanceFormatterOptions,
        formatter: ValueFormatter<SpeedData, Int> = PostedAndCurrentSpeedFormatter(),
    ): SpeedInfoValue {
        val action = SpeedLimitAction.FindPostedAndCurrentSpeed(
            formatter,
            locationMatcherResult,
            distanceFormatterOptions
        )
        val result = processor.process(action) as SpeedLimitResult.PostedAndCurrentSpeed
        return SpeedInfoValue(
            result.postedSpeed,
            result.currentSpeed,
            result.postedSpeedUnit,
            result.speedSignConvention
        )
    }
}
