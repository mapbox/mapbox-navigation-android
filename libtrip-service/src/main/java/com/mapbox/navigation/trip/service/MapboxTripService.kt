package com.mapbox.navigation.trip.service

import com.mapbox.navigation.base.trip.TripNotification
import com.mapbox.navigation.base.trip.TripService

class MapboxTripService(override val tripNotification: TripNotification) : TripService {
    override fun startService(stateListener: TripService.StateListener) {
        TODO("not implemented")
    }

    override fun stopService() {
        TODO("not implemented")
    }
}
