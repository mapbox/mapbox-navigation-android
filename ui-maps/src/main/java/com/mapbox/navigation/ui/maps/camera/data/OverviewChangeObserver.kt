package com.mapbox.navigation.ui.maps.camera.data

/**
 * Observer of the route overview properties which can lead to frame jumping by a large amount, as opposed to
 * the gradual drift of regular viewport updates:
 *  - the overview mode changed,
 *  - the active leg changed while the overview frames the active leg only.
 *
 */
internal fun interface OverviewChangeObserver {

    /**
     * Called after when relevant overview property has changed.
     */
    fun onOverviewChange()
}
