package com.mapbox.navigation.navigator

import android.location.Location
import com.mapbox.navigator.Navigator
import com.mapbox.navigation.base.route.model.Route
import java.util.Date

interface MapboxNativeNavigator {
    val navigator: Navigator

    fun updateLocation(rawLocation: Location)
    fun getStatus(date: Date): TripStatus
    fun setRoute(route: Route)
}
