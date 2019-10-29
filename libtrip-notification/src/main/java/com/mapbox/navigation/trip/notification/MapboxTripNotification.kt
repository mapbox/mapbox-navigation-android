package com.mapbox.navigation.trip.notification

import android.app.Notification
import android.content.Context
import android.location.Location
import com.mapbox.annotation.navigation.module.MapboxNavigationModule
import com.mapbox.annotation.navigation.module.MapboxNavigationModuleType
import com.mapbox.navigation.base.trip.RouteProgress
import com.mapbox.navigation.base.trip.TripNotification

@MapboxNavigationModule(MapboxNavigationModuleType.TripNotification, skipConfiguration = true)
class MapboxTripNotification : TripNotification {
    override fun getNotification(): Notification {
        TODO("not implemented")
    }

    override fun getNotificationId(): Int {
        TODO("not implemented")
    }

    override fun updateRouteProgress(routeProgress: RouteProgress?) {
        TODO("not implemented")
    }

    override fun updateLocation(rawLocation: Location, enhancedLocation: Location) {
        TODO("not implemented")
    }

    override fun onTripSessionStopped(context: Context) {
        TODO("not implemented")
    }
}
