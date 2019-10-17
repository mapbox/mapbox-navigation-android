package com.mapbox.navigation.navigator

import android.location.Location
import com.mapbox.navigation.base.route.Route
import java.util.Date

interface MapboxNativeNavigator {
    fun updateLocation(rawLocation: Location)
    fun getStatus(date: Date): TripStatus
    fun setRoute(route: Route)
}
