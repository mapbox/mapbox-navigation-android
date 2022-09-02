package com.mapbox.navigation.core.trip.session

import androidx.annotation.UiThread

/**
 * Interface to provide opportunity to fetch [TripSession] state updates
 */
@UiThread
fun interface TripSessionStateObserver {
    /**
     * Called whenever [TripSession] state has changed like `Start/Stop session`
     */
    fun onSessionStateChanged(tripSessionState: TripSessionState)
}
