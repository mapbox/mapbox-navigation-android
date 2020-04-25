package com.mapbox.navigation.core.replay.route

import android.location.Location

/**
 * Replay location listener for [ReplayRouteLocationEngine]
 */
internal interface ReplayLocationListener {

    /**
     * Called whenever replayed location has updated
     */
    fun onLocationReplay(location: Location)
}
