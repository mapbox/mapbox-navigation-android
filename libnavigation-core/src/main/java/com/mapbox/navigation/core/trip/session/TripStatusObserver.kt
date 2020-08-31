package com.mapbox.navigation.core.trip.session

import com.mapbox.navigation.navigator.internal.TripStatus

internal interface TripStatusObserver {
    fun onTripStatusChanged(status: TripStatus)
}
