package com.mapbox.navigation.core.trip.session

/**
 * Describes the [TripSession]'s state.
 */
enum class TripSessionState {
    /**
     * State when the session is active, running a foreground service and requesting and returning location updates.
     */
    STARTED,

    /**
     * State when the session is inactive.
     */
    STOPPED,
}
