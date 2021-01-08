package com.mapbox.navigation.core.trip.session

import com.mapbox.navigation.navigator.internal.MapboxNativeNavigatorImpl
import com.mapbox.navigation.utils.internal.JobControl
import com.mapbox.navigator.NavigationStatus
import com.mapbox.navigator.NavigationStatusOrigin
import com.mapbox.navigator.NavigatorObserver
import kotlinx.coroutines.launch

internal class NavigatorObserverImpl(
    private val tripStatusObserver: TripStatusObserver,
    private val jobController: JobControl
) : NavigatorObserver() {
    override fun onStatus(origin: NavigationStatusOrigin, status: NavigationStatus) {
        jobController.scope.launch {
            val tripStatus = MapboxNativeNavigatorImpl.generateTripStatusFrom(status)
            tripStatusObserver.onTripStatusChanged(tripStatus)
        }
    }
}
