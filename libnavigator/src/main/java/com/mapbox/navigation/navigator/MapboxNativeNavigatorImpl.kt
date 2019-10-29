package com.mapbox.navigation.navigator

import android.location.Location
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.route.model.Route
import com.mapbox.navigation.base.trip.RouteProgress
import com.mapbox.navigator.FixLocation
import com.mapbox.navigator.NavigationStatus
import com.mapbox.navigator.Navigator
import java.util.Date

object MapboxNativeNavigatorImpl : MapboxNativeNavigator {

    init {
        System.loadLibrary("navigator-android")
    }

    private val navigator: Navigator = Navigator()

    override fun updateLocation(rawLocation: Location) {
        navigator.updateLocation(rawLocation.toFixLocation())
    }

    override fun setRoute(route: Route) {
        TODO("not implemented")
    }

    override fun getStatus(date: Date): TripStatus {
        val status = navigator.getStatus(date)
        return TripStatus(
            status.location.toLocation(),
            status.getRouteProgress()
        )
    }

    private fun Location.toFixLocation() = FixLocation(
        Point.fromLngLat(this.longitude, this.latitude),
        Date(this.time),
        this.speed,
        this.bearing,
        this.altitude.toFloat(),
        this.accuracy,
        this.provider
    )

    private fun FixLocation.toLocation(): Location = Location(this.provider).also {
        it.latitude = this.coordinate.latitude()
        it.longitude = this.coordinate.longitude()
        it.time = this.time.time
        it.speed = this.speed ?: 0f
        it.bearing = this.bearing ?: 0f
        it.altitude = this.altitude?.toDouble() ?: 0.0
        it.accuracy = this.accuracyHorizontal ?: 0f
    }

    private fun NavigationStatus.getRouteProgress(): RouteProgress {
        return RouteProgress()
    }
}
