package com.mapbox.services.android.navigation.v5.snap

import android.location.Location
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress

abstract class Snap {

    abstract fun getSnappedLocation(location: Location, routeProgress: RouteProgress): Location
}
