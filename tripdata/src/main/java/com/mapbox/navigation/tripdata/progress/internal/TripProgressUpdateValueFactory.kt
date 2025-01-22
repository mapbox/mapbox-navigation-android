package com.mapbox.navigation.tripdata.progress.internal

import androidx.annotation.ColorInt
import androidx.annotation.RestrictTo
import com.mapbox.navigation.tripdata.progress.model.RouteLegTripOverview
import com.mapbox.navigation.tripdata.progress.model.TripOverviewError
import com.mapbox.navigation.tripdata.progress.model.TripOverviewValue
import com.mapbox.navigation.tripdata.progress.model.TripProgressUpdateFormatter
import com.mapbox.navigation.tripdata.progress.model.TripProgressUpdateValue
import java.util.TimeZone

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
object TripProgressUpdateValueFactory {

    @JvmStatic
    fun createTripProgressUpdateValue(
        estimatedTimeToArrival: Long,
        arrivalTimeZone: TimeZone?,
        distanceRemaining: Double,
        currentLegTimeRemaining: Double,
        totalTimeRemaining: Double,
        percentRouteTraveled: Double,
        @ColorInt trafficCongestionColor: Int,
        formatter: TripProgressUpdateFormatter,
    ) = TripProgressUpdateValue(
        estimatedTimeToArrival = estimatedTimeToArrival,
        arrivalTimeZone = arrivalTimeZone,
        distanceRemaining = distanceRemaining,
        currentLegTimeRemaining = currentLegTimeRemaining,
        totalTimeRemaining = totalTimeRemaining,
        percentRouteTraveled = percentRouteTraveled,
        trafficCongestionColor = trafficCongestionColor,
        formatter = formatter,
    )

    @JvmStatic
    fun createRouteLegTripOverview(
        legIndex: Int,
        legTime: Double,
        legDistance: Double,
        estimatedTimeToArrival: Long,
        arrivalTimeZone: TimeZone?,
    ) = RouteLegTripOverview(
        legIndex = legIndex,
        legTime = legTime,
        legDistance = legDistance,
        estimatedTimeToArrival = estimatedTimeToArrival,
        arrivalTimeZone = arrivalTimeZone,
    )

    @JvmStatic
    fun createTripOverviewValue(
        routeLegTripDetail: List<RouteLegTripOverview>,
        totalTime: Double,
        totalDistance: Double,
        totalEstimatedTimeToArrival: Long,
        arrivalTimeZone: TimeZone?,
        formatter: TripProgressUpdateFormatter,
    ) = TripOverviewValue(
        routeLegTripDetail = routeLegTripDetail,
        totalTime = totalTime,
        totalDistance = totalDistance,
        totalEstimatedTimeToArrival = totalEstimatedTimeToArrival,
        arrivalTimeZone = arrivalTimeZone,
        formatter = formatter,
    )

    @JvmStatic
    fun createTripOverviewError(
        errorMessage: String?,
        throwable: Throwable?,
    ) = TripOverviewError(
        errorMessage = errorMessage,
        throwable = throwable,
    )
}
