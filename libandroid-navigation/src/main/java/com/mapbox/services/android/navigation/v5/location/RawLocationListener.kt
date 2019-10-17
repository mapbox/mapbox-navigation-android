package com.mapbox.services.android.navigation.v5.location

import android.location.Location

/**
 * A listener for getting [Location] updates as they are
 * received directly from the [com.mapbox.android.core.location.LocationEngine]
 * running in [com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation].
 */
interface RawLocationListener {

    /**
     * Invoked as soon as a new [Location] has been received.
     *
     * @param rawLocation un-snapped update
     */
    fun onLocationUpdate(rawLocation: Location)
}
