package com.mapbox.navigation.ui.androidauto.navigation

import androidx.car.app.model.CarColor
import androidx.car.app.model.DateTimeWithZone
import androidx.car.app.navigation.model.TravelEstimate
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.tripdata.progress.api.MapboxTripProgressApi
import com.mapbox.navigation.tripdata.progress.model.TripProgressUpdateValue
import java.util.TimeZone
import java.util.concurrent.TimeUnit

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

    private fun remainingTimeSeconds(tripProgressUpdateValue: TripProgressUpdateValue): Long {
        val halfSecond = TimeUnit.MINUTES.toSeconds(1) / 2
        return tripProgressUpdateValue.currentLegTimeRemaining.toLong() + halfSecond
    }
}
