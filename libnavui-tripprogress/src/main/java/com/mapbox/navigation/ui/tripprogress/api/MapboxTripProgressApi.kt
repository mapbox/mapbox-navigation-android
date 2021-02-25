package com.mapbox.navigation.ui.tripprogress.api

import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.ui.base.model.Expected
import com.mapbox.navigation.ui.base.model.tripprogress.TripProgressUpdateFormatter
import com.mapbox.navigation.ui.tripprogress.TripProgressAction
import com.mapbox.navigation.ui.tripprogress.TripProgressProcessor
import com.mapbox.navigation.ui.tripprogress.TripProgressResult
import com.mapbox.navigation.ui.tripprogress.model.TripProgressUpdateError
import com.mapbox.navigation.ui.tripprogress.model.TripProgressUpdateValue

/**
 * Used for calculating data needed for trip progress.
 *
 * @param formatter an instance of [TripProgressUpdateFormatter]
 * @param processor an instance of [TripProgressProcessor]
 */
class MapboxTripProgressApi internal constructor(
    var formatter: TripProgressUpdateFormatter,
    private val processor: TripProgressProcessor,
) {

    /**
     * @param formatter contains various instances for use in formatting trip related data
     * for display in the UI
     *
     * @return a [MapboxTripProgressApi]
     */
    constructor(formatter: TripProgressUpdateFormatter) : this(formatter, TripProgressProcessor())

    /**
     * Calculates a trip progress update based on a [RouteProgress]
     *
     * @param routeProgress a [RouteProgress] object
     * @return an update to be rendered
     */
    fun getTripProgress(routeProgress: RouteProgress):
        Expected<TripProgressUpdateValue, TripProgressUpdateError> {
            val action = TripProgressAction.CalculateTripProgress(routeProgress)
            val result = processor.process(action) as TripProgressResult.RouteProgressCalculation

            return Expected.Success(
                TripProgressUpdateValue(
                    result.estimatedTimeToArrival,
                    result.distanceRemaining,
                    result.currentLegTimeRemaining,
                    result.totalTimeRemaining,
                    result.percentRouteTraveled,
                    -1,
                    formatter
                )
            )
        }
}
