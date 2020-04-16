package com.mapbox.navigation.core.trip.session

/**
 * Interface to provide opportunity to fetch [TripSession] state updates
 */
interface TripSessionStateObserver {
    /**
     * Called whenever [TripSession] state has changed like `Start/Stop session`
     */
    fun onSessionStateChanged(tripSessionState: TripSessionState)
}
