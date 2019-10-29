package com.mapbox.navigation.trip.service

import com.mapbox.annotation.navigation.module.MapboxNavigationModule
import com.mapbox.annotation.navigation.module.MapboxNavigationModuleType
import com.mapbox.navigation.base.trip.TripNotification
import com.mapbox.navigation.base.trip.TripService

@MapboxNavigationModule(MapboxNavigationModuleType.TripService, skipConfiguration = true)
class MapboxTripService(override val tripNotification: TripNotification) : TripService {

    override fun startService(stateListener: TripService.StateListener) {
        TODO("not implemented")
    }

    override fun stopService() {
        TODO("not implemented")
    }
}
