package com.mapbox.navigation.core.trip.session

/**
 * Callback that provides the various session states that can happen within a navigation session
 *
 * @see NavigationSessionStateV2
 */
fun interface NavigationSessionStateObserverV2 {
    /**
     * Called whenever the navigation session state has changed
     *
     * @param navigationSession [NavigationSessionStateV2]
     */
    fun onNavigationSessionStateChanged(navigationSession: NavigationSessionStateV2)
}
