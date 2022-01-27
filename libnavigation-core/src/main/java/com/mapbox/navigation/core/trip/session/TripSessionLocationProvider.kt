package com.mapbox.navigation.core.trip.session

import android.location.Location

internal interface TripSessionLocationProvider {

    fun getRawLocation(): Location?
    val zLevel: Int?
    val locationMatcherResult: LocationMatcherResult?

    fun registerLocationObserver(locationObserver: LocationObserver)
    fun unregisterLocationObserver(locationObserver: LocationObserver)
    fun unregisterAllLocationObservers()
}
