package com.mapbox.navigation.ui.androidauto.navigation

import androidx.car.app.model.CarColor
import androidx.car.app.model.DateTimeWithZone
import androidx.car.app.navigation.model.TravelEstimate
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.tripdata.progress.api.MapboxTripProgressApi
import com.mapbox.navigation.tripdata.progress.model.TripProgressUpdateValue
import java.util.TimeZone

class CarNavigationEtaMapper(private val tripProgressApi: MapboxTripProgressApi) {

    fun getDestinationTravelEstimate(routeProgress: RouteProgress): TravelEstimate {
        val result = tripProgressApi.getTripProgress(routeProgress)
        val distance = CarDistanceFormatter.carDistance(result.distanceRemaining)
        val zonedDateTime =
            DateTimeWithZone.create(result.estimatedTimeToArrival, TimeZone.getDefault())
        return TravelEstimate.Builder(distance, zonedDateTime)
            .setRemainingTimeSeconds(remainingTimeSeconds(result))
            .setRemainingTimeColor(CarColor.GREEN)
            .build()
    }

    // The destination estimate covers the whole route, like its distance and arrival time.
    private fun remainingTimeSeconds(tripProgressUpdateValue: TripProgressUpdateValue): Long {
        val secondsRemaining = tripProgressUpdateValue.totalTimeRemaining
        return if (secondsRemaining.isFinite() && secondsRemaining >= 0.0) {
            secondsRemaining.toLong()
        } else {
            TravelEstimate.REMAINING_TIME_UNKNOWN
        }
    }
}
