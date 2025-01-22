package com.mapbox.navigation.tripdata.progress.api

import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.tripdata.progress.TripProgressAction
import com.mapbox.navigation.tripdata.progress.TripProgressProcessor
import com.mapbox.navigation.tripdata.progress.TripProgressResult
import com.mapbox.navigation.tripdata.progress.model.RouteLegTripOverview
import com.mapbox.navigation.tripdata.progress.model.TripOverviewError
import com.mapbox.navigation.tripdata.progress.model.TripOverviewValue
import com.mapbox.navigation.tripdata.progress.model.TripProgressUpdateFormatter
import com.mapbox.navigation.tripdata.progress.model.TripProgressUpdateValue

/**
 * Used for calculating data needed for trip progress.
 *
 * @param formatter an instance of [TripProgressUpdateFormatter]
 * @param processor an instance of [TripProgressProcessor]
 */
class MapboxTripProgressApi internal constructor(
    @Deprecated("The property will be removed in the future")
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
    fun getTripProgress(routeProgress: RouteProgress): TripProgressUpdateValue {
        val action = TripProgressAction.CalculateTripProgress(routeProgress)
        val result = processor.process(action) as TripProgressResult.RouteProgressCalculation

        return TripProgressUpdateValue(
            result.estimatedTimeToArrival,
            result.arrivalTimeZone,
            result.distanceRemaining,
            result.currentLegTimeRemaining,
            result.totalTimeRemaining,
            result.percentRouteTraveled,
            -1,
            formatter,
        )
    }

    /**
     * Calculates trip details based on [NavigationRoute]. The return value contains `leg time`,
     * `leg duration` and `estimated time to arrival` for each [RouteLeg] in
     * [NavigationRoute.directionsRoute] in the form of [RouteLegTripOverview]. It also contains
     * `totalTime`, `totalDistance` and `totalEstimatedTimeToArrival` for the entire
     * [NavigationRoute.directionsRoute]
     *
     * The API would return [TripOverviewError] in case the [NavigationRoute] does not have [RouteLeg]
     * or it does have [RouteLeg] but [RouteLeg.duration] or [RouteLeg.distance] is `null`.
     *
     * @param route to be used to compute the trip details for.
     * @return an update to be rendered with `MapboxTripProgressView`
     */
    fun getTripDetails(route: NavigationRoute): Expected<TripOverviewError, TripOverviewValue> {
        val action = TripProgressAction.CalculateTripDetails(route)

        return when (val result = processor.process(action) as TripProgressResult.TripOverview) {
            is TripProgressResult.TripOverview.Success -> {
                ExpectedFactory.createValue(
                    TripOverviewValue(
                        routeLegTripDetail = result.routeLegTripDetail.map {
                            RouteLegTripOverview(
                                legIndex = it.legIndex,
                                legTime = it.legTime,
                                legDistance = it.legDistance,
                                estimatedTimeToArrival = it.estimatedTimeToArrival,
                                arrivalTimeZone = it.arrivalTimeZone,
                            )
                        },
                        totalTime = result.totalTime,
                        totalDistance = result.totalDistance,
                        totalEstimatedTimeToArrival = result.totalEstimatedTimeToArrival,
                        arrivalTimeZone = result.arrivalTimeZone,
                        formatter = formatter,
                    ),
                )
            }
            is TripProgressResult.TripOverview.Failure -> {
                ExpectedFactory.createError(
                    TripOverviewError(
                        errorMessage = result.errorMessage,
                        throwable = result.throwable,
                    ),
                )
            }
        }
    }
}
