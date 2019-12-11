package com.mapbox.navigation.trip.service

import com.mapbox.annotation.navigation.module.MapboxNavigationModule
import com.mapbox.annotation.navigation.module.MapboxNavigationModuleType
import com.mapbox.navigation.base.trip.TripNotification
import com.mapbox.navigation.base.trip.TripService

@MapboxNavigationModule(MapboxNavigationModuleType.TripService, skipConfiguration = true)
class MapboxTripService : TripService {

    override fun startNavigation(tripNotification: TripNotification): Boolean {
        TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
    }

    override fun stopNavigation() {
        TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
    }
}
