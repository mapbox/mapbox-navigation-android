package com.mapbox.services.android.navigation.v5.snap

import android.location.Location
import com.mapbox.navigator.NavigationStatus
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress

class SnapToRoute : Snap() {

    override fun getSnappedLocation(location: Location, routeProgress: RouteProgress): Location {
        // No impl
        return location
    }

    fun getSnappedLocationWith(status: NavigationStatus, rawLocation: Location): Location {
        return buildSnappedLocation(status, rawLocation)
    }

    private fun buildSnappedLocation(status: NavigationStatus, rawLocation: Location): Location {
        val snappedLocation = Location(rawLocation)
        val fixLocation = status.location
        val coordinate = fixLocation.coordinate
        snappedLocation.latitude = coordinate.latitude()
        snappedLocation.longitude = coordinate.longitude()
        fixLocation.bearing?.let { snappedLocation.bearing = it }
        snappedLocation.time = fixLocation.time.time
        // TODO This is for testing / debugging purposes
        // route - active guidance
        // mpp - free drive
        // raw1 - mpp did not produce any result (failed with exception or something similar)
        // raw2 - mpp returned status, but location is not valid
        snappedLocation.provider = status.location.provider
        return snappedLocation
    }
}
