package com.mapbox.navigation.core.trip.session

import androidx.annotation.UiThread
import com.mapbox.navigation.core.MapboxNavigation

/**
 * This interface is for turn-by-turn route navigation, but not in free-drive mode.
 * This interface can be registered via a [MapboxNavigation] object.
 */
@UiThread
fun interface OffRouteObserver {
    /**
     * Called whenever the user is off route
     */
    fun onOffRouteStateChanged(offRoute: Boolean)
}
