package com.mapbox.navigation.navigator

import android.location.Location
import com.mapbox.navigation.base.route.model.Route
import com.mapbox.navigator.RouterResult
import java.util.Date

interface MapboxNativeNavigator {

    fun getRoute(url: String): RouterResult
    fun updateLocation(rawLocation: Location)
    fun getStatus(date: Date): TripStatus
    fun setRoute(route: Route)
}
